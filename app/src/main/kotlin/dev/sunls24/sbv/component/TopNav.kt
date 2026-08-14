package dev.sunls24.sbv.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.tv.material3.TabRowScope
import androidx.tv.material3.Text
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.ui.theme.SBVSpacing

@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    items: List<TopNavItem>,
    selectedItem: TopNavItem? = null,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {}
) {
    val fallbackFocusRequester = remember { FocusRequester() }
    val firstTabFocusRequester = focusRequester ?: fallbackFocusRequester
    val initialSelectedTabIndex =
        selectedItem?.let { item -> items.indexOf(item).takeIf { it >= 0 } } ?: 0

    var selectedTabIndex by remember(initialSelectedTabIndex) {
        mutableIntStateOf(initialSelectedTabIndex)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SBVSpacing.md, vertical = SBVSpacing.sm),
        horizontalArrangement = Arrangement.Center
    ) {
        TabRow(
            modifier = Modifier.focusRestorer(firstTabFocusRequester),
            selectedTabIndex = selectedTabIndex,
            indicator = { tabPositions, doesTabRowHaveFocus ->
                tabPositions.getOrNull(selectedTabIndex)?.let { position ->
                    TabRowDefaults.UnderlinedIndicator(
                        currentTabPosition = position,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                    )
                }
            },
        ) {
            items.forEachIndexed { index, tab ->
                NavItemTab(
                    modifier = Modifier
                        .ifElse(
                            index == selectedTabIndex,
                            Modifier.focusRequester(firstTabFocusRequester)
                        ),
                    topNavItem = tab,
                    selected = index == selectedTabIndex,
                    onFocus = {
                        selectedTabIndex = index
                        onSelectedChanged(tab)
                    },
                    onClick = { onClick(tab) }
                )
            }
        }
    }
}

@Composable
private fun TabRowScope.NavItemTab(
    modifier: Modifier = Modifier,
    topNavItem: TopNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val context = LocalContext.current

    Tab(
        modifier = modifier,
        selected = selected,
        onFocus = onFocus,
        onClick = onClick,
        colors = TabDefaults.underlinedIndicatorTabColors(),
    ) {
        Text(
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = SBVSpacing.lg, vertical = SBVSpacing.sm),
            text = topNavItem.getDisplayName(context),
            color = LocalContentColor.current,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

interface TopNavItem {
    fun getDisplayName(context: Context = SBVApp.context): String
}


enum class HomeTopNavItem(val code: Int, private val displayName: String) : TopNavItem {
    Dynamics(0, "动态"),
    Recommend(1, "推荐"),
    Popular(2, "热门");

    companion object{
        fun fromCode(code: Int): HomeTopNavItem {
            return HomeTopNavItem.entries.find { it.code == code } ?: Dynamics
        }
    }

    override fun getDisplayName(context: Context): String {
        return displayName
    }
}

enum class PersonalTopNavItem : TopNavItem {
    ToView,
    History,
    Favorite,
    FollowingSeason;

    companion object {
        val displayOrder = listOf(ToView, Favorite, History, FollowingSeason)
    }

    override fun getDisplayName(context: Context): String {
        return when (this) {
            ToView -> "稍后再看"
            History -> "历史"
            Favorite -> "收藏"
            FollowingSeason -> "我追的番"
        }
    }
}

enum class SearchTypeTopNavItem: TopNavItem {
    Video,
    MediaBangumi,
    MediaFt,
    BiliUser;
    override fun getDisplayName(context: Context): String {
        return when (this) {
            Video -> "视频"
            MediaBangumi -> "番剧"
            MediaFt -> "影视"
            BiliUser -> "用户"
        }
    }
}
