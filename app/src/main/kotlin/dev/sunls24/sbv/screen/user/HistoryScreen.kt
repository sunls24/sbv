package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.firstRowActionFocus
import dev.sunls24.sbv.viewmodel.user.HistoryViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    fallbackFocusRequester: FocusRequester,
    historyViewModel: HistoryViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val firstContentFocusRequester = remember { FocusRequester() }
    val focusRestorerFallback = if (historyViewModel.histories.isNotEmpty()) {
        firstContentFocusRequester
    } else {
        fallbackFocusRequester
    }

    LazyGridLoadMoreEffect(
        gridState = gridState,
        itemCount = historyViewModel.histories.size,
        onLoadMore = historyViewModel::update
    )

    TvLazyVerticalGrid(
        modifier = modifier.focusRestorer(
            fallback = focusRestorerFallback,
        ),
        state = gridState,
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(SBVSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
    ) {
        if (historyViewModel.histories.isNotEmpty()) {
            itemsIndexed(
                items = historyViewModel.histories,
                key = { _, history -> history.avid },
            ) { index, history ->
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    SmallVideoCard(
                        modifier = if (history.avid == historyViewModel.histories.firstOrNull()?.avid) {
                            Modifier.focusRequester(firstContentFocusRequester)
                        } else {
                            Modifier
                        },
                        actionModifier = Modifier.firstRowActionFocus(
                            index = index,
                            columns = 4,
                            focusRequester = fallbackFocusRequester,
                        ),
                        data = history,
                        onClick = {
                            VideoPlayerV3Activity.play(
                                context = context,
                                aid = history.avid,
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.addToView(history.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.showDetail(
                                context = context,
                                aid = history.avid,
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
