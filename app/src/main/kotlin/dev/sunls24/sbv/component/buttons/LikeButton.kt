package dev.sunls24.sbv.component.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButtonDefaults
import dev.sunls24.sbv.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LikeButton(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedColor = animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(
            durationMillis = 2000,
            easing = FastOutSlowInEasing
        )
    )
    Button(
        modifier = modifier.onPreviewKeyEvent {
            when (it.key) {
                Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                    if (it.type == KeyEventType.KeyDown) {
                        if (!isPressed) {
                            coroutineScope.launch {
                                repeat(20) { index ->
                                    if (!isPressed) return@launch
                                    progress = index / 20f
                                    delay(100)
                                }
                                if (progress >= 0.95f) {
                                    onLongClick()
                                }
                            }
                        }
                        isPressed = true
                    } else {
                        isPressed = false
                        if (progress < 0.95f) onClick()
                    }
                }
            }
            false
        },
        colors = ButtonDefaults.colors(pressedContainerColor = animatedColor.value),
        onClick = {}
    ) {
        Icon(
            painter = painterResource(if (isLiked) R.drawable.ic_symbol_thumb_up_filled else R.drawable.ic_symbol_thumb_up),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
fun LikeButtonEnablePreview() {
    LikeButton(
        isLiked = false,
        onClick = {},
        onLongClick = {}
    )
}
