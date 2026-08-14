package dev.sunls24.sbv.entity

import android.content.Context
import dev.sunls24.sbv.R

enum class DanmakuSpeedFactor(val strRes: Int, val factor: Float) {
    S1(R.string.danmaku_speed_factor_x1_35, 1.35f),
    S2(R.string.danmaku_speed_factor_x1, 1f),
    S3(R.string.danmaku_speed_factor_x0_65, 0.65f);

    fun getDisplayName(context: Context) = context.getString(strRes)

    companion object {
        fun getIndexByFactor(targetFactor: Float): Int {
            return entries.find { it.factor == targetFactor }?.ordinal ?: S2.ordinal
        }
    }
}
