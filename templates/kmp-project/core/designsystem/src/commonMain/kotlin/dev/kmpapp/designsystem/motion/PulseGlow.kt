package dev.kmpapp.designsystem.motion

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

/**
 * Attention loop — an infinite glow pulse drawn behind the element (e.g. a primary CTA).
 * A flat alpha-pulsing disc — a visual approximation of a CSS `box-shadow` glow, not a true blur.
 * Reduced-motion ⇒ no glow.
 *
 * @param minAlpha / [maxAlpha] glow opacity range (the design's captured box-shadow pulse).
 * @param radiusFactor glow radius as a multiple of the element's max dimension.
 */
fun Modifier.pulseGlow(
    color: Color,
    minAlpha: Float = 0.25f,
    maxAlpha: Float = 0.9f,
    radiusFactor: Float = 0.72f,
): Modifier =
    composed {
        if (rememberReducedMotion()) {
            return@composed this
        }
        val transition = rememberInfiniteTransition(label = "glow")
        val glowAlpha by transition.animateFloat(
            initialValue = minAlpha,
            targetValue = maxAlpha,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(XMotion.GLOW, easing = XMotion.Standard),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glowAlpha",
        )
        drawBehind {
            drawCircle(
                color = color.copy(alpha = glowAlpha),
                radius = size.maxDimension * radiusFactor,
            )
        }
    }
