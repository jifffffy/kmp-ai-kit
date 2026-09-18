package dev.kmpapp.designsystem

/*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterialApi::class)
fun Modifier.xPullRefresh(
    state: XPullRefreshState,
    enabled: Boolean = true,
): Modifier {
    return pullRefresh(
        state = state.materialState,
        enabled = enabled,
    )
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun rememberXPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
): XPullRefreshState {
    val state =
        rememberPullRefreshState(
            refreshing = refreshing,
            onRefresh = onRefresh,
        )
    return remember(state) { XPullRefreshState(state) }
}

@OptIn(ExperimentalMaterialApi::class)
class XPullRefreshState internal constructor(internal val materialState: PullRefreshState)

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun XPullRefreshIndicator(
    refreshing: Boolean,
    state: XPullRefreshState,
    modifier: Modifier = Modifier,
) {
    PullRefreshIndicator(
        refreshing = refreshing,
        state = state.materialState,
        modifier = modifier,
    )
}*/
