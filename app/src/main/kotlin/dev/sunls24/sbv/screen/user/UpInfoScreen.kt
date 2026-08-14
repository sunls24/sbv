package dev.sunls24.sbv.screen.user

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.ui.effect.UiEffect
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.toast
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import dev.sunls24.sbv.viewmodel.user.UpInfoViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun UpSpaceScreen(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("mid")) {
            val mid = intent.getLongExtra("mid", 0)
            val name = intent.getStringExtra("name") ?: ""
            upInfoViewModel.upMid = mid
            upInfoViewModel.upName = name
            upInfoViewModel.update()
        } else {
            context.finish()
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    LazyGridLoadMoreEffect(
        gridState = gridState,
        itemCount = upInfoViewModel.spaceVideos.size,
        onLoadMore = upInfoViewModel::update
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(
                    start = SBVSpacing.xxxl,
                    top = SBVSpacing.xl,
                    bottom = SBVSpacing.sm,
                    end = SBVSpacing.xxxl
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = upInfoViewModel.upName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.load_data_count,
                                upInfoViewModel.spaceVideos.size
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedVisibility(visible = upInfoViewModel.noMore) {
                            Text(
                                text = stringResource(R.string.load_data_no_more),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        TvLazyVerticalGrid(
            modifier = Modifier.padding(innerPadding),
            columns = GridCells.Fixed(4),
            state = gridState,
            contentPadding = PaddingValues(SBVSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
            horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
        ) {
            if (upInfoViewModel.spaceVideos.isNotEmpty()) {
                itemsIndexed(
                    items = upInfoViewModel.spaceVideos,
                    key = { _, video -> video.avid }
                ) { _, video ->
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        SmallVideoCard(
                            data = video,
                            onClick = {
                                VideoPlayerV3Activity.play(
                                    context = context,
                                    aid = video.avid
                                )
                            },
                            onAddWatchLater = {
                                toViewViewModel.addToView(video.avid)
                            },
                            onGoToDetailPage = {
                                VideoInfoActivity.showDetail(
                                    context = context,
                                    aid = video.avid
                                )
                            },
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
}
