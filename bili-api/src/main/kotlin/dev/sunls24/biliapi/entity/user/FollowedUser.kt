package dev.sunls24.biliapi.entity.user

data class FollowedUser(
    val mid: Long,
    val name: String,
    val avatar: String,
    val sign: String
) {
    companion object {
        fun fromHttpFollowedUser(followedUser: dev.sunls24.biliapi.http.entity.user.UserFollowData.FollowedUser) =
            FollowedUser(
                mid = followedUser.mid,
                name = followedUser.uname,
                avatar = followedUser.face,
                sign = followedUser.sign
            )
    }
}

data class FollowedUserPage(
    val items: List<FollowedUser>,
    val nextPage: Int,
    val hasMore: Boolean,
    val total: Int
)
