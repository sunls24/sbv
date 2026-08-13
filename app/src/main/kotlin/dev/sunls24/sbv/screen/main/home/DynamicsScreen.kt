package dev.sunls24.sbv.screen.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LoadingTip
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toWanString
import dev.sunls24.sbv.viewmodel.home.DynamicViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DynamicsScreen(
    onAddWatchLater: (Long) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    dynamicViewModel: DynamicViewModel = koinViewModel()
) {
    val context = LocalContext.current

    LazyGridLoadMoreEffect(
        gridState = gridState,
        itemCount = dynamicViewModel.dynamicList.size,
        onLoadMore = dynamicViewModel::loadMore
    )

    if (dynamicViewModel.isLogin) {
        TvLazyVerticalGrid(
            modifier = modifier,
            state = gridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = dynamicViewModel.dynamicList,
                key = { index, _ -> index }
            ) { _, item ->
                SmallVideoCard(
                    data = remember(item) {         // `VideoCardData` 只在 item 变动时重建
                        VideoCardData(
                            avid = item.aid,
                            title = item.title,
                            cover = item.cover,
                            playString = item.play.takeIf { it != -1 }.toWanString(),
                            danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                            upName = item.author,
                            timeString = (item.duration * 1000L).formatHourMinSec(),
                            pubTime = item.pubTime
                        )
                    },
                    onClick = {
                        VideoPlayerV3Activity.play(
                            context = context,
                            aid = item.aid,
                            epid = item.epid
                        )
                    },
                    onAddWatchLater = {
                        onAddWatchLater(item.aid)
                    },
                    onGoToDetailPage = {
                        VideoInfoActivity.showDetail(
                            context = context,
                            aid = item.aid,
                            epid = item.epid,
                        )
                    },
                    onGoToUpPage = {
                        UpInfoActivity.actionStart(context, item.authorMid, item.author)
                    }
                )
            }

            if (dynamicViewModel.loading)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }

            if (!dynamicViewModel.hasMore)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "没有更多了捏",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "请先登录")
        }
    }
}
