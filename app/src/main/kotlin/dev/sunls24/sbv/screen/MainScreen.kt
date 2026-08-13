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
import dev.sunls24.sbv.screen.main.UgcContent
import dev.sunls24.sbv.screen.search.SearchInputScreen
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
    var lastPressBack: Long by remember { mutableLongStateOf(0L) }
    var selectedDestination by remember {
        mutableStateOf<MainDestination>(MainDestination.Home(Prefs.firstHomeTopNavItem))
    }
    val selectedSection = selectedDestination.section
    val isLogin by userViewModel.isLoginFlow.collectAsState()

    val personalFocusRequester = remember { FocusRequester() }
    val homeContentFocusRequester = remember { FocusRequester() }
    val homeNavigationFocusRequester = remember { FocusRequester() }
    val avatarFocusRequester = remember { FocusRequester() }
    val ugcFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    val onFocusToContent: () -> Boolean = {
        runCatching {
            when (selectedSection) {
                MainSection.Home -> homeContentFocusRequester.requestFocus()
                MainSection.Ugc -> ugcFocusRequester.requestFocus()
                MainSection.Search -> searchFocusRequester.requestFocus()
                MainSection.Personal -> personalFocusRequester.requestFocus()
            }
        }.getOrDefault(false)
    }

    LaunchedEffect(Unit) {
        runCatching { homeNavigationFocusRequester.requestFocus() }
    }

    LaunchedEffect(showUserPanel) {
        if (!showUserPanel && restoreAvatarFocus) {
            avatarFocusRequester.requestFocus()
            restoreAvatarFocus = false
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
        MainTopBar(
            selectedDestination = selectedDestination,
            isLogin = isLogin,
            avatar = userViewModel.face,
            homeFocusRequester = homeNavigationFocusRequester,
            avatarFocusRequester = avatarFocusRequester,
            onDestinationChanged = { selectedDestination = it },
            onAvatarClick = { showUserPanel = true },
            onFocusToContent = onFocusToContent,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (selectedSection) {
                MainSection.Search -> SearchInputScreen(defaultFocusRequester = searchFocusRequester)
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
                    toViewViewModel = toViewViewModel
                )
                MainSection.Ugc -> UgcContent(
                    navFocusRequester = ugcFocusRequester,
                    toViewViewModel = toViewViewModel
                )
            }
        }

        if (showUserPanel) {
            val hideUserPanel = {
                restoreAvatarFocus = true
                showUserPanel = false
            }
            UserPanelDialog(
                isLogin = isLogin,
                username = userViewModel.username,
                face = userViewModel.face,
                level = userViewModel.responseData?.level ?: 0,
                currentExp = userViewModel.responseData?.levelExp?.currentExp ?: 0,
                nextLevelExp = with(userViewModel.responseData?.levelExp?.nextExp) {
                    if (this == null) {
                        1
                    } else if (this <= 0) {
                        userViewModel.responseData?.levelExp?.currentExp ?: 1
                    } else {
                        (userViewModel.responseData?.levelExp?.currentExp ?: 1) +
                            (userViewModel.responseData?.levelExp?.nextExp ?: 0)
                    }
                },
                onHide = hideUserPanel,
                onLogin = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                onLogout = userViewModel::logout,
                onOpenSettings = {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                },
                onOpenPersonal = {
                    selectedDestination = MainDestination.Personal
                },
                onGoFollowingUp = {
                    context.startActivity(Intent(context, FollowActivity::class.java))
                },
            )
        }
    }
}
