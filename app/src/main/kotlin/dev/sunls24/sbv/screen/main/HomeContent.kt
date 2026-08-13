package dev.sunls24.sbv.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import dev.sunls24.sbv.component.HomeTopNavItem
import dev.sunls24.sbv.screen.main.home.DynamicsScreen
import dev.sunls24.sbv.screen.main.home.HomeUgcGrid
import dev.sunls24.sbv.viewmodel.UserViewModel
import dev.sunls24.sbv.viewmodel.home.DynamicViewModel
import dev.sunls24.sbv.viewmodel.home.PopularViewModel
import dev.sunls24.sbv.viewmodel.home.RecommendViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeContent(
    navFocusRequester: FocusRequester,
    contentFocusRequester: FocusRequester,
    selectedTab: HomeTopNavItem,
    recommendViewModel: RecommendViewModel = koinViewModel(),
    popularViewModel: PopularViewModel = koinViewModel(),
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val isLogin by userViewModel.isLoginFlow.collectAsState()
    val recommendGridState = rememberLazyGridState()
    val popularGridState = rememberLazyGridState()
    val dynamicsGridState = rememberLazyGridState()

    fun loadTabData(tab: HomeTopNavItem) {
        if (tab == HomeTopNavItem.Dynamics && !isLogin) return
        when (tab) {
            HomeTopNavItem.Recommend -> recommendViewModel.ensureLoaded()
            HomeTopNavItem.Popular -> popularViewModel.ensureLoaded()
            HomeTopNavItem.Dynamics -> dynamicViewModel.ensureLoaded()
        }
    }

    fun refreshPageData(tab: HomeTopNavItem) {
        when (tab) {
            HomeTopNavItem.Recommend -> recommendViewModel.refresh()
            HomeTopNavItem.Popular -> popularViewModel.refresh()
            HomeTopNavItem.Dynamics -> dynamicViewModel.refresh()
        }
    }

    LaunchedEffect(selectedTab) {
        loadTabData(selectedTab)
    }

    LaunchedEffect(isLogin) {
        if (isLogin) {
            userViewModel.updateUserInfo()
            if (selectedTab == HomeTopNavItem.Dynamics) loadTabData(selectedTab)
        } else {
            dynamicViewModel.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent {
                if (it.key == Key.Menu) {
                    if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                    refreshPageData(selectedTab)
                    navFocusRequester.requestFocus()
                    return@onPreviewKeyEvent true
                }
                return@onPreviewKeyEvent false
            },
    ) {
        when (selectedTab) {
            HomeTopNavItem.Recommend -> HomeUgcGrid(
                modifier = Modifier.focusRequester(contentFocusRequester),
                gridState = recommendGridState,
                items = recommendViewModel.recommendVideoList,
                loading = recommendViewModel.loading,
                onLoadMore = recommendViewModel::loadMore,
                onAddWatchLater = toViewViewModel::addToView,
            )

            HomeTopNavItem.Popular -> HomeUgcGrid(
                modifier = Modifier.focusRequester(contentFocusRequester),
                gridState = popularGridState,
                items = popularViewModel.popularVideoList,
                loading = popularViewModel.loading,
                showNoMore = !popularViewModel.hasMore,
                onLoadMore = popularViewModel::loadMore,
                onAddWatchLater = toViewViewModel::addToView,
            )

            HomeTopNavItem.Dynamics -> DynamicsScreen(
                modifier = Modifier.focusRequester(contentFocusRequester),
                gridState = dynamicsGridState,
                onAddWatchLater = toViewViewModel::addToView,
                dynamicViewModel = dynamicViewModel,
            )
        }
    }
}
