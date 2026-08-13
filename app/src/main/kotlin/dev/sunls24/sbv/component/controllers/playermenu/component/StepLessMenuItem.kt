package dev.sunls24.sbv.component.controllers.playermenu.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import dev.sunls24.sbv.R

@Composable
fun PresetMenuItem(
    modifier: Modifier = Modifier,
    value: Float = 1f,
    text: String,
    values: List<Float>,
    onValueChange: (Float) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    require(values.isNotEmpty()) { "Preset values must not be empty" }

    PresetMenuItemLayout(
        modifier = modifier,
        text = text,
        onIncrease = {
            onValueChange(
                values.firstOrNull { preset -> preset > value + FLOAT_PRESET_EPSILON }
                    ?: values.last()
            )
        },
        onDecrease = {
            onValueChange(
                values.lastOrNull { preset -> preset < value - FLOAT_PRESET_EPSILON }
                    ?: values.first()
            )
        },
        onFocusBackToParent = onFocusBackToParent
    )
}

private const val FLOAT_PRESET_EPSILON = 0.0001f

@Composable
fun PresetMenuItem(
    modifier: Modifier = Modifier,
    value: Int = 100,
    text: String,
    values: List<Int>,
    onValueChange: (Int) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    require(values.isNotEmpty()) { "Preset values must not be empty" }

    PresetMenuItemLayout(
        modifier = modifier,
        text = text,
        onIncrease = {
            onValueChange(values.firstOrNull { preset -> preset > value } ?: values.last())
        },
        onDecrease = {
            onValueChange(values.lastOrNull { preset -> preset < value } ?: values.first())
        },
        onFocusBackToParent = onFocusBackToParent
    )
}

@Composable
private fun PresetMenuItemLayout(
    modifier: Modifier,
    text: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onFocusBackToParent: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                if (it.key == Key.DirectionRight) onFocusBackToParent()
                false
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painter = painterResource(R.drawable.ic_symbol_arrow_drop_up_filled), contentDescription = null)
            MenuListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent {
                        when (it.key) {
                            Key.DirectionUp -> {
                                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                onIncrease()
                                return@onPreviewKeyEvent true
                            }

                            Key.DirectionDown -> {
                                if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                                onDecrease()
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    },
                text = text,
                selected = false
            ) { }
            Icon(painter = painterResource(R.drawable.ic_symbol_arrow_drop_down_filled), contentDescription = null)
        }
    }
}
