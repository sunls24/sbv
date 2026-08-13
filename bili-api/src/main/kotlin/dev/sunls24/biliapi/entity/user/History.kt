package dev.sunls24.biliapi.entity.user

data class HistoryData(
    val cursor: Long,
    val data: List<HistoryItem>
) {
    companion object {
        fun fromHistoryResponse(data: dev.sunls24.biliapi.http.entity.history.HistoryData) =
            HistoryData(
                cursor = data.cursor.viewAt,
                data = data.list
                    .filter { it.history.business == "archive" || it.history.business == "pgc" }
                    .map(HistoryItem::fromHistoryItem)
            )
    }
}

data class HistoryItem(
    val oid: Long,
    val bvid: String,
    val cid: Long,
    val kid: Long,
    val epid: Int?,
    val seasonId: Int?,
    val title: String,
    val cover: String,
    val author: String,
    val mid: Long?,
    val duration: Int,
    val progress: Int,
    val type: HistoryItemType
) {
    companion object {
        fun fromHistoryItem(item: dev.sunls24.biliapi.http.entity.history.HistoryItem) =
            HistoryItem(
                oid = item.history.oid,
                bvid = item.history.bvid,
                cid = item.history.cid,
                kid = 0,
                epid = item.history.epid,
                seasonId = null,
                title = item.title,
                cover = item.cover,
                author = item.authorName,
                mid = item.authorMid,
                duration = item.duration,
                progress = item.progress,
                type = when (item.history.business) {
                    "archive" -> HistoryItemType.Archive
                    "pgc" -> HistoryItemType.Pgc
                    else -> HistoryItemType.Unknown
                }
            )
    }
}

enum class HistoryItemType { Unknown, Archive, Pgc }
