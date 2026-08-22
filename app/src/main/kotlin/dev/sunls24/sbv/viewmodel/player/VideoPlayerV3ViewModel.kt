package dev.sunls24.sbv.viewmodel.player

import android.util.Log
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.sunls24.biliapi.http.BiliHttpApi
import dev.sunls24.biliapi.repositories.VideoPlayRepository
import dev.sunls24.biliapi.repositories.AuthRepository
import dev.sunls24.sbv.player.subtitle.SubtitleParser
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.R
import dev.sunls24.sbv.component.controllers.DanmakuType
import dev.sunls24.sbv.entity.Audio
import dev.sunls24.sbv.entity.PlaybackEndAction
import dev.sunls24.sbv.entity.Resolution
import dev.sunls24.sbv.entity.VideoAspectRatio
import dev.sunls24.sbv.entity.VideoCodec
import dev.sunls24.sbv.entity.VideoListItem
import dev.sunls24.sbv.player.SBVPlayer
import dev.sunls24.sbv.player.SBVPlayerOptions
import dev.sunls24.sbv.player.PlaybackResourceLoader
import dev.sunls24.sbv.player.PlaybackResources
import dev.sunls24.sbv.player.DanmakuSession
import dev.sunls24.sbv.repository.VideoInfoRepository
import dev.sunls24.sbv.repository.PlaybackProgress
import dev.sunls24.sbv.repository.PlaybackProgressReporter
import dev.sunls24.sbv.ui.effect.PlayerUiEffect
import dev.sunls24.sbv.ui.state.DanmakuState
import dev.sunls24.sbv.ui.state.MediaProfileState
import dev.sunls24.sbv.ui.state.PlayerState
import dev.sunls24.sbv.ui.state.PlayerUiState
import dev.sunls24.sbv.ui.state.SeekerState
import dev.sunls24.sbv.ui.state.SubtitleState
import dev.sunls24.sbv.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import kotlin.coroutines.cancellation.CancellationException

@KoinViewModel

