# Phase 4: Orchestrated Implementation

**Purpose**: Invoke specialized agents to implement the feature layers.

**Prerequisites**: the OpenSpec spec and tasks confirmed by user.

---

## Checklist

```
Implementation Progress:
- [ ] Step 4.0: Read Platform Profile → select agent set
- [ ] Step 4.1: Choose execution strategy (Sequential or Parallel)
- [ ] Step 4.2: Invoke specialized agents
      ↳ (design-aware only) passthrough: XTheme · typography · motion · component constraints
- [ ] Step 4.3: Verify the living spec
- [ ] Step 4.4: Platform follow-ups (iOS-Swift bridge route, if flagged)
```

---

## Step 4.0: Agent Set by Platform Profile (Rule 14)

Read the **Platform Profile & Capabilities** section from the OpenSpec spec (`openspec/changes/{change-id}/`; the living spec is `openspec/specs/{capability}/spec.md`). The tag selects which agents run:

| Tag | data-layer | **platform** | ui-layer | integrator |
|-----|:---:|:---:|:---:|:---:|
| `network` | ✅ REST | — | ✅ | ✅ |
| `platform-capability` | — | ✅ provider | ✅ | ✅ (+`platformModule`) |
| `native-view` | — | ✅ if a capability backs the view¹ | ✅ (+ `expect/actual` composable) | ✅ (+`platformModule` **only if** ¹ a provider exists) |
| `mixed` | ✅ REST | ✅ provider | ✅ | ✅ (+`platformModule`) |

¹ A pure display view with no device data (e.g. a static WebView) may need no provider — then `kmp-platform` is skipped and `kmp-ui-layer` writes only the `expect/actual` composable.

