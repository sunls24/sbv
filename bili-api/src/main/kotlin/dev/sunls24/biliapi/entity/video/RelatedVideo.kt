package dev.sunls24.biliapi.entity.video

import dev.sunls24.biliapi.entity.user.Author

data class RelatedVideo(
    val aid: Long,
    val cid: Long,
    val cover: String,
    val title: String,
    val duration: Int,
    val author: Author?,
    val view: Int,
    val danmaku: Int
) {
    companion object {
        fun fromRelate(relate: dev.sunls24.biliapi.http.entity.video.RelatedVideoInfo) =
            RelatedVideo(
                aid = relate.aid,
                cid = relate.cid,
                cover = relate.pic,
                title = relate.title,
                duration = relate.duration,
                author = relate.owner.let { Author.fromVideoOwner(it) },
                view = relate.stat.view,
                danmaku = relate.stat.danmaku
            )
    }
}
