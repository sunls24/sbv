package dev.sunls24.sbv.player.subtitle

import kotlinx.serialization.json.Json

object SubtitleParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromBccString(bcc: String): List<SubtitleItem> =
        runCatching { json.decodeFromString<BiliSubtitle>(bcc) }
            .getOrNull()
            ?.body
            ?.map { item ->
                SubtitleItem(
                    fromMs = (item.from * 1_000).toLong(),
                    toMs = (item.to * 1_000).toLong(),
                    content = item.content,
                )
            }
            .orEmpty()
}
