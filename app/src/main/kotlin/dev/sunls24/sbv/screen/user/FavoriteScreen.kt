package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import dev.sunls24.biliapi.entity.FavoriteFolderMetadata
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.ifElse
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.firstRowActionFocus
import dev.sunls24.sbv.viewmodel.user.FavoriteViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    fallbackFocusRequester: FocusRequester,
    favoriteViewModel: FavoriteViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val currentTabFocusRequester = remember { FocusRequester() }
    val defaultFocusRequester = remember { FocusRequester() }
    val firstContentFocusRequester = remember { FocusRequester() }
    val lazyGridState = rememberLazyGridState()
    val focusRestorerFallback = if (favoriteViewModel.favorites.isNotEmpty()) {
        firstContentFocusRequester
    } else {
        fallbackFocusRequester
    }

    val currentTabIndex by remember {
        derivedStateOf {
            favoriteViewModel.favoriteFolderMetadataList.indexOf(favoriteViewModel.currentFavoriteFolderMetadata)
        }
    }

    val updateCurrentFavoriteFolder: (folderMetadata: FavoriteFolderMetadata) -> Unit =
        { folderMetadata ->
            favoriteViewModel.currentFavoriteFolderMetadata = folderMetadata
            favoriteViewModel.favorites.clear()
            favoriteViewModel.updateFolderItems(force = true)
        }

    LazyGridLoadMoreEffect(
        gridState = lazyGridState,
        itemCount = favoriteViewModel.favorites.size,
        onLoadMore = favoriteViewModel::updateFolderItems
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.Start

    ) {
        TabRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusRequester(defaultFocusRequester)
                .focusRestorer(focusRequester),
            selectedTabIndex = currentTabIndex,
            separator = { Spacer(modifier = Modifier.width(12.dp)) },
            indicator = { tabPositions, doesTabRowHaveFocus ->
                tabPositions.getOrNull(currentTabIndex)?.let { position ->
                    TabRowDefaults.UnderlinedIndicator(
                        currentTabPosition = position,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        ) {
            favoriteViewModel.favoriteFolderMetadataList.forEachIndexed { index, folderMetadata ->
                Tab(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester))
                        .ifElse(index == currentTabIndex, Modifier.focusRequester(currentTabFocusRequester)),
                    selected = currentTabIndex == index,
                    colors = TabDefaults.underlinedIndicatorTabColors(),
                    onFocus = {
                        if (favoriteViewModel.currentFavoriteFolderMetadata != folderMetadata) {
                            updateCurrentFavoriteFolder(folderMetadata)
                        }
                    },
                    onClick = { updateCurrentFavoriteFolder(folderMetadata) }
                ) {
                    Box(
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            text = folderMetadata.title,
                            color = LocalContentColor.current,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TvLazyVerticalGrid(
            modifier = modifier.focusRestorer(
                fallback = focusRestorerFallback,
            ),
            state = lazyGridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(SBVSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
            horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
        ) {
            if (favoriteViewModel.favorites.isNotEmpty()) {
                itemsIndexed(
                    items = favoriteViewModel.favorites,
                    key = { _, favorite -> favorite.avid })
                { index, favorite ->
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        SmallVideoCard(
                            modifier = if (favorite.avid == favoriteViewModel.favorites.firstOrNull()?.avid) {
                                Modifier.focusRequester(firstContentFocusRequester)
                            } else {
                                Modifier
                            },
                            actionModifier = Modifier.firstRowActionFocus(
                                index = index,
                                columns = 4,
                                focusRequester = currentTabFocusRequester,
                            ),
                            data = favorite,
                            onClick = {
                                VideoPlayerV3Activity.play(
                                    context = context,
                                    aid = favorite.avid,
                                )
                            },
                            onAddWatchLater = {
                                toViewViewModel.addToView(favorite.avid)
                            },
                            onGoToDetailPage = {
                                VideoInfoActivity.showDetail(
                                    context = context,
                                    aid = favorite.avid,
                                )
                            },
                            onGoToUpPage = favorite.upMid?.let {
                                { UpInfoActivity.actionStart(context, it, favorite.upName) }
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

}
