package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.ugc.UgcTypeV2
import dev.sunls24.biliapi.entity.ugc.region.UgcFeedData
import dev.sunls24.biliapi.entity.ugc.region.UgcFeedPage
import dev.sunls24.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class UgcRepository(
    private val authRepository: AuthRepository
) {
    suspend fun getRegionFeedRcmd(ugcType: UgcTypeV2, page: UgcFeedPage): UgcFeedData {
        val responseData = BiliHttpApi.getRegionFeedRcmd(
            displayId = page.nextPage,
            fromRegion = ugcType.tid,
            sessData = authRepository.sessionData
        ).getResponseData()
        return UgcFeedData.fromRegionFeedRcmd(
            data = responseData,
            nextPage = UgcFeedPage(page.nextPage + 1)
        )
    }
}
