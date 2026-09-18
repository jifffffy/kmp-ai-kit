package dev.kmpapp.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable

/**
 * App-global motion tokens (durations in ms + easings). Generic DS tier — reused by every feature's
 * motion code. See `.claude/skills/_shared/motion.md`.
 */
object XMotion {
    // Durations (ms)
    const val SHIMMER = 2000
    const val PULSE = 2000
    const val GLOW = 2000
    const val FLOAT = 3000
    const val ENTRANCE = 800
    const val AMBIENT = 15000
    const val VALUE = 1500

    // Easings (mapped from the verified Stitch vocabulary)
    val Standard: Easing = FastOutSlowInEasing // cubic-bezier(.4,0,.2,1)
    val Linear: Easing = LinearEasing
    val EaseOutExpo: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f) // slide-up
    val Overshoot: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) // progress spring
}

/**
 * Honors `prefers-reduced-motion`. When `true`, callers MUST skip the animation and jump to the
 * end/target state (no loop, no entrance slide, instant value).
 *
 * `expect/actual` (not a stub) so it actually reads the OS setting per platform:
 * - android: `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`
 * - ios: `UIAccessibility.isReduceMotionEnabled`
 * - desktop: `false` (no system reduce-motion signal)
 */
@Composable
expect fun rememberReducedMotion(): Boolean
