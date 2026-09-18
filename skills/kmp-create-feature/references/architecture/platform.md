# Platform Capabilities & Native Views (Rule 14)

How a feature uses a **device capability** (GPS, camera, BLE, biometrics, sensors) or embeds a **native view** (map, camera preview, WebView) under Clean Architecture.

**Note**: Uses `{PKG_PREFIX}`, `{featurename}`, `{Feature}` placeholders (resolved via Context Discovery).

**Loaded on demand by**: `platform` (the capability/provider half) and `ui-layer` (the native-view interop half). The orchestrator loads it when a feature's **Platform Profile** is `platform-capability`, `native-view`, or `mixed`.

---

## The principle (Rule 14)

> A platform capability is **just another DataSource**. Hide every platform/native API behind a `commonMain` interface returning `Either<T>`; implement it per-platform. The ViewModel / Repository / `*UiModel` never see platform types — they cannot tell a GPS read from an HTTP GET. A native view embeds via an `expect @Composable` whose actuals use `AndroidView` / `UIKitView` / a desktop fallback.

This does **not** add a new architecture — it widens "DataSource" beyond `ApiClient`+Ktor and adds one UI-interop shape. **Rules 1–13 are unchanged**: interface+impl (Rule 1), `Either<DTO>` (Rule 2, 11), repository delegates (Rule 11 — data layer never imports `presentation`), ViewModel stores the DTO in `UiState<DTO>` (Rule 11).

---

## Library-selection decision tree (the ONE judgment call — gate with AskUserQuestion)

Given a needed capability, pick the cheapest row that works. Present the choice via `AskUserQuestion` (recommended-first) when more than one row is viable.

| # | Option | Use when | Cost |
|---|--------|----------|------|
| 1 | **Multiplatform library** (single `commonMain` dep) | a mature KMP lib already wraps the capability | lowest — no `expect/actual` |
| 2 | **expect/actual** in the feature module | no good KMP lib; each platform has its own SDK | medium — actuals per target |
| 3 | **iOS-Swift bridge** (`/kmp-bridge-swift`) | the iOS actual needs Swift (SDK not clean from Kotlin/Native) | highest — Swift + DI plumbing |

- Option 1 example — **MapLibre Compose**: `implementation(libs.maplibre.compose)` in `commonMain`, then `Map(state = rememberMapState())`.
  ⚠ **Not zero-config on iOS**: still needs the native MapLibre framework via SPM or CocoaPods + framework export (see "Gradle / source-set deltas").
- Option 3 is **only the iOS leg** of Option 2. Android almost never needs a bridge — Kotlin calls the Android SDK directly.
- **Permissions are their own capability** — a separate DataSource/provider, never buried inside the map/camera one.

Record the chosen option in the spec's **Platform Profile & Capabilities** section.

---

## Target reality — actuals for ALL targets (incl. desktop)

Feature targets are **android + iosArm64/iosSimulatorArm64 + jvm("desktop")** (see `build-gradle-template.md`). Every `expect` therefore needs actuals for **android / ios / desktop**, or the build breaks.

**Desktop is the easy one to forget.** Give it a graceful fallback:
- capability provider → returns `Either.Failure(ErrorModel...)` ("unavailable on this platform"),
- native view → a placeholder composable (e.g. `XText("Map unavailable on desktop")`).

`iosMain` is the intermediate source set shared by the three iOS targets (Kotlin default hierarchy) — one `actual` there covers all three.

---

## Pattern A — capability as a DataSource (platform writes this)

Interface in `commonMain`, one `actual` class per platform. The Repository delegates exactly as today.

```
feature/{featurename}/src/
├── commonMain/kotlin/{PKG_PREFIX}/{featurename}/data/datasource/
│   └── {Capability}DataSource.kt        # interface — suspend fun ...(): Either<DTO>
├── androidMain/kotlin/{PKG_PREFIX}/{featurename}/data/datasource/
│   └── {Capability}DataSourceAndroid.kt # actual class
├── iosMain/kotlin/{PKG_PREFIX}/{featurename}/data/datasource/
│   └── {Capability}DataSourceIos.kt     # actual class
└── desktopMain/kotlin/{PKG_PREFIX}/{featurename}/data/datasource/
    └── {Capability}DataSourceDesktop.kt # fallback
```

