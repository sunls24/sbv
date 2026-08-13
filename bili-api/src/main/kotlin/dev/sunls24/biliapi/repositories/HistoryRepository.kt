package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.user.HistoryData
import dev.sunls24.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class HistoryRepository(
    private val authRepository: AuthRepository
) {
    suspend fun getHistories(cursor: Long): HistoryData = BiliHttpApi.getHistories(
        viewAt = cursor,
        sessData = authRepository.sessionData.orEmpty()
    ).getResponseData().let(HistoryData::fromHistoryResponse)
}
