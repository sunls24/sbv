package dev.sunls24.sbv.player.subtitle

data class SubtitleItem(
    val fromMs: Long,
    val toMs: Long,
    val content: String,
) {
    fun isShowing(timeMs: Long) = timeMs in fromMs..toMs
}
