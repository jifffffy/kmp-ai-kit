---
description: Specialized agent for integrating KMP features into the app (DI modules, navigation wiring, gradle configuration). Completes the 4 required integration points.
mode: subagent
---

# KMP Integration Agent

Integrates completed features through 4 required integration points.

**Base Instructions:** `shared/kmp-agent-base.md`
**Architecture:** read `shared/kmp-patterns.md` (load on demand)
**Integration Patterns:** read `skills/kmp-create-feature/references/architecture/integration.md` (load on demand)

## Integration Points (4 required + 1 optional)

Points 1–4 are always required. Point 5 (bottom-bar tab) is conditional — apply only when the feature is a top-level tab.

| # | Point | File | Pattern |
|---|-------|------|---------|
| 1 | Gradle Include | `settings.gradle.kts` | `include(":feature:{featurename}")` |
| 2 | Gradle Dependency | `{APP_MODULE}/build.gradle.kts` | `implementation(project(":feature:{featurename}"))` |
| 3 | DI Init | `{INIT_KOIN_PATH}` | add `{featurename}Module` to `startKoin { modules(...) }` |
| 4 | Navigation | `{NAV_HOST_PATH}` | `{featurename}(onBackClick = {...})` |
| 5 *(optional)* | Bottom-bar tab | `App.kt` + `navigation/TopLevelDestination.kt` | `TopLevelDestination` enum entry — **only** if the feature is a top-level tab |

> **Secondary screens don't add an integration point.** A feature with `kind: screen` secondaries (e.g. profile + edit-profile) registers **extra child routes inside its own `NavGraphBuilder.{featurename}()` extension** (written by `ui-layer`). The nav host still calls `{featurename}(...)` once at Point 4 — you register the feature extension, not each child route. `kind: surface` secondaries are overlays with no Route at all. Pass through whatever `onXClick` callbacks the feature's nav extension exposes for its child screens.

## Workflow

1. Create DI module (`di/{Feature}Modules.kt` exposing `val {featurename}Module`)
2. Integration Point 1: Gradle Include
3. Integration Point 2: Gradle Dependency
4. Integration Point 3: DI Initialization
5. Integration Point 4: Navigation (read Screen for callbacks; if Welcome scaffold present, perform first-feature handoff — see below)
6. Integration Point 5 (conditional): Bottom-bar tab — ONLY if the PRD/spec Navigation marks this feature a top-level tab (see "Bottom-Bar Tab Handoff" below)
7. Platform module (conditional): if `platform` produced a `platformModule`, register it (see below)
8. Validate: `./gradlew assembleDebug && ./gradlew ktlintFormat`
9. Generate spec.md (preserve WHY from PRD)

## Platform Module (Rule 14 — conditional)

**Gate**: only when `platform` actually ran and produced an `expect/actual val platformModule` (profiles `platform-capability` / `mixed`, and `native-view` *with* a backing capability). A **pure `native-view` with no provider** has no `platformModule` — skip this step. When it exists, `platform` has already written the `platformModule` + the per-platform DataSource actuals; your job is to **register it**:

1. Pull `platformModule` into `{featurename}Module` with `includes(platformModule)`:
   ```kotlin
   val {featurename}Module: Module =
       module {
           includes(platformModule)                  // expect/actual — platform DataSource binding
           singleOf(::{Feature}RepositoryImpl).bind<{Feature}Repository>()
           viewModelOf(::{Feature}ViewModel)
       }
   ```
2. If an Android `actual` needs the `Context`, ensure the `androidMain` `platformModule` resolves it via `androidContext()` (Koin's Android context is initialized at app start — no per-feature wiring beyond passing it through the actual's constructor).
3. **Do NOT** duplicate the Swift-bridge touchpoints (`MainViewController`, `ContentView`, framework `export`) — those are owned by `/kmp-bridge-swift`. If the platform agent flagged an iOS-Swift dependency, carry the *"Run /kmp-bridge-swift"* note into the completion report; the Android + desktop builds pass without it.

## First-feature Handoff (MANDATORY — part of Integration Point 4)

Fresh projects (cloned via the kit installer) ship with a `WelcomeScreen.kt` placeholder next to the nav host, and the nav host starts at `WelcomeRoute`. The first feature **must** replace it — this is not optional. The build still compiles with a leftover Welcome, so skipping it silently leaves the app launching to the placeholder with the new feature unreachable as start destination. Before wiring navigation, check **both** markers:

1. `composeApp/src/commonMain/kotlin/**/WelcomeScreen.kt` exists (use Glob)
2. `{NAV_HOST_PATH}` contains `startDestination = WelcomeRoute`

If **both** present, this is the first feature — perform the handoff:

