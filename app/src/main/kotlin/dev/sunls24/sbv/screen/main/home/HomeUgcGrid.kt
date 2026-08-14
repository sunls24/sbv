package dev.sunls24.sbv.screen.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.biliapi.entity.ugc.UgcItem
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.loadingTipItem
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toWanString

@Composable
internal fun HomeUgcGrid(
    items: List<UgcItem>,
    loading: Boolean,
    showNoMore: Boolean = false,
    onLoadMore: () -> Unit,
    onAddWatchLater: (Long) -> Unit,
    navFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val context = LocalContext.current

    LazyGridLoadMoreEffect(gridState, items.size, onLoadMore = onLoadMore)

    TvLazyVerticalGrid(
        modifier = modifier,
        state = gridState,
        columns = GridCells.Fixed(HOME_GRID_COLUMNS),
        contentPadding = PaddingValues(SBVSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SBVSpacing.md)
    ) {
        itemsIndexed(items = items, key = { _, item -> item.aid }) { index, item ->
            val gridFocusModifier = Modifier.homeGridFocus(index, items.size, navFocusRequester)
            SmallVideoCard(
                modifier = gridFocusModifier,
                actionModifier = Modifier.homeGridActionFocus(index, navFocusRequester),
                data = remember(item) {
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
                onGoToDetailPage = {
                    VideoInfoActivity.showDetail(
                        context = context,
                        aid = item.aid
                    )
                },
                onGoToUpPage = item.authorMid?.takeIf { it != 0L }?.let { mid ->
                    { UpInfoActivity.actionStart(context, mid, item.author) }
                }
            )
        }

        if (loading) {
            loadingTipItem()
        } else if (showNoMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "没有更多了捏",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
