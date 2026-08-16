package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.user.DynamicVideoData
import dev.sunls24.biliapi.entity.user.FollowedUser
import dev.sunls24.biliapi.entity.user.FollowedUserPage
import dev.sunls24.biliapi.entity.user.SpaceVideoData
import dev.sunls24.biliapi.entity.user.SpaceVideoOrder
import dev.sunls24.biliapi.entity.user.SpaceVideoPage
import dev.sunls24.biliapi.entity.user.UpProfile
import dev.sunls24.biliapi.http.BiliHttpApi
import dev.sunls24.biliapi.http.entity.user.FollowAction
import dev.sunls24.biliapi.http.entity.user.FollowActionSource
import dev.sunls24.biliapi.http.entity.user.RelationType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single

@Single
class UserRepository(
    private val authRepository: AuthRepository
) {
    private val _followingChanges = MutableSharedFlow<FollowingChange>()
    val followingChanges = _followingChanges.asSharedFlow()

    private suspend fun modifyFollow(
        mid: Long,
        action: FollowAction
    ): Boolean {
        val response = BiliHttpApi.modifyFollow(
            mid = mid,
            action = action,
            actionSource = FollowActionSource.Space,
            csrf = authRepository.biliJct,
            sessData = requireNotNull(authRepository.sessionData)
        )
        val success = response.code == 0
        if (success) {
            _followingChanges.emit(
                FollowingChange(
                    mid = mid,
                    following = action == FollowAction.AddFollow
                )
            )
        }
        return success
    }

    suspend fun followUser(mid: Long): Boolean = modifyFollow(mid, FollowAction.AddFollow)

    suspend fun unfollowUser(mid: Long): Boolean = modifyFollow(mid, FollowAction.DelFollow)

    suspend fun checkIsFollowing(mid: Long): Boolean? {
        val sessData = authRepository.sessionData ?: return null
        return runCatching {
            val response = BiliHttpApi.getRelations(
                mid = mid,
                sessData = sessData
            ).getResponseData()
            listOf(
                RelationType.Followed,
                RelationType.FollowedQuietly,
                RelationType.BothFollowed
            ).contains(response.relation.attribute)
        }.getOrNull()
    }

    suspend fun getUpProfile(mid: Long): UpProfile = BiliHttpApi.getWebUserCard(
        mid = mid,
        sessData = authRepository.sessionData
    ).getResponseData().let { UpProfile.fromWebUserCardData(mid, it) }

    suspend fun addSeasonFollow(seasonId: Int): String = BiliHttpApi.addSeasonFollow(
        seasonId = seasonId,
        csrf = requireNotNull(authRepository.biliJct),
        sessData = requireNotNull(authRepository.sessionData)
    ).getResponseData().toast

    suspend fun delSeasonFollow(seasonId: Int): String = BiliHttpApi.delSeasonFollow(
        seasonId = seasonId,
        csrf = requireNotNull(authRepository.biliJct),
        sessData = requireNotNull(authRepository.sessionData)
    ).getResponseData().toast

    suspend fun getSpaceVideos(
        mid: Long,
        order: SpaceVideoOrder = SpaceVideoOrder.PubDate,
        page: SpaceVideoPage = SpaceVideoPage()
    ): SpaceVideoData = BiliHttpApi.getWebUserSpaceVideos(
        mid = mid,
        order = order.value,
        pageNumber = page.nextWebPageNumber,
        pageSize = page.nextWebPageSize,
        sessData = authRepository.sessionData.orEmpty(),
        dedeUserID = authRepository.mid
    ).getResponseData().let(SpaceVideoData::fromWebSpaceVideoData)

    suspend fun getDynamicVideos(
        page: Int,
        offset: String
    ): DynamicVideoData = BiliHttpApi.getDynamicList(
        type = "video",
        page = page,
        offset = offset,
        sessData = authRepository.sessionData.orEmpty()
    ).getResponseData().let(DynamicVideoData::fromDynamicData)

    suspend fun getFollowedUsers(
        mid: Long,
        page: Int,
        pageSize: Int = 50
    ): FollowedUserPage {
        val sessData = requireNotNull(authRepository.sessionData)
        val data = BiliHttpApi.getUserFollow(
            mid = mid,
            pageNumber = page,
            pageSize = pageSize,
            sessData = sessData
        ).getResponseData()
        val items = data.list.map(FollowedUser::fromHttpFollowedUser)
        return FollowedUserPage(
            items = items,
            nextPage = page + 1,
            hasMore = items.size == pageSize && page * pageSize < data.total,
            total = data.total
        )
    }
}

data class FollowingChange(
    val mid: Long,
    val following: Boolean
)
