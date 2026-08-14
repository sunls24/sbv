package dev.sunls24.sbv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.sunls24.sbv.R
import dev.sunls24.sbv.ui.state.SeekerState
import dev.sunls24.sbv.ui.theme.SBVFocus
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.util.formatHourMinSec

enum class InfoSeekFocus {
    Seek,
    Actions,
}

@Composable
fun ControllerVideoInfo(
    modifier: Modifier = Modifier,
    show: Boolean,
    isSeeking: Boolean,
    isPlaying: Boolean,
    initialFocus: InfoSeekFocus = InfoSeekFocus.Seek,
    goTime: Long,
    seekerState: SeekerState,
    title: String,
    clock: Pair<Int, Int>,
    fromSeason: Boolean,
    danmakuEnabled: Boolean,
    isLooping: Boolean,
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onPlayPause: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerTopVideoInfo"
        ) {
            ControllerVideoInfoTop(
                modifier = Modifier.align(Alignment.TopCenter),
                title = title,
                clock = clock
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = show,
            enter = expandVertically(),
            exit = shrinkVertically(),
            label = "ControllerBottomVideoInfo"
        ) {
            ControllerVideoInfoBottom(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                show = show,
                isSeeking = isSeeking,
                isPlaying = isPlaying,
                initialFocus = initialFocus,
                goTime = goTime,
                seekerState = seekerState,
                fromSeason = fromSeason,
                danmakuEnabled = danmakuEnabled,
                isLooping = isLooping,
                onDirectionLeft = onDirectionLeft,
                onDirectionRight = onDirectionRight,
                onSeekGoTime = onSeekGoTime,
                onPlayPause = onPlayPause,
                onDanmakuSwitchChange = onDanmakuSwitchChange,
                onShowSettings = onShowSettings,
                onShowRelatedVideos = onShowRelatedVideos,
                onGoToVideoInfo = onGoToVideoInfo,
                onToggleLoop = onToggleLoop,
                onGoToUpPage = onGoToUpPage
            )
        }
    }
}

@Composable
fun ControllerVideoInfoTop(
    modifier: Modifier = Modifier,
    title: String,
    clock: Pair<Int, Int>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                MaterialTheme.shapes.large.copy(
                    topStart = CornerSize(0.dp),
                    topEnd = CornerSize(0.dp)
                )
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f), // 上部颜色较深
                        Color.Black.copy(alpha = 0f)  // 下部颜色较浅
                    )
                )
            )
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        blurRadius = 1f
                    ),
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Clock(
                hour = clock.first,
                minute = clock.second,
            )
        }
    }
}

