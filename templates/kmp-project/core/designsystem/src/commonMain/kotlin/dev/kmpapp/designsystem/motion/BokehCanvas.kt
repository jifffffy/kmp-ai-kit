package dev.kmpapp.designsystem.motion

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient bg — drifting "bokeh" particle field (the design's `#bokeh-canvas` JS sim).
 *
 * JS-driver-sourced (`getContext('2d')` + `Math.random`), so it has **no CSS keyframes** — the
 * extractor flags it only as a JS hint and there is **no captured magnitude**. The particle
 * count / size / speed below are sensible defaults, not design tokens. Reduced-motion ⇒ a static
 * particle field (no drift).
 */
@Composable
fun BokehCanvas(
    color: Color,
    modifier: Modifier = Modifier,
    particleCount: Int = 24,
) {
    val reduced = rememberReducedMotion()
    val particles =
        remember(particleCount) {
            List(particleCount) {
                Particle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    radius = Random.nextFloat() * 6f + 2f,
                    speed = Random.nextFloat() * 0.4f + 0.1f,
                    phase = Random.nextFloat() * 6.2832f,
                    alpha = Random.nextFloat() * 0.4f + 0.1f,
                )
            }
        }
    val timeMs by produceState(0L) {
        if (!reduced) {
            while (true) {
                withInfiniteAnimationFrameMillis { value = it }
            }
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val t = timeMs / 1000f
        particles.forEach { p ->
            val driftY = if (reduced) p.y else (p.y - t * p.speed * 0.05f).mod(1f)
            val wobbleX = if (reduced) p.x else p.x + 0.02f * sin(t * p.speed + p.phase)
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.radius,
                center = Offset(wobbleX * size.width, driftY * size.height),
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val alpha: Float,
)
