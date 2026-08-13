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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sunls24.biliapi.entity.video.Subtitle
import dev.sunls24.biliapi.entity.video.SubtitleType
import dev.sunls24.sbv.component.controllers.LocalMenuFocusStateData
import dev.sunls24.sbv.component.controllers.MenuFocusState
import dev.sunls24.sbv.component.controllers.VideoPlayerClosedCaptionMenuItem
import dev.sunls24.sbv.component.controllers.playermenu.component.MenuListItem
import dev.sunls24.sbv.component.controllers.playermenu.component.RadioMenuList
import dev.sunls24.sbv.component.controllers.playermenu.component.PresetMenuItem
import dev.sunls24.sbv.component.ifElse
import java.text.NumberFormat

@Composable
fun ClosedCaptionMenuList(
    modifier: Modifier = Modifier,
    currentSubtitleId: Long,
    availableSubtitleTracks: List<Subtitle>,
    currentFontSize: TextUnit,
    currentOpacity: Float,
    currentPadding: Dp,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusState = LocalMenuFocusStateData.current
    val restorerFocusRequester = remember { FocusRequester() }

    val focusRequester = remember { FocusRequester() }
    var selectedClosedCaptionMenuItem by remember { mutableStateOf(VideoPlayerClosedCaptionMenuItem.Switch) }

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val menuItemsModifier = Modifier
            .width(216.dp)
            .padding(horizontal = 8.dp)
        AnimatedVisibility(visible = focusState.focusState != MenuFocusState.MenuNav) {
            when (selectedClosedCaptionMenuItem) {
                VideoPlayerClosedCaptionMenuItem.Switch -> RadioMenuList(
                    modifier = menuItemsModifier,
                    items = availableSubtitleTracks.map {
                        it.langDoc
                            .replace("（自动生成）", "")
                            .replace("（自动翻译）", "")
                            .trim() + if (it.type == SubtitleType.AI) "(AI)" else ""
                    },
                    selected = availableSubtitleTracks.indexOfFirst { it.id == currentSubtitleId },
                    onSelectedChanged = { onSubtitleChange(availableSubtitleTracks[it]) },
                    onFocusBackToParent = {
                        onFocusStateChange(MenuFocusState.Menu)
                        focusRequester.requestFocus()
                    },
                )

                VideoPlayerClosedCaptionMenuItem.Size -> PresetMenuItem(
                    modifier = menuItemsModifier,
                    value = currentFontSize.value.toInt(),
                    values = SUBTITLE_FONT_SIZE_PRESETS,
                    text = "${currentFontSize.value.toInt()} SP",
                    onValueChange = { onSubtitleSizeChange(it.sp) },
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Opacity -> PresetMenuItem(
                    modifier = menuItemsModifier,
                    value = currentOpacity,
                    values = PERCENTAGE_PRESETS,
                    text = NumberFormat.getPercentInstance()
                        .apply { maximumFractionDigits = 0 }
                        .format(currentOpacity),
                    onValueChange = onSubtitleBackgroundOpacityChange,
                    onFocusBackToParent = { onFocusStateChange(MenuFocusState.Menu) }
                )

                VideoPlayerClosedCaptionMenuItem.Padding -> PresetMenuItem(
                    modifier = menuItemsModifier,
                    value = currentPadding.value.toInt(),
                    values = SUBTITLE_PADDING_PRESETS,
                    text = "${currentPadding.value.toInt()} DP",
                    onValueChange = { onSubtitleBottomPadding(it.dp) },
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
            itemsIndexed(VideoPlayerClosedCaptionMenuItem.entries) { index, item ->
                MenuListItem(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(restorerFocusRequester)),
                    text = item.getDisplayName(context),
                    selected = selectedClosedCaptionMenuItem == item,
                    onClick = {},
                    onFocus = { selectedClosedCaptionMenuItem = item },
                )
            }
        }
    }
}

private val SUBTITLE_FONT_SIZE_PRESETS = (12..48 step 4).toList()
private val SUBTITLE_PADDING_PRESETS = (0..48 step 4).toList()
private val PERCENTAGE_PRESETS = (0..100 step 10).map { it / 100f }
