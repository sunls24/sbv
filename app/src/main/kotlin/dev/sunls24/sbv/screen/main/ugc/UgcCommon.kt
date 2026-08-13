package dev.sunls24.sbv.screen.main.ugc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LoadingTip
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toWanString
import dev.sunls24.sbv.viewmodel.ugc.UgcRegionState

@Composable
fun UgcRegionScaffold(
    modifier: Modifier = Modifier,
    state: UgcRegionState,
    gridState: LazyGridState,
    onLoadMore: () -> Unit,
    onAddWatchLater: ((Long) -> Unit),
    onGoToDetailPage: ((Long) -> Unit),
    onGoToUpPage: ((Long, String) -> Unit),
) {
    val context = LocalContext.current


    LazyGridLoadMoreEffect(
        gridState = gridState,
        itemCount = state.items.size,
        preloadCount = 1,
        onLoadMore = onLoadMore
    )

    TvLazyVerticalGrid(
        modifier = modifier,
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 用index的话快速刷新有概率闪退
        items(state.items) {item ->
            SmallVideoCard(
                data = remember(item) {         // `VideoCardData` 只在 item 变动时重建
                    VideoCardData(
                        avid = item.aid,
                        title = item.title,
                        cover = item.cover,
                        playString = item.play.takeIf { it != -1 }.toWanString(),
                        danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        upName = item.author,
                        pubTime = item.pubTime
                    )
                },
                onClick = { VideoPlayerV3Activity.play(context, item.aid) },
                onAddWatchLater = { onAddWatchLater(item.aid) },
                onGoToDetailPage = { onGoToDetailPage(item.aid) },
                onGoToUpPage = item.authorMid?.let {
                    { onGoToUpPage(it, item.author) }
                }
            )
        }

        if (state.updating) {
            item(span = { GridItemSpan(maxLineSpan) }) {    // 网格里占整行
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) { LoadingTip() }
            }
        }
    }
}
