package dev.kmpapp.designsystem.motion

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Loading loop — an infinite shimmer sweep across the element's background (skeleton placeholders).
 * Size-relative: the highlight band is a fraction of the element width, so it works at any size.
 * Reduced-motion ⇒ a flat [baseColor] background, no animation.
 *
 * @param sweepFraction width of the moving highlight band as a fraction of the element width.
 */
fun Modifier.shimmer(
    baseColor: Color,
    highlightColor: Color,
    sweepFraction: Float = 0.6f,
): Modifier =
    composed {
        if (rememberReducedMotion()) {
            return@composed background(baseColor)
        }
        val transition = rememberInfiniteTransition(label = "shimmer")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(XMotion.SHIMMER, easing = XMotion.Linear),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmerProgress",
        )
        drawWithCache {
            val sweep = size.width * sweepFraction
            // travel from fully off the left edge to fully off the right edge
            val x = progress * (size.width + 2f * sweep) - sweep
            val brush =
                Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(x, 0f),
                    end = Offset(x + sweep, 0f),
                )
            onDrawBehind { drawRect(brush) }
        }
    }
