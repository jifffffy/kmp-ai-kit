package dev.kmpapp.designsystem

/**
 * Created by Ali Sadeghi
 * on 01,May,2025
 */

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class ToastState {
    private var _isVisible by mutableStateOf(false)
    private var _message by mutableStateOf("")
    private var _duration by mutableStateOf(ToastDuration.SHORT)

    val isVisible: Boolean get() = _isVisible
    val message: String get() = _message
    val duration: ToastDuration get() = _duration

    fun show(
        message: String,
        duration: ToastDuration = ToastDuration.SHORT,
    ) {
        _message = message
        _duration = duration
        _isVisible = true
    }

    fun hide() {
        _isVisible = false
    }
}

enum class ToastDuration(
    val durationMs: Long,
) {
    SHORT(2000),
    LONG(3500),
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

@Composable
fun Toast(
    state: ToastState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xE6333333),
    contentColor: Color = Color.White,
) {
    val alpha by animateFloatAsState(
        targetValue = if (state.isVisible) 1f else 0f,
        label = "toast_alpha",
    )

    if (state.isVisible) {
        LaunchedEffect(state.isVisible, state.message) {
            delay(state.duration.durationMs)
            state.hide()
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(bottom = 64.dp, start = 16.dp, end = 16.dp)
                        .alpha(alpha)
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(8.dp),
                        ).padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = state.message,
                    color = contentColor,
                )
            }
        }
    }
}
