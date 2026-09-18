package dev.kmpapp.designsystem

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.kmpapp.common.setState
import kotlin.concurrent.Volatile

/**
 * Created by Ali Sadeghi
 * on 30,Jun,2025
 */

// 1. Create a Snackbar data class
data class SnackbarMessage(
    val message: String = "",
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onActionClick: (() -> Unit)? = null,
)

class SnackbarManager private constructor() {
    private val _snackbarMessage = MutableStateFlow(SnackbarMessage())
    val snackbarMessage = _snackbarMessage.asStateFlow()

    fun showMessage(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onActionClick: (() -> Unit)? = null,
    ) {
        _snackbarMessage.setState {
            copy(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
                onActionClick = onActionClick,
            )
        }
    }

    fun clearMessage() {
        _snackbarMessage.setState { SnackbarMessage() }
    }

    companion object {
        @Volatile
        private var INSTANCE: SnackbarManager? = null

        fun getInstance(): SnackbarManager = INSTANCE ?: SnackbarManager().also { INSTANCE = it }
    }
}

@Composable
fun SnackbarController(
    snackbarHostState: SnackbarHostState,
    snackbarManager: SnackbarManager = SnackbarManager.getInstance(),
) {
    val message by snackbarManager.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        if (message.message.isNotEmpty()) {
            val result =
                snackbarHostState.showSnackbar(
                    message = message.message,
                    actionLabel = message.actionLabel,
                    duration = message.duration,
                )

            if (result == SnackbarResult.ActionPerformed) {
                message.onActionClick?.invoke()
            }

            // Clear the message after it's been displayed
            snackbarManager.clearMessage()
        }
    }
}
