# Local Persistence & App-State

How **persisted local state** is structured in KMP, following Clean Architecture. This is the
counterpart to [data.md](data.md): that doc covers **remote** data (Ktor/`ApiClient`,
`Either<T>`); this one covers **on-device persistence** — theme, locale, auth token, flags, cached
structured data.

**Note**: Uses `{PKG_PREFIX}` placeholder for package prefix (resolved via Context Discovery).

## Where it lives — `:core:data`, not the feature

Remote data layers are **per-feature by default** (`feature/{name}/.../data/`) — but a remote
endpoint/wire model shared by **≥2 features** moves to the `data.app` tier ([data.md → "Shared
remote data"](data.md)). Persisted **app-state** is **core infrastructure**: it lives once in
`:core:data` and is shared app-wide. Theme, locale, and token are app-global, not owned by any
single feature — so they are written here by hand, not generated per feature.

A feature that genuinely owns its *own* persisted state follows the same pattern inside its own
module, but that is rare; default to remote-only feature data layers.

## Two co-equal backends — pick by data shape

There are **two** local backends. Neither is a fallback for the other — choose by the **shape** of
the data:

| Backend | Use for | Class |
|---------|---------|-------|
| **DataStore (key-value)** | Simple scalar/flag state: theme mode, locale tag, auth token, booleans | `PreferencesManager` (`data/local/pref/`) |
| **Room (relational)** | Structured / queryable / multi-row data: cached lists, entities, joins | `AppDatabase` (`data/local/db/`) |

DataStore is the common case and the only one wired today; `AppDatabase` is a **placeholder** for
when a concern needs a real database. Do not force relational data into KV, or scalar flags into a
DB.

## Package layout (`:core:data`)

```
{PKG_PREFIX}.data/                   # GENERIC tier — universal app-state, ships to every project
├── local/
│   ├── pref/PreferencesManager.kt   # KV backend — generic, infra (see "Rule-1 exception")
│   └── db/AppDatabase.kt            # Room backend — placeholder for structured data
├── datasource/local/{domain}/       # {X}LocalDataSource  (interface + impl, internal)
│   └── theme/  locale/  token/
├── repository/{domain}/             # {X}Repository       (impl internal; interface per visibility rule)
│   └── theme/  locale/  token/
├── di/LocalDataSourceModule.kt      # generic local DI: backends + theme/locale/token only
└── app/                             # APP tier — project (domain) state, stripped downstream
    ├── {domain}/                    # domain-first group (datasource+repo+helpers)
    ├── datasource/local/{domain}/   # …or layer-first
    ├── repository/{domain}/
    └── di/AppDataModule.kt          # app local DI — the single strip seam
```

**Packaging is layer-first** (`datasource/local/{domain}/`, `repository/{domain}/`) — mirrors the
remote layout. **Domain-first exception**: a concern that carries domain *helpers* fitting no single
layer may instead group everything under one `{domain}/` root (utils + datasource + repository
together). Use this only when the helpers force it; layer-first is the default.

### Generic vs app tier — where a new persisted concern goes

`:core:data` mirrors the `:core:designsystem` two-tier split (generic vs `app/`):

| Tier | Package | Holds | Downstream |
|------|---------|-------|------------|
| **Generic** | `{PKG_PREFIX}.data.*` | Universal app-state every app has: **theme, locale, token**, plus all backends/network infra | ships as-is |
| **App** | `{PKG_PREFIX}.data.app.*` | **All** project/domain data — persisted state **and** shared cross-feature remote (endpoints/wire models/datasource); see [data.md → "Shared remote data"](data.md) | **stripped** |

**Decision rule** for a new persisted concern: is it a universal app primitive (theme/locale/auth/
flags) → generic `data.*`. Is it this project's own domain (anything feature-named / not universal
to every app) → `data.app.*`. When unsure, it's app.

- **Boundary rule** (same as designsystem): generic `data.*` files must **never** import
  `data.app.*`. App may import generic. Enforce with a grep over generic files.
- **Strip seam**: `data.app` has exactly one DI module, `AppDataModule.appDataModule`, pulled into
  `dataModule` via `includes(appDataModule)`. That single `includes` line in `DataModules.kt` is the
  **one sanctioned generic → app reference** (the documented exception the boundary grep skips).
  Removing the app tier = drop that line + the `data.app` package. Backends (`PreferencesManager`)
  stay generic, so app datasources still resolve them across the included modules.

## The two pairs per concern

Each persisted concern is **one `LocalDataSource` pair + one `Repository` pair**, both delegating
down to a backend.

### 1. LocalDataSource (`datasource/local/{domain}/`)

**Purpose**: Read/write one concern's value from a backend. The local twin of `RemoteDataSource`.

**Pattern**:
- **Interface + Impl**, both `internal` (Rule 1 still holds for datasources).
- Impl injects a backend (`PreferencesManager` or `AppDatabase`), maps to/from storage keys/rows.
- Naming: `{Entity}LocalDataSource` / `{Entity}LocalDataSourceImpl`.

**No `Either` here.** A KV/DB read is non-fallible from the app's view — there is no network error
surface to model. So local datasources return the **plain value or `null`** (or a `Flow`), *not*
`Either<T>`. This is the deliberate contrast with remote (data.md), where `Either<T>` wraps network
failure. (Rule 2's `Either` is for fallible network/IO ops.)

```kotlin
// datasource/local/theme/ThemeLocalDataSource.kt
internal interface ThemeLocalDataSource {
    suspend fun getThemeMode(): AppThemeMode?      // null ⇒ nothing stored yet
    suspend fun setThemeMode(mode: AppThemeMode)
}

// datasource/local/theme/ThemeLocalDataSourceImpl.kt
internal class ThemeLocalDataSourceImpl(
    private val preferencesManager: PreferencesManager,
) : ThemeLocalDataSource {
    override suspend fun getThemeMode(): AppThemeMode? =
        preferencesManager.getString(KEY).first()?.let { name ->
            AppThemeMode.entries.firstOrNull { it.name == name }
        }
    override suspend fun setThemeMode(mode: AppThemeMode) =
        preferencesManager.putString(KEY, mode.name)

    private companion object { const val KEY = "theme_mode" }
}
```

### 2. Repository (`repository/{domain}/`)

**Purpose**: The public API for the concern. The local twin of the remote `Repository`.

**App-state shape** (theme, locale): seed from the datasource on init, expose an in-memory
`StateFlow` as the **synchronous read source** for callers, and **write through** on every set
(update the flow, persist async). Callers (app root, settings screen) never touch the datasource.

```kotlin
// repository/theme/ThemeRepository.kt  — interface
interface ThemeRepository {
    val themeMode: StateFlow<AppThemeMode>
    fun setThemeMode(mode: AppThemeMode)
}

// repository/theme/ThemeRepositoryImpl.kt  — impl (internal)
internal class ThemeRepositoryImpl(
    private val localDataSource: ThemeLocalDataSource,
) : ThemeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    override val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    init { scope.launch { localDataSource.getThemeMode()?.let { _themeMode.value = it } } }

    override fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode                              // synchronous read updates immediately
        scope.launch { localDataSource.setThemeMode(mode) }  // persist write-through
    }
}
```

The concern's model/enum (e.g. `AppThemeMode`) lives in the **repository** package — the datasource
imports it upward. Local repos never import from `presentation` (Rule 11 still holds).

## Naming — `*Repository`, never `*Manager`

Persisted-state types are **`{Entity}Repository`**, not `{Entity}Manager`. "Manager" is reserved for
the one KV-backend infra class, `PreferencesManager`. A `ThemeManager`/`LocaleManager` is a naming
violation — use `ThemeRepository`/`LanguageRepository`.

## Visibility

| Type | Visibility |
|------|-----------|
| `{X}LocalDataSource` interface + impl | **`internal`** — always |
| `{X}RepositoryImpl` | **`internal`** — always |
| `{X}Repository` **interface** | **public** if a feature/app consumes it (theme, locale) **OR** kept as deliberate API headroom (token); otherwise `internal` |

`TokenRepository` is the documented **headroom** case: public even though only `:core:data` uses it
today, because the auth feature will consume it. Don't make a repo public "just in case" beyond a
named, intended consumer.

## `PreferencesManager` — Rule-1 exception (infra, not a DataSource)

`PreferencesManager` is a **plain class with no interface** — a deliberate exception to Rule 1
(interface + impl). It is not a per-concern DataSource; it is the **single shared KV backend** that
every `*LocalDataSource` delegates to. Giving it an interface would add a seam with no second
implementation. Do not "fix" it by adding an interface.

```kotlin
// data/local/pref/PreferencesManager.kt
class PreferencesManager(private val dataStorePreference: DataStore<Preferences>) {
    suspend fun putString(key: String, value: String) { /* edit */ }
    fun getString(key: String): Flow<String?> = /* data.map { it[key] } */
    suspend fun clear() { /* edit { clear() } */ }
}
```

## DI — generic `localDataSourceModule` (+ app `appDataModule`)

Generic local persistence DI lives in `di/LocalDataSourceModule.kt`: the backend(s), then every
**generic** datasource/repository (theme/locale/token). **App-tier** concerns are bound separately
in `app/di/AppDataModule.kt` (`internal val appDataModule`); both are pulled into the `dataModule`
aggregate via `includes(...)` (see "Generic vs app tier" above). Backends bound here are shared
across both modules, so app datasources resolve `PreferencesManager` without re-binding it.

```kotlin
internal val localDataSourceModule = module {
    single { PreferencesManager(get()) }
    single<DataStore<Preferences>> { /* PreferenceDataStoreFactory.createWithPath(...) */ }

    // Local data sources (internal)
    singleOf(::ThemeLocalDataSourceImpl).bind<ThemeLocalDataSource>()
    singleOf(::LanguageLocalDataSourceImpl).bind<LanguageLocalDataSource>()
    singleOf(::TokenLocalDataSourceImpl).bind<TokenLocalDataSource>()

    // Repositories
    singleOf(::ThemeRepositoryImpl).bind<ThemeRepository>()
    singleOf(::LanguageRepositoryImpl).bind<LanguageRepository>()
    singleOf(::TokenRepositoryImpl).bind<TokenRepository>()
}
```

The **DataStore file path is platform-specific** → provided via an `internal expect val
platformLocalDataSourceModule` with one `actual` per target (android/ios/desktop), composed into the
data module's aggregate. (Same `internal expect/actual val platformModule` mechanism as Rule 14.)

## Module roles (where app-global state is consumed)

Persisted state in `:core:data` is read at the edges by other modules — see
[kmp-patterns.md → Module Dependencies](../../../../shared/kmp-patterns.md) for the full role map. In short:

- **`:core:common`** — primitives only (Either, UiState, UiText, ext, util). **No** state holders or
  platform stores.
- **`:core:designsystem`** — app-global UI runtime context: `XTheme(darkTheme: Boolean)` and the
  `LocalAppLocale` CompositionLocal (+ its android/ios/desktop actuals).
- **`composeApp`** — app-shell glue: collects `ThemeRepository.themeMode` → maps to `darkTheme`;
  `ProvideAppLocale` feeds `LanguageRepository`'s tag into `LocalAppLocale`.
- **`:core:data`** — owns all persisted state (the repositories above).

## Canonical examples

`theme`, `locale`, and `token` are the three reference concerns — app-global, hand-authored core
infra, in the **generic** tier (`data.*`).

App-specific persisted concerns follow the identical pattern but live in the **app** tier
(`data.app.*`) and are stripped downstream — e.g. a simple `{domain}/` concern grouped layer-first,
or a domain-first `{domain}/` group that carries its own helpers (utils + datasource + repository
together). See "Generic vs app tier" above for the placement rule.
