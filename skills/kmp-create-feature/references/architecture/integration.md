# Integration Architecture Principles

Principles for integrating features into KMP apps. Every feature requires exactly 4 integration points; a 5th (bottom-bar tab) is **optional** and applies only to features that are top-level destinations.

**Note**: Uses placeholders that are resolved via Context Discovery:
- `{PKG_PREFIX}` - Package prefix (e.g., `com.example`, `com.myapp`)
- `{PROJECT_NAMESPACE}` - Root segment of the generated-resources package (derived from the app module, e.g. `kmp`)
- `{CORE_DESIGNSYSTEM_PKG}` - Package of the `:core:designsystem` module
- `{INIT_KOIN_PATH}` - Path to initKoin.kt file
- `{NAV_HOST_PATH}` - Path to navigation host file

## Integration Overview

**4 Required Integration Points**:
1. **Gradle Include** - Add module to project
2. **Gradle Dependency** - Link feature to app
3. **DI Initialization** - Register Koin modules
4. **Navigation Wiring** - Wire navigation callbacks

Missing any of these will result in build errors or runtime crashes.

**1 Optional Integration Point** (only when the feature is a bottom-bar destination):
5. **Bottom-Bar Tab** - Register the feature as a top-level tab (see "5. Bottom-Bar Tab (Optional)" below)

Most features are pushed screens (reached via a callback `navController.navigate(...)`), **not** tabs. Only add point 5 when the spec Navigation section marks the feature as a top-level destination.

## Critical Rules (Integration)

1. **Lowercase Packages**: `{PKG_PREFIX}.featurename` (never `feature-name`, `featureName`, or `feature_name`)
2. **DI Pattern**: top-level `val {featurename}Module = module { singleOf(::Impl).bind<Interface>() + viewModelOf(::ViewModel) }`; listed in `initKoin`'s `modules(...)`
3. **Navigation Callbacks**: Features receive callbacks, navigation logic stays in navigation host file

## 1. Gradle Include (Module Registration)

**Purpose**: Register feature module with Gradle build system

**File**: `settings.gradle.kts` (project root)

**Pattern**: Add `include(":feature:{featurename}")` at end of file

**Example**:
```kotlin
include(":feature:productdetail")
```

**Key Points**:
- Feature name matches directory name (lowercase, no hyphens)
- Add after existing feature includes
- Build will fail if this is missing

## 2. Gradle Dependency (App Link)

**Purpose**: Link feature module to main app

**File**: `composeApp/build.gradle.kts`

**Pattern**: Add `implementation(project(":feature:{featurename}"))` to dependencies block

**Example**:
```kotlin
sourceSets {
    commonMain {
        dependencies {
            // ... existing dependencies
            implementation(project(":feature:productdetail"))
        }
    }
}
```

**Key Points**:
- Goes in `commonMain` dependencies (KMP shared code)
- Feature name must match settings.gradle.kts
- Build will fail if this is missing or incorrect

## 3. DI Initialization (Koin Registration)

**Purpose**: Register feature's Koin modules with app's dependency injection system

**File**: `{INIT_KOIN_PATH}` (auto-detected via Context Discovery)

**Pattern**:
1. Import feature's `{featurename}Module` val
2. Add `{featurename}Module` to `startKoin { modules(...) }`

**Example** (using detected `{PKG_PREFIX}`):
```kotlin
import {PKG_PREFIX}.productdetail.di.productdetailModule

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(
            appModule,
            commonModule,
            dataModule,
            // ... existing feature modules
            productdetailModule,   // ← add the new feature module
        )
    }
```

