package dev.sunls24.sbv.screen.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import dev.sunls24.sbv.component.PersonalTopNavItem
import dev.sunls24.sbv.component.TopNav
import dev.sunls24.sbv.component.TopNavIndicatorStyle
import dev.sunls24.sbv.screen.user.FavoriteScreen
import dev.sunls24.sbv.screen.user.FollowingSeasonScreen
import dev.sunls24.sbv.screen.user.HistoryScreen
import dev.sunls24.sbv.screen.user.ToViewScreen
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.viewmodel.UserViewModel
import dev.sunls24.sbv.viewmodel.user.FavoriteViewModel
import dev.sunls24.sbv.viewmodel.user.FollowingSeasonViewModel
import dev.sunls24.sbv.viewmodel.user.HistoryViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PersonalContent(
    navFocusRequester: FocusRequester,
    favouriteViewModel: FavoriteViewModel = koinViewModel(),
    historyViewModel: HistoryViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
    followingSeasonViewModel: FollowingSeasonViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val isLogin by userViewModel.isLoginFlow.collectAsState()
    var focusOnContent by remember { mutableStateOf(false) }

    val firstTab = remember { Prefs.firstPersonalTopNavItem }
    var selectedTab by remember { mutableStateOf(firstTab) }

    val reorderedItems = remember {
        val allItems = PersonalTopNavItem.entries
        val startIndex = allItems.indexOf(firstTab)
        allItems.drop(startIndex) + allItems.take(startIndex)
    }

    fun refreshPageData(nav: PersonalTopNavItem) {
        if (!isLogin) return
        when (nav) {
            PersonalTopNavItem.ToView -> {
                toViewViewModel.clearData()
                toViewViewModel.update()
            }

            PersonalTopNavItem.History -> {
                historyViewModel.clearData()
                historyViewModel.update()
            }

            PersonalTopNavItem.Favorite -> {
                favouriteViewModel.clearData()
                favouriteViewModel.updateFoldersInfo()
            }

            PersonalTopNavItem.FollowingSeason -> {
                followingSeasonViewModel.clearData()
                followingSeasonViewModel.loadMore()
            }
        }
    }

    LaunchedEffect(selectedTab, isLogin) {
        if (!isLogin) return@LaunchedEffect
        when (selectedTab) {
            PersonalTopNavItem.ToView -> toViewViewModel.ensureLoaded()
            PersonalTopNavItem.History -> historyViewModel.ensureLoaded()
            PersonalTopNavItem.Favorite -> favouriteViewModel.ensureLoaded()
            PersonalTopNavItem.FollowingSeason -> followingSeasonViewModel.ensureLoaded()
        }
    }
    Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier
                    .focusRequester(navFocusRequester),
                items = reorderedItems,
                isLargePadding = !focusOnContent,
                indicatorStyle = TopNavIndicatorStyle.Underline,
                onSelectedChanged = { nav ->
                    selectedTab = nav as PersonalTopNavItem
                },
                onClick = { nav ->
                    refreshPageData(nav as PersonalTopNavItem)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
                .onKeyEvent {
                    if (it.key == Key.Menu) {
                        if (it.type == KeyEventType.KeyDown) return@onKeyEvent true
                        refreshPageData(selectedTab)
                        navFocusRequester.requestFocus()
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                },
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "personal animated content",
                transitionSpec = {
                    val coefficient = 10
                    if (reorderedItems.indexOf(targetState) < reorderedItems.indexOf(initialState)) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                }
            ) { screen ->
                when (screen) {
                    PersonalTopNavItem.ToView -> {
                        ToViewScreen(toViewViewModel = toViewViewModel)
                    }

                    PersonalTopNavItem.History -> {
                        HistoryScreen(toViewViewModel = toViewViewModel)
                    }

                    PersonalTopNavItem.Favorite -> {
                        FavoriteScreen(toViewViewModel = toViewViewModel)
                    }

                    PersonalTopNavItem.FollowingSeason -> {
                        FollowingSeasonScreen()
                    }
                }
            }
        }
    }
}
