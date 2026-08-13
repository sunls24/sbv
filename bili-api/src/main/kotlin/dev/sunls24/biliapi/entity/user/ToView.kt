package dev.sunls24.biliapi.entity.user

data class ToViewItem(
    val oid: Long,
    val bvid: String,
    val cid: Long,
    val kid: Int,
    val epid: Int?,
    val seasonId: Int?,
    val title: String,
    val cover: String,
    val author: String,
    val mid: Long?,
    val duration: Int,
    val progress: Int,
    val type: ToViewItemType
) {
    companion object {
        fun fromToViewItem(item: dev.sunls24.biliapi.http.entity.toview.ToViewItem) =
            ToViewItem(
                oid = item.aid,
                bvid = item.bvid,
                cid = item.cid,
                kid = 0,
                epid = 0,
                seasonId = null,
                title = item.title,
                cover = item.pic,
                author = item.owner.name,
                mid = item.owner.mid,
                duration = item.duration,
                progress = item.progress,
                type = ToViewItemType.Archive
            )
    }
}

enum class ToViewItemType { Unknown, Archive, Pgc }
