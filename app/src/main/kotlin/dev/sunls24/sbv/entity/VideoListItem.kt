package dev.sunls24.sbv.entity

data class VideoListItem(
    val aid: Long,
    val cid: Long,
    val epid: Int? = null,
    val seasonId: Int? = null,
    val title: String,
    val authorMid: Long? = null,
    val authorName: String? = null
)
