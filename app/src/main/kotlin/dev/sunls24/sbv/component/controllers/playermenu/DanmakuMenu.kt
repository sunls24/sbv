package dev.sunls24.sbv.component.controllers.playermenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.sunls24.sbv.component.controllers.DanmakuType
import dev.sunls24.sbv.component.controllers.LocalMenuFocusStateData
import dev.sunls24.sbv.component.controllers.MenuFocusState
import dev.sunls24.sbv.component.controllers.VideoPlayerDanmakuMenuItem
import dev.sunls24.sbv.component.controllers.playermenu.component.CheckBoxMenuList
import dev.sunls24.sbv.component.controllers.playermenu.component.MenuListItem
import dev.sunls24.sbv.component.controllers.playermenu.component.RadioMenuList
import dev.sunls24.sbv.component.controllers.playermenu.component.PresetMenuItem
import dev.sunls24.sbv.component.ifElse
import dev.sunls24.sbv.entity.DanmakuSpeedFactor
import java.text.NumberFormat

@Composable
fun DanmakuMenuList(
    modifier: Modifier = Modifier,
    currentEnabledTypes: List<DanmakuType>,
    currentScale: Float,
    currentOpacity: Float,
    currentSpeedFactor: Float,
    currentArea: Float,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuSpeedFactorChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }

    val focusRequester = remember { FocusRequester() }
    var selectedDanmakuMenuItem by remember { mutableStateOf(VideoPlayerDanmakuMenuItem.Switch) }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val menuItemsModifier = Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)
        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (selectedDanmakuMenuItem) {
                VideoPlayerDanmakuMenuItem.Switch -> CheckBoxMenuList(
                    modifier = menuItemsModifier,
                    items = DanmakuType.entries.map { it.getDisplayName(context) },
                    selected = currentEnabledTypes.map { it.ordinal },
                    onSelectedChanged = { indices ->
                        onDanmakuSwitchChange(indices.map { DanmakuType.entries[it] })
                    },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    }
                )

                VideoPlayerDanmakuMenuItem.Size -> PresetMenuItem(
                    modifier = menuItemsModifier,
                    value = currentScale,
                    values = DANMAKU_SCALE_PRESETS,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentScale),
                    onValueChange = onDanmakuSizeChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerDanmakuMenuItem.Opacity -> PresetMenuItem(
                    modifier = menuItemsModifier,
                    value = currentOpacity,
                    values = PERCENTAGE_PRESETS,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentOpacity),
                    onValueChange = onDanmakuOpacityChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerDanmakuMenuItem.SpeedFactor -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = DanmakuSpeedFactor.entries.map { it.getDisplayName(context) },
                    selected = DanmakuSpeedFactor.getIndexByFactor(currentSpeedFactor),
                    onSelectedChanged = {
                        onDanmakuSpeedFactorChange(DanmakuSpeedFactor.entries[it].factor) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    }
                )

                VideoPlayerDanmakuMenuItem.Area -> PresetMenuItem(
                    modifier = menuItemsModifier,
                    value = currentArea,
                    values = PERCENTAGE_PRESETS,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentArea),
                    onValueChange = onDanmakuAreaChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .focusRequester(focusRequester)
                .padding(horizontal = 8.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                            return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    when (it.key) {
                        Key.DirectionRight -> onFocusStateChange(MenuFocusState.MenuNav)
                        Key.DirectionLeft -> onFocusStateChange(MenuFocusState.Items)
                        else -> {}
                    }
                    false
                }
                .focusRestorer(restorerFocusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            itemsIndexed(VideoPlayerDanmakuMenuItem.entries) { index, item ->
                MenuListItem(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                    text = item.getDisplayName(context),
                    selected = selectedDanmakuMenuItem == item,
                    onClick = {},
                    onFocus = { selectedDanmakuMenuItem = item },
                )
            }
        }
    }
}

private val DANMAKU_SCALE_PRESETS = (50..200 step 25).map { it / 100f }
private val PERCENTAGE_PRESETS = (0..100 step 10).map { it / 100f }
