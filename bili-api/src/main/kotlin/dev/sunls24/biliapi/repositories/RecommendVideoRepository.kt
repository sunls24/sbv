package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.home.RecommendData
import dev.sunls24.biliapi.entity.home.RecommendPage
import dev.sunls24.biliapi.entity.rank.PopularVideoData
import dev.sunls24.biliapi.entity.rank.PopularVideoPage
import dev.sunls24.biliapi.entity.ugc.UgcItem
import dev.sunls24.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class RecommendVideoRepository(
    private val authRepository: AuthRepository
) {
    suspend fun getPopularVideos(page: PopularVideoPage): PopularVideoData {
        val response = BiliHttpApi.getPopularVideoData(
            pageSize = page.nextWebPageSize,
            pageNumber = page.nextWebPageNumber,
            sessData = authRepository.sessionData.orEmpty()
        ).getResponseData()
        return PopularVideoData(
            list = response.list.map(UgcItem::fromVideoInfo),
            nextPage = PopularVideoPage(
                nextWebPageSize = page.nextWebPageSize,
                nextWebPageNumber = page.nextWebPageNumber + 1
            ),
            noMore = response.noMore
        )
    }

    suspend fun getRecommendVideos(
        page: RecommendPage = RecommendPage()
    ): RecommendData {
        val responseItems = BiliHttpApi.getFeedRcmd(
            freshType = if (page.nextWebIdx == 1) 5 else 4,
            idx = page.nextWebIdx,
            fetchRow = page.nextFetchRow,
            lastShowlist = page.showlistGroups.joinToString(";").ifEmpty { null },
            sessData = authRepository.sessionData
        ).getResponseData().item
        val videoItems = responseItems.filter {
            it.goto == "av" && it.owner != null && it.stat != null
        }
        val showlistGroup = videoItems
            .distinctBy { it.goto to it.id }
            .joinToString(",") { "av_n_${it.id}" }
        return RecommendData(
            items = videoItems.map(UgcItem::fromRcmdItem),
            nextPage = RecommendPage(
                nextWebIdx = page.nextWebIdx + 1,
                nextFetchRow = page.nextFetchRow + 3,
                showlistGroups = if (showlistGroup.isEmpty()) {
                    page.showlistGroups
                } else {
                    page.showlistGroups + showlistGroup
                }
            )
        )
    }
}
