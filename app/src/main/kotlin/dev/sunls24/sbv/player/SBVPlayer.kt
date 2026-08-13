package dev.sunls24.sbv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource

data class SBVPlayerOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val enableSoftwareVideoDecoder: Boolean,
)

@OptIn(UnstableApi::class)
class SBVPlayer(
    context: Context,
    options: SBVPlayerOptions,
) {
    val player: ExoPlayer

    private val dataSourceFactory =
        OkHttpDataSource.Factory(OkHttpUtil.generateCustomSslOkHttpClient(context)).apply {
            options.userAgent?.let(::setUserAgent)
            options.referer?.let { setDefaultRequestProperties(mapOf("referer" to it)) }
        }

    init {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            if (options.enableSoftwareVideoDecoder) {
                setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val decoders = MediaCodecUtil.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder,
                    )
                    decoders.filter {
                        it.name.startsWith("OMX.google.") || it.name.startsWith("c2.android.")
                    }.ifEmpty { decoders }
                }
            } else {
                setMediaCodecSelector(MediaCodecSelector.DEFAULT)
            }
        }

        player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setSeekForwardIncrementMs(10_000)
            .setSeekBackIncrementMs(5_000)
            .build()
    }

    fun setMedia(videoUrl: String?, audioUrl: String?) {
        val sources = listOfNotNull(
            videoUrl?.let(::createMediaSource),
            audioUrl?.let(::createMediaSource),
        )
        require(sources.isNotEmpty()) { "At least one media URL is required" }

        val source = if (sources.size == 1) sources.single() else {
            MergingMediaSource(*sources.toTypedArray())
        }
        player.setMediaSource(source)
    }

    fun addListener(listener: Player.Listener) = player.addListener(listener)

    fun prepare() = player.prepare()

    fun play() = player.play()

    fun pause() = player.pause()

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun release() = player.release()

    val isPlaying: Boolean
        get() = player.isPlaying

    val currentPosition: Long
        get() = player.currentPosition

    val duration: Long
        get() = player.duration

    val bufferedPercentage: Int
        get() = player.bufferedPercentage

    var speed: Float
        get() = player.playbackParameters.speed
        set(value) = player.setPlaybackSpeed(value)

    val videoWidth: Int
        get() = player.videoSize.width

    val videoHeight: Int
        get() = player.videoSize.height

    val debugInfo: String
        get() = """
            player: ${MediaLibraryInfo.VERSION_SLASHY}
            time: ${currentPosition.formatMinSec()} / ${duration.formatMinSec()}
            buffered: $bufferedPercentage%
            resolution: $videoWidth x $videoHeight
            audio: ${player.audioFormat?.bitrate ?: 0} kbps
            video codec: ${player.videoFormat?.sampleMimeType ?: "null"}
            audio codec: ${player.audioFormat?.sampleMimeType ?: "null"} (${audioRendererName()})
        """.trimIndent()

    private fun createMediaSource(url: String): MediaSource =
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

    private fun audioRendererName(): String {
        for (index in 0 until player.rendererCount) {
            val renderer = player.getRenderer(index)
            if (renderer.trackType == C.TRACK_TYPE_AUDIO && renderer.state == Renderer.STATE_STARTED) {
                return renderer.name
            }
        }
        return "UnknownRenderer"
    }
}