```kotlin
// commonMain — interface (Rule 1, Rule 2)
interface LocationDataSource {
    suspend fun current(): Either<LatLng>   // LatLng is a @Serializable DTO in data/model/
}

// androidMain — actual class (calls FusedLocationProviderClient)
class LocationDataSourceAndroid(
    private val context: Context,            // injected via Koin (androidContext())
) : LocationDataSource {
    override suspend fun current(): Either<LatLng> =
        try { Either.Success(/* fused location → LatLng */) }
        catch (e: Exception) { Either.Failure(ErrorModel.Exception(e)) }
}

// iosMain — actual class (CLLocationManager)
class LocationDataSourceIos : LocationDataSource {
    override suspend fun current(): Either<LatLng> = /* CoreLocation */
}

// desktopMain — fallback
class LocationDataSourceDesktop : LocationDataSource {
    override suspend fun current(): Either<LatLng> =
        Either.Failure(ErrorModel.Message("Location unavailable on desktop"))
}
```

**Naming**: the bridge skill calls this a "Provider" in the iOS context — same role, the Clean-Arch name here is **DataSource**. The Repository wraps it and returns `Either<DTO>` (Rule 11). The ViewModel uses the Repository, never the DataSource.

---

## Pattern B — DI via `expect/actual val platformModule` (platform writes; integrator wires)

Platform `actual` classes can't be bound in the `commonMain` Koin module (the impl types don't exist there). Use an `expect/actual val platformModule` (same mechanism the bridge skill uses in its step 5).

```
di/
├── commonMain/.../di/PlatformModule.kt          # internal expect val platformModule: Module
├── androidMain/.../di/PlatformModule.android.kt  # actual
├── iosMain/.../di/PlatformModule.ios.kt          # actual
└── desktopMain/.../di/PlatformModule.desktop.kt  # actual
```

```kotlin
// commonMain — internal: only {featurename}Module (which includes it) is public
internal expect val platformModule: Module

// androidMain
internal actual val platformModule = module {
    singleOf(::LocationDataSourceAndroid).bind<LocationDataSource>()
}
// iosMain → binds ::LocationDataSourceIos ; desktopMain → binds ::LocationDataSourceDesktop
```

`{featurename}Module` pulls in `platformModule` via `includes(...)` (integrator adds this line):

```kotlin
val {featurename}Module: Module =
    module {
        includes(platformModule)                          // expect/actual — platform DataSource binding
        singleOf(::{Feature}RepositoryImpl).bind<{Feature}Repository>()
        viewModelOf(::{Feature}ViewModel)
    }
```

---

## Pattern C — native view via `expect @Composable` (ui-layer writes this)

A native view is hosted inside `{Feature}Content` through an `expect @Composable`. This is **Shape C** in `ui.md`.

```
presentation/ui/components/
├── commonMain  → PlatformMap.kt          # expect @Composable fun PlatformMap(...)
├── androidMain → PlatformMap.android.kt  # AndroidView { ... }
├── iosMain     → PlatformMap.ios.kt      # UIKitView(factory = { ... })
├── desktopMain → PlatformMap.desktop.kt  # XText fallback
└── commonMain  → {Feature}Content.kt     # plain Compose — calls PlatformMap()
```

```kotlin
// commonMain
@Composable
expect fun PlatformMap(
    center: LatLng,
    onPick: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
)

// androidMain  (androidx.compose.ui.viewinterop.AndroidView)
@Composable
actual fun PlatformMap(center: LatLng, onPick: (LatLng) -> Unit, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> MapView(context).apply { /* configure, set listener → onPick */ } },
        update = { /* move camera to center */ },
    )
}

// iosMain  (androidx.compose.ui.interop.UIKitView)
@Composable
actual fun PlatformMap(center: LatLng, onPick: (LatLng) -> Unit, modifier: Modifier) {
    UIKitView(
        modifier = modifier,
        factory = { MKMapView() /* configure, delegate → onPick */ },
    )
}

// desktopMain — fallback
@Composable
actual fun PlatformMap(center: LatLng, onPick: (LatLng) -> Unit, modifier: Modifier) {
    XText(stringResource(Res.string.map_unavailable_desktop), modifier = modifier)
}
```

