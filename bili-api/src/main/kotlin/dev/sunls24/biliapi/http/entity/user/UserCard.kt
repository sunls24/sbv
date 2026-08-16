package dev.sunls24.biliapi.http.entity.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebUserCardData(
    val card: WebUserCard,
    val follower: Long = 0,
    @SerialName("archive_count")
    val archiveCount: Long = 0,
    @SerialName("like_num")
    val likeCount: Long = 0
)

@Serializable
data class WebUserCard(
    val mid: String = "",
    val name: String = "",
    val face: String = "",
    val sign: String = "",
    val attention: Long = 0,
    @SerialName("level_info")
    val levelInfo: WebUserLevelInfo? = null,
    @SerialName("Official")
    val official: WebUserOfficial? = null
)

@Serializable
data class WebUserLevelInfo(
    @SerialName("current_level")
    val currentLevel: Int = 0
)

@Serializable
data class WebUserOfficial(
    val role: Int = 0,
    val title: String = ""
)
