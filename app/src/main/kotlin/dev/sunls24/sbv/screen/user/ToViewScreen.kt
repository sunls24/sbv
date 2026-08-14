package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.firstRowActionFocus
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun ToViewScreen(
    modifier: Modifier = Modifier,
    fallbackFocusRequester: FocusRequester,
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val firstContentFocusRequester = remember { FocusRequester() }

    // 按 playString 分组
    val (unwatched, watched) = toViewViewModel.histories.partition { it.timeString != "已看完" }
    val firstContentAid = (unwatched.firstOrNull() ?: watched.firstOrNull())?.avid
    val hasContent = firstContentAid != null
    val focusRestorerFallback = if (hasContent) {
        firstContentFocusRequester
    } else {
        fallbackFocusRequester
    }
    TvLazyVerticalGrid(
        modifier = modifier.focusRestorer(
            fallback = focusRestorerFallback,
        ),
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(SBVSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
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
            itemsIndexed(items = unwatched, key = { _, item -> "unwatched:${item.avid}" }) { index, item ->
                Box(contentAlignment = Alignment.Center) {
                    SmallVideoCard(
                        modifier = if (item.avid == firstContentAid) {
                            Modifier.focusRequester(firstContentFocusRequester)
                        } else {
                            Modifier
                        },
                        actionModifier = Modifier.firstRowActionFocus(
                            index = index,
                            columns = 4,
                            focusRequester = fallbackFocusRequester,
                        ),
                        data = item,
                        delToView = true,
                        onClick = {
                            VideoPlayerV3Activity.play(
                                context = context,
                                aid = item.avid,
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.delToView(item.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.showDetail(
                                context = context,
                                aid = item.avid,
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
            itemsIndexed(items = watched, key = { _, item -> "watched:${item.avid}" }) { index, item ->
                Box(contentAlignment = Alignment.Center) {
                    SmallVideoCard(
                        modifier = if (item.avid == firstContentAid) {
                            Modifier.focusRequester(firstContentFocusRequester)
                        } else {
                            Modifier
                        },
                        actionModifier = if (unwatched.isEmpty()) {
                            Modifier.firstRowActionFocus(
                                index = index,
                                columns = 4,
                                focusRequester = fallbackFocusRequester,
                            )
                        } else {
                            Modifier
                        },
                        data = item,
                        delToView = true,
                        onClick = {
                            VideoPlayerV3Activity.play(
                                context = context,
                                aid = item.avid,
                            )
                        },
                        onAddWatchLater = {
                            toViewViewModel.delToView(item.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.showDetail(
                                context = context,
                                aid = item.avid,
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
