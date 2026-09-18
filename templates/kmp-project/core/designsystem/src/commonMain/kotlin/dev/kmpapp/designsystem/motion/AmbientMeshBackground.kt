package dev.kmpapp.designsystem.motion

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Ambient bg — a slowly drifting mesh-gradient backdrop. Decorative; place behind screen content.
 * Size-relative: the gradient sweep is expressed in fractions of the element, so it scales to any
 * screen. Reduced-motion ⇒ a static gradient.
 */
@Composable
fun AmbientMeshBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "mesh")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(XMotion.AMBIENT, easing = XMotion.Linear),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "meshShift",
    )
    val pos = if (reduced) 0.5f else shift
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush =
                            Brush.linearGradient(
                                colors = colors,
                                start = Offset(pos * size.width, 0f),
                                end = Offset(pos * size.width + size.width, size.height),
                            ),
                    )
                },
    )
}
