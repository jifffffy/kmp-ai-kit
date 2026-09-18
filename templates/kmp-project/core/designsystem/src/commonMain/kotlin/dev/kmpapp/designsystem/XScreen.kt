package dev.kmpapp.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Feature screen container (Rule 13 — single app-shell Scaffold).
 *
 * The one [androidx.compose.material3.Scaffold] in the app lives in `App.kt` and owns ALL window
 * insets. `XScreen` is a plain `Column { topBar(); content (weight 1f); bottomBar() }` and
 * intentionally touches **no** window insets: the shell pads the NavHost, and `XTopAppBar` already
 * declares `WindowInsets(0,0,0,0)`. Nesting a `Scaffold`/`XScaffold` inside a feature would
 * re-read and double-count safe-area / navigation-bar padding — so feature screens always use
 * `XScreen`, never `XScaffold`.
 *
 * @param topBar usually an [dev.kmpapp.designsystem.toolbar.XTopAppBar].
 * @param bottomBar optional sticky CTA (NOT the app-shell tab navigation bar, which lives in `App.kt`).
 * @param content fills the `weight(1f)` region between the bars; receives no `PaddingValues`.
 */
@Composable
fun XScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(containerColor),
        ) {
            topBar()
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                content = content,
            )
            bottomBar()
        }
    }
}