`PlatformMap` is the **only** composable that touches a native view. `{Feature}Content` and everything above it stay pure Compose — they receive `LatLng` (a DTO) and emit callbacks, never platform types. The `expect/actual` files are one-concept-per-file under `components/` and are exempt from the "commonMain only" reading of the file-layout rule (they have `.android/.ios/.desktop` siblings by design).

---

## Gradle / source-set deltas

See `build-gradle-template.md` → "Platform-specific dependencies" for the canonical blocks. Summary:

- **Multiplatform lib (Option 1)** → `commonMain.dependencies { implementation(libs.<lib>) }`.
- **Per-platform SDK (Option 2)** → add to the matching source set:
  ```kotlin
  sourceSets {
      androidMain.dependencies { implementation(libs.play.services.maps) /* etc. */ }
      // iosMain / desktopMain as needed
  }
  ```
- **iOS native framework** (e.g. MapLibre, MapKit-via-SPM) → `swiftPackageConfig { … }` or `cocoapods { pod(...) }` on the iOS targets, plus `binaries.framework { export(...) }`. Required even when the Kotlin API is a `commonMain` lib.

The feature still depends on `:core:common` + `:core:designsystem` (always) and `:core:data` only if it **also** does REST (a `mixed` profile).

---

## Permissions

Runtime permissions (location, camera, mic) are a **separate capability** — model them as their own DataSource/provider (`PermissionDataSource.requestLocation(): Either<Unit>`), or use a permissions KMP lib (Option 1). Do not request permissions inside the map/camera composable. The ViewModel orchestrates: request permission → on success, read capability → update `UiState`.

---

## iOS-Swift handoff (when to stop and route)

If the iOS `actual` needs a Swift class (the SDK isn't cleanly callable from Kotlin/Native — custom MapKit delegates, Apple Pay, biometrics with UI), **do not write Swift here**. Instead:

1. Write the `iosMain` Kotlin **interface** + a thin provider stub that delegates to it (the `bridge-swift` "Bridge"/"Provider" pattern).
2. Leave the Swift impl unwritten and **surface a follow-up**:

   > Run `/kmp-bridge-swift` for `{Feature}Bridge` to implement the Swift side, then continue.

Skills never call each other — the user invokes `/kmp-bridge-swift` as the next step. Full mechanics: [`kmp-bridge-swift/SKILL.md`](../../../kmp-bridge-swift/SKILL.md).

---

## Testing platform features

Platform `actual` classes are **not** unit-tested in `commonTest` (no MockEngine, no device). Test by **faking the DataSource interface** (Mokkery) at the Repository / ViewModel level — `test-repository` and `test-viewmodel` already do this. The `test-datasource` agent (MockEngine + Ktor) applies only to REST DataSources, not platform ones.

---

## Checklist (platform path)

- [ ] Capability behind a `commonMain` interface returning `Either<DTO>` (Rule 14, Rule 2)
- [ ] `actual` for **every** target: android, ios, **desktop** (fallback)
- [ ] DI via `expect/actual val platformModule`; `{featurename}Module` pulls it in with `includes(platformModule)`
- [ ] Native view isolated in an `expect @Composable`; `{Feature}Content` stays pure Compose (Shape C)
- [ ] ViewModel/Repository/`*UiModel` import **no** platform types (Rule 11 self-check)
- [ ] iOS Swift needed? → `iosMain` interface + stub + `/kmp-bridge-swift` follow-up emitted
- [ ] iOS native framework wired (SPM/CocoaPods + export) if a per-platform SDK is used
- [ ] Build: `./gradlew assembleDebug` (android+desktop) and `:composeApp:embedAndSignAppleFrameworkForXcode` (iOS)
