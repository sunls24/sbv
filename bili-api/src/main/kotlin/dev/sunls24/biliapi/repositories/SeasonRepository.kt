package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.season.FollowingSeason
import dev.sunls24.biliapi.entity.season.FollowingSeasonData
import dev.sunls24.biliapi.entity.season.FollowingSeasonStatus
import dev.sunls24.biliapi.entity.season.FollowingSeasonType
import dev.sunls24.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class SeasonRepository(
    private val authRepository: AuthRepository
) {
    /**
     * 获取追番/追剧列表
     *
     * @param type 追番/追剧类型
     * @param status 追剧状态
     * @param pageNumber 页码
     * @param pageSize 每页数量
     */
    suspend fun getFollowingSeasons(
        type: FollowingSeasonType = FollowingSeasonType.Bangumi,
        status: FollowingSeasonStatus = FollowingSeasonStatus.All,
        pageNumber: Int = 1,
        pageSize: Int = 30
    ): FollowingSeasonData = BiliHttpApi.getFollowingSeasons(
        type = type.id,
        status = status.id,
        pageNumber = pageNumber,
        pageSize = pageSize,
        mid = requireNotNull(authRepository.mid),
        sessData = authRepository.sessionData
    ).getResponseData().let { data ->
        FollowingSeasonData(
            list = data.list.map(FollowingSeason::fromFollowingSeason),
            total = data.total
        )
    }
}
