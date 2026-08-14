package dev.sunls24.sbv.component

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun LazyGridLoadMoreEffect(
    gridState: LazyGridState,
    itemCount: Int,
    preloadCount: Int = 8,
    contentKey: Any? = Unit,
    onLoadMore: () -> Unit
) {
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(gridState, preloadCount, contentKey) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to currentItemCount
        }
            .distinctUntilChanged()
            .filter { (lastVisibleIndex, count) ->
                count > 0 &&
                    lastVisibleIndex != null &&
                    lastVisibleIndex >= count - preloadCount
            }
            .collect { currentOnLoadMore() }
    }
}
