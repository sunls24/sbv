package dev.sunls24.sbv.viewmodel.player

import android.util.Log
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.sunls24.biliapi.entity.PlayData
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

    var videoPlayer: SBVPlayer? by mutableStateOf(null)
        private set
    var danmakuPlayer: DanmakuPlayer? by mutableStateOf(null)
        private set

    private var playData: PlayData? = null
    private var initialized = false
    val isInitialized: Boolean get() = initialized
    private var relatedLoadedAid = 0L

    private val detachedWorkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var danmakuConfig = DanmakuConfig()
    private val danmakuTypeFilter = TypeFilter()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()
    private val _seekerState = MutableStateFlow(SeekerState())
    val seekerState = _seekerState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PlayerUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var seekerUpdateJob: Job? = null
    private var loadVideoJob: Job? = null
    private var subtitleJob: Job? = null

    private var backToStartCountdownJob: Job? = null
    private var playNextCountdownJob: Job? = null
    private var previewTipCountdownJob: Job? = null

    private val videoPlayerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            _uiState.update {
                it.copy(
                    playerState = PlayerState.Error(
                        error.message ?: "Unknown error"
                    )
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    danmakuPlayer?.pause()
                    stopSeekerUpdater()
                    _uiState.update { it.copy(isBuffering = false) }
                }
                Player.STATE_BUFFERING -> {
                    danmakuPlayer?.pause()
                    _uiState.update { it.copy(isBuffering = true) }
                }
                Player.STATE_READY -> {
                    _uiState.update { it.copy(playerState = PlayerState.Ready) }
                    applyPlaySpeed(_uiState.value.playSpeed)
                    startSeekerUpdater()
                }
                Player.STATE_ENDED -> {
                    danmakuPlayer?.pause()
                    stopSeekerUpdater()
                    _uiState.update { it.copy(playerState = PlayerState.Ended) }
                    viewModelScope.launch {
                        _uiEffect.emit(PlayerUiEffect.PlayEnded)
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                danmakuPlayer?.start()
                _uiState.update { it.copy(playerState = PlayerState.Playing, isBuffering = false) }
                if (_uiState.value.lastPlayed > 0) {
                    seekToLastPlayed()
                    _uiState.update { it.copy(lastPlayed = 0) }
                }
            } else {
                danmakuPlayer?.pause()
                if (_uiState.value.playerState != PlayerState.Ended) {
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
        _uiState.update { it.copy(playerState = PlayerState.Error(message)) }
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
        syncProgress(scope = detachedWorkScope, isDetaching = true)

        videoPlayer?.release()
        videoPlayer = null
    }

    fun initDanmakuPlayer() {
        if (danmakuPlayer != null) return
        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
        initDanmakuConfig()
    }

    fun releaseDanmakuPlayer() {
        danmakuPlayer?.release()
        danmakuPlayer = null
    }

    override fun onCleared() {
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
            } catch (error: Exception) {
                Log.w("VideoPlayer", "Subtitle loading failed", error)
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
        danmakuPlayer?.updatePlaySpeed(speed)
    }

    fun updateVideoAspectRatio(aspectRatio: VideoAspectRatio) {
        _uiState.update {
            it.copy(aspectRatio = aspectRatio)
        }
    }

    fun updateMediaProfile(action: MediaProfileSettingAction) {
        val old = _uiState.value.mediaProfileState
        val codecSelection = if (action is MediaProfileSettingAction.SetQuality) {
            playData?.let {
                selectVideoCodec(
                    playData = it,
                    qualityId = action.value,
                    preferredCodec = old.videoCodec
                )
            }
        } else {
            null
        }
        val new = when (action) {
            is MediaProfileSettingAction.SetQuality -> old.copy(
                qualityId = action.value,
                videoCodec = codecSelection?.selected ?: old.videoCodec
            )
            is MediaProfileSettingAction.SetVideoCodec -> old.copy(videoCodec = action.value)
            is MediaProfileSettingAction.SetAudio -> old.copy(audio = action.value)
        }

        if (old == new) return

        _uiState.update {
            it.copy(
                mediaProfileState = new,
                availableVideoCodec = codecSelection?.available ?: it.availableVideoCodec
            )
        }

        videoPlayer?.let { player ->
            player.pause()
            val currentPosition = player.currentPosition

            // 解析新配置下的 URL
            val mediaUrls = resolveMediaUrls(new.qualityId, new.videoCodec, new.audio)

            if (mediaUrls != null) {
                // 执行播放逻辑
                player.setMedia(mediaUrls.videoUrl, mediaUrls.audioUrl)
                player.prepare()
                if (currentPosition > 0) {
                    player.seekTo(currentPosition)
                }
                player.play()
            }
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

        // ===== 副作用处理 =====
        if (new.enabledTypes != old.enabledTypes) {
            updateDanmakuConfigTypeFilter(new.enabledTypes)
            Prefs.defaultDanmakuTypes = new.enabledTypes
        }
        if (new.scale != old.scale) {
            applyDanmakuConfig(danmakuConfig.copy(textSizeScale = new.scale))
            Prefs.defaultDanmakuScale = new.scale
        }
        if (new.speedFactor != old.speedFactor) {
            danmakuPlayer?.setDanmakuRollingSpeed(new.speedFactor)
            Prefs.defaultDanmakuSpeedFactor = new.speedFactor
        }
        if (new.area != old.area) {
            applyDanmakuConfig(danmakuConfig.copy(screenPart = new.area))
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
        danmakuPlayer?.seekTo(0)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()
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
        danmakuPlayer?.seekTo(time)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()
    }

    fun playNewVideo(newVideo: VideoListItem) {
        videoPlayer?.pause()
        subtitleJob?.cancel()

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

        // 更新UiState
        _uiState.update {
            it.copy(
                aid = newVideo.aid,
                cid = newVideo.cid,
                epid = newVideo.epid,
                seasonId = newVideo.seasonId ?: 0,
                title = newVideo.title,
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

    fun loadVideoWithResources() {
        val state = _uiState.value
        val avid = state.aid
        val cid = state.cid
        val epid = state.epid

        loadVideoJob?.cancel()
        loadVideoJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaUrls = loadMediaUrls(avid, cid, epid ?: 0)
                withContext(Dispatchers.Main) {
                    val player = checkNotNull(videoPlayer) { "Video player is not initialized" }
                    player.setMedia(mediaUrls.videoUrl, mediaUrls.audioUrl)
                    player.prepare()
                    player.play()
                }

                launch {
                    updateSubtitle()
                    val lastPlayEnabledSubtitle = _uiState.value.subtitleId != -1L
                    if (lastPlayEnabledSubtitle) {
                        _uiState.value.subtitleList
                            .firstOrNull { it.id != -1L }
                            ?.let { loadSubtitle(it.id) }
                    }
                }
                launch { loadDanmaku(cid) }
                launch { loadRelatedVideos(avid) }
            } catch (e: CancellationException) {
                throw e // 让结构化并发正常取消，不作为播放错误处理
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Video loading failed", e)

                _uiState.update {
                    it.copy(playerState = PlayerState.Error(e.message ?: "未知错误"))
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

    private suspend fun loadMediaUrls(
        avid: Long,
        cid: Long,
        epid: Int = 0
    ): MediaUrls {
        val playData = fetchPlayData(avid, cid, epid)
        this.playData = playData

        val resolutionMap = playData.dashVideos.associate { video ->
            video.quality to Resolution.fromCode(video.quality)
                .getShortDisplayName(SBVApp.context)
        }
        val availableAudioList = buildList {
            addAll(playData.dashAudios.map { Audio.fromCode(it.codecId) })
            playData.dolby?.let { add(Audio.fromCode(it.codecId)) }
            playData.flac?.let { add(Audio.fromCode(it.codecId)) }
        }.distinct()
        val targetQualityId =
            calculateTargetQuality(resolutionMap.keys, Prefs.defaultQuality.code)
        val targetAudio = calculateTargetAudio(availableAudioList, Prefs.defaultAudio)
        val codecSelection = selectVideoCodec(
            playData = playData,
            qualityId = targetQualityId,
            preferredCodec = Prefs.defaultVideoCodec
        )

        _uiState.update {
            it.copy(
                availableQuality = resolutionMap,
                availableAudio = availableAudioList,
                availableVideoCodec = codecSelection.available,
                mediaProfileState = it.mediaProfileState.copy(
                    qualityId = targetQualityId,
                    videoCodec = codecSelection.selected,
                    audio = targetAudio
                )
            )
        }

        if (playData.needPay) startShowPreviewTipCountdown()

        return resolveMediaUrls(
            qn = targetQualityId,
            codec = codecSelection.selected,
            audio = targetAudio
        ) ?: throw IllegalStateException("视频源解析失败")
    }

    private suspend fun fetchPlayData(avid: Long, cid: Long, epid: Int): PlayData {
        return if (_uiState.value.fromSeason) {
            videoPlayRepository.getPgcPlayData(
                aid = avid,
                cid = cid,
                epid = epid,
                preferCodec = Prefs.defaultVideoCodec.toBiliApiCodeType()
            )
        } else {
            videoPlayRepository.getPlayData(
                aid = avid,
                cid = cid,
                preferCodec = Prefs.defaultVideoCodec.toBiliApiCodeType()
            )
        }
    }

    private fun calculateTargetQuality(availableQualities: Set<Int>, defaultQualityCode: Int): Int {
        if (availableQualities.contains(defaultQualityCode)) return defaultQualityCode

        val sortedQualities = availableQualities.sorted()
        return sortedQualities.findLast { it <= defaultQualityCode }
            ?: sortedQualities.firstOrNull()
            ?: 0
    }

    private fun calculateTargetAudio(availableAudio: List<Audio>, defaultAudio: Audio): Audio {
        if (availableAudio.contains(defaultAudio)) return defaultAudio

        // Fallback 逻辑
        return when {
            defaultAudio == Audio.ADolbyAtoms && availableAudio.contains(Audio.AHiRes) -> Audio.AHiRes
            defaultAudio == Audio.AHiRes && availableAudio.contains(Audio.ADolbyAtoms) -> Audio.ADolbyAtoms
            availableAudio.contains(Audio.A192K) -> Audio.A192K
            availableAudio.contains(Audio.A132K) -> Audio.A132K
            availableAudio.contains(Audio.A64K) -> Audio.A64K
            else -> availableAudio.firstOrNull() ?: Audio.A132K
        }
    }

    private fun selectVideoCodec(
        playData: PlayData,
        qualityId: Int,
        preferredCodec: VideoCodec
    ): VideoCodecSelection {
        val codecs = playData.codec[qualityId]
            ?.mapNotNull(VideoCodec::fromCodecString)
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: playData.dashVideos
                .filter { it.quality == qualityId }
                .map { video ->
                    video.codecs
                        ?.let(VideoCodec::fromCodecString)
                        ?: VideoCodec.fromCodecId(video.codecId)
                }
                .distinct()

        val selected = preferredCodec.takeIf(codecs::contains)
            ?: codecs.minByOrNull(VideoCodec::ordinal)
            ?: preferredCodec
        return VideoCodecSelection(available = codecs, selected = selected)
    }

    private fun resolveMediaUrls(
        qn: Int,
        codec: VideoCodec,
        audio: Audio
    ): MediaUrls? {
        val currentPlayData = playData ?: return null

        val foundVideoItem = currentPlayData.dashVideos.find {
            val codecs = it.codecs
            it.quality == qn &&
                    (codecs.isNullOrEmpty() || codecs.startsWith(codec.prefix))
        }

        val actualVideoItem = foundVideoItem ?: currentPlayData.dashVideos.firstOrNull()
            ?: return null

        val videoUrl = actualVideoItem.baseUrl

        val audioItem = currentPlayData.dashAudios.find { it.codecId == audio.code }
            ?: currentPlayData.dolby.takeIf { it?.codecId == audio.code }
            ?: currentPlayData.flac.takeIf { it?.codecId == audio.code }
            ?: currentPlayData.dashAudios.minByOrNull { it.codecId }

        val audioUrl = audioItem?.baseUrl


        _uiState.update {
            it.copy(
                videoHeight = actualVideoItem.height,
                videoWidth = actualVideoItem.width
            )
        }

        return MediaUrls(videoUrl, audioUrl)
    }

    private suspend fun loadDanmaku(cid: Long) {
        runCatching {
            val danmakuXmlData = BiliHttpApi.getDanmakuXml(
                cid = cid,
                sessData = authRepository.sessionData.orEmpty()
            )

            danmakuXmlData.data.map {
                DanmakuItemData(
                    danmakuId = it.dmid,
                    position = (it.time * 1000).toLong(),
                    content = it.text,
                    mode = when (it.type) {
                        4 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                        5 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                        else -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    },
                    textSize = it.size,
                    textColor = Color(it.color).toArgb()
                )
            }
        }.onSuccess { list ->
            danmakuPlayer?.updateData(list)
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

    private fun initDanmakuConfig() {
        val danmakuTypes = Prefs.defaultDanmakuTypes
        val area = Prefs.defaultDanmakuArea
        val scale = Prefs.defaultDanmakuScale
        val factor = Prefs.defaultDanmakuSpeedFactor

        rebuildDanmakuTypeFilter(danmakuTypes)
        danmakuConfig = danmakuConfig.copy(
            density = 120,
            textSizeScale = scale,
            screenPart = area,
            dataFilter = listOf(danmakuTypeFilter),
            rollingSpeedFactor = factor
        )
        danmakuConfig.updateFilter()
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun updateDanmakuConfigTypeFilter(enabledDanmakuTypes: List<DanmakuType>) {
        rebuildDanmakuTypeFilter(enabledDanmakuTypes)
        danmakuConfig.updateFilter()
        danmakuPlayer?.updateConfig(danmakuConfig)
    }

    private fun rebuildDanmakuTypeFilter(enabledDanmakuTypes: List<DanmakuType>) {
        danmakuTypeFilter.clear()

        DanmakuType.entries
            .filterNot(enabledDanmakuTypes::contains)
            .map {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                }
            }
            .forEach(danmakuTypeFilter::addFilterItem)
    }

    private fun applyDanmakuConfig(config: DanmakuConfig) {
        danmakuConfig = config
        danmakuPlayer?.updateConfig(danmakuConfig)

        // 更新弹幕库之后updateConfig会导致滚动速度被重置，所以这里需要重新设置
        danmakuPlayer?.setDanmakuRollingSpeed(_uiState.value.danmakuState.speedFactor)
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

    private fun seekToLastPlayed() {
        val time = _uiState.value.lastPlayed.toLong()

        videoPlayer?.seekTo(time)
        danmakuPlayer?.seekTo(time)
        // akdanmaku 会在跳转后立即播放，如果需要缓冲则会导致弹幕不同步
        danmakuPlayer?.pause()

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

    private data class VideoCodecSelection(
        val available: List<VideoCodec>,
        val selected: VideoCodec
    )

    private data class MediaUrls(
        val videoUrl: String,
        val audioUrl: String?
    )
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
