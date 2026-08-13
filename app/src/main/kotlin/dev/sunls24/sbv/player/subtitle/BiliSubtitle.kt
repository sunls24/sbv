package dev.sunls24.sbv.player.subtitle

import kotlinx.serialization.Serializable

@Serializable
internal data class BiliSubtitle(val body: List<BiliSubtitleItem> = emptyList())

@Serializable
internal data class BiliSubtitleItem(
    val from: Float,
    val to: Float,
    val content: String,
)
