package dev.kmpapp.designsystem.motion

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Attention loop — a pulsing dot (scale + alpha), e.g. a live-status indicator.
 * Reduced-motion ⇒ a static dot.
 *
 * @param scaleTo peak scale of the pulse (the design's captured magnitude; e.g. `1.2` for scale 1→1.2).
 * @param minAlpha trough alpha of the pulse (e.g. `0.5` for opacity 1→0.5).
 */
@Composable
fun PulseDot(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    scaleTo: Float = 1.2f,
    minAlpha: Float = 0.5f,
) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduced) 1f else scaleTo,
        animationSpec =
            infiniteRepeatable(
                animation = tween(XMotion.PULSE, easing = XMotion.Standard),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseScale",
    )
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduced) 1f else minAlpha,
        animationSpec =
            infiniteRepeatable(
                animation = tween(XMotion.PULSE, easing = XMotion.Standard),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseAlpha",
    )
    Box(
        modifier =
            modifier
                .size(dotSize)
                .scale(scale)
                .alpha(dotAlpha)
                .background(color, CircleShape),
    )
}
