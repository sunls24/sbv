package dev.sunls24.sbv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.FavoriteFolderMetadata
import dev.sunls24.biliapi.entity.FavoriteItemType
import dev.sunls24.biliapi.repositories.AuthRepository
import dev.sunls24.biliapi.repositories.FavoriteRepository
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.swapList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FavoriteViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val favoriteFolderMetadataList = mutableStateListOf<FavoriteFolderMetadata>()
    val favorites = mutableStateListOf<VideoCardData>()

    var currentFavoriteFolderMetadata: FavoriteFolderMetadata? by mutableStateOf(null)

    private val pageSize = 20
    private var pageNumber = 1
    private var hasMore = true
    private var foldersInitialized = false
    private var itemsInitialized = false

    private var updateFoldersJob: Job? = null
    private var updateItemsJob: Job? = null
    private var folderRequestVersion = 0
    private var itemRequestVersion = 0

    fun ensureLoaded() {
        when {
            !foldersInitialized -> updateFoldersInfo()
            currentFavoriteFolderMetadata != null && !itemsInitialized -> updateFolderItems()
        }
    }

    fun clearData() {
        folderRequestVersion++
        itemRequestVersion++
        updateFoldersJob?.cancel()
        updateItemsJob?.cancel()
        updateFoldersJob = null
        updateItemsJob = null

        favoriteFolderMetadataList.clear()
        favorites.clear()
        currentFavoriteFolderMetadata = null
        foldersInitialized = false
        itemsInitialized = false
        resetPageNumber()
    }

    fun updateFoldersInfo() {
        if (updateFoldersJob?.isActive == true) return
        val userId = authRepository.mid ?: return
        val version = ++folderRequestVersion

        updateFoldersJob = viewModelScope.launch {
            try {
                val folders = withContext(Dispatchers.IO) {
                    favoriteRepository.getAllFavoriteFolderMetadataList(mid = userId)
                }
                if (version != folderRequestVersion) return@launch

                favoriteFolderMetadataList.swapList(folders)
                currentFavoriteFolderMetadata = folders.firstOrNull()
                favorites.clear()
                resetPageNumber()
                foldersInitialized = true
                itemsInitialized = false
                if (currentFavoriteFolderMetadata != null) updateFolderItems()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // 保持未初始化，重新进入 Tab 时会再次尝试。
            } finally {
                if (version == folderRequestVersion) updateFoldersJob = null
            }
        }
    }

    fun updateFolderItems(force: Boolean = false) {
        if (force) {
            updateItemsJob?.cancel()
            updateItemsJob = null
            resetPageNumber()
            itemsInitialized = false
        }
        if (updateItemsJob?.isActive == true || !hasMore) return

        val folderId = currentFavoriteFolderMetadata?.id ?: return
        val requestedPage = pageNumber
        val version = ++itemRequestVersion

        updateItemsJob = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    favoriteRepository.getFavoriteFolderData(
                        mediaId = folderId,
                        pageSize = pageSize,
                        pageNumber = requestedPage,
                    )
                }
                if (version != itemRequestVersion ||
                    currentFavoriteFolderMetadata?.id != folderId
                ) return@launch

                favorites.addAll(
                    data.medias.mapNotNull { item ->
                        if (item.type != FavoriteItemType.Video) return@mapNotNull null
                        VideoCardData(
                            avid = item.id,
                            title = item.title,
                            cover = item.cover,
                            upName = item.upper.name,
                            upMid = item.upper.mid,
                            timeString = (item.duration * 1000L).formatHourMinSec()
                        )
                    }
                )
                hasMore = data.hasMore
                pageNumber = requestedPage + 1
                itemsInitialized = true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // 保持当前页码，下一次触发时重试同一页。
            } finally {
                if (version == itemRequestVersion) updateItemsJob = null
            }
        }
    }

    private fun resetPageNumber() {
        pageNumber = 1
        hasMore = true
    }
}
