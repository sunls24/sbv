package dev.sunls24.sbv.entity

import android.content.Context
import androidx.annotation.StringRes
import dev.sunls24.sbv.R

enum class PlaybackSpeed(
    val code: Int,
    @StringRes private val labelRes: Int,
    val speed: Float
) {
    x2(4, R.string.play_speed_x2, 2f),
    x1_5(3, R.string.play_speed_x1_5, 1.5f),
    x1_25(2, R.string.play_speed_x1_25, 1.25f),
    x1(1, R.string.play_speed_x1, 1f),
    x0_5(0, R.string.play_speed_x0_5, 0.5f);

    fun getDisplayName(context: Context): String = context.getString(labelRes)

    companion object {
        fun fromCode(code: Int): PlaybackSpeed = entries.find { it.code == code } ?: x1

        fun fromSpeed(speed: Float): PlaybackSpeed = entries.find { it.speed == speed } ?: x1
    }
}

enum class PlaybackEndAction(val code: Int, val displayName: String) {
    Pause(0, "暂停"),
    PlayNext(1, "播放下一集"),
    Exit(2, "退出播放器");

    companion object {
        fun fromCode(code: Int): PlaybackEndAction = entries.find { it.code == code } ?: Exit
    }
}
