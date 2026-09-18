package dev.kmpapp.designsystem

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// source: https://stackoverflow.com/a/77431285/2389923
@Composable
inline fun LazyGridState.ScrollEndCallback(crossinline callback: () -> Unit) {
    LaunchedEffect(key1 = this) {
        snapshotFlow { layoutInfo }
            .filter { it.totalItemsCount > 0 }
            .map { it.totalItemsCount == (it.visibleItemsInfo.lastOrNull()?.index ?: -1).inc() }
            .distinctUntilChanged()
            .filter { it }
            .onEach { callback() }
            .collect()
    }
}

@Composable
inline fun LazyListState.ScrollEndCallback(crossinline callback: () -> Unit) {
    LaunchedEffect(key1 = this) {
        snapshotFlow { layoutInfo }
            .filter { it.totalItemsCount > 0 }
            .map { it.totalItemsCount == (it.visibleItemsInfo.lastOrNull()?.index ?: -1).inc() }
            .distinctUntilChanged()
            .filter { it }
            .onEach { callback() }
            .collect()
    }
}
