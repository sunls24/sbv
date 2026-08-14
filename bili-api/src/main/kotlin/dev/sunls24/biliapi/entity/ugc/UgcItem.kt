package dev.sunls24.biliapi.entity.ugc

import dev.sunls24.biliapi.http.entity.home.RcmdTopData
import dev.sunls24.biliapi.http.util.smartDate

data class UgcItem(
    val aid: Long,
    val bvid: String = "",
    val title: String,
    val cover: String,
    val author: String,
    val authorMid: Long?,
    val play: Int,
    val danmaku: Int,
    val duration: Int,
    val idx: Int = -1,
    val pubTime: String? = null,
) {
    companion object {
        fun fromRcmdItem(rcmdItem: RcmdTopData.RcmdItem) =
            UgcItem(
                aid = rcmdItem.id,
                bvid = rcmdItem.bvid,
                title = rcmdItem.title,
                cover = rcmdItem.pic,
                author = rcmdItem.owner?.name ?: "",
                authorMid = rcmdItem.owner?.mid,
                play = rcmdItem.stat?.view ?: -1,
                danmaku = rcmdItem.stat?.danmaku ?: -1,
                duration = rcmdItem.duration,
                pubTime = rcmdItem.pubdate.smartDate
            )

        fun fromVideoInfo(videoInfo: dev.sunls24.biliapi.http.entity.video.VideoInfo) =
            UgcItem(
                aid = videoInfo.aid,
                title = videoInfo.title,
                duration = videoInfo.duration,
                author = videoInfo.owner.name,
                authorMid = videoInfo.owner.mid,
                cover = videoInfo.pic,
                play = videoInfo.stat.view,
                danmaku = videoInfo.stat.danmaku,
                pubTime = videoInfo.pubdate.smartDate
            )
    }
}
