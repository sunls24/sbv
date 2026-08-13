package dev.sunls24.sbv.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.focus.focusRequester
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

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    selectedDestination: MainDestination,
    isLogin: Boolean,
    avatar: String,
    homeFocusRequester: FocusRequester,
    avatarFocusRequester: FocusRequester,
    onDestinationChanged: (MainDestination) -> Unit,
    onAvatarClick: () -> Unit,
    onFocusToContent: () -> Boolean,
) {
    val homeItems = remember {
        listOf(HomeTopNavItem.Recommend, HomeTopNavItem.Popular, HomeTopNavItem.Dynamics)
    }
    val homeDestinations = remember(homeItems) { homeItems.map(MainDestination::Home) }
    val routeDestinations = remember {
        listOf(MainDestination.Ugc, MainDestination.Search)
    }
    val selectedHomeIndex = homeDestinations.indexOf(selectedDestination)
    val selectedRouteIndex = routeDestinations.indexOf(selectedDestination)

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SBVSpacing.xl, vertical = SBVSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MainTopTabRow(
                        labels = homeDestinations.map { it.displayName },
                        selectedIndex = selectedHomeIndex,
                        showIndicator = selectedHomeIndex >= 0,
                        indicatorStyle = MainTopBarIndicatorStyle.Pill,
                        selectedFocusRequester = homeFocusRequester,
                        onFocus = { onDestinationChanged(homeDestinations[it]) },
                        onClick = { onDestinationChanged(homeDestinations[it]) },
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = SBVSpacing.lg, vertical = SBVSpacing.sm)
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.borderVariant),
                    )

                    MainTopTabRow(
                        labels = routeDestinations.map { it.displayName },
                        selectedIndex = selectedRouteIndex,
                        showIndicator = selectedRouteIndex >= 0,
                        indicatorStyle = MainTopBarIndicatorStyle.Underline,
                        onFocus = { onDestinationChanged(routeDestinations[it]) },
                        onClick = { onDestinationChanged(routeDestinations[it]) },
                    )
                }
            }

            Spacer(modifier = Modifier.width(SBVSpacing.lg))

            IconButton(
                modifier = Modifier
                    .size(56.dp)
                    .focusRequester(avatarFocusRequester)
                    .then(
                        if (selectedDestination == MainDestination.Personal) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
                onClick = onAvatarClick,
            ) {
                if (isLogin && avatar.isNotBlank()) {
                    AsyncImage(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        model = avatar,
                        contentDescription = "个人菜单",
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_account_circle_filled),
                        contentDescription = "登录和个人菜单",
                    )
                }
            }
        }
    }
}

private enum class MainTopBarIndicatorStyle {
    Pill,
    Underline,
}

@Composable
private fun MainTopTabRow(
    labels: List<String>,
    selectedIndex: Int,
    showIndicator: Boolean,
    indicatorStyle: MainTopBarIndicatorStyle,
    selectedFocusRequester: FocusRequester? = null,
    onFocus: (Int) -> Unit,
    onClick: (Int) -> Unit,
) {
    TabRow(
        selectedTabIndex = selectedIndex.coerceAtLeast(0),
        separator = { Spacer(modifier = Modifier.width(SBVSpacing.sm)) },
        indicator = { tabPositions, doesTabRowHaveFocus ->
            if (showIndicator) tabPositions.getOrNull(selectedIndex)?.let { position ->
                when (indicatorStyle) {
                    MainTopBarIndicatorStyle.Pill -> TabRowDefaults.PillIndicator(
                        currentTabPosition = position,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.primaryContainer,
                    )

                    MainTopBarIndicatorStyle.Underline -> TabRowDefaults.UnderlinedIndicator(
                        currentTabPosition = position,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                modifier = if (index == selectedIndex && selectedFocusRequester != null) {
                    Modifier.focusRequester(selectedFocusRequester)
                } else {
                    Modifier
                },
                selected = index == selectedIndex,
                colors = when (indicatorStyle) {
                    MainTopBarIndicatorStyle.Pill -> TabDefaults.pillIndicatorTabColors()
                    MainTopBarIndicatorStyle.Underline -> TabDefaults.underlinedIndicatorTabColors()
                },
                onFocus = { onFocus(index) },
                onClick = { onClick(index) },
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = SBVSpacing.lg,
                        vertical = SBVSpacing.sm,
                    ),
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
