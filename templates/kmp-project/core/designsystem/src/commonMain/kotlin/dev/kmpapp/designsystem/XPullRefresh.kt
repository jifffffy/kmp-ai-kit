package dev.kmpapp.designsystem

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Design-system pull-to-refresh container.
 *
 * A thin, generic wrapper over Material 3's [PullToRefreshBox] so feature code never
 * touches `androidx.compose.material3.pulltorefresh` directly (Rule 5). The caller owns
 * the refresh state (typically an `isRefreshing` flag on its `*UiModel`) and passes the
 * refresh action down as a callback.
 *
 * Usage:
 * ```
 * XPullRefreshBox(
 *     refreshing = uiModel.isRefreshing,
 *     onRefresh = onRefresh,
 *     modifier = Modifier.fillMaxSize(),
 * ) {
 *     // scrollable content
 * }
 * ```
 *
 * @param refreshing whether a refresh is currently in flight; drives the indicator.
 * @param onRefresh invoked when the user pulls past the trigger threshold.
 * @param modifier applied to the container (the container already fills its content).
 * @param indicator the indicator slot; defaults to the themed M3 indicator.
 * @param content the scrollable content being refreshed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XPullRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicator: @Composable BoxScope.() -> Unit = {
        PullToRefreshDefaults.Indicator(
            state = rememberPullToRefreshState(),
            isRefreshing = refreshing,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    },
    content: @Composable BoxScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        indicator = indicator,
        content = content,
    )
}
