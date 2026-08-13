package dev.sunls24.sbv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.sunls24.sbv.entity.VideoListItem

@Composable
fun VideoListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    currentCid: Long,
    videoList: List<VideoListItem>,
    onPlayNewVideo: (VideoListItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            val currentIndex = videoList.indexOfFirst { it.cid == currentCid }
            if (currentIndex != -1) {
                listState.animateScrollToItem(currentIndex)
                focusRequester.requestFocus()
            }
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Surface(
            modifier = modifier,
            colors = SurfaceDefaults.colors(
                containerColor = Color.Black.copy(alpha = 0.5f)
            )
        ) {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 60.dp)
                ) {
                    items(
                        items = videoList,
                        key = { it.cid }
                    ) { video ->
                        val isSelected = video.cid == currentCid
                        DenseListItem(
                            modifier = if (isSelected) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            },
                            selected = isSelected,
                            onClick = { if (!isSelected) onPlayNewVideo(video) },
                            headlineContent = {
                                Text(
                                    text = video.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