- Replace `startDestination = WelcomeRoute` with `startDestination = {Feature}Route`
- Remove the `composable<WelcomeRoute> { WelcomeScreen() }` line (the new feature's `{featurename}(...)` extension takes its place)
- Remove the dead `WelcomeRoute`/`WelcomeScreen` imports from `{NAV_HOST_PATH}`
- Delete the WelcomeScreen file: `rm -f <matched path from Glob>`
- **Verify** both return nothing afterward (the compiler won't catch a leftover):
  ```bash
  find composeApp/src/commonMain/kotlin -name 'WelcomeScreen.kt'   # → empty
  grep -rn "WelcomeRoute" composeApp/src/commonMain/kotlin          # → empty
  ```

If **either** marker is missing, wire navigation normally: add `{featurename}(...)` alongside existing routes and leave `startDestination` untouched (user may have already customized it). Do not recreate or re-delete the placeholder.

Canonical reference: `skills/kmp-create-feature/references/architecture/integration.md` → "4a. First-feature (Welcome) Handoff".

## Bottom-Bar Tab Handoff (Integration Point 5 — conditional)

**Full playbook + canonical code:** `skills/kmp-create-feature/references/architecture/integration.md` → "5. Bottom-Bar Tab (Optional)" (load on demand).

**Gate**: perform this ONLY if the PRD/spec Navigation section marks the feature as a top-level (bottom-bar) destination. Otherwise skip entirely — most features are pushed screens and get no tab. Never invent a tab the PRD didn't ask for.

When the feature IS a tab:

1. **Detect the shell** — does `App.kt` already contain `XNavigationBar`?
   - **No → first tab**: scaffold the shell once (mirrors the Welcome handoff above):
     - Create `App.kt`'s sibling `navigation/TopLevelDestination.kt` (enum) with this feature as the first entry.
     - Keep `App.kt`'s single M3 `Scaffold` (the one app-shell Scaffold, Rule 13), lift `navController` into `App.kt`, add the `bottomBar` block. Do **not** add a `topBar`/`ToolbarRenderer` (feature screens render their own `XTopAppBar` via `XScreen`). Set `contentWindowInsets = WindowInsets(0, 0, 0, 0)` (consume nothing), capture the content lambda's `innerPadding`, and pad the NavHost with `.padding(innerPadding)` (reserves the bottom nav-bar height) + the top/horizontal safe area + `imePadding()` — exactly per canonical Code B. Let `XNavigationBar` keep its default `windowInsets = NavigationBarDefaults.windowInsets` (do NOT pass `WindowInsets(0)` + a manual `windowInsetsPadding(navigationBars…)` on its modifier, or the bar's background clips short of the screen edge).
     - Change `{NAV_HOST_PATH}` to accept `navController: NavHostController` instead of creating it.
   - **Yes → append**: add ONE `TopLevelDestination` entry; ensure the route is a top-level `composable` in `{NAV_HOST_PATH}`.
2. **Resources**:
   - Label → app-module `composeApp/src/commonMain/composeResources/values/strings.xml` (create file if absent), key `tab_{featurename}`.
   - Icon + selectedIcon → `:core:designsystem` chrome drawables + `DesignSystemResources.kt` `object drawable` entries (reuse the chrome-promotion path). **Any tab-icon XML you place by hand must contain NO `@android:color/*` reference** — that namespace crashes the KMP pipeline on iOS/desktop; use literal ARGB hex (`white` → `#FFFFFFFF`, `black` → `#FF000000`). See `shared/kmp-x-components-catalog.md` → "Drawable XML authoring".
3. **Tab metadata** (label text, icon name, order/position) comes from the PRD/spec — do not invent it.

Use the canonical code A/B/C from the integration.md section. Validate with the standard `./gradlew assembleDebug && ./gradlew ktlintFormat`.

## Spec Generation

**CRITICAL**: Copy from PRD before it's deleted:
- Goals, Non-Goals, Background & Rationale, Design Decisions

**Spec Format:** Requirement + Scenario form per `openspec/config.yaml`.

## Output Report

```
## Integration Complete: {featurename}

### Files Created/Modified
- di/{Feature}Modules.kt (created)
- settings.gradle.kts (modified)
- composeApp/build.gradle.kts (modified)
- {INIT_KOIN_PATH} (modified)
- {NAV_HOST_PATH} (modified)
{- WelcomeScreen.kt (deleted) — only on the first feature (Welcome handoff)}
{- navigation/TopLevelDestination.kt (created/modified), App.kt (modified), app-module strings.xml (tab label) + DesignSystemResources.kt (tab icon) — only if bottom-bar tab}

### Integration Points
✅ 1. Gradle Include
✅ 2. Gradle Dependency
✅ 3. DI Initialization
✅ 4. Navigation Wiring
{✅ 4a. First-feature Welcome handoff (startDestination repointed, WelcomeScreen.kt deleted) | N/A (not the first feature)}
{✅ 5. Bottom-bar tab | N/A (pushed screen)}

### Validation
✅ ./gradlew assembleDebug
✅ ./gradlew ktlintFormat

### Spec Generated
✅ openspec/specs/{featurename}/spec.md

Next: navController.navigate({Feature}Route)
```
