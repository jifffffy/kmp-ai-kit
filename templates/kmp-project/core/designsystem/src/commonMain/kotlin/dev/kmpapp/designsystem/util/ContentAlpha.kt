package dev.kmpapp.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * Created by Ali Sadeghi
 * on 30,Jun,2025
 */

object ContentAlpha {
    const val high = 1.00f
    const val medium = 0.60f
    const val disabled = 0.38f
}

val LocalContentAlpha = compositionLocalOf { ContentAlpha.high }

@Composable
fun ProvideContentAlpha(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled

    CompositionLocalProvider(LocalContentAlpha provides alpha) {
        content()
    }
}
