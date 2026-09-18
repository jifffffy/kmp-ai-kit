# Motion (Single Canonical Reference)

All skills and agents that touch animation link here. Loaded **only** when motion work happens, so `shared/kmp-patterns.md` stays lean. Do not duplicate this file's tables elsewhere — link to it.

The pipeline **captures → implements → verifies** whatever motion the design contains. It is **capture-only**: it never injects a motion directive into the design. The user controls motion aggressiveness through their own design; the design encodes it (CSS `@keyframes`, a tailwind `theme.extend.animation` config, custom `<style>` classes, JS drivers), and the pipeline mirrors it in Compose.

---

## Web-Motion Policy (KEEP vs DROP)

Primary targets are **android + ios** — there is no pointer and press feedback is the platform's job, not the design's.

**DROP (no Compose output)** — recorded in the audit's "Dropped" line for transparency, never a design entry:

| Web source | Why dropped |
|------------|-------------|
| `active:scale-*`, `active:rotate-*`, `.ripple`, `.ripple:active::after` | **Touch press feedback** — the platform/X-component supplies its own |
| `hover:*`, `group-hover:*`, `focus:*`, `.interactive-card:hover/:active`, `cursor-*` | **Pointer/hover** — no pointer on mobile |

**KEEP (implement in Compose)** — the 4 non-interaction families below, plus the `prefers-reduced-motion` honor. These are motion the design *means*, not input feedback.

---

## The 4 Families + Compose Mapping

Every kept token belongs to one family and maps to one Compose primitive.

| Family | What it is | Compose primitive |
|--------|-----------|-------------------|
| **Ambient background** | Decorative looping backdrop (mesh-gradient shift, bokeh/particle canvas) | `rememberInfiniteTransition` + animated `Brush` offset behind the screen; `Canvas` + `withInfiniteAnimationFrameMillis` for particles |
| **Loading + attention loop** | Infinite loops that draw the eye (shimmer skeleton, ping ring, dot pulse, glow pulse, ring-burst, badge bounce, float bob, icon pulse) | `Modifier.shimmer()`, `rememberInfiniteTransition` scale/alpha, `Modifier.pulseGlow()`, infinite `translationY` |
| **Entrance** | One-shot reveal on first composition (section slide-up + fade, staggered list reveal, accordion expand + chevron rotate) | `AnimatedVisibility(fadeIn() + slideInVertically())`, staggered via index delay / `animateItem`, `animateContentSize()` + `animateFloatAsState` rotation |
| **Value-driven** | Animates to a target driven by data/state (progress ring/bar fill, count-up number, chart bars grow, toggle thumb / segment pill / tab underline slide, slider fill) | `animateFloatAsState` / `animateIntAsState` / `animateDpAsState` / `Animatable`; `XCircularProgressIndicator` / `Canvas drawArc`; X-switch / X-slider built-ins |

### Easing map (from verified design vocabulary)

| CSS easing | Compose |
|------------|---------|
| `cubic-bezier(.4,0,.2,1)` (`custom-ease`) | `FastOutSlowInEasing` |
| `cubic-bezier(.68,-.55,.265,1.55)` (`bounce-ease`) | overshoot — `spring(dampingRatio < 1)` or a custom `CubicBezierEasing` |
| `cubic-bezier(.34,1.56,.64,1)` (progress spring) | overshoot — `spring(dampingRatio < 1)` |
| `cubic-bezier(.22,1,.36,1)` (`slide-up`) | ease-out-expo — custom `CubicBezierEasing(0.22f, 1f, 0.36f, 1f)` |
| `linear` (shimmer, bg-gradient) | `LinearEasing` |

### Reduced-motion gate (mandatory)

`@media (prefers-reduced-motion: reduce)` in a design zeroes durations. **Every** kept animation — loops, entrances, ambient, value-driven — must be gated by `rememberReducedMotion()`: when reduced, skip the animation and jump straight to the end/target state (no loop, no entrance slide, instant value). This is not optional; a kept row that isn't gated is an implementation defect.

`rememberReducedMotion()` is an **`expect/actual`** (like Rule 14), not a commonMain stub — a stub would *claim* accessibility compliance without honoring the OS setting. Provide all three actuals or the build breaks:

