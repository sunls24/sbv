package dev.sunls24.sbv.entity.carddata

data class VideoCardData(
    val avid: Long,
    val cid: Long? = null,
    val title: String,
    val cover: String,
    val upName: String,
    val upMid: Long? = null,
    val playString: String =  "",
    val danmakuString: String =  "",
    val timeString: String =  "",
    val pubTime: String? = null
)
