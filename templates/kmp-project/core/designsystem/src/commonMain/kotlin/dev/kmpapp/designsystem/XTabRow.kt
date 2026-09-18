package dev.kmpapp.designsystem

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun XPrimaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        scrollState = scrollState,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        divider = divider,
        tabs = tabs,
    )
}
