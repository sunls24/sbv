package dev.sunls24.sbv.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.component.DanmakuPlayerCompose
import dev.sunls24.sbv.component.controllers.VideoPlayerController
import dev.sunls24.sbv.component.controllers.VideoProgressSeek
import dev.sunls24.sbv.entity.VideoAspectRatio
import dev.sunls24.sbv.entity.VideoListItem
import dev.sunls24.sbv.player.SBVVideoPlayer
import dev.sunls24.sbv.ui.effect.PlayerUiEffect
import dev.sunls24.sbv.ui.state.PlayerState
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.viewmodel.player.VideoPlayerV3ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel

@Composable
fun VideoPlayerV3Screen(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerV3ViewModel = koinViewModel(),
    onRetryInitialization: () -> Unit = {},
) {
    val context = LocalContext.current
    val videoPlayer = playerViewModel.videoPlayer
    val danmakuPlayer = playerViewModel.danmakuPlayer

    var isLooping by remember { mutableStateOf(false) }
    var showPersistentSeek by remember { mutableStateOf(Prefs.showPersistentSeek) }
    val uiState by playerViewModel.uiState.collectAsState()
    val seekerState = playerViewModel.seekerState.collectAsState()

    LaunchedEffect(Unit) {
        playerViewModel.uiEffect.collect { effect ->
            when (effect) {
                PlayerUiEffect.FinishActivity -> {
                    (context as Activity).finish()
                }

                PlayerUiEffect.PlayEnded -> {
                    if (isLooping) {
                        playerViewModel.backToStart()
                        return@collect
                    }

                    playerViewModel.checkAndPlayNext()
                }
            }
        }
    }

    // 循环发送心跳
    LaunchedEffect(Unit) {
        delay(5000)
        while (isActive) {
            if (playerViewModel.videoPlayer?.isPlaying == true) {
                playerViewModel.trySendHeartbeat()
            }
            // 周期延迟
            delay(15000)
        }
    }

    VideoPlayerController(
        modifier = modifier,
        aid = uiState.aid,
        fromSeason = uiState.fromSeason,
        isLooping = isLooping,
        isPlaying = videoPlayer?.isPlaying ?: false,
        uiState = uiState,
        seekerState = seekerState,
        onPlay = {
            if (uiState.playerState is PlayerState.Error) {
                when {
                    uiState.isRetrying -> Unit
                    !playerViewModel.isInitialized -> onRetryInitialization()
                    else -> {
                        val retryPosition = seekerState.value.currentTime.coerceAtLeast(0L)
                        playerViewModel.loadVideoWithResources(
                            startPosition = retryPosition,
                            randomizeCdn = true,
                        )
                    }
                }
            } else {
                videoPlayer?.play()
            }
        },
        onPause = {
            videoPlayer?.pause()
            playerViewModel.trySendHeartbeat()
        },
        onExit = {
            (context as Activity).finish()
        },
        onGoTime = { time ->
            playerViewModel.seekToTime(time)
        },
        onBackToStart = { playerViewModel.backToStart() },
        onPlayNewVideo = {
            playerViewModel.trySendHeartbeat()
            playerViewModel.playNewVideo(it)
        },
        onPlayPrevious = {
            playerViewModel.playPreviousNow()
        },
        onPlayNext = {
            playerViewModel.playNextNow()
        },
        onCancelSkipToNextEp = {
            playerViewModel.cancelPlayNext()
        },
        onToggleLoop = {
            isLooping = !isLooping
        },
        onToggleSubtitle = {
            playerViewModel.toggleSubtitle()
        },
        onGoToUpPage = {
            UpInfoActivity.actionStart(
                context,
                mid = uiState.authorMid,
                name = uiState.authorName
            )
        },

        onMediaProfileSettingChange = { action ->
            playerViewModel.updateMediaProfile(action)
        },
        onAspectRatioChange = { aspectRadio ->
            playerViewModel.updateVideoAspectRatio(aspectRadio)
        },
        onPlaySpeedChange = { speed ->
            playerViewModel.updatePlaySpeed(speed)
        },
        onDanmakuSettingChange = { action ->
            playerViewModel.updateDanmakuState(action)
        },
        onSubtitleChange = { subtitle ->
            playerViewModel.loadSubtitle(subtitle.id)
        },
        onSubtitleSettingChange = { action ->
            playerViewModel.updateSubtitleState(action)
        },
        onRelatedVideoClicked = { video ->
            video.cid?.let {
                playerViewModel.playNewVideo(
                    VideoListItem(
                        aid = video.avid,
                        cid = video.cid,
                        title = video.title,
                        authorMid = video.upMid ?: 0L,
                        authorName = video.upName,
                    )
                )
            }
        },
    ) {
        Box(
            modifier = Modifier.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val aspectRatio = when (uiState.aspectRatio) {
                VideoAspectRatio.Default -> {
                    if (uiState.videoHeight > 0 && uiState.videoWidth > 0) {
                        uiState.videoWidth / uiState.videoHeight.toFloat()
                    } else {
                        16 / 9f
                    }
                }

                VideoAspectRatio.FourToThree -> 4 / 3f
                VideoAspectRatio.SixteenToNine -> 16 / 9f
            }

            SBVVideoPlayer(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspectRatio)
                    .align(Alignment.Center),
                videoPlayer = videoPlayer,
            )
            DanmakuPlayerCompose(
                modifier = Modifier
                    .fillMaxSize()
                    // 在之前版本中，设置 DanmakuConfig 透明度后，更改其它弹幕设置后，可能会导致弹幕透明度
                    // 突然变成完全不透明一瞬间，因此这次新版选择直接在此处设置透明度
                    .alpha(uiState.danmakuState.opacity),
                danmakuPlayer = danmakuPlayer
            )
            if (showPersistentSeek) {
                VideoProgressSeek(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    duration = seekerState.value.totalDuration,
                    position = seekerState.value.currentTime,
                    bufferedPercentage = seekerState.value.bufferedPercentage,
                    isPersistentSeek = true
                )
            }
        }
    }
}
