package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun ToViewScreen(
    modifier: Modifier = Modifier,
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current

    // 按 playString 分组
    val (unwatched, watched) = toViewViewModel.histories.partition { it.timeString != "已看完" }

    TvLazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 未看完标题
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "未看完",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (unwatched.isNotEmpty()) {
            items(items = unwatched) { item ->
                Box(contentAlignment = Alignment.Center) {
                    SmallVideoCard(
                        data = item,
                        delToView = true,
                        onClick = {
                            VideoPlayerV3Activity.play(
                                context = context,
                                aid = item.avid,
                                epid = item.epId
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.delToView(item.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.showDetail(
                                context = context,
                                aid = item.avid,
                                epid = item.epId
                            )
                        },
                        onGoToUpPage = item.upMid?.let {
                            { UpInfoActivity.actionStart(context, it, item.upName) }
                        }
                    )
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyTip()
            }
        }

        // 已看完标题
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "已看完",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (watched.isNotEmpty()) {
            items(items = watched) { item ->
                Box(contentAlignment = Alignment.Center) {
                    SmallVideoCard(
                        data = item,
                        delToView = true,
                        onClick = {
                            VideoPlayerV3Activity.play(
                                context = context,
                                aid = item.avid,
                                epid = item.epId
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.delToView(item.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.showDetail(
                                context = context,
                                aid = item.avid,
                                epid = item.epId
                            )
                        },
                        onGoToUpPage = item.upMid?.let {
                            { UpInfoActivity.actionStart(context, it, item.upName) }
                        }
                    )
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyTip()
            }
        }
    }
}
