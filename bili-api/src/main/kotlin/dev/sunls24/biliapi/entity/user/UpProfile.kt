package dev.sunls24.biliapi.entity.user

import dev.sunls24.biliapi.http.entity.user.WebUserCardData

data class UpProfile(
    val mid: Long,
    val name: String,
    val avatar: String,
    val sign: String,
    val level: Int?,
    val officialTitle: String?,
    val followingCount: Long,
    val followerCount: Long,
    val archiveCount: Long,
    val likeCount: Long
) {
    companion object {
        fun fromWebUserCardData(
            requestedMid: Long,
            data: WebUserCardData
        ) = UpProfile(
            mid = data.card.mid.toLongOrNull() ?: requestedMid,
            name = data.card.name,
            avatar = data.card.face,
            sign = data.card.sign,
            level = data.card.levelInfo?.currentLevel,
            officialTitle = data.card.official
                ?.title
                ?.takeIf { data.card.official.role != 0 && it.isNotBlank() },
            followingCount = data.card.attention,
            followerCount = data.follower,
            archiveCount = data.archiveCount,
            likeCount = data.likeCount
        )
    }
}
