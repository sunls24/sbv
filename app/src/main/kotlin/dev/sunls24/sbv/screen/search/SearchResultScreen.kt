package dev.sunls24.sbv.screen.search

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.biliapi.repositories.SearchType
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.video.SeasonInfoActivity
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LoadingTip
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.SearchTypeTopNavItem
import dev.sunls24.sbv.component.TopNav
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SeasonCard
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.screen.user.EmptyTip
import dev.sunls24.sbv.screen.user.UpCard
import dev.sunls24.sbv.ui.effect.UiEffect
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.focusedScale
import dev.sunls24.sbv.util.firstRowActionFocus
import dev.sunls24.sbv.util.requestFocus
import dev.sunls24.sbv.util.toast
import dev.sunls24.sbv.viewmodel.search.SearchLoadState
import dev.sunls24.sbv.viewmodel.search.SearchResultUiItem
import dev.sunls24.sbv.viewmodel.search.SearchResultViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchResultScreen(
    modifier: Modifier = Modifier,
    keyword: String,
    onExit: () -> Unit,
    searchResultViewModel: SearchResultViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val videoGridState = rememberLazyGridState()
    val bangumiGridState = rememberLazyGridState()
    val filmGridState = rememberLazyGridState()
    val userGridState = rememberLazyGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabRowFocusRequester = remember { FocusRequester() }

    val searchResult = searchResultViewModel.result(searchResultViewModel.searchType)
    val loadState = searchResultViewModel.loadState(searchResult.type)
    val rowSize = when (searchResultViewModel.searchType) {
        SearchType.Video -> 4
        SearchType.MediaBangumi, SearchType.MediaFt -> 6
        SearchType.BiliUser -> 3
    }
    val gridState = when (searchResultViewModel.searchType) {
        SearchType.Video -> videoGridState
        SearchType.MediaBangumi -> bangumiGridState
        SearchType.MediaFt -> filmGridState
        SearchType.BiliUser -> userGridState
    }
    var focusOnContent by remember { mutableStateOf(false) }

    val onClickResult: (SearchResultUiItem) -> Unit = { resultItem ->
        when (resultItem) {
            is SearchResultUiItem.Video -> {
                VideoPlayerV3Activity.play(
                    context = context,
                    aid = resultItem.aid
                )
            }

            is SearchResultUiItem.Pgc -> {
                SeasonInfoActivity.actionStart(
                    context = context,
                    seasonId = resultItem.card.seasonId
                )
            }

            is SearchResultUiItem.User -> {
                UpInfoActivity.actionStart(
                    context = context,
                    mid = resultItem.mid,
                    name = resultItem.name
                )
            }

        }
    }

    LaunchedEffect(keyword) {
        if (keyword.isBlank()) {
            onExit()
        } else {
            searchResultViewModel.updateKeyword(keyword)
            tabRowFocusRequester.requestFocus(scope)
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

    LaunchedEffect(searchResultViewModel.searchType) {
        searchResultViewModel.ensureLoaded(searchResultViewModel.searchType)
    }

    LazyGridLoadMoreEffect(
        gridState = gridState,
        itemCount = searchResult.count,
        preloadCount = rowSize * 2,
        contentKey = searchResult.type,
        onLoadMore = { searchResultViewModel.loadMore(searchResult.type) }
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(
                    start = SBVSpacing.xl,
                    top = SBVSpacing.lg,
                    bottom = SBVSpacing.sm,
                    end = SBVSpacing.xl,
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = searchResultViewModel.keyword,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.load_data_count, searchResult.count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    ) { innerPadding ->
        BackHandler(focusOnContent) {
            tabRowFocusRequester.requestFocus(scope)
        }
        BackHandler(enabled = !focusOnContent) {
            onExit()
        }

        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            TopNav(
                focusRequester = tabRowFocusRequester,
                items = SearchTypeTopNavItem.entries,
                onSelectedChanged = { nav ->
                    when (nav) {
                        SearchTypeTopNavItem.Video -> searchResultViewModel.searchType =
                            SearchType.Video

                        SearchTypeTopNavItem.MediaBangumi -> searchResultViewModel.searchType =
                            SearchType.MediaBangumi

                        SearchTypeTopNavItem.MediaFt -> searchResultViewModel.searchType =
                            SearchType.MediaFt

                        SearchTypeTopNavItem.BiliUser -> searchResultViewModel.searchType =
                            SearchType.BiliUser
                    }
                },
                onClick = { }
            )

            Spacer(modifier = Modifier.height(SBVSpacing.sm))

            TvLazyVerticalGrid(
                modifier = Modifier
                    .onFocusChanged { focusOnContent = it.hasFocus },
                state = gridState,
                columns = GridCells.Fixed(rowSize),
                contentPadding = PaddingValues(SBVSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
            ) {
                itemsIndexed(
                    items = searchResult.items,
                    key = { _, item -> item.key },
                    contentType = { _, item -> item.contentType },
                ) { index, searchResultItem ->
                    SearchResultListItem(
                        actionModifier = Modifier.firstRowActionFocus(
                            index = index,
                            columns = rowSize,
                            focusRequester = tabRowFocusRequester,
                        ),
                        searchResult = searchResultItem,
                        onClick = { onClickResult(searchResultItem) },
                        onAddWatchLater = { aid ->
                            toViewViewModel.addToView(aid)
                        },
                        onGoToDetailPage = { aid ->
                            VideoInfoActivity.showDetail(
                                context = context,
                                aid = aid
                            )
                        },
                        onGoToUpPage = { mid, upName ->
                            UpInfoActivity.actionStart(context, mid, upName)
                        }
                    )
                }

                when (loadState) {
                    SearchLoadState.Loading -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingTip()
                            }
                        }
                    }

                    SearchLoadState.Error -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                onClick = {
                                    searchResultViewModel.loadMore(searchResult.type)
                                }
                            ) {
                                EmptyTip(text = stringResource(R.string.load_failed_retry))
                            }
                        }
                    }

                    SearchLoadState.Idle -> {
                        if (searchResult.count == 0) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyTip()
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun SearchResultListItem(
    modifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier,
    searchResult: SearchResultUiItem,
    onClick: () -> Unit,
    onAddWatchLater: ((Long) -> Unit),
    onGoToDetailPage: ((Long) -> Unit),
    onGoToUpPage: ((Long, String) -> Unit),
) {
    when (searchResult) {
        is SearchResultUiItem.Video -> {
            SmallVideoCard(
                modifier = modifier,
                actionModifier = actionModifier,
                data = searchResult.card,
                onClick = onClick,
                onAddWatchLater = { onAddWatchLater(searchResult.aid) },
                onGoToDetailPage = { onGoToDetailPage(searchResult.aid) },
                onGoToUpPage = { onGoToUpPage(searchResult.mid, searchResult.author) }
            )
        }

        is SearchResultUiItem.Pgc -> {
            SeasonCard(
                modifier = modifier,
                data = searchResult.card,
                onClick = onClick,
                onFocus = {}
            )
        }

        is SearchResultUiItem.User -> {
            UpCard(
                modifier = modifier.focusedScale(0.95f),
                face = searchResult.avatar,
                sign = searchResult.sign,
                username = searchResult.name,
                onFocusChange = { },
                onClick = onClick
            )
        }

    }
}

fun SearchType.getDisplayName(context: Context) = when (this) {
    SearchType.Video -> context.getString(R.string.search_result_type_name_video)
    SearchType.MediaBangumi -> context.getString(R.string.search_result_type_name_media_bangumi)
    SearchType.MediaFt -> context.getString(R.string.search_result_type_name_media_ft)
    SearchType.BiliUser -> context.getString(R.string.search_result_type_name_bili_user)
}
