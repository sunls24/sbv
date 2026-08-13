package dev.sunls24.biliapi.entity.video

import dev.sunls24.biliapi.entity.user.Author
import dev.sunls24.biliapi.entity.video.season.UgcSeason
import dev.sunls24.biliapi.http.entity.video.VideoStat
import java.util.Date

data class VideoDetail(
    val bvid: String,
    val aid: Long,
    val cid: Long,
    val cover: String,
    val title: String,
    val publishDate: Date,
    val description: String,
    val stat: Stat,
    val author: Author,
    val pages: List<VideoPage>,
    val ugcSeason: UgcSeason?,
    val relatedVideos: List<RelatedVideo>,
    val redirectToEp: Boolean,
    val epid: Int?,
    val argueTip: String?,
    val tags: List<Tag>,
    val userActions: UserActions,
    val history: History,
    val playerIcon: PlayerIcon? = null,
    val isUpowerExclusive: Boolean = false
) {
    companion object {
        fun fromVideoDetail(videoDetail: dev.sunls24.biliapi.http.entity.video.VideoDetail) =
            VideoDetail(
                bvid = videoDetail.view.bvid,
                aid = videoDetail.view.aid,
                cid = videoDetail.view.cid,
                cover = videoDetail.view.pic,
                title = videoDetail.view.title,
                publishDate = Date(videoDetail.view.pubdate * 1000L),
                description = videoDetail.view.desc,
                stat = Stat.fromVideoStat(videoDetail.view.stat),
                author = Author.fromVideoOwner(videoDetail.view.owner),
                pages = videoDetail.view.pages.map { VideoPage.fromVideoPage(it) },
                ugcSeason = videoDetail.view.ugcSeason?.let { UgcSeason.fromUgcSeason(it) },
                relatedVideos = videoDetail.related?.map { RelatedVideo.fromRelate(it) }
                    ?: emptyList(),
                redirectToEp = videoDetail.view.redirectUrl?.contains("ep") ?: false,
                epid = videoDetail.view.redirectUrl?.split("ep", "?")?.get(1)?.toInt(),
                argueTip = videoDetail.view.stat.argueMsg.takeIf { it.isNotEmpty() },
                tags = videoDetail.tags.map { Tag.fromTag(it) },
                userActions = UserActions(),
                history = History(0, 0),
                playerIcon = null,
                isUpowerExclusive = videoDetail.view.isUpowerExclusive?: false
            )
    }

    data class Stat(
        val view: Int,
        val danmaku: Int,
        val reply: Int,
        val favorite: Int,
        val coin: Int,
        val share: Int,
        val like: Int,
        val historyRank: Int
    ) {
        companion object {
            fun fromVideoStat(videoStat: VideoStat) = Stat(
                view = videoStat.view,
                danmaku = videoStat.danmaku,
                reply = videoStat.reply,
                favorite = videoStat.favorite,
                coin = videoStat.coin,
                share = videoStat.share,
                like = videoStat.like,
                historyRank = videoStat.hisRank
            )
        }
    }

    data class History(
        val progress: Int,
        val lastPlayedCid: Long
    ) {
    }

    data class PlayerIcon(
        val idle: String,
        val moving: String
    ) {
        companion object {
            fun fromPlayerIcon(playerIcon: dev.sunls24.biliapi.http.entity.video.VideoMoreInfo.PlayerIcon?) =
                playerIcon?.let {
                    PlayerIcon(
                        idle = playerIcon.url2,
                        moving = playerIcon.url1
                    )
                }

        }
    }
}

data class UserActions(
    val like: Boolean = false,
    val favorite: Boolean = false,
    val coin: Boolean = false,
    val dislike: Boolean = false
)
