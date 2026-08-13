package dev.sunls24.sbv.component.controllers.playermenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.sunls24.sbv.component.controllers.MenuFocusState
import dev.sunls24.sbv.component.controllers.playermenu.component.MenuListItem
import dev.sunls24.sbv.component.ifElse
import dev.sunls24.sbv.entity.PlaybackSpeed

@Composable
fun PlaySpeedMenuList(
    modifier: Modifier = Modifier,
    currentSelectedPlaySpeedItem: PlaybackSpeed,
    onPlaySpeedChange: (Float) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                            return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (it.key == Key.DirectionRight)
                        onFocusStateChange(MenuFocusState.MenuNav)
                    false
                }
                .focusRestorer(focusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            itemsIndexed(PlaybackSpeed.entries) { index, item ->
                MenuListItem(
                    modifier = Modifier
                        .ifElse(
                            index == currentSelectedPlaySpeedItem.ordinal,
                            Modifier.focusRequester(focusRequester)
                        ),
                    text = item.getDisplayName(context),
                    selected = currentSelectedPlaySpeedItem == item,
                    onClick = {
                        onPlaySpeedChange(item.speed)
                    },
                )
            }
        }
    }


}
