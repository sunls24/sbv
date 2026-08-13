package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.FavoriteFolderData
import dev.sunls24.biliapi.entity.FavoriteFolderMetadata
import dev.sunls24.biliapi.entity.FavoriteItemType
import dev.sunls24.biliapi.http.BiliHttpApi
import org.koin.core.annotation.Single

@Single
class FavoriteRepository(
    private val authRepository: AuthRepository
) {
    suspend fun checkVideoFavoured(aid: Long): Boolean = BiliHttpApi.checkVideoFavoured(
        avid = aid,
        sessData = authRepository.sessionData.orEmpty()
    )

    suspend fun updateVideoToFavoriteFolder(
        aid: Long,
        addMediaIds: List<Long>,
        delMediaIds: List<Long>
    ) {
        BiliHttpApi.setVideoToFavorite(
            avid = aid,
            type = FavoriteItemType.Video.value,
            addMediaIds = addMediaIds,
            delMediaIds = delMediaIds,
            sessData = requireNotNull(authRepository.sessionData),
            csrf = authRepository.biliJct
        )
    }

    suspend fun getAllFavoriteFolderMetadataList(
        mid: Long,
        type: FavoriteItemType = FavoriteItemType.Video,
        rid: Long? = null
    ): List<FavoriteFolderMetadata> {
        val userFavoriteFoldersData = BiliHttpApi.getAllFavoriteFoldersInfo(
            mid = mid,
            type = type.value,
            rid = rid,
            sessData = authRepository.sessionData.orEmpty()
        ).getResponseData()
        return userFavoriteFoldersData.list.map {
            FavoriteFolderMetadata.fromHttpUserFavoriteFolder(it)
        }
    }

    suspend fun getFavoriteFolderData(
        mediaId: Long,
        pageSize: Int = 20,
        pageNumber: Int = 1
    ): FavoriteFolderData {
        val favoriteFolderListData = BiliHttpApi.getFavoriteList(
            mediaId = mediaId,
            pageSize = pageSize,
            pageNumber = pageNumber,
            sessData = authRepository.sessionData.orEmpty()
        ).getResponseData()
        return FavoriteFolderData.fromHttpFavoriteFolderInfoListData(favoriteFolderListData)
    }
}
