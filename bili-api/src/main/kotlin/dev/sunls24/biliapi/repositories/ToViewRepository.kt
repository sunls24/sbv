package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.user.ToViewItem
import dev.sunls24.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class ToViewRepository(
    private val authRepository: AuthRepository
) {
    private fun requireSessData(): String =
        authRepository.sessionData?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("SESSDATA is empty")

    private fun requireCsrf(): String =
        authRepository.biliJct?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("bili_jct is empty")

    suspend fun getToView(): List<ToViewItem> = BiliHttpApi.getToView(
        sessData = requireSessData()
    ).getResponseData().list.map(ToViewItem::fromToViewItem)

    suspend fun addToView(
        aid: Long,
        bvid: String? = null
    ) {
        val (success, message) = BiliHttpApi.addToView(
            avid = aid, bvid = bvid, csrf = requireCsrf(), sessData = requireSessData()
        )
        if (!success) throw Exception("添加到稍后再看失败：$message")
    }

    suspend fun delToView(
        aid: Long,
        viewed: Boolean = false
    ) {
        val (success, message) = BiliHttpApi.delToView(
            viewed = viewed, avid = aid, csrf = requireCsrf(), sessData = requireSessData()
        )
        if (!success) throw Exception("删除稍后再看失败：$message")
    }
}
