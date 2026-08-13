package dev.sunls24.sbv.util

import android.content.Context
import dev.sunls24.biliapi.entity.season.FollowingSeasonStatus
import dev.sunls24.biliapi.entity.season.FollowingSeasonType
import dev.sunls24.sbv.R

fun FollowingSeasonStatus.getDisplayName(context: Context) = when (this) {
    FollowingSeasonStatus.All -> context.getString(R.string.following_season_status_all)
    FollowingSeasonStatus.Want -> context.getString(R.string.following_season_status_want)
    FollowingSeasonStatus.Watching -> context.getString(R.string.following_season_status_watching)
    FollowingSeasonStatus.Watched -> context.getString(R.string.following_season_status_watched)
}

fun FollowingSeasonType.getDisplayName(context: Context) = when (this) {
    FollowingSeasonType.Bangumi -> context.getString(R.string.following_season_type_bangumi)
    FollowingSeasonType.Cinema -> context.getString(R.string.following_season_type_film_and_television)
}