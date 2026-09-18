# Modify Feature

Apply changes to existing features using the spec-first workflow.

**Architecture Reference:** `shared/kmp-patterns.md`

## Feature-file guard (Required)

Before editing any feature files, activate the skill marker so the feature guard allows edits:
```bash
touch /tmp/.kmp-skill-active
```
After completion (or on any early exit), remove it:
```bash
rm -f /tmp/.kmp-skill-active
```

## Workflow

**Phase 0: Parse + Locate** → **Phase 1: Spec Check** → **Phase 2: Input Resolution** → **Phase 3: Understand + Plan** → **Phase 4: Draft Spec Delta** → [USER APPROVES] → **Phase 5: Implement (marker on)** → **Phase 6: Validate + Reconcile (marker off)** → Done

### Phase 0: Parse + Locate Feature (read-only)
Extract from request: "add sorting to productlist" → `productlist`
Validate: `ls feature/{featurename}/src/commonMain/kotlin/`

### Phase 1: Spec Check (read-only)
Load `openspec/specs/{featurename}/spec.md`

If missing, **stop and instruct the user**:

```
No spec found for '{featurename}'. Please run /opsx-propose {featurename} first
to create one, then re-invoke /kmp-modify-feature.
```

Do NOT auto-invoke `/opsx-propose` — skills do not call each other; the user controls the pipeline.

### Phase 2: Input Resolution

If the change is visual, confirm the Penpot handoff artifact exists: `DESIGN.md` (produced by the `penpot-design-md` skill, with an annotation layer from `penpot-document-handoff`). A feature is **design-aware** when a `DESIGN.md` exists. Otherwise, note that the change is design-independent.

Then read the spec's **Platform Profile & Capabilities** field (`network` / `platform-capability` / `native-view` / `mixed`).

### Phase 3: Understand + Plan

Read spec sections: Requirements, Architecture, State Management, Navigation

**Platform Profile check (Rule 14)**: read the spec's **Platform Profile & Capabilities** field. If this change **introduces** a device capability or native view (map, camera, GPS, BLE, biometrics, WebView) that the feature didn't have, set/confirm the tag (`platform-capability` / `native-view` / `mixed`) — ask once with `AskUserQuestion` if ambiguous — and load `skills/kmp-create-feature/references/architecture/platform.md`. A change that stays `network` skips this.

Determine affected layers and load architecture as needed:
- Data changes: `skills/kmp-create-feature/references/architecture/data.md`
- UI changes: `skills/kmp-create-feature/references/architecture/ui.md`
- **Platform capability / native-view changes (Rule 14)**: `skills/kmp-create-feature/references/architecture/platform.md`
- Integration changes: `skills/kmp-create-feature/references/architecture/integration.md`
- Bottom-bar tab changes ("add/remove bottom-bar tab", "make this a tab", "show in bottom nav"): `skills/kmp-create-feature/references/architecture/integration.md` → "5. Bottom-Bar Tab (Optional)"

**Design-aware branch**: If a `DESIGN.md` exists, read the Penpot handoff artifact's **Pre-Implementation Contract** section **and the `## Motion` table** (if present). Plan XTheme color updates (missing M3 roles) **and Typography Updates Required** (font swap + type-scale role overrides) first — both are app-global `:core:designsystem` edits. The DS `motion/` primitives already ship (verify present, reuse — see Phase 5, 1c); plan only feature-specific motion + any genuinely-missing DS primitive. Include the handoff's component tree, the Typography Scale `M3 Role` mapping, and the `## Motion` rows in the UI plan.

### Phase 4: Draft Spec Delta (✋ required)

Propose changes using diff format:
```markdown
## Proposed Spec Changes: {featurename}
**Current Version:** X.Y.Z → **Proposed:** X.Y+1.Z

### Section N: {Name}
```diff
  existing content
+ added content
- removed content
```
### Rationale
{Why these changes are needed}
```

**Review Gate (REQUIRED)** — present changes to user with:
- [ ] **Approve** - Proceed with implementation
- [ ] **Modify** - Request changes
- [ ] **Reject** - Do not proceed

**Never skip this step.**

### Phase 5: Implement Changes (marker on, ✋ per layer)

Follow patterns from `shared/kmp-patterns.md`

For UI changes: Load `skills/kmp-using-design-system/references/component-mappings.md`

**Strings (Rule 12)**: any new user-facing text → a key in the feature's `composeResources/values/strings.xml`, referenced via `stringResource(Res.string.*)` (or `UiText` for ViewModel-origin messages). Never add a hardcoded display literal. If the feature has no `strings.xml` yet, create it. See `shared/kmp-patterns.md` → "Strings & Localization (Rule 12)".