| Target | Source |
|--------|--------|
| android | `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (needs `androidContext()`) |
| ios | `UIAccessibility.isReduceMotionEnabled` |
| desktop | `false` (no system reduce-motion signal) |

So `XMotion.kt` holds the `@Composable expect fun rememberReducedMotion(): Boolean` (+ the duration/easing tokens), and `XMotion.android.kt` / `.ios.kt` / `.desktop.kt` hold the actuals.

### Durations & easings come from `XMotion` tokens

Never hardcode `tween(1730)` or a raw `CubicBezierEasing(...)` in feature/DS motion code — reference the `XMotion` tokens (`XMotion.SHIMMER`, `XMotion.Standard`, `XMotion.EaseOutExpo`, …). Same discipline as colors-through-M3-roles: one source, no per-feature drift. The design handoff's `## Motion` **Params** column states which token.

### Magnitudes come from the handoff, never invented

The animated value range (scale 1→1.2, translateY 30px→0, opacity 0→1, bg-position −200%→200%) is carried in the design handoff's `## Motion` **Magnitude** column. The handoff pins duration/easing/family **and** these magnitudes; the implementer copies them verbatim and must **not** guess scale factors / slide distances / glow amounts. Only `infer` (when the handoff couldn't record a keyframe) is an implementer judgment call.

---

## Code-Layout Rule (dedicated files, never inline)

Motion logic **always** lives in its own file — never inline in `{Feature}Screen.kt` or a component file. Two tiers:

- **Generic, verbatim-reusable primitives → `core/designsystem/.../motion/`** (generic DS tier; the kit installer keeps it, like `XTheme`/`XButton`). **These ship with the template DS — they already exist; the implementation skill does NOT recreate them.** The skill **verifies presence** and only writes a *genuinely new* generic primitive the shipped set lacks (rare). Each primitive takes the design's magnitude as a **parameter** (e.g. `PulseDot(scaleTo, minAlpha)`, `Modifier.shimmer(sweepFraction)`) — the implementer passes the design's captured magnitude; it is not baked in. Shipped DS primitives:
  - `XMotion.kt` — duration + easing tokens + `expect fun rememberReducedMotion()` (with `.android`/`.ios`/`.desktop` actuals)
  - `Modifier.shimmer(baseColor, highlightColor, sweepFraction)` — size-relative infinite `Brush` sweep
  - `PulseDot(color, dotSize, scaleTo, minAlpha)` — infinite scale + alpha dot
  - `AmbientMeshBackground(colors)` — drifting mesh-gradient backdrop
  - `BokehCanvas(color, particleCount)` — drifting particle field (`Canvas` + `withInfiniteAnimationFrameMillis`)
  - `Modifier.pulseGlow(color, minAlpha, maxAlpha, radiusFactor)` — infinite alpha-pulse glow disc
  - `RevealOnAppear(delayMillis, slideFraction)` — entrance fade + slide-up wrapper
- **Feature-specific wiring → `feature/{name}/src/commonMain/kotlin/.../presentation/ui/motion/{Feature}Motion.kt`** — the feature's one-off animated composables + specs (count-up for this screen, this chart's bar-grow, this segment/tab indicator). Keeps `Screen.kt` / component files lean.

**Generic-vs-feature split**: a primitive already in the shipped DS set above → reuse it (pass the magnitude param). Truly one-off (a specific count-up target, a chart's bar-grow) → feature `motion/`. Only add a **new** DS primitive when the shipped set genuinely lacks the family-level primitive.

### Imports are allowed (not Material3, not a Rule-5 violation)

`androidx.compose.animation.*`, `androidx.compose.animation.core.*`, and `androidx.compose.foundation.interaction.*` are **not** Material3 — using them is not a Rule-5 violation. Motion needs **no** asset download (unlike icons/fonts): it is pure Compose code in dedicated files. The animation APIs themselves are `commonMain` (build on android/ios/desktop with no per-platform `actual`); the **one** `expect/actual` is `rememberReducedMotion()`, which must read the OS setting per platform (see above).
