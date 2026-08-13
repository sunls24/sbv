package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.video.SeasonInfoActivity
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SeasonCard
import dev.sunls24.sbv.entity.carddata.SeasonCardData
import dev.sunls24.sbv.util.ImageSize
import dev.sunls24.sbv.util.resizedImageUrl
import dev.sunls24.sbv.viewmodel.user.FollowingSeasonViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FollowingSeasonScreen(
    modifier: Modifier = Modifier,
    followingSeasonViewModel: FollowingSeasonViewModel = koinViewModel()
) {
    val context = LocalContext.current

    var showFilter by remember { mutableStateOf(false) }

    val followingSeasons = followingSeasonViewModel.followingSeasons
    val followingSeasonType = followingSeasonViewModel.followingSeasonType
    val followingSeasonStatus = followingSeasonViewModel.followingSeasonStatus
    val noMore = followingSeasonViewModel.noMore

    LaunchedEffect(Unit) {
        followingSeasonViewModel.ensureLoaded()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent {
                if (it.key == Key.Menu) {
                    if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                    showFilter = true
                    return@onKeyEvent true
                }
                false
            },
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(R.string.filter_dialog_open_tip),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TvLazyVerticalGrid(
            modifier = Modifier,
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (followingSeasons.isNotEmpty()) {
                itemsIndexed(items = followingSeasons) { index, followingSeason ->
                    SeasonCard(
                        data = SeasonCardData(
                            seasonId = followingSeason.seasonId,
                            title = followingSeason.title,
                            cover = followingSeason.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail),
                            rating = null
                        ),
                        onFocus = {
                            if (index + 30 > followingSeasons.size) {
                                followingSeasonViewModel.loadMore()
                            }
                        },
                        onClick = {
                            SeasonInfoActivity.actionStart(
                                context = context,
                                seasonId = followingSeason.seasonId
                            )
                        }
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyTip()
                }
            }

            if (followingSeasons.isEmpty() && noMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(onClick = { showFilter = true }) {
                            Text(text = stringResource(R.string.filter_dialog_open_tip_click))
                        }
                    }
                }
            }
        }
    }

    FollowingSeasonFilter(
        show = showFilter,
        onHideFilter = { showFilter = false },
        selectedType = followingSeasonType,
        selectedStatus = followingSeasonStatus,
        onSelectedTypeChange = { followingSeasonViewModel.updateFilter(type = it) },
        onSelectedStatusChange = { followingSeasonViewModel.updateFilter(status = it) }
    )
}
