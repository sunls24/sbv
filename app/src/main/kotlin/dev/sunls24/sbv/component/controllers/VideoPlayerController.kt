package dev.sunls24.sbv.component.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.biliapi.entity.video.Subtitle
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.entity.VideoAspectRatio
import dev.sunls24.sbv.entity.VideoListItem
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.ui.state.PlayerState
import dev.sunls24.sbv.ui.state.PlayerUiState
import dev.sunls24.sbv.ui.state.SeekerState
import dev.sunls24.sbv.util.toast
import dev.sunls24.sbv.viewmodel.player.DanmakuSettingAction
import dev.sunls24.sbv.viewmodel.player.MediaProfileSettingAction
import dev.sunls24.sbv.viewmodel.player.SubtitleSettingAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    aid: Long,
    fromSeason: Boolean,

    // play state
    isLooping: Boolean,
    isPlaying: Boolean,
    
    // UI related state
    uiState: PlayerUiState,
    seekerState: State<SeekerState>,

    // player events
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onGoTime: (time: Long) -> Unit,
    onBackToStart: () -> Unit,
    onCancelSkipToNextEp: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleSubtitle: () -> Unit,
    onGoToUpPage: () -> Unit,

    //menu events
    onMediaProfileSettingChange: (MediaProfileSettingAction) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onDanmakuSettingChange: (DanmakuSettingAction) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSettingChange: (SubtitleSettingAction) -> Unit,
    onRelatedVideoClicked: (VideoCardData) -> Unit,

    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activePanel by remember { mutableStateOf(PlayerControllerPanel.None) }
    val showInfoPanel = activePanel == PlayerControllerPanel.InfoSeek
    val clock by produceState(initialValue = currentClock(), key1 = showInfoPanel) {
        if (!showInfoPanel) return@produceState
        while (true) {
            value = currentClock()
            delay(1_000)
        }
    }
    var infoSeekFocus by remember { mutableStateOf(InfoSeekFocus.Seek) }
    var resumeAfterRelatedVideos by remember { mutableStateOf(false) }

    var lastPressBack by remember { mutableLongStateOf(0L) }
    var goTime by remember { mutableLongStateOf(0L) }
    var seekStartTime by remember { mutableLongStateOf(0L) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf(SeekDirection.Forward) }
    var seekChangeCount by remember { mutableIntStateOf(0) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }

    var seekCountdown: Job? by remember { mutableStateOf(null) }
    var hideInfoSeekControllerCountdown: Job? by remember { mutableStateOf(null) }

    fun calCoefficient(): Int {
        return if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }
    }

    fun onTimeForward() {
        isSeeking = true
        seekDirection = SeekDirection.Forward
        val targetTime = goTime + (10000 + calCoefficient() * 5000)
        goTime =
            if (targetTime > seekerState.value.totalDuration) seekerState.value.totalDuration else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
    }

    fun onTimeBack() {
        isSeeking = true
        seekDirection = SeekDirection.Backward
        val targetTime = goTime - (10000 + calCoefficient() * 5000)
        goTime = if (targetTime < 0) 0 else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
    }

    fun startSeekCountdown() {
        seekCountdown?.cancel()
        seekCountdown = scope.launch {
            delay(1000)

            onGoTime(goTime)
            if (!isPlaying) onPlay()

            isSeeking = false
        }
    }

    fun onDirection(direction: SeekDirection) {
        if (!isSeeking || seekDirection != direction) {
            seekStartTime = if (isSeeking) goTime else seekerState.value.currentTime
            if (!isSeeking) goTime = seekStartTime
        }
        when (direction) {
            SeekDirection.Backward -> onTimeBack()
            SeekDirection.Forward -> onTimeForward()
        }
        startSeekCountdown()
    }

    fun onDirectionLeft() = onDirection(SeekDirection.Backward)

    fun onDirectionRight() = onDirection(SeekDirection.Forward)

    fun onSeekGoTime() {
        onGoTime(goTime)
        isSeeking = false
        if (!isPlaying) onPlay()
        if (activePanel == PlayerControllerPanel.InfoSeek) {
            activePanel = PlayerControllerPanel.None
        }
        seekCountdown?.cancel()
    }

    fun onPlayPause() {
        if (isPlaying) onPause() else onPlay()
    }

    fun closeActivePanel() {
        val shouldResume =
            activePanel == PlayerControllerPanel.RelatedVideos && resumeAfterRelatedVideos

        activePanel = PlayerControllerPanel.None
        resumeAfterRelatedVideos = false

        if (shouldResume) onPlay()
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        // 中键需要区分短按和长按
        val isConfirmKey =
            event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.Spacebar

        if (event.type == KeyEventType.KeyUp && !isConfirmKey) {
            return true
        }


        when (event.key) {
            Key.Back -> {
                if (activePanel != PlayerControllerPanel.None) {
                    closeActivePanel()
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastPressBack < 3000) {
                        onExit()
                    } else {
                        lastPressBack = currentTime
                        R.string.video_player_press_back_again_to_exit.toast(context)
                    }
                }
                return true
            }

            Key.Menu -> {
                activePanel = if (activePanel == PlayerControllerPanel.Menu) {
                    PlayerControllerPanel.None
                } else {
                    PlayerControllerPanel.Menu
                }
                return true
            }

            Key(763) -> {
                activePanel = PlayerControllerPanel.Menu
                return true
            }

            Key.MediaPlayPause -> {
                onPlayPause()
                return true
            }

            Key.MediaPlay -> {
                if (!isPlaying) onPlay()
                return true
            }

            Key.MediaPause -> {
                if (isPlaying) onPause()
                return true
            }
        }

        // 错误页的重试按钮需要接收确认键，不能被播放器根布局提前消费。
        if (uiState.playerState is PlayerState.Error &&
            event.key in setOf(Key.DirectionCenter, Key.Enter, Key.Spacebar)
        ) {
            return false
        }

        if (activePanel != PlayerControllerPanel.None) {
            return false
        } else {
            when (event.key) {
                Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.nativeKeyEvent.isLongPress) {
                            activePanel = PlayerControllerPanel.Menu
                        }
                        return true
                    } else {
                        if (uiState.showBackToStart) {
                            onBackToStart()
                        } else {
                            onPlayPause()
                        }
                        return true
                    }
                }

                Key.DirectionUp -> {
                    activePanel = PlayerControllerPanel.VideoList
                    return true
                }

                Key.DirectionDown -> {
                    infoSeekFocus = InfoSeekFocus.Actions
                    activePanel = PlayerControllerPanel.InfoSeek
                    return true
                }

                Key.MediaRewind, Key.DirectionLeft -> {
                    if (uiState.showSkipToNextEp) onCancelSkipToNextEp()
                    onDirectionLeft()
                    return true
                }

                Key.MediaFastForward, Key.DirectionRight -> {
                    onDirectionRight()
                    return true
                }
            }
        }

        return false
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                // 重置 info 控制器的隐藏倒计时 (只要有按键活动就重置)
                if (activePanel == PlayerControllerPanel.InfoSeek) {
                    hideInfoSeekControllerCountdown?.cancel()
                    hideInfoSeekControllerCountdown = scope.launch {
                        delay(5000)
                        if (activePanel == PlayerControllerPanel.InfoSeek) {
                            activePanel = PlayerControllerPanel.None
                        }
                    }
                }
                // 调用分离出去的处理函数
                handleKeyEvent(event)
            }
    ) {
        content()
        if (uiState.subtitleId != -1L) {
            val currentTime = seekerState.value.currentTime

            BottomSubtitle(
                subtitleData = uiState.subtitleData,
                currentTime = currentTime,
                fontSize = uiState.subtitleState.fontSize,
                opacity = uiState.subtitleState.opacity,
                padding = uiState.subtitleState.bottomPadding,
            )
        }

        SkipTips(
            showBackToStart = uiState.showBackToStart,
            showSkipToNextEp = uiState.showSkipToNextEp,
            showPreviewTip = uiState.showPreviewTip,
        )

        PlayStateTips(
            isPlaying = uiState.playerState == PlayerState.Playing,
            isBuffering = uiState.isBuffering,
            isError = uiState.playerState is PlayerState.Error,
            errorMessage = (uiState.playerState as? PlayerState.Error)?.message,
            isRetrying = uiState.isRetrying,
            onRetry = onPlay,
        )

        RelatedVideosController(
            show = activePanel == PlayerControllerPanel.RelatedVideos,
            relatedVideos = uiState.relatedVideos,
            onVideoClicked = {
                resumeAfterRelatedVideos = false
                onRelatedVideoClicked(it)
                activePanel = PlayerControllerPanel.None
            }
        )

        ControllerVideoInfo(
            modifier = Modifier.focusable(),
            show = showInfoPanel,
            isSeeking = isSeeking,
            isPlaying = isPlaying,
            initialFocus = infoSeekFocus,
            goTime = goTime,
            seekerState = seekerState.value,
            title = uiState.title,
            clock = clock,
            fromSeason = fromSeason,
            danmakuEnabled = uiState.danmakuState.enabledTypes.isNotEmpty(),
            isLooping = isLooping,
            onDirectionLeft = { onDirectionLeft() },
            onDirectionRight = { onDirectionRight() },
            onSeekGoTime = { onSeekGoTime() },
            onPlayPause = { onPlayPause() },
            onDanmakuSwitchChange = {
                if (uiState.danmakuState.enabledTypes.isEmpty()) {
                    onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(DanmakuType.entries))
                } else {
                    onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(emptyList()))
                }
            },
            onShowSettings = {
                activePanel = PlayerControllerPanel.Menu
            },
            onShowRelatedVideos = {
                resumeAfterRelatedVideos = isPlaying
                if (resumeAfterRelatedVideos) onPause()

                activePanel = PlayerControllerPanel.RelatedVideos
            },
            onGoToVideoInfo = {
                VideoInfoActivity.showDetail(
                    context = context,
                    aid = aid
                )
            },
            onToggleLoop = onToggleLoop,
            onGoToUpPage = onGoToUpPage
        )

        if (isSeeking && activePanel == PlayerControllerPanel.None) {
            ControllerVideoInfoSeekProgress(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = ControllerVideoInfoActionRowHeight),
                isSeeking = true,
                goTime = goTime,
                seekerState = seekerState.value,
            )
        }

        val showSeekTip = isSeeking &&
            (activePanel == PlayerControllerPanel.None || activePanel == PlayerControllerPanel.InfoSeek)
        val seekDeltaSeconds = when (seekDirection) {
            SeekDirection.Backward -> (seekStartTime - goTime).coerceAtLeast(0L) / 1000L
            SeekDirection.Forward -> (goTime - seekStartTime).coerceAtLeast(0L) / 1000L
        }

        if (showSeekTip && seekDeltaSeconds > 0L) {
            val direction = seekDirection
            val text = when (direction) {
                SeekDirection.Backward -> stringResource(
                    R.string.video_player_seek_backward_by,
                    formatSeekDuration(seekDeltaSeconds),
                )

                SeekDirection.Forward -> stringResource(
                    R.string.video_player_seek_forward_by,
                    formatSeekDuration(seekDeltaSeconds),
                )
            }

            Text(
                text = text,
                modifier = Modifier
                    .align(
                        if (direction == SeekDirection.Backward) {
                            Alignment.BottomStart
                        } else {
                            Alignment.BottomEnd
                        }
                    )
                    .padding(
                        start = 64.dp,
                        end = 64.dp,
                        bottom = 120.dp,
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 22.sp,
            )
        }

        VideoListController(
            show = activePanel == PlayerControllerPanel.VideoList,
            currentCid = uiState.cid,
            videoList = uiState.availableVideoList,
            onPlayNewVideo = onPlayNewVideo
        )

        MenuController(
            show = activePanel == PlayerControllerPanel.Menu,
            uiState = uiState,
            onResolutionChange = { qualityId ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetQuality(qualityId)
                )
            },
            onCodecChange = { codec ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetVideoCodec(codec)
                )
            },
            onAudioChange = { audio ->
                onMediaProfileSettingChange(
                    MediaProfileSettingAction.SetAudio(audio)
                )
            },
            onAspectRatioChange = onAspectRatioChange,
            onPlaySpeedChange = onPlaySpeedChange,
            onDanmakuSwitchChange = { danmakuTypes ->
                onDanmakuSettingChange(DanmakuSettingAction.SetEnabledTypes(danmakuTypes))
            },
            onDanmakuSizeChange = { scale ->
                onDanmakuSettingChange(DanmakuSettingAction.SetScale(scale))
            },
            onDanmakuOpacityChange = { opacity ->
                onDanmakuSettingChange(DanmakuSettingAction.SetOpacity(opacity))
            },
            onDanmakuSpeedFactorChange = { factor ->
                onDanmakuSettingChange(DanmakuSettingAction.SetSpeedFactor(factor))
            },
            onDanmakuAreaChange = { area ->
                onDanmakuSettingChange(DanmakuSettingAction.SetArea(area))
            },
            onSubtitleChange = onSubtitleChange,
            onSubtitleSizeChange = { size ->
                onSubtitleSettingChange(SubtitleSettingAction.SetFontSize(size))
            },
            onSubtitleBackgroundOpacityChange = { opacity ->
                onSubtitleSettingChange(SubtitleSettingAction.SetOpacity(opacity))
            },
            onSubtitleBottomPadding = { padding ->
                onSubtitleSettingChange(SubtitleSettingAction.SetBottomPadding(padding))
            }
        )
    }
}

private fun formatSeekDuration(seconds: Long): String {
    if (seconds < 60) return "${seconds}秒"

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (remainingSeconds == 0L) {
        "${minutes}分钟"
    } else {
        "${minutes}分${remainingSeconds.toString().padStart(2, '0')}秒"
    }
}

private fun currentClock(): Pair<Int, Int> = Calendar.getInstance().let {
    it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
}

private enum class PlayerControllerPanel {
    None,
    VideoList,
    Menu,
    InfoSeek,
    RelatedVideos
}

private enum class SeekDirection {
    Backward,
    Forward
}
