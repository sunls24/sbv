package dev.sunls24.sbv.viewmodel.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.repositories.CoinRepository
import dev.sunls24.biliapi.repositories.AuthRepository
import dev.sunls24.biliapi.repositories.FavoriteRepository
import dev.sunls24.biliapi.repositories.LikeRepository
import dev.sunls24.biliapi.repositories.OneClickTripleActionRepository
import dev.sunls24.biliapi.repositories.UserRepository
import dev.sunls24.sbv.entity.VideoListItem
import dev.sunls24.sbv.repository.VideoInfoRepository
import dev.sunls24.sbv.ui.effect.VideoDetailUiEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class VideoDetailViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val favoriteRepository: FavoriteRepository,
    private val likeRepository: LikeRepository,
    private val coinRepository: CoinRepository,
    private val oneClickTripleActionRepository: OneClickTripleActionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<VideoDetailUiEffect>()
    val uiEvent = _uiEffect.asSharedFlow()
    private var detailStateJob: Job? = null
    private var initParams: InitParams? = null

    fun init(aid: Long) {
        if (initParams?.aid == aid && detailStateJob?.isActive == true) return

        initParams = InitParams(aid)
        detailStateJob?.cancel()

        _uiState.update {
            it.copy(
                isLoggedIn = authRepository.isLoggedIn
            )
        }

        // 先监听detail流
        detailStateJob = videoInfoRepository.videoDetailState
            .filter { it?.aid == aid }
            .onEach { newState ->
                if (newState == null) return@onEach

                // 如果是PGC，则跳转至SeasonInfo
                if (newState.redirectToEp) {

                    _uiEffect.emit(
                        VideoDetailUiEffect.LaunchSeasonInfoActivity(
                            seasonId = null,
                            epid = newState.epid
                        )
                    )
                    return@onEach
                }

                _uiState.update {
                    it.copy(
                        videoDetailState = newState,
                        loadingState = VideoInfoState.Success
                    )
                }

                if (authRepository.isLoggedIn) {
                    updateFollowingState()
                    fetchFavoriteData(aid)
                }
            }.launchIn(viewModelScope)

        // 再加载detail
        loadVideoDetail(aid, includeUserActions = true)
    }

    private data class InitParams(val aid: Long)

    fun updateVideoList(sectionIndex: Int) {
        val videoDetail = _uiState.value.videoDetailState ?: return

        val partVideoList =
            videoDetail.ugcSeason?.sections?.get(sectionIndex)?.episodes?.map { episode ->
                VideoListItem(
                    aid = episode.aid,
                    cid = episode.cid,
                    title = episode.title
                )
            }
        videoInfoRepository.updateVideoList(partVideoList ?: emptyList())
    }

    fun updateVideoList(videoListItem: List<VideoListItem>) {
        videoInfoRepository.updateVideoList(videoListItem)
    }

    fun setFollow(follow: Boolean) {
        val userMid = _uiState.value.videoDetailState?.author?.mid ?: return

        viewModelScope.launch(Dispatchers.IO) {

            if (follow) {
                userRepository.followUser(
                    mid = userMid,
                )
            } else {
                userRepository.unfollowUser(
                    mid = userMid,
                )
            }


            updateFollowingState()
        }
    }

    fun updateVideoFavoriteData(folderIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val videoDetail = _uiState.value.videoDetailState ?: return@launch
            val favoriteFolders = _uiState.value.favoriteFolders
            runCatching {
                val avid = videoDetail.aid
                require(favoriteFolders.isNotEmpty()) { "Favorite folders not found" }

                favoriteRepository.updateVideoToFavoriteFolder(
                    aid = avid,
                    addMediaIds = folderIds,
                    delMediaIds = favoriteFolders.map { it.id } - folderIds.toSet()
                )
            }.onFailure {
                _uiEffect.emit(VideoDetailUiEffect.ShowToast(it.message ?: "unknown error"))
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        videoDetailState = videoDetail.copy(isFavorite = folderIds.isNotEmpty()),
                        videoFavoriteFolderIds = folderIds.toSet()
                    )
                }
            }
        }
    }

    fun loadVideoDetail(aid: Long, includeUserActions: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                videoInfoRepository.loadVideoDetail(aid, includeUserActions)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorTip = e.localizedMessage ?: "未知错误",
                        loadingState = VideoInfoState.Error
                    )
                }
            }
        }
    }

    fun addVideoToDefaultFavoriteFolder() {
        val videoFavoriteFolderIds = _uiState.value.videoFavoriteFolderIds
        val defaultFavoriteFolderId = getDefaultFavoriteFolderId()

        updateVideoFavoriteData(
            (videoFavoriteFolderIds + listOfNotNull(defaultFavoriteFolderId)).toList()
        )
    }

    fun updateVideoLiked(like: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDetail = _uiState.value.videoDetailState ?: run {
                return@launch
            }

            runCatching {
                likeRepository.updateVideoLiked(
                    like = like,
                    aid = currentDetail.aid,
                    bvid = currentDetail.bvid
                )
            }.onSuccess {
                _uiState.update { currentState ->
                    currentState.copy(videoDetailState = currentDetail.copy(isLiked = like))
                }
            }.onFailure { throwable ->
                _uiEffect.emit(VideoDetailUiEffect.ShowToast("点赞失败:${throwable.message ?: "unknown error"}"))
            }
        }
    }

    fun sendVideoCoin() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDetail = _uiState.value.videoDetailState ?: run {
                return@launch
            }

            runCatching {
                coinRepository.sendVideoCoin(
                    aid = currentDetail.aid,
                    bvid = currentDetail.bvid
                )
            }.onSuccess {
                _uiState.update { currentState ->
                    currentState.copy(videoDetailState = currentDetail.copy(isCoined = true))
                }
            }.onFailure { throwable ->
                _uiEffect.emit(VideoDetailUiEffect.ShowToast("投币失败:${throwable.message ?: "unknown error"}"))
            }
        }
    }

    fun sendVideoOneClickTripleAction() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDetail = _uiState.value.videoDetailState ?: run {
                return@launch
            }

            runCatching {
                oneClickTripleActionRepository.sendVideoOneClickTripleAction(
                    aid = currentDetail.aid,
                    bvid = currentDetail.bvid
                )
            }.onSuccess { data ->
                if (data != null) {
                    _uiState.update { currentState ->
                        val defaultFolderId = getDefaultFavoriteFolderId()

                        currentState.copy(
                            videoDetailState = currentDetail.copy(
                                isLiked = data.like,
                                isCoined = data.coin,
                                isFavorite = data.fav
                            ),
                            videoFavoriteFolderIds = defaultFolderId?.let {
                                currentState.videoFavoriteFolderIds + it
                            } ?: currentState.videoFavoriteFolderIds
                        )
                    }
                    _uiEffect.emit(VideoDetailUiEffect.ShowToast("一键三连"))
                }
            }.onFailure { throwable ->
                _uiEffect.emit(VideoDetailUiEffect.ShowToast("一键三连失败:${throwable.message ?: "unknown error"}"))
            }
        }
    }

    private fun fetchFavoriteData(avid: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = authRepository.mid ?: return@launch
            runCatching {
                favoriteRepository.getAllFavoriteFolderMetadataList(
                    mid = userId,
                    rid = avid,
                )
            }.onSuccess { result ->
                _uiState.update { it.copy(favoriteFolders = result) }
                val videoInFavoriteFolderIdsResult = result
                    .filter { it.videoInThisFav }.map { it.id }
                _uiState.update { it.copy(videoFavoriteFolderIds = videoInFavoriteFolderIdsResult.toSet()) }
            }
        }
    }

    private fun getDefaultFavoriteFolderId(): Long? {
        val defaultFavoriteFolder =
            _uiState.value.favoriteFolders.firstOrNull { it.title == "默认收藏夹" }
        return defaultFavoriteFolder?.id
    }

    private fun updateFollowingState() {
        viewModelScope.launch(Dispatchers.IO) {
            val userMid = _uiState.value.videoDetailState?.author?.mid ?: -1
            val isFollowing = userRepository.checkIsFollowing(
                mid = userMid,
            )
            _uiState.update { it.copy(isFollowingUp = isFollowing ?: false) }
        }
    }

    override fun onCleared() {
        videoInfoRepository.reset()
    }
}

enum class VideoInfoState {
    Loading,
    Success,
    Error
}
