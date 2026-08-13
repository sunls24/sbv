package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.viewmodel.user.HistoryViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    LazyGridLoadMoreEffect(
        gridState = gridState,
        itemCount = historyViewModel.histories.size,
        onLoadMore = historyViewModel::update
    )

    TvLazyVerticalGrid(
        modifier = modifier,
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (historyViewModel.histories.isNotEmpty()) {
            itemsIndexed(historyViewModel.histories) { _, history ->
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    SmallVideoCard(
                        data = history,
                        onClick = {
                            VideoPlayerV3Activity.play(
                                context = context,
                                aid = history.avid,
                                epid = history.epId
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.addToView(history.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.showDetail(
                                context = context,
                                aid = history.avid,
                                epid = history.epId
                            )
                        },
                        onGoToUpPage = history.upMid?.let {
                            { UpInfoActivity.actionStart(context, it, history.upName) }
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
