package dev.sunls24.sbv.screen.main.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import dev.sunls24.sbv.util.firstRowActionFocus

internal const val HOME_GRID_COLUMNS = 4

internal fun Modifier.homeGridFocus(
    index: Int,
    itemCount: Int,
    navFocusRequester: FocusRequester,
): Modifier = focusProperties {
    if (index < HOME_GRID_COLUMNS) {
        up = navFocusRequester
    }
    if (index % HOME_GRID_COLUMNS == 0) {
        left = navFocusRequester
    }
    if (index % HOME_GRID_COLUMNS == HOME_GRID_COLUMNS - 1 || index == itemCount - 1) {
        right = navFocusRequester
    }
}

internal fun Modifier.homeGridActionFocus(
    index: Int,
    navFocusRequester: FocusRequester,
): Modifier = firstRowActionFocus(index, HOME_GRID_COLUMNS, navFocusRequester)
