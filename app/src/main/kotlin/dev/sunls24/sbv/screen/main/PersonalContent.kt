package dev.sunls24.sbv.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.sbv.component.PersonalTopNavItem
import dev.sunls24.sbv.component.TopNav
import dev.sunls24.sbv.screen.user.FavoriteScreen
import dev.sunls24.sbv.screen.user.FollowingSeasonScreen
import dev.sunls24.sbv.screen.user.HistoryScreen
import dev.sunls24.sbv.screen.user.ToViewScreen
import dev.sunls24.sbv.ui.theme.SBVSpacing
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
    val firstTab = remember { Prefs.firstPersonalTopNavItem }
    var selectedTab by remember { mutableStateOf(firstTab) }

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
            Column {
                Text(
                    modifier = Modifier.padding(
                        start = SBVSpacing.xxl,
                        top = SBVSpacing.lg,
                        bottom = SBVSpacing.sm,
                    ),
                    text = "我的内容",
                    style = MaterialTheme.typography.titleLarge,
                )
                TopNav(
                    focusRequester = navFocusRequester,
                    items = PersonalTopNavItem.displayOrder,
                    selectedItem = firstTab,
                    onSelectedChanged = { nav ->
                        selectedTab = nav as PersonalTopNavItem
                    },
                    onClick = { nav ->
                        refreshPageData(nav as PersonalTopNavItem)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
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
            when (selectedTab) {
                PersonalTopNavItem.ToView -> {
                    ToViewScreen(
                        fallbackFocusRequester = navFocusRequester,
                        toViewViewModel = toViewViewModel,
                    )
                }

                PersonalTopNavItem.History -> {
                    HistoryScreen(
                        fallbackFocusRequester = navFocusRequester,
                        toViewViewModel = toViewViewModel,
                    )
                }

                PersonalTopNavItem.Favorite -> {
                    FavoriteScreen(
                        fallbackFocusRequester = navFocusRequester,
                        toViewViewModel = toViewViewModel,
                    )
                }

                PersonalTopNavItem.FollowingSeason -> {
                    FollowingSeasonScreen(
                        fallbackFocusRequester = navFocusRequester,
                    )
                }
            }
        }
    }
}