class VideoPlayerV3ViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository,
    private val progressReporter: PlaybackProgressReporter,
    private val authRepository: AuthRepository
) : ViewModel() {

    private companion object {
        const val TAG = "VideoPlayer"
    }

    var videoPlayer: SBVPlayer? by mutableStateOf(null)
        private set
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
        private set

    private val playbackResourceLoader = PlaybackResourceLoader(videoPlayRepository)
    private val danmakuSession = DanmakuSession()
    private var initialized = false
    val isInitialized: Boolean get() = initialized
    private var relatedLoadedAid = 0L

    private val detachedWorkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val _seekerState = MutableStateFlow(SeekerState())
    val seekerState = _seekerState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlayerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var seekerUpdateJob: Job? = null
    private var loadVideoJob: Job? = null
    private var subtitleJob: Job? = null
    private var danmakuLoadJob: Job? = null
    private var danmakuLoadingCid: Long? = null
    private var danmakuLoadedCid: Long? = null

    private var backToStartCountdownJob: Job? = null
    private var pendingBackToStartPrompt = false
    private var playNextCountdownJob: Job? = null
    private var previewTipCountdownJob: Job? = null

    private val videoPlayerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error", error)
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    isRetrying = false,
                    playerState = PlayerState.Error(
                        error.message ?: "Unknown error"
                    )
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    danmakuSession.pause()
                    stopSeekerUpdater()
                    _uiState.update {
                        if (it.isRetrying) it else it.copy(isBuffering = false)
                    }
                }
                Player.STATE_BUFFERING -> {
                    danmakuSession.pause()
                    _uiState.update { it.copy(isBuffering = true) }
                }
                Player.STATE_READY -> {
                    _uiState.update {
                        it.copy(
                            playerState = PlayerState.Ready,
                            isBuffering = false,
                            isRetrying = false,
                        )
                    }
                    applyPlaySpeed(_uiState.value.playSpeed)
                }
                Player.STATE_ENDED -> {
                    danmakuSession.pause()
                    stopSeekerUpdater()
                    _uiState.update {
                        it.copy(playerState = PlayerState.Ended, isRetrying = false)
                    }
                    viewModelScope.launch {
                        _uiEffect.emit(PlayerUiEffect.PlayEnded)
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                danmakuSession.start()
                startSeekerUpdater()
                _uiState.update {
                    it.copy(
                        playerState = PlayerState.Playing,
                        isBuffering = false,
                        isRetrying = false,
                    )
                }
                if (pendingBackToStartPrompt) {
                    pendingBackToStartPrompt = false
                    showBackToStartPrompt()
                }
            } else {
                danmakuSession.pause()
                stopSeekerUpdater()
                if (_uiState.value.playerState != PlayerState.Ended &&
                    !_uiState.value.isRetrying &&
                    _uiState.value.playerState !is PlayerState.Error
                ) {
                    _uiState.update { it.copy(playerState = PlayerState.Paused) }
                }
            }
        }
    }

    fun init(
        aid: Long,
        cid: Long,
        epid: Int?,
        title: String,
        lastPlayed: Int,
        fromSeason: Boolean,
        subType: Int,
        seasonId: Int,
        authorMid: Long = 0,
        authorName: String
    ): Boolean {
        if (initialized) return false
        initialized = true

        _uiState.update {
            it.copy(
                aid = aid,
                cid = cid,
                epid = epid.takeIf { epid -> epid != 0 },
                seasonId = seasonId,
                title = title,
                lastPlayed = lastPlayed,
                fromSeason = fromSeason,
                subType = subType,
                authorMid = authorMid,
                authorName = authorName,
                mediaProfileState = MediaProfileState(
                    qualityId = Prefs.defaultQuality.code,
                    videoCodec = Prefs.defaultVideoCodec,
                    audio = Prefs.defaultAudio
                ),
                playSpeed = Prefs.defaultPlaySpeed.speed,
                danmakuState = DanmakuState(
                    scale = Prefs.defaultDanmakuScale,
                    opacity = Prefs.defaultDanmakuOpacity,
                    area = Prefs.defaultDanmakuArea,
                    speedFactor = Prefs.defaultDanmakuSpeedFactor,
                    enabledTypes = Prefs.defaultDanmakuTypes,
                ),
                subtitleState = SubtitleState(
                    fontSize = Prefs.defaultSubtitleFontSize,
                    opacity = Prefs.defaultSubtitleBackgroundOpacity,
                    bottomPadding = Prefs.defaultSubtitleBottomPadding
                )
            )
        }

        videoInfoRepository.videoList
            .onEach { newList ->
                // 过滤DetailViewModel销毁时repo重置
                if (newList.isEmpty()) return@onEach

                _uiState.update { currentState ->
                    currentState.copy(availableVideoList = newList)
                }
            }
            .launchIn(viewModelScope)

        return true
    }

    suspend fun initDirectPlayback(aid: Long): DirectPlaybackInitResult {
        if (initialized) return DirectPlaybackInitResult.Ready

        val data = try {
            withContext(Dispatchers.IO) { videoInfoRepository.resolveDirectPlayback(aid) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return DirectPlaybackInitResult.Failure(error.message ?: "视频信息加载失败")
        }

        data.redirectEpid?.let { return DirectPlaybackInitResult.RedirectToSeason(it) }
        videoInfoRepository.updateVideoList(data.videoList)
        init(
            aid = data.aid,
            cid = data.cid,
            epid = null,
            title = data.title,
            lastPlayed = data.played,
            fromSeason = false,
            subType = 0,
            seasonId = 0,
            authorMid = data.author.mid,
            authorName = data.author.name
        )
        relatedLoadedAid = data.aid
        _uiState.update { it.copy(relatedVideos = data.relatedVideos) }
        return DirectPlaybackInitResult.Ready
    }

    fun showInitializationError(message: String) {
        _uiState.update {
            it.copy(
                isBuffering = false,
                isRetrying = false,
                playerState = PlayerState.Error(message),
            )
        }
    }

    fun beginInitializationRetry(): Boolean {
        if (initialized || _uiState.value.isRetrying) return false
        _uiState.update {
            it.copy(
                isBuffering = true,
                isRetrying = true,
            )
        }
        return true
    }

    fun initVideoPlayer(context: Context) {
        if (videoPlayer != null) return

        val options = SBVPlayerOptions(
            userAgent = context.getString(R.string.video_player_user_agent_http),
            referer = context.getString(R.string.video_player_referer),
            enableSoftwareVideoDecoder = Prefs.enableSoftwareVideoDecoder
        )

        val newVideoPlayer = SBVPlayer(context.applicationContext, options)
        newVideoPlayer.addListener(videoPlayerListener)
        videoPlayer = newVideoPlayer
    }

    fun detachPlayer() {
        loadVideoJob?.cancel()
        loadVideoJob = null
        syncProgress(scope = detachedWorkScope, isDetaching = true)

        videoPlayer?.release()
        videoPlayer = null
    }

    fun initDanmakuPlayer() {
        if (danmakuPlayer != null || _uiState.value.danmakuState.enabledTypes.isEmpty()) {
            return
        }
        danmakuPlayer = danmakuSession.initialize(_uiState.value.danmakuState)
        danmakuSession.seekTo(videoPlayer?.currentPosition ?: 0L)
    }

    fun releaseDanmakuPlayer() {
        danmakuLoadJob?.cancel()
        danmakuLoadJob = null
        danmakuLoadingCid = null
        danmakuLoadedCid = null
        danmakuSession.release()
        danmakuPlayer = null
    }

    override fun onCleared() {
        loadVideoJob?.cancel()
        videoPlayer?.release()
        videoPlayer = null
        releaseDanmakuPlayer()
    }

    fun loadSubtitle(id: Long) {
        subtitleJob?.cancel()
        if (id == -1L) {
            _uiState.update {
                it.copy(
                    subtitleId = -1,
                    subtitleData = emptyList()
                )
            }
            return
        }

        val state = _uiState.value
        val subtitle = state.subtitleList.find { it.id == id } ?: return
        val requestedAid = state.aid
        val requestedCid = state.cid
        subtitleJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val responseText = BiliHttpApi.downloadText(subtitle.url)
                val subtitleData = SubtitleParser.fromBccString(responseText)
                _uiState.update {
                    if (it.aid != requestedAid || it.cid != requestedCid) return@update it
                    it.copy(
                        subtitleId = id,
                        subtitleData = subtitleData
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
    }

    fun updatePlaySpeed(speed: Float) {
        if (_uiState.value.playSpeed == speed) return
        _uiState.update { it.copy(playSpeed = speed) }
        applyPlaySpeed(speed)
    }

    private fun applyPlaySpeed(speed: Float) {
        videoPlayer?.speed = speed
        danmakuSession.updatePlaySpeed(speed)
    }

    fun updateVideoAspectRatio(aspectRatio: VideoAspectRatio) {
        _uiState.update {
            it.copy(aspectRatio = aspectRatio)
        }
    }

    fun updateMediaProfile(action: MediaProfileSettingAction) {
        val old = _uiState.value.mediaProfileState
        val new = when (action) {
            is MediaProfileSettingAction.SetQuality -> old.copy(qualityId = action.value)
            is MediaProfileSettingAction.SetVideoCodec -> old.copy(videoCodec = action.value)
            is MediaProfileSettingAction.SetAudio -> old.copy(audio = action.value)
        }

        if (old == new) return

        _uiState.update {
            it.copy(
                mediaProfileState = new,
            )
        }

        videoPlayer?.let { player ->
            val currentPosition = _seekerState.value.currentTime.coerceAtLeast(0L)
            val shouldPlay = player.isPlaying || _uiState.value.playerState == PlayerState.Playing
            player.pause()
            loadVideoWithResources(
                startPosition = currentPosition,
                autoPlay = shouldPlay
            )
        }
    }

    fun updateDanmakuState(action: DanmakuSettingAction) {
        val old = _uiState.value.danmakuState
        val new = when (action) {
            is DanmakuSettingAction.SetScale -> old.copy(scale = action.value)
            is DanmakuSettingAction.SetOpacity -> old.copy(opacity = action.value)
            is DanmakuSettingAction.SetArea -> old.copy(area = action.value)
            is DanmakuSettingAction.SetSpeedFactor -> old.copy(speedFactor = action.value)
            is DanmakuSettingAction.SetEnabledTypes -> old.copy(enabledTypes = action.types)
        }

        if (old == new) return

        // 首先更新UI
        _uiState.update { it.copy(danmakuState = new) }

        when {
            old.enabledTypes.isEmpty() && new.enabledTypes.isNotEmpty() -> {
                initDanmakuPlayer()
                val cid = _uiState.value.cid
                if (cid > 0L) {
                    startDanmakuLoad(cid)
                }
            }
            old.enabledTypes.isNotEmpty() && new.enabledTypes.isEmpty() -> {
                releaseDanmakuPlayer()
            }
            new.enabledTypes.isNotEmpty() -> danmakuSession.updateSettings(old, new)
        }

        // ===== 副作用处理 =====
        if (new.enabledTypes != old.enabledTypes) {
            Prefs.defaultDanmakuTypes = new.enabledTypes
        }
        if (new.scale != old.scale) {
            Prefs.defaultDanmakuScale = new.scale
        }
        if (new.speedFactor != old.speedFactor) {
            Prefs.defaultDanmakuSpeedFactor = new.speedFactor
        }
        if (new.area != old.area) {
            Prefs.defaultDanmakuArea = new.area
        }
        if (new.opacity != old.opacity) {
            Prefs.defaultDanmakuOpacity = new.opacity
        }
    }

    fun updateSubtitleState(action: SubtitleSettingAction) {
        val old = _uiState.value.subtitleState
        val new = when (action) {
            is SubtitleSettingAction.SetFontSize -> old.copy(fontSize = action.value)
            is SubtitleSettingAction.SetOpacity -> old.copy(opacity = action.value)
            is SubtitleSettingAction.SetBottomPadding -> old.copy(bottomPadding = action.value)
        }

        if (old == new) return

        _uiState.update { it.copy(subtitleState = new) }

        // ===== 持久化副作用 =====
        if (new.fontSize != old.fontSize) {
            Prefs.defaultSubtitleFontSize = new.fontSize
        }

        if (new.opacity != old.opacity) {
            Prefs.defaultSubtitleBackgroundOpacity = new.opacity
        }

        if (new.bottomPadding != old.bottomPadding) {
            Prefs.defaultSubtitleBottomPadding = new.bottomPadding
        }
    }

    /**
     * 触发播放结束后的检查逻辑
     */
    fun checkAndPlayNext() {
        when (Prefs.actionAfterPlay) {
            PlaybackEndAction.Pause -> return
            PlaybackEndAction.Exit -> {
                viewModelScope.launch {
                    _uiEffect.emit(PlayerUiEffect.FinishActivity)
                }

                return
            }

            PlaybackEndAction.PlayNext -> {
                /* 继续执行 */
            }
        }

        val nextTarget = findNextPlayTarget()

        // 3. 根据查找结果执行操作
        if (nextTarget != null) {
            startNextEpisodeCountdown(nextTarget)
        } else {
            // 没有下一集了，发送事件关闭页面
            viewModelScope.launch {
                _uiEffect.emit(PlayerUiEffect.FinishActivity)
            }
        }
    }

    fun playNextNow() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }

        findNextPlayTarget()?.let(::playNewVideo)
    }

    fun playPreviousNow() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }

        findPreviousPlayTarget()?.let(::playNewVideo)
    }

    fun toggleSubtitle() {
        val state = _uiState.value
        if (state.subtitleId != -1L) {
            loadSubtitle(-1L)
            return
        }

        val firstSubtitleId = state.subtitleList
            .firstOrNull { it.id != -1L }
            ?.id
            ?: return
        loadSubtitle(firstSubtitleId)
    }

    fun cancelPlayNext() {
        playNextCountdownJob?.cancel()
        _uiState.update { it.copy(showSkipToNextEp = false) }
    }

    fun backToStart() {
        backToStartCountdownJob?.cancel()
        _uiState.update { it.copy(showBackToStart = false) }

        videoPlayer?.seekTo(0)
        _seekerState.update { it.copy(currentTime = 0L) }
        danmakuSession.seekTo(0)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuSession.pause()
    }

    /**
     * 开始周期性更新播放进度
     */
    fun startSeekerUpdater() {
        // 防止重复启动
        if (seekerUpdateJob?.isActive == true) return

        seekerUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                updateSeekerState()
                delay(1000)
            }
        }
    }

    fun seekToTime(time: Long) {
        videoPlayer?.seekTo(time)
        _seekerState.update { it.copy(currentTime = time) }
        danmakuSession.seekTo(time)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuSession.pause()
    }

    fun playNewVideo(newVideo: VideoListItem) {
        videoPlayer?.pause()
        subtitleJob?.cancel()
        pendingBackToStartPrompt = false

        val state = _uiState.value
        val shouldUpdateVideoList = state.availableVideoList.none {
            it.aid == newVideo.aid && it.cid == newVideo.cid
        }

        // 新视频不在当前视频列表时更新列表
        if (shouldUpdateVideoList) {
            videoInfoRepository.updateVideoList(listOf(newVideo))
        }

        // 更新播放历史并上传
        syncProgress(viewModelScope)

        // 重置弹幕
        releaseDanmakuPlayer()
        initDanmakuPlayer()

        // 新视频不继承上一个视频的进度或续播位置
        _seekerState.value = SeekerState()

        // 更新UiState
        _uiState.update {
            it.copy(
                aid = newVideo.aid,
                cid = newVideo.cid,
                epid = newVideo.epid,
                seasonId = newVideo.seasonId ?: 0,
                title = newVideo.title,
                authorMid = newVideo.authorMid ?: it.authorMid,
                authorName = newVideo.authorName ?: it.authorName,
                lastPlayed = 0,
                isBuffering = true,
                subtitleList = emptyList(),
                subtitleData = emptyList(),
                relatedVideos = emptyList(),
            )
        }
        relatedLoadedAid = 0L

        // 加载新播放url
        loadVideoWithResources()
    }

    fun trySendHeartbeat() {
        syncProgress(scope = viewModelScope, updateLocal = false)
    }

    fun loadVideoWithResources(
        startPosition: Long? = null,
        autoPlay: Boolean = true,
        randomizeCdn: Boolean = false,
    ) {
        val state = _uiState.value
        val avid = state.aid
        val cid = state.cid
        val epid = state.epid
        val fromSeason = state.fromSeason
        val profile = state.mediaProfileState
        val resolvedStartPosition = (startPosition ?: state.lastPlayed.toLong()).coerceAtLeast(0L)
        val isInitialResume = startPosition == null && resolvedStartPosition > 0L

        loadVideoJob?.cancel()
        _uiState.update { current ->
            if (current.aid == avid && current.cid == cid) {
                current.copy(
                    isBuffering = true,
                    isRetrying = randomizeCdn,
                    playerState = if (randomizeCdn) {
                        current.playerState
                    } else {
                        PlayerState.Ready
                    }
                )
            } else {
                current
            }
        }
        loadVideoJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val resources = loadMediaResources(
                    avid = avid,
                    cid = cid,
                    epid = epid ?: 0,
                    fromSeason = fromSeason,
                    profile = profile,
                    randomizeCdn = randomizeCdn,
                )
                val shouldContinue = withContext(Dispatchers.Main) {
                    if (!isCurrentMediaRequest(avid, cid, profile) || videoPlayer == null) {
                        return@withContext false
                    }

                    val player = checkNotNull(videoPlayer) { "Video player is not initialized" }
                    applyPlaybackResources(resources)
                    player.setMedia(resources.media.videoUrl, resources.media.audioUrl)
                    if (resolvedStartPosition > 0L) {
                        player.seekTo(resolvedStartPosition)
                        danmakuSession.seekTo(resolvedStartPosition)
                        danmakuSession.pause()
                        _seekerState.update { it.copy(currentTime = resolvedStartPosition) }
                    }
                    player.prepare()
                    if (isInitialResume) {
                        _uiState.update { it.copy(lastPlayed = 0) }
                        pendingBackToStartPrompt = true
                    }
                    if (autoPlay) {
                        player.play()
                    }
                    true
                }
                if (!shouldContinue) return@launch

                launch {
                    updateSubtitle()
                    val lastPlayEnabledSubtitle = _uiState.value.subtitleId != -1L
                    if (lastPlayEnabledSubtitle) {
                        _uiState.value.subtitleList
                            .firstOrNull { it.id != -1L }
                            ?.let { loadSubtitle(it.id) }
                    }
                }
                withContext(Dispatchers.Main.immediate) {
                    if (_uiState.value.danmakuState.enabledTypes.isNotEmpty()) {
                        startDanmakuLoad(cid)
                    }
                }
                launch { loadRelatedVideos(avid) }
            } catch (e: CancellationException) {
                throw e // 让结构化并发正常取消，不作为播放错误处理
            } catch (e: Exception) {
                if (videoPlayer == null || !isCurrentMediaRequest(avid, cid, profile)) {
                    return@launch
                }
                Log.e(TAG, "Video loading failed", e)

                _uiState.update {
                    it.copy(
                        isBuffering = false,
                        isRetrying = false,
                        playerState = PlayerState.Error(e.message ?: "未知错误")
                    )
                }
            }
        }
    }

    private suspend fun loadRelatedVideos(aid: Long) {
        if (_uiState.value.fromSeason || relatedLoadedAid == aid) return
        runCatching { videoInfoRepository.getRelatedVideos(aid) }
            .onSuccess { relatedVideos ->
                if (_uiState.value.aid == aid) {
                    relatedLoadedAid = aid
                    _uiState.update { it.copy(relatedVideos = relatedVideos) }
                }
            }
    }

    private suspend fun loadMediaResources(
        avid: Long,
        cid: Long,
        epid: Int,
        fromSeason: Boolean,
        profile: MediaProfileState,
        randomizeCdn: Boolean,
    ): PlaybackResources = playbackResourceLoader.load(
        aid = avid,
        cid = cid,
        epid = epid,
        fromSeason = fromSeason,
        preferredQuality = profile.qualityId,
        preferredCodec = profile.videoCodec,
        preferredAudio = profile.audio,
        randomizeCdn = randomizeCdn,
    )

    private fun applyPlaybackResources(resources: PlaybackResources) {
        val resolutionMap = resources.qualities.associateWith { quality ->
            Resolution.fromCode(quality)
                .getShortDisplayName(SBVApp.context)
        }

        _uiState.update {
            it.copy(
                availableQuality = resolutionMap,
                availableAudio = resources.audio,
                availableVideoCodec = resources.codecs,
                videoHeight = resources.media.height,
                videoWidth = resources.media.width,
                mediaProfileState = it.mediaProfileState.copy(
                    qualityId = resources.selectedQuality,
                    videoCodec = resources.selectedCodec,
                    audio = resources.selectedAudio
                )
            )
        }

        if (resources.needPay) startShowPreviewTipCountdown()
    }

    private fun isCurrentMediaRequest(
        aid: Long,
        cid: Long,
        profile: MediaProfileState
    ): Boolean {
        val state = _uiState.value
        return state.aid == aid &&
            state.cid == cid &&
            state.mediaProfileState == profile
    }

    private fun startDanmakuLoad(cid: Long) {
        if (danmakuLoadedCid == cid ||
            (danmakuLoadingCid == cid && danmakuLoadJob?.isActive == true)
        ) {
            return
        }

        danmakuLoadJob?.cancel()
        danmakuLoadingCid = cid
        danmakuLoadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = BiliHttpApi.getDanmakuXml(
                    cid = cid,
                    sessData = authRepository.sessionData.orEmpty(),
                    transform = danmakuSession::toItemData
                ).data

                withContext(Dispatchers.Main.immediate) {
                    if (_uiState.value.cid != cid ||
                        _uiState.value.danmakuState.enabledTypes.isEmpty()
                    ) {
                        return@withContext
                    }
                    danmakuSession.updateData(data)
                    danmakuLoadedCid = cid
                    if (videoPlayer?.isPlaying == true) {
                        danmakuSession.start()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun updateSubtitle() {
        val state = _uiState.value

        runCatching {
            val subtitleList = videoPlayRepository.getSubtitle(
                aid = state.aid,
                cid = state.cid,
            )
            _uiState.update { currentState ->
                currentState.copy(
                    subtitleList = subtitleList
                )
            }
        }
    }

    private fun syncProgress(
        scope: CoroutineScope,
        updateLocal: Boolean = true,
        isDetaching: Boolean = false
    ) {
        val player = videoPlayer ?: return
        val state = _uiState.value

        val currentTime = (player.currentPosition.coerceAtLeast(0) / 1000).toInt()
        val totalTime = (player.duration.coerceAtLeast(0) / 1000).toInt()
        val reportTime = if (totalTime > 0 && currentTime >= totalTime) -1 else currentTime

        progressReporter.report(
            scope = scope,
            progress = PlaybackProgress(
                aid = state.aid,
                cid = state.cid,
                time = reportTime,
                fromSeason = state.fromSeason,
                subType = state.subType,
                epid = state.epid,
                seasonId = state.seasonId
            ),
            updateLocal = updateLocal,
            timeoutMillis = 3000L.takeIf { isDetaching }
        )
    }

    private fun findNextPlayTarget(): VideoListItem? {
        val currentState = _uiState.value
        val videoList = currentState.availableVideoList

        val videoListIndex = videoList.indexOfFirst {
            it.aid == currentState.aid && it.cid == currentState.cid
        }
        if (videoListIndex == -1) return null

        if (videoListIndex + 1 < videoList.size) {
            return videoList[videoListIndex + 1]
        }

        return null
    }

    private fun findPreviousPlayTarget(): VideoListItem? {
        val currentState = _uiState.value
        val videoList = currentState.availableVideoList

        val videoListIndex = videoList.indexOfFirst {
            it.aid == currentState.aid && it.cid == currentState.cid
        }
        if (videoListIndex == -1) return null

        if (videoListIndex > 0) {
            return videoList[videoListIndex - 1]
        }

        return null
    }

    private fun startNextEpisodeCountdown(target: VideoListItem) {
        playNextCountdownJob?.cancel()

        playNextCountdownJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showSkipToNextEp = true,
                )
            }
            delay(5000)

            playNewVideo(target)
            _uiState.update { it.copy(showSkipToNextEp = false) }
        }
    }

    private fun startShowPreviewTipCountdown() {
        previewTipCountdownJob?.cancel()

        previewTipCountdownJob = viewModelScope.launch {
            _uiState.update {
                it.copy(showPreviewTip = true)
            }

            delay(5000)

            _uiState.update {
                it.copy(showPreviewTip = false)
            }
        }
    }

    private fun showBackToStartPrompt() {
        _uiState.update { it.copy(showBackToStart = true) }

        backToStartCountdownJob?.cancel()
        backToStartCountdownJob = viewModelScope.launch {
            delay(5000)
            _uiState.update { it.copy(showBackToStart = false) }
        }
    }

    private fun stopSeekerUpdater() {
        seekerUpdateJob?.cancel()
        seekerUpdateJob = null
    }

    private fun updateSeekerState() {
        val player = videoPlayer ?: return

        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        _seekerState.update {
            it.copy(
                totalDuration = duration,
                currentTime = currentPos,
                bufferedPercentage = player.bufferedPercentage,
            )
        }
    }

}

sealed interface DirectPlaybackInitResult {
    data object Ready : DirectPlaybackInitResult
    data class RedirectToSeason(val epid: Int) : DirectPlaybackInitResult
    data class Failure(val message: String) : DirectPlaybackInitResult
}

sealed interface DanmakuSettingAction {
    data class SetScale(val value: Float) : DanmakuSettingAction
    data class SetOpacity(val value: Float) : DanmakuSettingAction
    data class SetArea(val value: Float) : DanmakuSettingAction
    data class SetSpeedFactor(val value: Float) : DanmakuSettingAction
    data class SetEnabledTypes(val types: List<DanmakuType>) : DanmakuSettingAction
}

sealed interface SubtitleSettingAction {
    data class SetFontSize(val value: TextUnit) : SubtitleSettingAction
    data class SetOpacity(val value: Float) : SubtitleSettingAction
    data class SetBottomPadding(val value: Dp) : SubtitleSettingAction
}

sealed interface MediaProfileSettingAction {
    data class SetQuality(val value: Int) : MediaProfileSettingAction
    data class SetVideoCodec(val value: VideoCodec) : MediaProfileSettingAction
    data class SetAudio(val value: Audio) : MediaProfileSettingAction
}
