package dev.sunls24.sbv.component

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import dev.sunls24.biliapi.entity.ugc.UgcTypeV2
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.getDisplayName

@Composable
fun TopNav(
    modifier: Modifier = Modifier,
    items: List<TopNavItem>,
    isLargePadding: Boolean,
    indicatorStyle: TopNavIndicatorStyle = TopNavIndicatorStyle.Pill,
    onSelectedChanged: (TopNavItem) -> Unit = {},
    onClick: (TopNavItem) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val verticalPadding by animateDpAsState(
        targetValue = if (isLargePadding) SBVSpacing.md else SBVSpacing.sm,
        label = "top nav vertical padding"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SBVSpacing.md, vertical = verticalPadding),
        horizontalArrangement = Arrangement.Center
    ) {
        TabRow(
            modifier = Modifier
            .focusRestorer(focusRequester),
            selectedTabIndex = selectedTabIndex,
            separator = { Spacer(modifier = Modifier.width(SBVSpacing.lg)) },
            indicator = { tabPositions, doesTabRowHaveFocus ->
                val currentTabPosition = tabPositions.getOrNull(selectedTabIndex) ?: return@TabRow
                when (indicatorStyle) {
                    TopNavIndicatorStyle.Pill -> TabRowDefaults.PillIndicator(
                        currentTabPosition = currentTabPosition,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.primaryContainer,
                    )

                    TopNavIndicatorStyle.Underline -> TabRowDefaults.UnderlinedIndicator(
                        currentTabPosition = currentTabPosition,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        ) {
            items.forEachIndexed { index, tab ->
                NavItemTab(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                    topNavItem = tab,
                    selected = index == selectedTabIndex,
                    indicatorStyle = indicatorStyle,
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
    indicatorStyle: TopNavIndicatorStyle,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val context = LocalContext.current

    Tab(
        modifier = modifier,
        selected = selected,
        colors = when (indicatorStyle) {
            TopNavIndicatorStyle.Pill -> TabDefaults.pillIndicatorTabColors()
            TopNavIndicatorStyle.Underline -> TabDefaults.underlinedIndicatorTabColors()
        },
        onFocus = onFocus,
        onClick = onClick
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

enum class TopNavIndicatorStyle {
    Pill,
    Underline,
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

enum class UgcTopNavItem(val ugcTypeV2: UgcTypeV2) : TopNavItem {
    Douga(UgcTypeV2.Douga),
    Game(UgcTypeV2.Game),
    Kichiku(UgcTypeV2.Kichiku),
    Music(UgcTypeV2.Music),
    Dance(UgcTypeV2.Dance),
    Cinephile(UgcTypeV2.Cinephile),
    Ent(UgcTypeV2.Ent),
    Knowledge(UgcTypeV2.Knowledge),
    Tech(UgcTypeV2.Tech),
    Information(UgcTypeV2.Information),
    Food(UgcTypeV2.Food),
    Life(UgcTypeV2.LifeJoy),
    Car(UgcTypeV2.Car),
    Fashion(UgcTypeV2.Fashion),
    Sports(UgcTypeV2.Sports),
    Animal(UgcTypeV2.Animal);

    override fun getDisplayName(context: Context): String {
        return ugcTypeV2.getDisplayName(context)
    }
}

enum class PersonalTopNavItem : TopNavItem {
    ToView,
    History,
    Favorite,
    FollowingSeason;

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