**Key Points**:
- Add the `{featurename}Module` on its **own line** in the `modules(...)` call (one module per line — keeps the kit installer's strip exact).
- Import must use correct package name (lowercase).
- Order doesn't matter (features are independent).
- Runtime crash if missing (Koin will fail to resolve the ViewModel/Repository).

**Typical initKoin.kt structure**:
- `private val appModule = module { … }` (app-shell bindings)
- `fun initKoin(appDeclaration: KoinAppDeclaration = {})` that calls `startKoin { appDeclaration(); modules(appModule, commonModule, dataModule, …featureModules) }`
- No registry, no `initializeFeatures()` — every module is listed directly in `modules(...)`.

## 4. Navigation Wiring (Route Registration)

**Purpose**: Wire feature's navigation routes into app's navigation graph

**File**: `{NAV_HOST_PATH}` (auto-detected via Context Discovery)

**Pattern**:
1. Import feature's navigation extension function and route type
2. Call extension function inside `XNavHost` with callback parameters
3. Callbacks use `navController` to perform navigation

**Example** (using detected `{PKG_PREFIX}`):
```kotlin
import {PKG_PREFIX}.productdetail.presentation.navigation.ProductDetailRoute
import {PKG_PREFIX}.productdetail.presentation.navigation.productdetail

@Composable
fun BaseAppNavHost(navController: NavHostController) {
    XNavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        // ... other features

        productdetail(
            onBackClick = { navController.navigateUp() },
            onOrderSuccess = { navController.navigate(OrdersRoute) }
        )
    }
}
```

**Key Points**:
- Extension function name is lowercase feature name (e.g., `productdetail`)
- Route type is PascalCase (e.g., `ProductDetailRoute`)
- Callbacks handle all navigation (features don't have navController)
- Each callback uses navController to navigate or pop
- Navigation logic centralized in BaseAppNavHost.kt
- **First feature in a fresh project**: also perform the Welcome handoff — see "4a. First-feature (Welcome) Handoff" below (mandatory, not optional)

**Common callback patterns**:
- `onBackClick = { navController.navigateUp() }` - Go back
- `onNavigateTo{Feature} = { navController.navigate({Feature}Route) }` - Navigate to another screen
- `onNavigateTo{Feature} = { id -> navController.navigate({Feature}Route(id)) }` - Navigate with parameters

### 4a. First-feature (Welcome) Handoff — MANDATORY when wiring the FIRST feature

Fresh kit-managed projects (cloned via the kit installer) ship a placeholder `WelcomeScreen.kt` next to the nav host, and the nav host starts at `WelcomeRoute`. The **first** feature must replace it — the Welcome screen is a temporary shell, not a real destination. This handoff is **part of Integration Point 4 and is not optional**; if you skip it the app keeps launching to the Welcome screen and the new feature is unreachable as the start destination.

**Detect the first-feature condition** — check **both** markers:

1. `composeApp/src/commonMain/kotlin/**/WelcomeScreen.kt` exists (Glob)
2. `{NAV_HOST_PATH}` contains `startDestination = WelcomeRoute`

**If BOTH present → this is the first feature. Perform the handoff:**

```kotlin
// {NAV_HOST_PATH} — BEFORE (the kit installer shell)
XNavHost(modifier = modifier, navController = navController, startDestination = WelcomeRoute) {
    composable<WelcomeRoute> { WelcomeScreen() }
}

// {NAV_HOST_PATH} — AFTER (first feature wired in)
XNavHost(modifier = modifier, navController = navController, startDestination = {Feature}Route) {
    {featurename}(onBackClick = { navController.navigateUp() })
}
```

1. Change `startDestination = WelcomeRoute` → `startDestination = {Feature}Route`.
2. Remove the `composable<WelcomeRoute> { WelcomeScreen() }` line (the feature's `{featurename}(...)` extension replaces it).
3. Remove the now-dead `WelcomeRoute`/`WelcomeScreen` imports from `{NAV_HOST_PATH}`.
4. Delete the placeholder file: `rm -f <path matched by Glob>` (`WelcomeScreen.kt`). It is the only thing that references `WelcomeRoute`/`WelcomeScreen` — nothing else imports it.

**If EITHER marker is missing → not the first feature.** Wire navigation normally: add `{featurename}(...)` alongside existing routes and leave `startDestination` untouched (the user may have already customized it). Do **not** recreate or re-delete `WelcomeScreen.kt`.

**Verify after handoff** (the build won't catch a leftover Welcome — it still compiles):
```bash
# Both must return nothing once the first feature is wired:
find composeApp/src/commonMain/kotlin -name 'WelcomeScreen.kt' && echo "❌ WelcomeScreen.kt still present"
grep -rn "WelcomeRoute" composeApp/src/commonMain/kotlin && echo "❌ WelcomeRoute still referenced"
```

## 5. Bottom-Bar Tab (Optional)

**Purpose**: Register a feature as a **top-level destination** in the app's bottom navigation bar.

**When to apply**: ONLY if the feature's spec Navigation section marks it as a top-level tab. Skip for pushed screens (the common case). A bottom bar holds 3–5 tabs (Material guidance) — do not add more.

**First-tab scenario (sibling screens not yet implemented)**: When the first tab feature is being built but its sibling tabs (e.g. Orders, Profile) do not exist yet — wire Point 5 NOW. Add placeholder `TopLevelDestination` entries for future tabs (icon = a generic placeholder drawable, route = a `@Serializable data object PlaceholderRoute` in the app module, label = TBD string). Do NOT place the nav bar inside the feature's `XScreen.bottomBar` as a temporary measure. A feature-owned nav bar cannot navigate between features (it has no `navController`), making it decorative-only and a design violation. Placeholder entries in `TopLevelDestination` are explicitly supported and carry no runtime cost until their routes are registered.

**Where it lives**: the app shell, NOT the feature. The bottom bar is rendered in `App.kt` (`{NAV_HOST_PATH}`'s sibling) and the tab list is a single enum in the app module. This is consistent with point 4 — `{NAV_HOST_PATH}` already imports every feature's `Route`. The feature module stays independent (it never imports another feature); the app module composes the tabs.

### Design (official Compose Navigation best practices)

- **Tab list = one `enum class TopLevelDestination`** in the app module. Tabs are homogeneous (route + icon + label), so an enum's `entries` gives the ordered list for free. Each tab feature appends exactly one entry.
- **Selected tab is derived from the back stack** via `currentBackStackEntryAsState()` + `hierarchy` + `hasRoute` — never a parallel `selectedIndex` state (which breaks on back press / deep links).
- **Tab switching uses the multiple-back-stack pattern** (`popUpTo(start){ saveState } + launchSingleTop + restoreState`) so each tab keeps its own back stack.
- **The bar is hidden on non-top-level (pushed) screens**, computed from the current destination.
- **Rendering**: plain `XNavigationBar` + `XNavigationBarItem` (already in `:core:designsystem`). Adaptive (`NavigationSuiteScaffold` rail/drawer) is intentionally **out of scope** for mobile-first apps.

### Resource placement (forced by the `internal` Res rule)

The bar renders in the app module, and each feature's generated `Res` is `internal` per module (Rule 12) — so the app module **cannot** import a feature's `Res`. Therefore:

- **Tab label → app-module strings**: `composeApp/src/commonMain/composeResources/values/strings.xml` (create if absent). Key `tab_{featurename}`. Referenced via `{PROJECT_NAMESPACE}.composeapp.generated.resources.Res.string.tab_{featurename}`.
- **Tab icon → app-module drawables**: vector XML in `composeApp/src/commonMain/composeResources/drawable/`. Referenced via `{PROJECT_NAMESPACE}.composeapp.generated.resources.Res.drawable.{icon_name}`. Co-located with strings — both are app-specific chrome, neither belongs in `:core:designsystem` (which ships generic primitives to all downstream projects). **These tab icons are added by hand, so apply the drawable-XML rule yourself: no `@android:color/*` references — translate each to its literal ARGB hex (`white` → `#FFFFFFFF`, etc.) or the app crashes on iOS/desktop. See "Drawable XML authoring" in `shared/kmp-x-components-catalog.md`.**

### First tab vs. append

**Detect the shell**: does `App.kt` already contain `XNavigationBar`?

- **No → this is the first tab**: scaffold the shell (code A + B + C below). This mirrors the First-feature (Welcome) Handoff — a one-time app-shell mutation.
- **Yes → append**: add one `TopLevelDestination` entry (code A) and ensure the route is registered as a top-level `composable` in `{NAV_HOST_PATH}` (point 4). No structural change to `App.kt`.

### Code A — `TopLevelDestination.kt` (app module `navigation/` package)

```kotlin
package {PKG_PREFIX}.{PROJECT_NAMESPACE}.navigation

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import {PKG_PREFIX}.{featurename}.presentation.navigation.{Feature}Route
import {PROJECT_NAMESPACE}.composeapp.generated.resources.Res
import {PROJECT_NAMESPACE}.composeapp.generated.resources.ic_tab_{featurename}
import {PROJECT_NAMESPACE}.composeapp.generated.resources.tab_{featurename}

/** App bottom-bar destinations. Each tab feature appends ONE entry. route = the feature's @Serializable nav route. */
enum class TopLevelDestination(
    val route: Any,
    val icon: DrawableResource,
    val label: StringResource,
) {
    {FEATURE}({Feature}Route, Res.drawable.ic_tab_{featurename}, Res.string.tab_{featurename}),
    // append new tab features here
}
```

### Code B — `App.kt` `AppContent` (the single app-shell Scaffold, lift `navController`)

This is the **one** `Scaffold` in the app (Rule 13). It owns shared chrome (snackbar, bottom-bar tabs); `contentWindowInsets = WindowInsets(0, 0, 0, 0)` (consumes nothing). There is **no** `topBar` here: feature screens render their own `XTopAppBar` via `XScreen`. The NavHost is padded by the **top + horizontal** safe area + `imePadding()`; the **bottom** is owned per-screen (a bottom action bar bleeds to the edge and self-applies `windowInsetsPadding(WindowInsets.navigationBars.exclude(WindowInsets.ime))` so its nav-bar pad collapses when the keyboard lifts the screen; a no-bar scroll screen self-insets its content with `navigationBarsPadding()`). When a tab nav bar is shown (`onTopLevel`), `padding(innerPadding)` reserves its height and `XNavigationBar` self-insets the nav bar. Lift `navController` out of `{NAV_HOST_PATH}` into `App.kt` so the bar and the NavHost share it.

```kotlin
val navController = rememberNavController()
val entry by navController.currentBackStackEntryAsState()
val onTopLevel = TopLevelDestination.entries.any { dest ->
    entry?.destination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
}

Scaffold(
    snackbarHost = {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),   // consume nothing; apply insets explicitly (Rule 13)
    bottomBar = {
        if (onTopLevel) {
            // Keep the default windowInsets = NavigationBarDefaults.windowInsets so the bar's
            // containerColor fills behind the system nav-bar strip. Do NOT pass windowInsets =
            // WindowInsets(0) + a manual windowInsetsPadding(navigationBars…) on the modifier —
            // that clips the background short of the screen edge (mismatched strip color).
            //
            // Always set containerColor + XNavigationBarItem colors explicitly — bare M3 defaults
            // use secondaryContainer (indicator) and secondary (selected icon), which leak M3
            // baseline purple/grey if those roles are absent from XTheme.kt.
            XNavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,   // match page background
            ) {
                TopLevelDestination.entries.forEach { dest ->
                    val selected = entry?.destination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
                    XNavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        icon = {
                            XIcon(
                                painter = painterResource(dest.icon),
                                contentDescription = stringResource(dest.label),
                            )
                        },
                        label = { XText(stringResource(dest.label)) },
                    )
                }
            }
        }
    },
) { innerPadding ->
    BaseAppNavHost(
        navController = navController,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)   // reserves the tab nav-bar height when top-level
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .imePadding(),
    )
    Toast(state = toastState)
    SnackbarController(snackbarHostState = snackbarHostState)
}
```

Imports to add in `App.kt`: `androidx.navigation.compose.rememberNavController`, `androidx.navigation.compose.currentBackStackEntryAsState`, `androidx.navigation.NavDestination.Companion.hierarchy`, `androidx.navigation.NavDestination.Companion.hasRoute`, `androidx.navigation.NavGraph.Companion.findStartDestination`, `org.jetbrains.compose.resources.painterResource`, `org.jetbrains.compose.resources.stringResource`, and the `XNavigationBar`/`XNavigationBarItem`/`XIcon`/`XText` X-components. Also `androidx.compose.material3.NavigationBarItemDefaults` (for the explicit item colors), `androidx.compose.material3.MaterialTheme`, and the inset helpers `androidx.compose.foundation.layout.{WindowInsets, WindowInsetsSides, safeDrawing, only, windowInsetsPadding, imePadding}`. The shell uses M3 `Scaffold` + `SnackbarHost` directly (`androidx.compose.material3.*`) — this is the **one** place a real `Scaffold` is correct (Rule 13); do **not** add `topBar`/`ToolbarRenderer`.

### Code C — `{NAV_HOST_PATH}` (accept `navController` instead of creating it)

```kotlin
@Composable
fun BaseAppNavHost(navController: NavHostController, modifier: Modifier) {
    XNavHost(modifier = modifier, navController = navController, startDestination = {Start}Route) {
        // existing route registrations unchanged
    }
}
```

### Removing a tab

Delete the feature's `TopLevelDestination` entry. The route stays a valid destination (now reachable only programmatically). **There is no registry** — orphaned entries are not auto-removed; deleting a tab feature requires removing its enum entry, label string, and (if unused elsewhere) its chrome icon by hand. This is the accepted trade-off for keeping the wiring static and dependency-free.

## DI Pattern (feature/di/{Feature}Modules.kt)

**Purpose**: Define feature's dependency injection module

**Pattern**:
- One top-level `val {featurename}Module: Module` (idiomatic Koin — no base class, no registry, no `initialize()`)
- Uses `singleOf` + `bind` for interface/impl pairs
- Uses `viewModelOf` for ViewModels
- `includes(...)` to compose a `platformModule` (Rule 14) or sub-modules
- Listed in `initKoin`'s `modules(...)` (integration point 3)

**Structure**:
```kotlin
package {PKG_PREFIX}.{featurename}.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val {featurename}Module: Module =
    module {
        // DataSource (if exists)
        singleOf(::{Feature}RemoteDataSourceImpl).bind<{Feature}RemoteDataSource>()

        // Repository
        singleOf(::{Feature}RepositoryImpl).bind<{Feature}Repository>()

        // ViewModel
        viewModelOf(::{Feature}ViewModel)
    }
```

**Key Points**:
- Symbol name: `{featurename}Module` (camelCase, lowercase feature segment) — matches `dataModule`/`commonModule`
- No base class / no `FeatureRegistry` / no `initialize()` — registration is a single line in `initKoin`'s `modules(...)`
- `singleOf(::Impl).bind<Interface>()` creates singleton with interface binding
- `viewModelOf(::ViewModel)` registers ViewModel
- A `platformModule` (Rule 14) is pulled in via `includes(platformModule)` inside this module

## Navigation Pattern (feature/presentation/navigation/)

**Purpose**: Define feature routes and navigation integration point

**Pattern**:
- Define `@Serializable` route object(s)
- Create `NavGraphBuilder` extension function
- Use `composable<Route>` type-safe builder
- Accept callback parameters for navigation
- Inject ViewModel with `koinViewModel()`

**Structure**:
```kotlin
package {PKG_PREFIX}.{featurename}.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

// Route definition(s)
@Serializable
data class {Feature}Route(val param: Type)  // or data object for no params

// Navigation extension
fun NavGraphBuilder.{featurename}(
    onBackClick: () -> Unit,
    onNavigateTo{Destination}: (Type) -> Unit,
) {
    composable<{Feature}Route> { backStackEntry ->
        val route = backStackEntry.toRoute<{Feature}Route>()
        {Feature}Screen(
            param = route.param,  // Extract parameters
            onBackClick = onBackClick,
            onNavigateTo{Destination} = onNavigateTo{Destination},
            viewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
        )
    }
}
```

**Key Points**:
- Extension function name: lowercase feature name (e.g., `productdetail`, not `productDetail`)
- Route name: PascalCase with `Route` suffix (e.g., `ProductDetailRoute`)
- Route parameters: constructor properties (e.g., `val productId: Int`)
- No parameters: use `data object` instead of `data class`
- Extract params with `backStackEntry.toRoute<{Feature}Route>()`
- Pass callbacks through to Screen composable
- ViewModel scoped to backStackEntry for proper lifecycle

**Multiple screens in one feature**:
If feature has multiple screens, create multiple routes and multiple composable entries:
```kotlin
@Serializable
data class MainRoute(val id: Int)

@Serializable
data class DetailRoute(val id: Int, val subId: String)

fun NavGraphBuilder.feature(
    onBackClick: () -> Unit,
) {
    composable<MainRoute> { /* ... */ }
    composable<DetailRoute> { /* ... */ }
}
```

## Build Validation

**Incremental validation** (during development):
```bash
./gradlew :feature:{featurename}:assembleAndroidMain
```

**Full validation** (integration complete):
```bash
./gradlew assembleDebug
./gradlew ktlintFormat
```

**What full validation checks**:
- All required integration points (1–4) configured correctly (+ bottom-bar tab if the feature is a top-level tab)
- No compilation errors
- No DI configuration issues
- Code formatting with ktlint
- All dependencies resolved

## Common Integration Errors

**1. Missing Gradle Include**:
- Error: "Project ':feature:{featurename}' not found"
- Fix: Add `include(":feature:{featurename}")` to settings.gradle.kts

**2. Wrong Package Name**:
- Error: "Unresolved reference: {featurename}"
- Fix: Ensure lowercase package naming everywhere (import statements, package declarations)

**3. Missing DI Initialization**:
- Error: Runtime crash "No definition found for type {ViewModel/Repository}"
- Fix: Add `{featurename}Module` to `startKoin { modules(...) }` in initKoin.kt

**4. Missing Navigation Wiring**:
- Error: Navigation doesn't work, screen not found
- Fix: Add navigation extension call in BaseAppNavHost.kt

**5. Wrong Extension Function Name**:
- Error: "Unresolved reference" in BaseAppNavHost.kt
- Fix: Extension function must be lowercase (e.g., `productdetail`, not `productDetail`)
