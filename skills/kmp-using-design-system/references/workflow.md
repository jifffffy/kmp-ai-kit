# Using Design System

Use X-components from `:core:designsystem` instead of Material3 in feature modules.

**Architecture Reference:** `shared/kmp-patterns.md`

## Design-Aware Mode

If the Penpot handoff artifact (`DESIGN.md`) exists, implementation skills auto-detect it and use its recorded component tree for UI implementation. This skill provides component mappings for existing code.

## Core Rule

**X-components ONLY in features. NO Material3 components.**

## Common Replacements

| Material3 | X-component |
|-----------|-------------|
| `Button` | `XButton` (7 variants) |
| `TextField` | `XTextField` |
| `Text` | `XText` |
| `Scaffold` (feature screen) | `XScreen` (Rule 13 — `XScaffold` is app-shell only) |
| `CircularProgressIndicator` | `XCircularProgressIndicator` |
| `coil3.compose.AsyncImage` | `{CORE_DESIGNSYSTEM_PKG}.AsyncImage` |

Full mappings: `skills/kmp-using-design-system/references/component-mappings.md`
Usage examples: `skills/kmp-using-design-system/references/usage-examples.md`

## Key Rules

1. **Imports**: `import {CORE_DESIGNSYSTEM_PKG}.*` (avoid Material3 in features)
2. **4-State UI**: Uninitialized → Loading → Success → Failed (mandatory)
3. **Theme**: Never wrap screens in `XTheme` (app-level only). Feature screens use `XScreen` (Rule 13) — never a `Scaffold`/`XScaffold`, which would nest a second Scaffold and double the insets
4. **Navigation**: Use `XNavHost` (pre-configured animations)
5. **ScreenRoot Pattern**: `{Feature}Screen` + `{Feature}ScreenRoot` pair required
6. **Strings (Rule 12)**: `XText`/`XIcon` text args take `stringResource(Res.string.*)`, never literals. Shared strings via `DesignSystemResources`; ViewModel-origin via `UiText`. See `shared/kmp-patterns.md` → "Strings & Localization (Rule 12)".
7. **Typography (app-global type scale)**: text picks an M3 type-scale role — `style = MaterialTheme.typography.{role}` (or an `XTextDefaults` preset) — never raw `fontSize`/`fontWeight`/`fontFamily`, except a design-recorded override (`…typography.{role}.copy(...)`). The font family is global (wired in `XTheme.kt`), never set per-screen. See `shared/kmp-patterns.md` → "Typography".

## Allowed Exceptions

Material3 allowed only in:
- `:core:designsystem` module (for creating wrappers)
- `MaterialTheme.colorScheme/typography` (accessing theme values)
- Compose Foundation (Row, Column, Box, Spacer)
- **Animation APIs** (`androidx.compose.animation.*`, `animation.core.*`, `foundation.interaction.*`) — **not** Material3, required for motion. Not a violation.

## Motion

Animation = captured from the design, in **dedicated `motion/` files** (never inline in `Screen.kt`/components). The generic motion primitives **ship in `:core:designsystem` `motion/`** — `Modifier.shimmer()`, `PulseDot`, `AmbientMeshBackground`, `BokehCanvas`, `Modifier.pulseGlow()`, `RevealOnAppear`, `rememberReducedMotion()`. Reuse them (pass the design's magnitude as a parameter); write only feature-specific one-offs in `feature/.../presentation/ui/motion/{Feature}Motion.kt`. Press/hover feedback is dropped (android+ios). Full policy: `shared/kmp-motion.md`.

## Validation Checklist

- [ ] No Material3 component imports in feature files
- [ ] All buttons use XButton variants
- [ ] All text uses XText
- [ ] Text uses `MaterialTheme.typography.{role}` / `XTextDefaults` — no raw `fontSize`/`fontWeight`/`fontFamily` except recorded overrides
- [ ] No hardcoded display strings — `stringResource`/`UiText` only (Rule 12)
- [ ] Screen uses `XScreen` (NOT `XScaffold`/`Scaffold`, NOT `XTheme`) — Rule 13
- [ ] XTopAppBar from `{CORE_DESIGNSYSTEM_PKG}.toolbar`
- [ ] AsyncImage uses `url` parameter (not `model`)
- [ ] 4-state pattern implemented
- [ ] ScreenRoot pattern: Screen + ScreenRoot pair exists
