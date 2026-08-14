package dev.sunls24.sbv.repository

import dev.sunls24.biliapi.entity.video.RelatedVideo
import dev.sunls24.biliapi.entity.user.Author
import dev.sunls24.biliapi.repositories.VideoDetailRepository
import dev.sunls24.sbv.entity.VideoListItem
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toWanString
import dev.sunls24.sbv.viewmodel.video.VideoDetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class VideoInfoRepository(private val videoDetailRepository: VideoDetailRepository) {
    private val _videoList = MutableStateFlow<List<VideoListItem>>(emptyList())
    private val _videoDetailState = MutableStateFlow<VideoDetailState?>(null)

    val videoList = _videoList.asStateFlow()
    val videoDetailState = _videoDetailState.asStateFlow()

    suspend fun loadVideoDetail(aid: Long, includeUserActions: Boolean = true) {
        val videoDetail = videoDetailRepository.getVideoDetail(aid, includeUserActions)

        val videoDetailState = VideoDetailState(
            aid = videoDetail.aid,
            bvid = videoDetail.bvid,
            epid = videoDetail.epid,
            title = videoDetail.title,
            lastPlayedCid = videoDetail.history.lastPlayedCid,
            lastPlayedTime = videoDetail.history.progress,
            isLiked = videoDetail.userActions.like,
            isCoined = videoDetail.userActions.coin,
            isFavorite = videoDetail.userActions.favorite,
            cid = videoDetail.cid,
            cover = videoDetail.cover,
            publishDate = videoDetail.publishDate,
            stat = videoDetail.stat,
            author = videoDetail.author,
            tags = videoDetail.tags,
            isUpowerExclusive = videoDetail.isUpowerExclusive,
            redirectToEp = videoDetail.redirectToEp,
            argueTip = videoDetail.argueTip,
            description = videoDetail.description,
            pages = videoDetail.pages,
            relatedVideos = mapToVideoCardData(videoDetail.relatedVideos),
            ugcSeason = videoDetail.ugcSeason,
        )

        _videoDetailState.update { videoDetailState }
    }

    suspend fun resolveDirectPlayback(aid: Long): DirectPlaybackData {
        val detail = videoDetailRepository.getVideoDetail(aid, includeUserActions = false)
        val targetCid = detail.history.lastPlayedCid.takeIf { it != 0L }
            ?: detail.pages.firstOrNull()?.cid
            ?: detail.cid
        val ugcSection = detail.ugcSeason?.sections
            ?.firstOrNull { section -> section.episodes.any { it.cid == targetCid } }
        val videoList = ugcSection?.episodes?.map { episode ->
            VideoListItem(
                aid = episode.aid,
                cid = episode.cid,
                title = episode.title
            )
        } ?: detail.pages.map { page ->
            VideoListItem(
                aid = detail.aid,
                cid = page.cid,
                title = page.title.ifBlank { detail.title }
            )
        }.ifEmpty {
            listOf(VideoListItem(detail.aid, targetCid, title = detail.title))
        }

        return DirectPlaybackData(
            aid = detail.aid,
            cid = targetCid,
            title = detail.title,
            played = if (targetCid == detail.history.lastPlayedCid) {
                detail.history.progress * 1000
            } else {
                0
            },
            author = detail.author,
            videoList = videoList,
            relatedVideos = mapToVideoCardData(detail.relatedVideos),
            redirectEpid = detail.epid.takeIf { detail.redirectToEp }
        )
    }

    suspend fun getRelatedVideos(aid: Long): List<VideoCardData> {
        _videoDetailState.value
            ?.takeIf { it.aid == aid }
            ?.let { return it.relatedVideos }
        return videoDetailRepository.getVideoDetail(aid, includeUserActions = false)
            .relatedVideos
            .let(::mapToVideoCardData)
    }

    fun updateVideoList(videoListItem: List<VideoListItem>) {
        _videoList.update { videoListItem }
    }

    fun updateHistory(progress: Int, lastPlayedCid: Long) {
        _videoDetailState.update { it?.copy(lastPlayedCid = lastPlayedCid, lastPlayedTime = progress) }
    }

    fun reset(){
        _videoList.update { emptyList() }
        _videoDetailState.update { null }
    }

    private fun mapToVideoCardData(relatedVideos:List<RelatedVideo>): List<VideoCardData> {
        val relateVideoCardDataList = relatedVideos.map {
            VideoCardData(
                avid = it.aid,
                cid = it.cid,
                title = it.title,
                cover = it.cover,
                upName = it.author?.name ?: "",
                upMid = it.author?.mid,
                timeString = (it.duration * 1000L).formatHourMinSec(),
                playString = it.view.toWanString(),
                danmakuString = it.danmaku.toWanString(),
            )
        }

        return relateVideoCardDataList
    }
}

data class DirectPlaybackData(
    val aid: Long,
    val cid: Long,
    val title: String,
    val played: Int,
    val author: Author,
    val videoList: List<VideoListItem>,
    val relatedVideos: List<VideoCardData>,
    val redirectEpid: Int?
)
