package dev.sunls24.sbv.entity

import android.content.Context
import dev.sunls24.sbv.R

enum class VideoAspectRatio(private val strRes: Int) {
    Default(R.string.video_aspect_ratio_default),
    FourToThree(R.string.video_aspect_ratio_four_to_three),
    SixteenToNine(R.string.video_aspect_ratio_sixteen_to_nine);

    fun getDisplayName(context: Context) = context.getString(strRes)
}