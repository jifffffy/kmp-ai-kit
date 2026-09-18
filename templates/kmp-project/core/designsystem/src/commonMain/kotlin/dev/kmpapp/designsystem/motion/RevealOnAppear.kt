package dev.kmpapp.designsystem.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Entrance — fade + slide-up on first composition. Reduced-motion ⇒ content shows immediately.
 *
 * @param delayMillis stagger offset for sequenced section reveals.
 * @param slideFraction slide distance as a fraction of the content height (size-relative).
 */
@Composable
fun RevealOnAppear(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    slideFraction: Float = 0.25f,
    content: @Composable () -> Unit,
) {
    if (rememberReducedMotion()) {
        content()
        return
    }
    val state = remember { MutableTransitionState(false) }
    state.targetState = true
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter =
            fadeIn(tween(XMotion.ENTRANCE, delayMillis = delayMillis)) +
                slideInVertically(
                    animationSpec = tween(XMotion.ENTRANCE, delayMillis = delayMillis, easing = XMotion.EaseOutExpo),
                    initialOffsetY = { (it * slideFraction).toInt() },
                ),
    ) {
        content()
    }
}
