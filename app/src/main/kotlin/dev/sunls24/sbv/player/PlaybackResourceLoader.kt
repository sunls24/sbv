package dev.sunls24.sbv.player

import dev.sunls24.biliapi.entity.PlayData
import dev.sunls24.biliapi.repositories.VideoPlayRepository
import dev.sunls24.sbv.entity.Audio
import dev.sunls24.sbv.entity.VideoCodec

internal class PlaybackResourceLoader(
    private val videoPlayRepository: VideoPlayRepository
) {
    private var playData: PlayData? = null

    suspend fun load(
        aid: Long,
        cid: Long,
        epid: Int,
        fromSeason: Boolean,
        preferredQuality: Int,
        preferredCodec: VideoCodec,
        preferredAudio: Audio,
        randomizeCdn: Boolean = false
    ): PlaybackResources {
        val data = if (fromSeason) {
            videoPlayRepository.getPgcPlayData(
                aid = aid,
                cid = cid,
                epid = epid,
                preferCodec = preferredCodec.toBiliApiCodeType()
            )
        } else {
            videoPlayRepository.getPlayData(
                aid = aid,
                cid = cid,
                preferCodec = preferredCodec.toBiliApiCodeType()
            )
        }
        playData = data

        val qualities = data.dashVideos.mapTo(linkedSetOf()) { it.quality }
        val audio = buildList {
            addAll(data.dashAudios.map { Audio.fromCode(it.codecId) })
            data.dolby?.let { add(Audio.fromCode(it.codecId)) }
            data.flac?.let { add(Audio.fromCode(it.codecId)) }
        }.distinct()
        val quality = selectQuality(qualities, preferredQuality)
        val selectedAudio = selectAudio(audio, preferredAudio)
        val codec = selectVideoCodec(quality, preferredCodec)
        val media = resolve(
            quality = quality,
            codec = codec.selected,
            audio = selectedAudio,
            randomizeCdn = randomizeCdn,
        )
            ?: throw IllegalStateException("视频源解析失败")

        return PlaybackResources(
            qualities = qualities,
            audio = audio,
            codecs = codec.available,
            selectedQuality = quality,
            selectedCodec = codec.selected,
            selectedAudio = selectedAudio,
            media = media,
            needPay = data.needPay
        )
    }

    fun selectVideoCodec(quality: Int, preferredCodec: VideoCodec): VideoCodecSelection {
        val data = playData ?: return VideoCodecSelection(emptyList(), preferredCodec)
        val codecs = data.codec[quality]
            ?.mapNotNull(VideoCodec::fromCodecString)
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: data.dashVideos
                .filter { it.quality == quality }
                .map { video ->
                    video.codecs
                        ?.let(VideoCodec::fromCodecString)
                        ?: VideoCodec.fromCodecId(video.codecId)
                }
                .distinct()

        val selected = preferredCodec.takeIf(codecs::contains)
            ?: codecs.minByOrNull(VideoCodec::ordinal)
            ?: preferredCodec
        return VideoCodecSelection(codecs, selected)
    }

    fun resolve(
        quality: Int,
        codec: VideoCodec,
        audio: Audio,
        randomizeCdn: Boolean = false,
    ): ResolvedMedia? {
        val data = playData ?: return null
        val video = data.dashVideos.find {
            val codecs = it.codecs
            it.quality == quality && (codecs.isNullOrEmpty() || codecs.startsWith(codec.prefix))
        } ?: data.dashVideos.firstOrNull() ?: return null
        val audioItem = data.dashAudios.find { it.codecId == audio.code }
            ?: data.dolby.takeIf { it?.codecId == audio.code }
            ?: data.flac.takeIf { it?.codecId == audio.code }
            ?: data.dashAudios.minByOrNull { it.codecId }

        return ResolvedMedia(
            videoUrl = selectCdnUrl(video.baseUrl, video.backUrl, randomizeCdn),
            audioUrl = audioItem?.let {
                selectCdnUrl(it.baseUrl, it.backUrl, randomizeCdn)
            },
            width = video.width,
            height = video.height
        )
    }

    private fun selectCdnUrl(
        primaryUrl: String,
        backupUrls: List<String>,
        randomizeCdn: Boolean,
    ): String {
        // 初次播放保持主地址；重试时优先随机选择 B 站返回的备用地址，
        // 避免在已经失败的 CDN 节点上重复等待。
        val candidates = if (randomizeCdn) {
            backupUrls.filter(String::isNotBlank).distinct()
        } else {
            emptyList()
        }
        return candidates.ifEmpty { listOf(primaryUrl) }.random()
    }

    private fun selectQuality(available: Set<Int>, preferred: Int): Int {
        if (preferred in available) return preferred
        val sorted = available.sorted()
        return sorted.findLast { it <= preferred } ?: sorted.firstOrNull() ?: 0
    }

    private fun selectAudio(available: List<Audio>, preferred: Audio): Audio {
        if (preferred in available) return preferred
        return when {
            preferred == Audio.ADolbyAtoms && Audio.AHiRes in available -> Audio.AHiRes
            preferred == Audio.AHiRes && Audio.ADolbyAtoms in available -> Audio.ADolbyAtoms
            Audio.A192K in available -> Audio.A192K
            Audio.A132K in available -> Audio.A132K
            Audio.A64K in available -> Audio.A64K
            else -> available.firstOrNull() ?: Audio.A132K
        }
    }
}

internal data class PlaybackResources(
    val qualities: Set<Int>,
    val audio: List<Audio>,
    val codecs: List<VideoCodec>,
    val selectedQuality: Int,
    val selectedCodec: VideoCodec,
    val selectedAudio: Audio,
    val media: ResolvedMedia,
    val needPay: Boolean
)

internal data class VideoCodecSelection(
    val available: List<VideoCodec>,
    val selected: VideoCodec
)

internal data class ResolvedMedia(
    val videoUrl: String,
    val audioUrl: String?,
    val width: Int,
    val height: Int
)
