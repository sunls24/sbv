package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import dev.sunls24.sbv.activities.video.SeasonInfoActivity
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SeasonCard
import dev.sunls24.sbv.entity.carddata.SeasonCardData
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.viewmodel.user.FollowingSeasonViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FollowingSeasonScreen(
    modifier: Modifier = Modifier,
    fallbackFocusRequester: FocusRequester,
    followingSeasonViewModel: FollowingSeasonViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val firstContentFocusRequester = remember { FocusRequester() }

    val followingSeasons = followingSeasonViewModel.followingSeasons
    val focusRestorerFallback = if (followingSeasons.isNotEmpty()) {
        firstContentFocusRequester
    } else {
        fallbackFocusRequester
    }

    LaunchedEffect(Unit) {
        followingSeasonViewModel.ensureLoaded()
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        TvLazyVerticalGrid(
            modifier = Modifier.focusRestorer(
                fallback = focusRestorerFallback,
            ),
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(SBVSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
            horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
        ) {
            if (followingSeasons.isNotEmpty()) {
                itemsIndexed(
                    items = followingSeasons,
                    key = { _, season -> season.seasonId },
                ) { index, followingSeason ->
                    SeasonCard(
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstContentFocusRequester)
                        } else {
                            Modifier
                        },
                        data = SeasonCardData(
                            seasonId = followingSeason.seasonId,
                            title = followingSeason.title,
                            cover = followingSeason.cover,
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
    }
}
}
