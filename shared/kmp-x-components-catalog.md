# X-Components Constraint Catalog

> **Purpose**: Document every X-component's internal constraints — `defaultMinSize`, default colors, hardcoded padding, internal modifiers, and behavior overrides — that survive even when feature code passes parameters. Consumed by the KMP build skills during design-aware UI work.
>
> **Maintenance**: Regenerate this file whenever any `core/designsystem/**/X*.kt` changes — the catalog must match `core/designsystem/**/X*.kt`, and out-of-date entries cause silent audit drift.

---

## Buttons

### `XButton`
- Disables M3's 48dp minimum touch target via `LocalMinimumInteractiveComponentSize provides Dp.Unspecified` — actual button can be smaller than 48×48.
- **Default shape**: `CircleShape`.
- **Default colors**: `ButtonDefaults.buttonColors()` — M3 `primary` background, `onPrimary` text.
- **Default elevation**: `ButtonDefaults.buttonElevation()`.
- **Default contentPadding**: `ButtonDefaults.ContentPadding` (24dp horizontal, 8dp vertical).
- `XButtonDefaults.IconSize = 20.dp`, `IconSpacing = 6.dp` (used by callers, not auto-applied).

### `XIconButton` *(delegates to `XButton`)*
- **Default `containerColor`**: `MaterialTheme.colorScheme.surface` ← **renders a visible surface-colored circle when caller passes no `colors`.** This is the most common audit-trap; if the design has no `bg-*` class on the icon button wrapper, the feature MUST pass `Color.Transparent` explicitly.
- **Default `contentColor`**: `contentColorFor(surface)`.
- **Default contentPadding**: `PaddingValues()` (zero — overrides XButton's 24dp/8dp).
- **Default shape**: `CircleShape` (from XButton).
- Inherits XButton's touch-target override.

### `XTextButton`
- Disables M3's 48dp minimum touch target.
- **Default shape**: `CircleShape`.
- **Default colors**: `XButtonDefaults.TextButtonColors` — transparent background, `primary` content.
- **Default contentPadding**: `ButtonDefaults.TextButtonContentPadding` (12dp horizontal, 8dp vertical).

### `XTextIconButton` *(delegates to `XTextButton`)*
- **Default contentPadding**: `PaddingValues()` (zero — overrides parent's 12dp/8dp).
- Other defaults inherited from `XTextButton`.

### `XOutlinedButton`
- Disables M3's 48dp minimum touch target.
- **Default shape**: `CircleShape`.
- **Default colors**: `ButtonDefaults.outlinedButtonColors()` — transparent background, `primary` content.
- **Default border**: `BorderStroke(1.dp, colors.contentColor)` — computed locally, not from `OutlinedButtonDefaults`. Border color follows `contentColor` by default; passing custom `colors` changes the border too.

### `XOutlinedIconButton` *(delegates to `XOutlinedButton`)*
- **Default contentPadding**: `PaddingValues()` (zero — overrides parent's 24dp/8dp).
- Other defaults inherited from `XOutlinedButton`.

### `XSelectionButton`
- A switching wrapper, not a single component:
  - `selected = true` → renders as `XButton` (CircleShape, primary bg).
  - `selected = false` → renders as `XTextButton` (CircleShape, transparent bg, primary content).
- `XSelectionButtonContainer` wraps with `Modifier.background(colorScheme.surface, CircleShape)` — applies surface circle automatically; design without bg on the container row will mismatch.

### `XFloatingActionButton`
- Pass-through to M3 `FloatingActionButton`. Defaults: `FloatingActionButtonDefaults.shape`, `FloatingActionButtonDefaults.containerColor`, `FloatingActionButtonDefaults.elevation()`. No additional internal constraints.

### `XButtonProgress`
- **Hardcoded size**: 24dp (`Modifier.size(24.dp)`).
- **Hardcoded strokeWidth**: 2dp.
- **Color**: `LocalContentColor.current`.
- Designed to be embedded inside a button. No parameters — completely fixed.

---

## Inputs

### `XTextField`
- **Always applies** `defaultMinSize(minWidth = OutlinedTextFieldDefaults.MinWidth = 280.dp, minHeight = 48.dp)` — feature `modifier` overrides cannot shrink below this.
- Adds `padding(top = 8.dp)` to the modifier when `label != null` — extra 8dp above the field that is invisible to the design.
- **Default shape (singleLine = true)**: `CircleShape`.
- **Default shape (singleLine = false)**: `RoundedCornerShape(20.dp)`.
- **Default contentPadding**: 16dp all sides (`XTextFieldDefaults.ContentPadding`).
- **Default colors** (`XTextFieldDefaults.Colors`):
  - focused/unfocused container: `colorScheme.surface`.
  - unfocusedBorderColor: `colorScheme.onSurface @ 12%`.
  - placeholder/label color: `colorScheme.onSurface @ 60%`.
- **Placeholder font size forced to 14sp** via internal `ProvideTextStyle` — caller cannot make placeholder larger.
- `trailingIcon` slot reserves space automatically — feature does NOT need to add explicit right padding.

### `XCheckbox`
- Default colors: `CheckboxDefaults.colors()` (M3 default — primary check, outline border).

### `XSwitch`
- Default colors: `SwitchDefaults.colors()` (M3 default).

### `XRadioButton`
- **Default colors override**: `selectedColor = primary` AND `unselectedColor = primary` (both!) — different from M3 default which uses outline for unselected. Both states render in primary color.

### `XSlider`
- Default colors: `SliderDefaults.colors()` (M3 default).

### `XFilterChip`
- **Default shape**: `CircleShape` (`XFilterChipDefaults.Shape`).
- **Default colors** (`XFilterChipDefaults.filterChipColors`):
  - containerColor: `colorScheme.surface`.
  - labelColor: `colorScheme.onSurfaceVariant`.
  - selectedContainerColor: `colorScheme.primaryContainer`.
  - selectedLabelColor: `colorScheme.onPrimaryContainer`.
- **Border always applied**: `FilterChipDefaults.filterChipBorder(enabled, selected)` — caller cannot remove it (no border parameter exposed).

---

## Containers & Surfaces

### `XCard` (clickable + non-clickable variants)
- **Default shape**: `MaterialTheme.shapes.medium` = `RoundedCornerShape(12.dp)` (per XTheme).
- Default colors: `CardDefaults.cardColors()` (M3: surfaceVariant bg, onSurface content).
- Default elevation: `CardDefaults.cardElevation()`.

### `XScreen` *(feature screen container — Rule 13)*
- The container **every feature screen** uses. Plain `Column { topBar(); Box(Modifier.fillMaxWidth().weight(1f)){ content() }; bottomBar() }`.
- Slots: `topBar` (usually `XTopAppBar`), `bottomBar` (optional sticky CTA), `content: @Composable BoxScope.() -> Unit`.
- **Touches NO window insets** — the single app-shell `Scaffold` (`App.kt`) owns them and pads the NavHost. This is the whole point: it cannot double-count insets the way a nested `Scaffold` does.
- `content` receives **no** `paddingValues` (it fills the weight box). Default `containerColor`: `XScaffoldDefaults.containerColor`.

### `XScaffold` *(app-shell only — NOT for features)*
- The thin M3 `Scaffold` wrapper. Used **only** by the app shell (`App.kt`) — feature screens must use `XScreen` instead (Rule 13). A feature-level `XScaffold` nests a second Scaffold and double-counts insets.
- **Default containerColor**: `XTheme.Colors.PaleLavender` *(referenced in source but not defined on `XTheme.Colors` — only `Success` and `Danger` are. Treat this as a known issue: passing an explicit `containerColor` is required, otherwise resolution may fail at compile/runtime.)*
- Default contentColor: `contentColorFor(containerColor)`.
- Default contentWindowInsets: `ScaffoldDefaults.contentWindowInsets`.

### `XDialog`
- **Always applies** `Modifier.fillMaxWidth(0.9f)` — dialog is **always 90% of screen width**, cannot be made narrower or full-width via modifier.
- **Always applies** `clip(shape)` to the inner Box.
- **Default shape**: `MaterialTheme.shapes.large` = `RoundedCornerShape(20.dp)`.
- **Default backgroundColor**: `colorScheme.background`.
- **Default DialogProperties**: `usePlatformDefaultWidth = false` (so the 90% width applies).
- `propagateMinConstraints = true` — child fills min constraints.

### `XModalBottomSheet`
- **Default tonalElevation: 0.dp** (overrides M3 default of 1dp+).
- Default sheetMaxWidth: `BottomSheetDefaults.SheetMaxWidth` (640dp).
- Default shape: `BottomSheetDefaults.ExpandedShape`.
- Default containerColor: `BottomSheetDefaults.ContainerColor`.
- Default scrimColor: `BottomSheetDefaults.ScrimColor`.
- Default dragHandle: `BottomSheetDefaults.DragHandle()` (rendered automatically).

---

## Top Bars & Navigation

### `XTopAppBar`
- Rendered **inside `XScreen`'s `topBar` slot** (Rule 13), never as a `Scaffold` topBar in a feature.
- **Always renders title center-aligned** via `CenterAlignedTopAppBar` — designs that show left-aligned titles cannot match exactly. (Treat this as a MINOR mismatch when the design appears left-aligned.)
- **Default backgroundColor**: `colorScheme.surface`.
- **windowInsets**: `WindowInsets(0, 0, 0, 0)` — explicitly zero (no system bar padding added).
- Title text style **forced** to `MaterialTheme.typography.headlineSmall.copy(fontWeight = SemiBold)` via `CompositionLocalProvider(LocalTextStyle …)` — passing custom typography to the title slot is overridden.
- Title content alpha: `ContentAlpha.high`. Navigation icon alpha: `ContentAlpha.high`. Actions alpha: `ContentAlpha.medium`.
- TopAppBarColors: `containerColor = backgroundColor`, `titleContentColor = contentColor`, `navigationIconContentColor = contentColor`, `actionIconContentColor = contentColor`.

### `XModalTopAppBar` *(wraps `XTopAppBar` with swapped slots)*
- Swaps `actions` into the navigation icon slot and `navigationIcon` into the actions slot — visual layout differs from a normal `XTopAppBar` even with identical params.
- Default backgroundColor: `Color.Transparent` (different from `XTopAppBar`).

### `XTopLogo`
- **Hardcoded image size**: 44dp (`Modifier.size(44.dp)`).
- Centered in a Column.

### `XNavigationBar`
- Default containerColor: `NavigationBarDefaults.containerColor` (M3: surface @ 2dp tonal elevation).
- Default tonalElevation: `NavigationBarDefaults.Elevation` (3dp).

### `XNavigationBarItem`
- Default colors: `NavigationBarItemDefaults.colors()` (M3 default — primary indicator, onSurface text/icon).

### `XPrimaryScrollableTabRow`
- **Default divider**: empty composable (`{}`) — **does NOT render a divider** (overrides M3 default which renders one).
- Default containerColor: `TabRowDefaults.primaryContainerColor`.
- Default contentColor: `TabRowDefaults.primaryContentColor`.
- Default edgePadding: `TabRowDefaults.ScrollableTabRowEdgeStartPadding` (52dp).

### `XNavHost`
- Custom default transitions:
  - enterTransition: slideIn from right (`IntOffset(width, 0)`) with `tween()`.
  - exitTransition: slideOut to left.
  - popEnterTransition: slideIn from left.
  - popExitTransition: slideOut to right.
- All transitions overridable per-call.

---

## Indicators, Dividers, Icons, Text

### `XCircularProgressIndicator` (indeterminate + determinate variants)
- **Default color override**: `MaterialTheme.colorScheme.primary` (overrides M3 default which uses `colorScheme.primary` of M3 — same color, but XProgressIndicatorDefaults.color makes it deliberate).
- Default strokeWidth: `ProgressIndicatorDefaults.CircularStrokeWidth` (4dp).
- Default trackColor: `ProgressIndicatorDefaults.circularTrackColor` (transparent for indeterminate).

### `XHorizontalDivider` / `XVerticalDivider`
- Default thickness: `DividerDefaults.Thickness` (1dp).
- **Default color override**: `MaterialTheme.colorScheme.outlineVariant` (matches M3 default explicitly).

### `XIcon`
- Default tint: `LocalContentColor.current` — inherits parent color (text color of enclosing scope).
- No size default — caller must pass `Modifier.size(...)`.
- **Two overloads** — pick by source:
  - `XIcon(imageVector: ImageVector, ...)` — legacy callers using `androidx.compose.material.icons.*`. The library is pinned/deprecated; do **not** reach for it for new features.
  - `XIcon(painter: Painter, ...)` — canonical path for Material Symbols rendered from XML vector drawables. Used by every feature whose `DESIGN.md` specifies a Material Symbols icon; the icon is materialized as a local XML vector drawable and referenced via `XIcon(painter = painterResource({res_reference}))`.
- Resource references follow the chrome/domain split decided by **cross-feature usage** (an icon used by ≥2 features is chrome, otherwise domain):
  - **Chrome icons** (shared across features) live in `core/designsystem/src/commonMain/composeResources/drawable/` and are referenced via the `DesignSystemResources.drawable.{name}` indirection (matches the existing pattern in `core/designsystem/.../DesignSystemResources.kt`). `/kmp-create-feature` and `/kmp-modify-feature` auto-extend `DesignSystemResources.kt` when promoting an icon.
  - **Domain icons** (feature-specific) live in `feature/{featurename}/src/commonMain/composeResources/drawable/` and are referenced via the feature's own generated `Res.drawable.{name}`.
  - **Skill ownership**: The feature's `DESIGN.md` handoff records which icons it uses. XML vector drawables, `DesignSystemResources.kt` extensions, and cross-feature Kotlin import migrations all happen in `/kmp-create-feature` or `/kmp-modify-feature` (design-aware mode), where the protect-feature-files hook marker is active and source mutations are legitimate.
  - Promotion from domain → chrome is automatic and idempotent; the implementing skill rewrites affected features' imports. Demotion is not performed.
- Filled-variant icons get a `_fill` filename and resource suffix (e.g. `bolt_fill.xml` → `Res.drawable.bolt_fill`).

#### Drawable XML authoring — NEVER `@android:color/*` (all drawable dirs)

> **Hard rule, applies to EVERY XML vector drawable under any `composeResources/drawable/` — feature, `composeApp`, and `:core:designsystem` alike (not only the script-downloaded Material Symbols).** Never leave an `@android:color/<name>` reference in a drawable's color attribute (`android:fillColor`, `android:strokeColor`, …). The `@android:color` namespace is Android-only; the Compose Multiplatform resource pipeline does not resolve it, so it **crashes at runtime on non-Android targets** (iOS/desktop).
>
> At download/author/commit time, replace every `@android:color/<name>` with its literal ARGB hex (`#RRGGBB` or `#AARRGGBB`). Look up the value in the Android platform `colors.xml` (e.g. `white` → `#FFFFFFFF`, `black` → `#FF000000`, `transparent` → `#00000000`); it is always a plain hex string. This covers downloaded Material Symbols, hand-added `composeApp` bottom-bar tab icons, brand drawables in the design system, and any third-party icon-pack XML. **Every icon must be cleaned this way before it is committed** — grep `composeResources/drawable/` for `@android:color` before committing.

### `XText` *(wraps M3 `Text`)*
- No additional internal constraints — pure pass-through to M3 Text with all M3 defaults.
- `XTextDefaults` provides preset `TextStyle`s (titleStyle, bodyStyle, labelStyle, errorStyle) — opt-in only.

---

## Dropdowns

### `XDropDown` / `XExposedDropdownMenuBox`
- Pass-through to M3 — no additional constraints.

### `XDropdownMenuItem`
- Default colors: all `Color.Unspecified` (transparent — items inherit from menu surface).

---

## Async / Special

### `XSnackbarHost`
- **Custom snackbar duration timing** (overrides M3 defaults):
  - `SnackbarDuration.Long` → 1200 ms (M3 default is much longer ~10s).
  - `SnackbarDuration.Short` → 500 ms.
  - `SnackbarDuration.Indefinite` → `Long.MAX_VALUE`.
- Custom fade-in/scale animation: 150ms fade-in, 75ms fade-out, scale 0.8 → 1.0.
- Wraps M3 `Snackbar` by default — appearance-wise it's M3 standard.

### `XPullRefresh`
- **Entire file is commented out** — non-functional. Treat as not available; do not reference in features.

### `XWebView`
- (Not catalogued — no visual constraints relevant to design audits.)

### `AsyncImage` *(design-system wrapper over coil3)*
- **For dynamic/content images only** — a runtime URL from data (avatar, flag, thumbnail, product photo). Static design assets (hero, decorative background, logo) use `Image(painter = painterResource(...))`, never AsyncImage.
- **First param is `url: String`** (NOT `model`). Always use `{CORE_DESIGNSYSTEM_PKG}.AsyncImage`, never `coil3.compose.AsyncImage`.
- **Default `contentScale = ContentScale.Fit`** (M3/coil default is Fit) — pass `ContentScale.Crop` for cover-style fills (`object-cover` in HTML).
- **`loadingResId: DrawableResource? = null`** — drawable shown during Loading **and** Error. Pass `DesignSystemResources.drawable.ds_image_placeholder` (generic) unless the design specifies a bespoke placeholder; when `null`, an empty `Box` fills the slot.
- Fills its measured bounds (`SubcomposeAsyncImageContent` with `Modifier.fillMaxSize()`); the caller sizes it via `modifier`.

---

## XTheme Reference (consumed by every X-component)

Theme values (`Shapes`, `XLightColors`, `XDarkColors`, `XTheme.Colors.*`) are project-specific and live in `XTheme.kt`.

**To look up live values:**
1. Read the project's `XTheme.kt` directly for the active scheme's hex values, the `Shapes` block, and any `XTheme.Colors.*` extensions.
2. Locate it via `core/designsystem/build.gradle.kts` namespace → dots-to-slashes → `core/designsystem/src/commonMain/kotlin/{path}/XTheme.kt`, or `Glob` `core/designsystem/src/commonMain/kotlin/**/XTheme.kt`.

Do not duplicate hex values here — they drift per project. `XTheme.kt` is the only source of truth.

---

## How to use this catalog

1. **Identify every X-component instance** in the feature's `presentation/ui/` files.
2. **For each instance**, look up its catalog entry above.
3. **For every default-rendered property** (containerColor, defaultMinSize, hardcoded padding, internal modifier):
   - If feature code passes an explicit override → use the override (token audit covers it).
   - If feature code does NOT override AND the design has no class declaring that property → **CODE MISMATCH** — the component renders something the design doesn't. Append a row to the audit.
4. **Always check** the most common traps:
   - `XIconButton` without explicit `colors` → renders `surface`-colored circle.
   - `XTextField` minHeight 48dp → cannot be smaller in design.
   - `XTextField` with label → adds invisible 8dp top padding.
   - `XTopAppBar` title → always center-aligned.
   - `XDialog` width → always 90% of screen.
   - `XPrimaryScrollableTabRow` → no divider by default (M3 differs).
   - `XRadioButton` → both selected/unselected are primary-colored.
