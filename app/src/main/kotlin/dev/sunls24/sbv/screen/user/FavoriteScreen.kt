package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
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
import dev.sunls24.sbv.viewmodel.user.FavoriteViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    favoriteViewModel: FavoriteViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val defaultFocusRequester = remember { FocusRequester() }
    val lazyGridState = rememberLazyGridState()

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
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
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
                        modifier = Modifier.height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            text = folderMetadata.title,
                            color = LocalContentColor.current,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TvLazyVerticalGrid(
            modifier = modifier,
            state = lazyGridState,
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (favoriteViewModel.favorites.isNotEmpty()) {
                items(
                    items = favoriteViewModel.favorites,
                    key = { favorite -> favorite.avid })
                { favorite ->
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        SmallVideoCard(
                            data = favorite,
                            onClick = {
                                VideoPlayerV3Activity.play(
                                    context = context,
                                    aid = favorite.avid,
                                    epid = favorite.epId,
                                )
                            },
                            onAddWatchLater = {
                                toViewViewModel.addToView(favorite.avid)
                            },
                            onGoToDetailPage = {
                                VideoInfoActivity.showDetail(
                                    context = context,
                                    aid = favorite.avid,
                                    epid = favorite.epId,
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