**Platform capability / native view (Rule 14)**: when the change adds a device capability or native view, follow `skills/kmp-create-feature/references/architecture/platform.md`:
- Capability → `commonMain` DataSource interface returning `Either<DTO>` + per-platform actuals (android/ios/**desktop** fallback) + `expect/actual val platformModule` pulled into `{featurename}Module` via `includes(platformModule)`.
- Native view → `expect @Composable PlatformX` + `AndroidView`/`UIKitView`/desktop actuals under `components/` (Shape C); `{Feature}Content` stays pure Compose.
- Update `build.gradle.kts` per-platform deps (`skills/kmp-create-feature/references/architecture/build-gradle-template.md` → "Platform-specific dependencies").
- **iOS actual needs Swift** → write the `iosMain` interface/stub and **stop**: emit *"Run `/kmp-bridge-swift` for `{Feature}Bridge`"* in the completion report. Do not write Swift; skills never call each other.
- Bump the spec's **Platform Profile** field to the new tag.

**Bottom-bar tab (optional)**: if the change is "add/remove bottom-bar tab", follow `skills/kmp-create-feature/references/architecture/integration.md` → "5. Bottom-Bar Tab (Optional)". This edits only the **app module** (`App.kt`, `navigation/TopLevelDestination.kt`, `composeApp/composeResources/`) — NOT the feature module itself; the feature stays independent. **Add**: append one `TopLevelDestination` enum entry (or scaffold the shell if this is the first tab); the tab label lives in `composeApp/src/commonMain/composeResources/values/strings.xml` (key `tab_{featurename}`), the icon as a vector XML in `composeApp/src/commonMain/composeResources/drawable/` — both referenced via `{PROJECT_NAMESPACE}.composeapp.generated.resources.Res`. **Remove**: delete the enum entry (the route remains a valid pushed destination). No registry exists — orphaned entries/labels/icons must be removed by hand.

**UI file layout (strict allowlist)**: when adding or moving composables, respect the rules in `shared/kmp-patterns.md` ("UI File Organization"):
- `{Feature}Screen.kt` accepts only the allowlist names (`Screen`, `ScreenRoot`, and optionally `EmptyContent`); Loading/Failed route to the shared `AppLoadingState`/`AppErrorState` (`{PKG_PREFIX}.designsystem.app`) — never private shells
- Every other composable, including `{Feature}Content`, lives one-per-file under `presentation/ui/components/`
- Non-composable helpers live in `presentation/ui/{Feature}Utils.kt`, never under `components/`

**Previews (mandatory for new components)**: when this modification **adds a new component**, you must also:

1. **Check feature build.gradle.kts** for preview deps:
   ```kotlin
   sourceSets.commonMain.dependencies { implementation(libs.compose.ui.tooling.preview) }
   dependencies { androidRuntimeClasspath(libs.compose.ui.tooling) }
   ```
   If either is missing, add it as part of this modification.

2. **Generate a `@Preview` composable** in the same file as the new component, marked `private`, wrapped in `XTheme`, with realistic sample data. Use the canonical import `androidx.compose.ui.tooling.preview.Preview` (CMP 1.11.0+ — common). Never use the deprecated `org.jetbrains.compose.ui.tooling.preview.Preview`.

   See `skills/kmp-create-feature/references/architecture/ui.md` → "Previews" for the full pattern, including `@PreviewParameter` for multi-variant previews.

**Design-aware branch**: If a `DESIGN.md` exists, implement in this order:
1. **XTheme update** — Add all missing M3 roles from the Penpot handoff artifact's Pre-Implementation Contract to **both** `XLightColors` and `XDarkColors` in `XTheme.kt`. Verify build: `./gradlew :core:designsystem:assembleAndroidMain`
1b. **Typography update** — Read the Penpot handoff artifact's **Typography Updates Required**. Typography is app-global (lands in `:core:designsystem`). **Font swap** (only if a *Font swap* row exists): apply the handoff's font set and replace `XFontFamily()`'s body in `XTheme.kt` with the corresponding `Font(Res.font.*)` lines (add `import androidx.compose.ui.text.font.FontVariation` for a variable route). Verify build: `./gradlew :core:designsystem:assembleAndroidMain`. **Type-scale role overrides** are applied per-node in the feature (sub-step 3), not the theme. Skip 1b entirely when neither sub-table is present.
1c. **Motion files** (only if the Penpot handoff artifact has a `## Motion` table; skip for a static design). Motion needs no asset download — pure Compose in dedicated `motion/` files (see `shared/kmp-motion.md`). The generic DS primitives **already ship** in `core/designsystem/.../motion/` (`XMotion.kt` + `expect/actual rememberReducedMotion()`, `Modifier.shimmer()`, `PulseDot`, `AmbientMeshBackground`, `BokehCanvas`, `Modifier.pulseGlow()`, `RevealOnAppear`) — **verify present, do not recreate**. Only if a "DS `motion/`" row needs a primitive the shipped set lacks (rare), add it (canonical names from `shared/kmp-motion.md`; a new platform-dependent one needs `.android`/`.ios`/`.desktop` actuals) and build `./gradlew :core:designsystem:assembleAndroidMain :core:designsystem:desktopMainClasses`. **Feature-specific rows** land in `feature/{featurename}/.../presentation/ui/motion/{Feature}Motion.kt` in sub-step 3, **reusing** the shipped DS primitives with the row's magnitude passed as a **parameter**. Durations/easings via `XMotion` tokens (never ad-hoc `tween(<literal>)`); magnitudes copied verbatim from the `## Motion` table (never invented); gate every kept row with `rememberReducedMotion()`; never inline in `Screen.kt`/components; never implement interaction/hover motion.
2. **X-Component Constraint Check** — Collect the unique set of design system source files needed by the Penpot handoff artifact's Component Tree (one file may define many composables — e.g. `XButton.kt` defines `XButton`, `XOutlinedButton`, `XIconButton`, `XTextIconButton`, `XOutlinedIconButton`). Read each file in full and catalog **every composable defined in it**, not just the one the handoff named. For each composable, extract:
   - `defaultMinSize` constraints (e.g. `XButton` enforces `minWidth=100.dp, minHeight=44.dp`)
   - Default parameter values that differ from what the handoff intends (e.g. `XIconButton` defaults to a visible `surface` background)
   - Hardcoded internal padding that overrides `contentPadding` (e.g. `XTextField` hardcodes `top=10.dp, bottom=10.dp`)
   - Any internal `Modifier` applied via `.then(...)` that the caller cannot override

   Reading the whole file matters: the implementation may legitimately reach for a sibling composable in the same file (e.g. use `XOutlinedButton` instead of `XButton` for a pill), and you need its constraints too.

   For each conflict found, decide the resolution **before writing any code**:
   - Override via modifier: `Modifier.defaultMinSize(Dp.Unspecified)` to remove a min-size floor
   - Override via parameter: pass explicit `colors`, `shape`, or `contentPadding` to win over the default
   - Accept as architectural limitation: note it — do not fight it with hacks

3. **Component implementation** — Implement UI from the Penpot handoff artifact's Component Tree. Use the handoff as the primary source, design screenshots as visual cross-reference only. Apply constraint resolutions from sub-step 2. Every text node uses `style = MaterialTheme.typography.{role}` (or an `XTextDefaults` preset) — never raw `fontSize`/`fontWeight`, except a *Type-scale role override* row (`…typography.{role}.copy(...)`). Never set `fontFamily` (global, wired in 1b). **Motion** (if the handoff has a `## Motion` table): write feature-specific rows into `presentation/ui/motion/{Feature}Motion.kt`, call the DS `motion/` primitives (from 1c) for generic rows, gate each with `rememberReducedMotion()` — never inline, never interaction/hover motion.
4. **Post-Implementation Checklist** — Verify every item in the Penpot handoff artifact's Post-Implementation Checklist:
   - All XTheme missing roles added to BOTH schemes
   - Font swap (if any) applied: `XFontFamily()` rewired, `:core:designsystem` builds
   - Every text node uses a `MaterialTheme.typography.{role}` — no raw `fontSize`/`fontWeight` except recorded overrides
   - Every component in the handoff exists in implementation
   - Every Modifier in the handoff is present in code
   - All colors use `MaterialTheme.colorScheme.{role}` — no raw `Color()` hex
   - Component override sizes/colors applied
   - Every `## Motion` row implemented in a `motion/` file (feature → `presentation/ui/motion/`, generic → DS `motion/`), reduced-motion gated, no inline/interaction/hover motion (n/a if no `## Motion` table)

### Phase 6: Validate + Reconcile (marker off)

Validate the build:
```bash
./gradlew :feature:{featurename}:assembleAndroidMain
./gradlew :feature:{featurename}:ktlintFormat
```

Run the architecture checker and read its report:
```bash
python3 shared/scripts/kmp_check.py {featurename}
```
Read `.kmp/check-report.json`.

Update the specification: Apply APPROVED changes (don't regenerate). Add changelog:
```markdown
## Last Updated
- {YYYY-MM-DD} - {Brief description}
```
Version bump: Patch (X.Y.Z+1) for fixes, Minor (X.Y+1.0) for features

When the change is complete, archive it via `/opsx-archive`.

**Design-aware branch**: Also add a UI Design section to the spec referencing the Penpot handoff artifact (`DESIGN.md`).

## Error Handling

Build errors: Load `skills/kmp-create-feature/references/troubleshooting/index.md`
Design system: Activate `kmp-using-design-system`

## Completion Checklist

- [ ] Spec changes drafted and USER APPROVED
- [ ] Build passes
- [ ] Code formatted (ktlint)
- [ ] Spec updated with approved changes
- [ ] Changelog entry added
- [ ] Version bumped

## What's next

Emit this blockquote as the very last line of output:

---

> **Next step —** run `/clear` to free the context window (the spec + the Penpot handoff artifact (`DESIGN.md`) are durable artifacts — the next skill re-reads them fresh, so clearing loses nothing), then `/kmp-review-feature {featurename}` to validate the changes.
