package dev.sunls24.sbv.screen.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.component.TopNav
import dev.sunls24.sbv.component.TopNavIndicatorStyle
import dev.sunls24.sbv.component.UgcTopNavItem
import dev.sunls24.sbv.screen.main.ugc.UgcRegionScaffold
import dev.sunls24.sbv.viewmodel.ugc.UgcViewModel
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun UgcContent(
    navFocusRequester: FocusRequester,
    ugcViewModel: UgcViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val states by ugcViewModel.states.collectAsState()

    var selectedTab by remember { mutableStateOf(UgcTopNavItem.Douga) }
    var focusOnContent by remember { mutableStateOf(false) }
    val ugcTopNavItems = UgcTopNavItem.entries
    val gridStates = remember {
        ugcTopNavItems.associateWith { LazyGridState() }
    }

    Scaffold(
        topBar = {
            TopNav(
                modifier = Modifier.padding(horizontal = 10.dp)
                    .focusRequester(navFocusRequester),
                items = ugcTopNavItems,
                isLargePadding = !focusOnContent,
                indicatorStyle = TopNavIndicatorStyle.Underline,
                onSelectedChanged = { nav ->
                    selectedTab = nav as UgcTopNavItem
                },
                onClick = { nav ->
                    ugcViewModel.reloadAll(nav as UgcTopNavItem)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
                .onPreviewKeyEvent {
                    if (it.key == Key.Menu) {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        ugcViewModel.reloadAll(selectedTab)
                        navFocusRequester.requestFocus()
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                },
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "ugc animated content",
                transitionSpec = {
                    val coefficient = 10
                    if (targetState.ordinal < initialState.ordinal) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                }
            ) { screen ->
                LaunchedEffect(screen) { ugcViewModel.ensureLoaded(screen) }
                states[screen]?.let { state ->
                    UgcRegionScaffold(
                        state = state,
                        gridState = gridStates.getValue(screen),
                        onLoadMore = { ugcViewModel.loadMoreData(screen) },
                        onAddWatchLater = { aid -> toViewViewModel.addToView(aid) },
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
            }
        }
    }
}
