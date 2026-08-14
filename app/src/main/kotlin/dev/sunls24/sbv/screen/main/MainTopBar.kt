package dev.sunls24.sbv.screen.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.sbv.R
import dev.sunls24.sbv.component.HomeTopNavItem
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.ImageSize
import dev.sunls24.sbv.util.resizedImageUrl

private val TopBarAvatarSize = 38.dp

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    selectedDestination: MainDestination.Home,
    isLogin: Boolean,
    avatar: String,
    homeFocusRequester: FocusRequester,
    searchFocusRequester: FocusRequester,
    avatarFocusRequester: FocusRequester,
    onDestinationChanged: (MainDestination.Home) -> Unit,
    onDestinationClick: (MainDestination.Home) -> Unit,
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onFocusToContent: () -> Boolean,
) {
    val homeItems = remember {
        listOf(HomeTopNavItem.Recommend, HomeTopNavItem.Popular, HomeTopNavItem.Dynamics)
    }
    val destinations = remember(homeItems) { homeItems.map(MainDestination::Home) }
    val selectedIndex = destinations.indexOf(selectedDestination)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionDown && event.type == KeyEventType.KeyDown) {
                    onFocusToContent()
                } else {
                    false
                }
            },
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SBVSpacing.xl, vertical = SBVSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp),
                painter = painterResource(R.drawable.ic_sbv_logo),
                tint = Color.Unspecified,
                contentDescription = "SBV",
            )

            MainTopTabRow(
                destinations = destinations,
                selectedIndex = selectedIndex,
                selectedFocusRequester = homeFocusRequester,
                searchFocusRequester = searchFocusRequester,
                onFocus = { onDestinationChanged(destinations[it]) },
                onClick = { onDestinationClick(destinations[it]) },
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(SBVSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(searchFocusRequester)
                        .focusProperties {
                            left = homeFocusRequester
                            right = avatarFocusRequester
                        },
                    onClick = onSearchClick,
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_symbol_search_filled),
                        contentDescription = "搜索",
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(avatarFocusRequester)
                        .focusProperties {
                            left = searchFocusRequester
                        },
                    onClick = onAvatarClick,
                ) {
                    if (isLogin && avatar.isNotBlank()) {
                        AsyncImage(
                            modifier = Modifier
                                .size(TopBarAvatarSize)
                                .clip(CircleShape),
                            model = avatar.resizedImageUrl(ImageSize.Avatar),
                            contentDescription = "个人菜单",
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(TopBarAvatarSize),
                            painter = painterResource(R.drawable.ic_symbol_account_circle_filled),
                            contentDescription = "登录和个人菜单",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTopTabRow(
    destinations: List<MainDestination>,
    selectedIndex: Int,
    selectedFocusRequester: FocusRequester,
    searchFocusRequester: FocusRequester,
    onFocus: (Int) -> Unit,
    onClick: (Int) -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedIndex.coerceAtLeast(0),
        separator = { Spacer(modifier = Modifier.width(SBVSpacing.sm)) },
        indicator = { tabPositions, doesTabRowHaveFocus ->
            tabPositions.getOrNull(selectedIndex)?.let { position ->
                TabRowDefaults.UnderlinedIndicator(
                    currentTabPosition = position,
                    doesTabRowHaveFocus = doesTabRowHaveFocus,
                )
            }
        },
    ) {
        destinations.forEachIndexed { index, destination ->
            Tab(
                modifier = Modifier
                    .then(
                        if (index == selectedIndex) {
                            Modifier.focusRequester(selectedFocusRequester)
                        } else {
                            Modifier
                        }
                    )
                    .focusProperties {
                        if (index == destinations.lastIndex) {
                            right = searchFocusRequester
                        }
                    },
                selected = index == selectedIndex,
                colors = TabDefaults.underlinedIndicatorTabColors(),
                onFocus = { onFocus(index) },
                onClick = { onClick(index) },
            ) {
                Box(
                    modifier = Modifier.height(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = SBVSpacing.lg,
                            vertical = SBVSpacing.sm,
                        ),
                        text = destination.displayName,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
