package dev.sunls24.sbv.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.settings.SettingsActivity
import dev.sunls24.sbv.activities.user.FollowActivity
import dev.sunls24.sbv.activities.user.LoginActivity
import dev.sunls24.sbv.component.UserPanelDialog
import dev.sunls24.sbv.screen.main.HomeContent
import dev.sunls24.sbv.screen.main.MainDestination
import dev.sunls24.sbv.screen.main.MainSection
import dev.sunls24.sbv.screen.main.MainTopBar
import dev.sunls24.sbv.screen.main.PersonalContent
import dev.sunls24.sbv.screen.search.SearchInputScreen
import dev.sunls24.sbv.screen.search.SearchResultScreen
import dev.sunls24.sbv.ui.effect.UiEffect
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.util.toast
import dev.sunls24.sbv.viewmodel.UserViewModel
import dev.sunls24.sbv.viewmodel.user.FavoriteViewModel
import dev.sunls24.sbv.viewmodel.user.FollowingSeasonViewModel
import dev.sunls24.sbv.viewmodel.user.HistoryViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = koinViewModel(),
    favoriteViewModel: FavoriteViewModel = koinViewModel(),
    historyViewModel: HistoryViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
    followingSeasonViewModel: FollowingSeasonViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var showUserPanel by remember { mutableStateOf(false) }
    var restoreAvatarFocus by remember { mutableStateOf(false) }
    var restoreAvatarAfterPanelDismiss by remember { mutableStateOf(true) }
    var lastPressBack: Long by remember { mutableLongStateOf(0L) }
    var homeRefreshRequest by remember { mutableLongStateOf(0L) }
    var searchResultKeyword by remember { mutableStateOf<String?>(null) }
    var selectedDestination by remember {
        mutableStateOf<MainDestination>(MainDestination.Home(Prefs.firstHomeTopNavItem))
    }
    var lastHomeDestination by remember {
        mutableStateOf(MainDestination.Home(Prefs.firstHomeTopNavItem))
    }
    var pendingHomeFocus by remember { mutableStateOf<MainSection?>(null) }
    val selectedSection = selectedDestination.section
    val isLogin by userViewModel.isLoginFlow.collectAsState()

    val personalFocusRequester = remember { FocusRequester() }
    val homeContentFocusRequester = remember { FocusRequester() }
    val homeNavigationFocusRequester = remember { FocusRequester() }
    val avatarFocusRequester = remember { FocusRequester() }
    val searchActionFocusRequester = remember { FocusRequester() }
    val searchInputFocusRequester = remember { FocusRequester() }

    val onFocusToContent: () -> Boolean = {
        runCatching {
            when (selectedSection) {
                MainSection.Home -> homeContentFocusRequester.requestFocus()
                MainSection.Search -> searchInputFocusRequester.requestFocus()
                MainSection.Personal -> personalFocusRequester.requestFocus()
            }
        }.getOrDefault(false)
    }

    LaunchedEffect(Unit) {
        runCatching { homeNavigationFocusRequester.requestFocus() }
    }

    LaunchedEffect(selectedSection, searchResultKeyword) {
        when (selectedSection) {
            MainSection.Personal -> runCatching { personalFocusRequester.requestFocus() }
            MainSection.Search -> if (searchResultKeyword == null) {
                runCatching { searchInputFocusRequester.requestFocus() }
            }
            MainSection.Home -> Unit
        }
    }

    LaunchedEffect(showUserPanel) {
        if (!showUserPanel) {
            if (restoreAvatarFocus) {
                avatarFocusRequester.requestFocus()
                restoreAvatarFocus = false
            } else if (selectedSection == MainSection.Personal) {
                personalFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(selectedSection, pendingHomeFocus) {
        if (selectedSection == MainSection.Home) {
            when (pendingHomeFocus) {
                MainSection.Personal -> avatarFocusRequester.requestFocus()
                MainSection.Search -> searchActionFocusRequester.requestFocus()
                MainSection.Home, null -> Unit
            }
            pendingHomeFocus = null
        }
    }

    LaunchedEffect(isLogin) {
        if (!isLogin) {
            favoriteViewModel.clearData()
            historyViewModel.clearData()
            toViewViewModel.clearData()
            followingSeasonViewModel.clearData()
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> event.message.toast(context)
            }
        }
    }

    BackHandler(enabled = !showUserPanel) {
        if (selectedSection == MainSection.Search && searchResultKeyword != null) {
            searchResultKeyword = null
            return@BackHandler
        }
        if (selectedSection != MainSection.Home) {
            pendingHomeFocus = selectedSection
            selectedDestination = lastHomeDestination
            return@BackHandler
        }
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressBack < 1000 * 3) {
            (context as Activity).finish()
        } else {
            lastPressBack = currentTime
            R.string.home_press_back_again_to_exit.toast(context)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (selectedDestination is MainDestination.Home) {
            MainTopBar(
                selectedDestination = selectedDestination as MainDestination.Home,
                isLogin = isLogin,
                avatar = userViewModel.face,
                homeFocusRequester = homeNavigationFocusRequester,
                searchFocusRequester = searchActionFocusRequester,
                avatarFocusRequester = avatarFocusRequester,
                onDestinationChanged = {
                    selectedDestination = it
                    lastHomeDestination = it
                },
                onDestinationClick = { destination ->
                    if (destination == selectedDestination) {
                        homeRefreshRequest++
                    } else {
                        selectedDestination = destination
                        lastHomeDestination = destination
                    }
                },
                onSearchClick = {
                    searchResultKeyword = null
                    selectedDestination = MainDestination.Search
                },
                onAvatarClick = {
                    restoreAvatarAfterPanelDismiss = true
                    showUserPanel = true
                },
                onFocusToContent = onFocusToContent,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (selectedSection) {
                MainSection.Search -> {
                    val keyword = searchResultKeyword
                    if (keyword == null) {
                        SearchInputScreen(
                            defaultFocusRequester = searchInputFocusRequester,
                            onSearchRequest = { searchResultKeyword = it },
                        )
                    } else {
                        SearchResultScreen(
                            keyword = keyword,
                            onExit = { searchResultKeyword = null },
                        )
                    }
                }
                MainSection.Personal -> PersonalContent(
                    navFocusRequester = personalFocusRequester,
                    favouriteViewModel = favoriteViewModel,
                    historyViewModel = historyViewModel,
                    toViewViewModel = toViewViewModel,
                    followingSeasonViewModel = followingSeasonViewModel
                )
                MainSection.Home -> HomeContent(
                    navFocusRequester = homeNavigationFocusRequester,
                    contentFocusRequester = homeContentFocusRequester,
                    selectedTab = (selectedDestination as MainDestination.Home).tab,
                    refreshRequest = homeRefreshRequest,
                    toViewViewModel = toViewViewModel
                )
            }
        }

        if (showUserPanel) {
            val hideUserPanel = {
                restoreAvatarFocus = restoreAvatarAfterPanelDismiss
                showUserPanel = false
            }
            UserPanelDialog(
                isLogin = isLogin,
                username = userViewModel.username,
                face = userViewModel.face,
                level = userViewModel.responseData?.level ?: 0,
                onHide = hideUserPanel,
                onLogin = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                onLogout = userViewModel::logout,
                onOpenSettings = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                onOpenPersonal = {
                    restoreAvatarAfterPanelDismiss = false
                    selectedDestination = MainDestination.Personal
                },
                onGoFollowingUp = {
                    context.startActivity(Intent(context, FollowActivity::class.java))
                },
            )
        }
    }
}