**Division of labor on the platform path** (no overlap):
- **`kmp-platform`** (provider-only): `commonMain` DataSource interface + per-platform `actual` classes (android/ios/**desktop**) + `expect/actual val platformModule`. Writes **no** `@Composable`.
- **`kmp-ui-layer`**: the `expect @Composable PlatformX` + `AndroidView`/`UIKitView`/desktop-fallback actuals under `components/`, plus the normal ViewModel/UiModel/Screen. Loads `skills/kmp-create-feature/references/architecture/platform.md` → "Pattern C".
- **`kmp-integrator`**: pulls `platformModule` into `{featurename}Module` via `includes(platformModule)`, lists `{featurename}Module` in `initKoin`'s `modules(...)`, and wires any `androidContext()` the Android actual needs.

**Module-scaffold owner (CRITICAL — build breaks if no one does it)**: `feature/{featurename}/build.gradle.kts` + the module dir structure are normally created by `kmp-data-layer`. When `kmp-data-layer` is **skipped** (pure `platform-capability` / `native-view`), the **first agent in the set** scaffolds the module instead, from `skills/kmp-create-feature/references/architecture/build-gradle-template.md`:

| Profile | Module-scaffold owner |
|---------|------------------------|
| `network`, `mixed` | `kmp-data-layer` (unchanged) |
| `platform-capability`, `native-view` **with** a capability | `kmp-platform` |
| `native-view` **without** a capability (no provider) | `kmp-ui-layer` |

Tell that agent explicitly: *"You are the module-scaffold owner — create `build.gradle.kts` from the template first."* In **parallel** mode the scaffold owner must finish (or the orchestrator must pre-create `build.gradle.kts`) before the other agent edits it, to avoid a write race.

Pass the **tag + chosen sourcing option + module-scaffold-owner flag** to every agent you invoke below.

---

## Step 4.1: Execution Strategy

Ask user for preference:

| Strategy | Flow | When to Use |
|----------|------|-------------|
| **Sequential** | Data → UI → Integration | Safer, traditional |
| **Parallel** (Recommended) | Data + UI simultaneously → Integration | Faster |

---

## Step 4.2: Invoke Specialized Agents

### Design-Aware Passthrough (Penpot handoff)

If in **design-aware mode** (a `DESIGN.md` is present for the capability):

1. **Before the UI agent**: consume the Penpot handoff artifact — the `DESIGN.md` produced by the `penpot-design-md` skill, plus the annotation layer produced by `penpot-document-handoff`. From it extract the XTheme roles the screen needs, the **Typography Updates Required** (font swap + type-scale role overrides), the component references each node binds to, and the `## Motion` table (when present).

2. **XTheme update**: Add all missing M3 roles from the handoff to **both** `XLightColors` and `XDarkColors` in `XTheme.kt`. Verify build: `./gradlew :core:designsystem:assembleAndroidMain`

3. **Typography update**: Typography is app-global — these edits land in `:core:designsystem`, like the XTheme color step.
   - **Font swap** (only if the handoff specifies one): wire the typeface into `core/designsystem/.../composeResources/font/` and replace `XFontFamily()`'s body in `XTheme.kt` with the resulting `FontFamily(...)` (add `import androidx.compose.ui.text.font.FontVariation` when the route is variable).
   - **Type-scale role overrides** (only if the handoff lists any): these are applied per-node in the **feature** by the UI agent as `style = MaterialTheme.typography.{role}.copy(...)` — pass the override table to the UI agent. No theme edit.
   - When the handoff specifies neither, the design font matches the theme and all nodes use stock roles → **skip this step entirely**.
   - Verify build after a font swap: `./gradlew :core:designsystem:assembleAndroidMain`
4. **Motion files** (only if the handoff has a `## Motion` table; skip entirely for a static design). Motion needs **no** asset download — it is pure Compose code in dedicated `motion/` files (see `shared/kmp-motion.md`). Two parts:
   - **The generic DS motion primitives already ship in the template** at `core/designsystem/.../motion/` (`XMotion.kt` + `expect/actual rememberReducedMotion()`, `Modifier.shimmer()`, `PulseDot`, `AmbientMeshBackground`, `BokehCanvas`, `Modifier.pulseGlow()`, `RevealOnAppear`). **Verify they're present** (`ls core/designsystem/src/commonMain/kotlin/**/designsystem/motion/`); for any `## Motion` row targeting "DS `motion/`" whose primitive is **missing** from the shipped set (rare), add it there using the canonical names/signatures from `shared/kmp-motion.md` — if it needs `rememberReducedMotion()` it's already the `expect/actual`; a new platform-dependent primitive needs `.android`/`.ios`/`.desktop` actuals or the build breaks. Build android **and** desktop only when you added a new primitive: `./gradlew :core:designsystem:assembleAndroidMain :core:designsystem:desktopMainClasses`. **Do not recreate primitives that already exist.**
   - **Feature-specific motion** (rows targeting `feature motion/`) is written by the UI agent into `feature/{featurename}/.../presentation/ui/motion/{Feature}Motion.kt` — pass the `## Motion` table (incl. the **Magnitude** column) to it in step 6. The agent **reuses** the shipped DS primitives, passing each row's magnitude as a **parameter** (e.g. `PulseDot(scaleTo = 1.2f, minAlpha = 0.5f)`); durations/easings via `XMotion` tokens (never ad-hoc `tween(<literal>)`); magnitudes copied verbatim from the table (never invented); never inline motion in `Screen.kt`/components; every kept row gated by `rememberReducedMotion()`; no interaction/hover motion.
5. **X-Component Constraint Check**: Collect the unique set of design system source files needed by the handoff's component references (one file may define many composables — e.g. `XButton.kt` defines `XButton`, `XOutlinedButton`, `XIconButton`, `XTextIconButton`, `XOutlinedIconButton`). Read each file in full and catalog **every composable defined in it**, not just the one the handoff named. For each composable, extract:
   - `defaultMinSize` constraints (e.g. `XButton` enforces `minWidth=100.dp, minHeight=44.dp`)
   - Default parameter values that differ from the handoff's intent (e.g. `XIconButton` defaults to a visible `surface` background)
   - Hardcoded internal padding that overrides `contentPadding` (e.g. `XTextField` hardcodes `top=10.dp, bottom=10.dp`)
   - Any internal `Modifier` applied via `.then(...)` that the caller cannot override

   Reading the whole file matters: the UI agent may legitimately reach for a sibling composable in the same file, and it needs those constraints too.

   For each conflict, decide the resolution before the UI agent writes any code:
   - Override via modifier: `Modifier.defaultMinSize(Dp.Unspecified)`
   - Override via parameter: explicit `colors`, `shape`, or `contentPadding`
   - Accept as architectural limitation: note it in the agent prompt

   **Pass the conflict list to the UI agent** as additional context alongside the handoff.
6. **Pass the handoff to the UI agent**: Include the `DESIGN.md` path, the annotation layer, the constraint conflict list, the Typography Scale `M3 Role` mapping + any *Type-scale role overrides*, and the `## Motion` table (when present) as context. For motion: the DS generic primitives are already created (step 4); the agent writes only the feature-specific rows into `presentation/ui/motion/{Feature}Motion.kt`, calls the DS primitives for generic rows, gates every kept row with `rememberReducedMotion()`, and implements **no** interaction/hover motion. The UI agent emits every text node as `style = MaterialTheme.typography.{role}` (or an `XTextDefaults` preset) — never raw `fontSize`/`fontWeight` except where an override row applies (then `…typography.{role}.copy(...)`). The font itself is already wired globally in step 3 — the agent never sets `fontFamily`. The handoff's component references name the design-system resources and `X*` components each node binds to; the UI agent emits each node against the referenced component/resource, exactly as declared. The handoff's Component Tree is the primary source for UI implementation.

---

### Option A: Sequential Execution

#### Step 1: Data Layer
```
Invoke kmp-data-layer with:
- Feature name: {featurename}
- Task files: openspec/changes/{change-id}/task-*-data-*.md
- Project context:
  - PKG_PREFIX, PKG_PATH
  - CORE_COMMON_PKG, CORE_DATA_PKG
  - CORE_MODULES, CORE_DESIGNSYSTEM_PKG
- Expected: Data layer complete + build validation
```

**Wait for completion** → Verify success

> For a **pure `platform-capability` / `native-view`** feature, **skip Step 1** (no REST) and run Step 1b instead. For `mixed`, run both Step 1 and Step 1b.

#### Step 1b: Platform Layer (tag ≠ `network`)
```
Invoke kmp-platform with:
- Feature name: {featurename}
- Platform Profile tag + chosen sourcing option (from the OpenSpec spec)
- Capabilities to implement (e.g. current-location GPS)
- Project context: PKG_PREFIX, PKG_PATH, CORE_COMMON_PKG, CORE_MODULES
- Expected: commonMain DataSource interface + actuals (android/ios/desktop)
  + expect/actual val platformModule; provider-only (no composables);
  flags any iOS-Swift-bridge follow-up
```

**Wait for completion** → Verify success. If the agent flagged an iOS-Swift dependency, carry it to Step 4.4.

#### Step 2: UI Layer
```
Invoke kmp-ui-layer with:
- Feature name: {featurename}
- Task files: openspec/changes/{change-id}/task-*-ui-*.md
- Project context:
  - PKG_PREFIX, PKG_PATH
  - CORE_COMMON_PKG, CORE_DESIGNSYSTEM_PKG
- Design-aware context (if applicable):
  - Handoff: the `DESIGN.md` for the capability (produced by the `penpot-design-md` skill) + the annotation layer produced by `penpot-document-handoff`.
- Secondary screens: if the handoff documents secondary screens, implement its `Component Tree → Secondary Screens` subsections per their `kind` (see `shared/kmp-patterns.md` → "Secondary Screens within a Feature"):
  - **`kind: surface`** → render the role's host X-component (`XModalBottomSheet`/`XDialog`/`XAlertDialog`/`XModalDrawerSheet`/inline `XSurface`) from `{Feature}ScreenRoot`, gated by a `{Feature}UiModel` visibility field, opened/closed via callbacks hoisted to the ViewModel (Rule 10). Content composable in its own `components/` file. No Route, no new `Screen.kt` allowlist entry.
  - **`kind: screen`** → a full child screen: its own `{Feature}{Role}Screen.kt` (own 3-name allowlist) + `components/`, its own `{Feature}{Role}Route`, and a `composable<{Feature}{Role}Route>` registered **inside the same `NavGraphBuilder.{featurename}()` extension** as the primary. The primary navigates to it via a hoisted callback (Rule 10 — e.g. `onEditClick`); screens never take a `navController`. This is a **child route of the feature** — it does **not** add an Integration Point.
- Localization (Rule 12): create `composeResources/values/strings.xml`; ALL display text via `stringResource(Res.string.*)` — no hardcoded literals. If a `DESIGN.md` is present, use its String Inventory keys.
- Motion (design-aware): if the `DESIGN.md` has a `## Motion` table, implement each feature-specific row in `presentation/ui/motion/{Feature}Motion.kt`, call the DS `motion/` primitives (already created in step 4) for generic rows, gate every row with `rememberReducedMotion()`, never inline in `Screen.kt`/components, never implement interaction/hover motion. See `shared/kmp-motion.md`.
- Native-view (Rule 14, tag = `native-view`/`mixed`): write the `expect @Composable PlatformX` + `AndroidView`/`UIKitView`/desktop-fallback actuals under `components/`; `{Feature}Content` calls it and stays pure Compose. Load `skills/kmp-create-feature/references/architecture/platform.md` → "Pattern C". Consume the DataSource interface from platform (do NOT write the provider).
- Expected: UI layer complete (incl. strings.xml) + build validation
```

**Wait for completion** → Verify success

#### Step 3: Integration
```
Invoke kmp-integrator with:
- Feature name: {featurename}
- Task files: openspec/changes/{change-id}/task-*-integration-*.md
- Project context:
  - PKG_PREFIX, PKG_PATH, PROJECT_NAMESPACE
  - CORE_COMMON_PKG, CORE_DATA_PKG, CORE_DESIGNSYSTEM_PKG
  - INIT_KOIN_PATH, NAV_HOST_PATH, CORE_MODULES
- Bottom-bar tab: read the spec's Navigation section — if the feature is a top-level tab, pass its label/icon/order (Integration Point 5); otherwise it is a pushed screen (skip point 5). **Contradiction check**: if the `DESIGN.md` Component Tree contains a tab nav bar note (`[App-shell chrome — Integration Point 5...]`) BUT the spec says "pushed screen", STOP — do not proceed. Fix the spec's Navigation section to say "top-level tab" first. The design is authoritative; a pushed-screen default in the spec is the error.
- **First-feature (Welcome) handoff (MANDATORY for the first feature)**: as part of Integration Point 4, check both markers — `WelcomeScreen.kt` exists under `composeApp/src/commonMain/kotlin/**` AND `{NAV_HOST_PATH}` has `startDestination = WelcomeRoute`. If both, replace `startDestination` with `{Feature}Route`, drop the `composable<WelcomeRoute> { WelcomeScreen() }` line + its imports, and `rm -f` the `WelcomeScreen.kt` file. See `skills/kmp-create-feature/references/architecture/integration.md` → "4a. First-feature (Welcome) Handoff".
- Platform module (Rule 14, tag ≠ `network`): pull `platformModule` (expect/actual) into `{featurename}Module` via `includes(platformModule)` and provide `androidContext()` if an Android actual needs it
- Expected: integration points 1–4 (+ point 5 if a tab) + first-feature Welcome handoff (if applicable) + full build + ktlint + the living spec
```

**Wait for completion** → Verify success

---

### Option B: Parallel Execution (Recommended)

#### Step 1: Launch Data + UI Agents in Parallel

> **Platform features (tag ≠ `network`)**: launch **platform** alongside ui-layer (and data-layer too, only for `mixed`). The platform agent (provider) and ui-layer (composable + ViewModel) touch disjoint files, so they parallelize cleanly. Pass each the Platform Profile tag + sourcing option. `kmp-ui-layer` gets the DataSource interface name so its wiring matches.

**In ONE message**, invoke the agents in this feature's set (per Step 4.0) simultaneously — 2 for `network`, 2–3 for platform profiles:

```
1. kmp-data-layer (network / mixed only) with:
   - Feature name: {featurename}
   - Project context: PKG_PREFIX, PKG_PATH, CORE_COMMON_PKG,
     CORE_DATA_PKG, CORE_MODULES, CORE_DESIGNSYSTEM_PKG

2. kmp-ui-layer with:
   - Feature name: {featurename}
   - Project context: PKG_PREFIX, PKG_PATH, CORE_COMMON_PKG,
     CORE_DESIGNSYSTEM_PKG
   - Design-aware context (if applicable):
     - Handoff: the `DESIGN.md` for the capability (produced by the `penpot-design-md` skill) + the annotation layer produced by `penpot-document-handoff`.
     - Secondary screens: if the handoff documents secondary screens, implement its `Component Tree → Secondary Screens` subsections per `kind` (`surface` → host X-component + `{Feature}UiModel` visibility field + callbacks, no Route; `screen` → own `{Feature}{Role}Screen.kt` + `{Feature}{Role}Route` + `composable<…>` in the same `NavGraphBuilder.{featurename}()` extension + hoisted-callback nav). Child route, not a new Integration Point. See `shared/kmp-patterns.md` → "Secondary Screens within a Feature".
   - Localization (Rule 12): create `composeResources/values/strings.xml`; ALL display text via `stringResource(Res.string.*)` — no hardcoded literals. If a `DESIGN.md` is present, use its String Inventory keys.
   - Motion (design-aware): if the `DESIGN.md` has a `## Motion` table, implement each feature-specific row in `presentation/ui/motion/{Feature}Motion.kt`, call the DS `motion/` primitives (created in step 4) for generic rows, gate every row with `rememberReducedMotion()`, never inline, never implement interaction/hover motion. See `shared/kmp-motion.md`.
   - Native-view (Rule 14, tag = `native-view`/`mixed`): write the `expect @Composable PlatformX` + `AndroidView`/`UIKitView`/desktop-fallback actuals under `components/`; consume platform's DataSource interface; load `skills/kmp-create-feature/references/architecture/platform.md` → "Pattern C".

3. kmp-platform (tag ≠ `network`) with:
   - Feature name + Platform Profile tag + sourcing option
   - Capabilities to implement
   - Project context: PKG_PREFIX, PKG_PATH, CORE_COMMON_PKG, CORE_MODULES
   - Provider-only: DataSource interface + actuals (android/ios/desktop) + platformModule; flags any iOS-Swift-bridge follow-up
```

Each agent works in isolated context window.

**Wait for ALL launched agents to complete** → Verify each succeeded

#### Step 2: Launch Integration Agent
```
Invoke kmp-integrator with:
- Feature name: {featurename}
- Project context: PKG_PREFIX, PKG_PATH, PROJECT_NAMESPACE, CORE_COMMON_PKG,
  CORE_DATA_PKG, CORE_DESIGNSYSTEM_PKG, INIT_KOIN_PATH,
  NAV_HOST_PATH, CORE_MODULES
- Bottom-bar tab: read the spec's Navigation section — if a top-level tab, pass label/icon/order (point 5); else pushed screen. **Contradiction check**: if the `DESIGN.md` Component Tree contains a tab nav bar note (`[App-shell chrome — Integration Point 5...]`) BUT the spec says "pushed screen", STOP — fix the spec first. Design is authoritative.
- **First-feature (Welcome) handoff (MANDATORY for the first feature)**: part of Integration Point 4 — if `WelcomeScreen.kt` exists AND `{NAV_HOST_PATH}` has `startDestination = WelcomeRoute`, switch `startDestination` to `{Feature}Route`, drop the `composable<WelcomeRoute> { WelcomeScreen() }` line + imports, and `rm -f` `WelcomeScreen.kt`. See `skills/kmp-create-feature/references/architecture/integration.md` → "4a. First-feature (Welcome) Handoff".
- Platform module (Rule 14, tag ≠ `network`): pull `platformModule` into `{featurename}Module` via `includes(platformModule)`; provide `androidContext()` if needed
- Integrates data, platform, and UI layers
- Completes integration points 1–4 (+ point 5 if a tab) + first-feature Welcome handoff (if applicable)
- Final validation + formatting
- Confirms the living spec
```

**Wait for completion** → Verify success

---

## Step 4.3: Verify the Living Spec

After all agents complete, verify the living specification exists:

```bash
ls -la openspec/specs/{featurename}/spec.md
```

**Expected**: The spec.md file should exist and contain the complete specification.

**If spec.md exists** → Proceed to Phase 5 (Handoff)

**If spec.md missing** → Check the OpenSpec change output, may need to re-invoke

---

## Step 4.4: Platform Follow-ups (iOS-Swift bridge route)

If `kmp-platform` (or `kmp-ui-layer` for a native view) flagged that an **iOS `actual` needs Swift**, the Kotlin side is complete but the iOS implementation is a stub. Skills never call each other — **surface a route to the user** as part of the completion report:

```
> iOS note — {Feature}'s iOS actual needs a Swift implementation.
> Run `/kmp-bridge-swift` for {Feature}Bridge to complete the iOS side.
```

Do **not** invoke `/kmp-bridge-swift` yourself. Android + desktop builds pass without it; the iOS framework links once the user completes the bridge. If no Swift dependency was flagged, omit this step entirely.

---

## Agent Context Passing

When invoking each agent, include the full project context from Phase 0:

```markdown
## Project Context

- PKG_PREFIX: {value}
- PKG_PATH: {value}
- CORE_COMMON_PKG: {value}
- CORE_DATA_PKG: {value}
- CORE_DESIGNSYSTEM_PKG: {value}
- INIT_KOIN_PATH: {value}
- NAV_HOST_PATH: {value}

## Feature

- Name: {featurename}
- Docs: openspec/specs/{featurename}/spec.md + openspec/changes/{change-id}/
- Ledger: .kmp/run.json
```

---

## Error Handling

| Error | Action |
|-------|--------|
| Agent build failure | Agent loads troubleshooting, fixes, retries |
| Agent reports failure | Review output, fix issues, re-invoke |
| Timeout | Check agent status, may need to restart |

---

## Output

After all agents complete:
- Data layer implemented and validated
- UI layer implemented and validated
- Integration complete (4 points + first-feature Welcome handoff if applicable)
- Build passing + ktlint formatted
- Living spec current at `openspec/specs/{featurename}/spec.md`
- Ready to proceed to **Phase 5: Handoff**
