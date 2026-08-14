package dev.sunls24.sbv.tv.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

object TvDialogDefaults {
    val minWidth = 520.dp
    val maxWidth = 720.dp
    val contentPadding = 24.dp
    val verticalPadding = 24.dp
    val buttonMinHeight = 48.dp

    val properties = DialogProperties(usePlatformDefaultWidth = false)
}

fun Modifier.tvDialogWidth(): Modifier = widthIn(
    min = TvDialogDefaults.minWidth,
    max = TvDialogDefaults.maxWidth,
)

fun Modifier.tvDialogButtonHeight(): Modifier = heightIn(
    min = TvDialogDefaults.buttonMinHeight,
)