@Composable
fun ControllerVideoInfoBottom(
    modifier: Modifier = Modifier,
    show: Boolean,
    isSeeking: Boolean,
    isPlaying: Boolean,
    initialFocus: InfoSeekFocus = InfoSeekFocus.Seek,
    goTime: Long,
    seekerState: SeekerState,
    fromSeason: Boolean,
    danmakuEnabled: Boolean,
    isLooping: Boolean,
    onDirectionLeft: () -> Unit,
    onDirectionRight: () -> Unit,
    onSeekGoTime: () -> Unit,
    onPlayPause: () -> Unit,
    onDanmakuSwitchChange: () -> Unit,
    onShowSettings: () -> Unit,
    onShowRelatedVideos: () -> Unit,
    onGoToVideoInfo: () -> Unit,
    onToggleLoop: () -> Unit,
    onGoToUpPage: () -> Unit
) {
    val seekFocusRequester = remember { FocusRequester() }
    val buttonsFocusRequester = remember { FocusRequester() }

    var isSeekFocused by remember { mutableStateOf(false) }

    LaunchedEffect(show, initialFocus) {
        if (show) {
            val focusRequester = when (initialFocus) {
                InfoSeekFocus.Seek -> seekFocusRequester
                InfoSeekFocus.Actions -> buttonsFocusRequester
            }
            var focused = false
            repeat(2) {
                if (!focused) {
                    withFrameNanos { }
                    focused = runCatching {
                        focusRequester.requestFocus()
                    }.getOrDefault(false)
                }
            }
        }
    }
    Column(
        modifier = modifier
            .clip(
                MaterialTheme.shapes.large
                    .copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
            ),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.padding(bottom = 2.dp, start = 24.dp),
                text = "${if (isSeeking) goTime.formatHourMinSec() else seekerState.currentTime.formatHourMinSec()} / ${seekerState.totalDuration.formatHourMinSec()}",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    shadow = Shadow(color = Color.Black, blurRadius = 1f),
                ),
            )
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (isSeekFocused) 1f else 0f),
                    shape = MaterialTheme.shapes.medium
                )
                .focusProperties {
                    down = buttonsFocusRequester
                }
                .focusRequester(seekFocusRequester)
                .focusable()
                .onKeyEvent {
                    when (it.key) {
                        Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            if (isSeeking) {
                                onSeekGoTime()
                            } else {
                                onPlayPause()
                            }
                            return@onKeyEvent true
                        }

                        Key.DirectionLeft, Key.MediaRewind -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            onDirectionLeft()
                            return@onKeyEvent true
                        }

                        Key.DirectionRight, Key.MediaFastForward -> {
                            if (it.type == KeyEventType.KeyUp) return@onKeyEvent true
                            onDirectionRight()
                            return@onKeyEvent true
                        }

                    }
                    return@onKeyEvent false
                }
                .onFocusChanged {
                    isSeekFocused = it.isFocused
                },
        ) {
            VideoProgressSeek(
                modifier = Modifier
                    .fillMaxWidth(),
                duration = seekerState.totalDuration,
                position = if (isSeeking) goTime else seekerState.currentTime,
                bufferedPercentage = seekerState.bufferedPercentage,
                isPersistentSeek = false
            )
        }

        val icons = listOfNotNull(
            ((if (isPlaying) R.drawable.ic_symbol_pause_filled else R.drawable.ic_symbol_play_arrow_filled) to
                stringResource(
                    if (isPlaying) R.string.video_player_control_pause
                    else R.string.video_player_control_play
                )) to onPlayPause,
            ((if (danmakuEnabled) (R.drawable.danmaku_on_24px) else (R.drawable.danmaku_off_24px)) to
                stringResource(R.string.video_player_control_danmaku)) to onDanmakuSwitchChange,
            (R.drawable.settings_24px to stringResource(R.string.video_player_control_settings)) to onShowSettings,
            if (!fromSeason) {
                (R.drawable.info_24px to stringResource(R.string.video_card_action_detail)) to onGoToVideoInfo
            } else null,
            if (!fromSeason) {
                (R.drawable.ic_up to stringResource(R.string.video_card_action_up_page)) to onGoToUpPage
            } else null,
            if (!fromSeason) {
                (R.drawable.related_videos_24px to stringResource(R.string.video_player_control_related)) to onShowRelatedVideos
            } else null,
            ((if (isLooping) (R.drawable.repeat_one_on_24px) else (R.drawable.repeat_one_24px)) to
                stringResource(R.string.video_player_control_loop)) to onToggleLoop,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            icons.forEachIndexed { index, (icon, function) ->
                Surface(
                    modifier = Modifier
                        .focusProperties {
                            up = seekFocusRequester
                        }
                        .then(
                            if (index == 0) {
                                Modifier.focusRequester(buttonsFocusRequester)
                            } else {
                                Modifier
                            }
                        ),
                    onClick = function,
                    scale = ClickableSurfaceDefaults.scale(focusedScale = SBVFocus.focusedScale),
                    shape = ClickableSurfaceDefaults.shape(
                        shape = MaterialTheme.shapes.small,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = icon.first),
                            contentDescription = null,
                        )
                        Text(
                            text = icon.second,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Clock(
    modifier: Modifier = Modifier,
    hour: Int,
    minute: Int,
) {
    Text(
        modifier = modifier,
        color = Color.White,
        style = MaterialTheme.typography.displaySmall.copy(
            shadow = Shadow(color = Color.Black, blurRadius = 1f),
        ),
        text = "${"$hour".padStart(2, '0')}:${"$minute".padStart(2, '0')}",
    )
}

@Preview
@Composable
private fun ClockPreview() {
    val clock = Triple(12, 30, 30)
    SBVTheme {
        Clock(
            hour = clock.first,
            minute = clock.second,
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ControllerVideoInfoPreview() {
    var show by remember { mutableStateOf(true) }

    SBVTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { show = !show }) {
                Text(text = "Switch")
            }
        }
        ControllerVideoInfo(
            modifier = Modifier.fillMaxSize(),
            show = show,
            isSeeking = false,
            isPlaying = false,
            goTime = 0,
            seekerState = SeekerState(0, 0, 0),
            title = "【A320】民航史上最佳逆袭！A320的前世今生！民航史上最佳逆袭！A320的前世今生！",
            clock = Pair(12, 30),
            fromSeason = false,
            danmakuEnabled = false,
            isLooping = false,
            onDirectionRight = {},
            onDirectionLeft = {},
            onSeekGoTime = {},
            onPlayPause = {},
            onDanmakuSwitchChange = {},
            onShowSettings = {},
            onShowRelatedVideos = {},
            onGoToVideoInfo = {},
            onToggleLoop = {},
            onGoToUpPage = {},
        )
    }
}
