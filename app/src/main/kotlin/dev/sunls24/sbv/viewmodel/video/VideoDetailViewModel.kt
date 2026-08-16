package dev.sunls24.sbv.viewmodel.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.repositories.AuthRepository
import dev.sunls24.biliapi.repositories.FavoriteRepository
import dev.sunls24.biliapi.repositories.LikeRepository
import dev.sunls24.biliapi.repositories.OneClickTripleActionRepository
import dev.sunls24.biliapi.repositories.UserRepository
import dev.sunls24.sbv.entity.VideoListItem
import dev.sunls24.sbv.repository.VideoInfoRepository
import dev.sunls24.sbv.ui.effect.VideoDetailUiEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import java.util.concurrent.ConcurrentHashMap

@KoinViewModel
class VideoDetailViewModel(
    private val videoInfoRepository: VideoInfoRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val favoriteRepository: FavoriteRepository,
    private val likeRepository: LikeRepository,
    private val oneClickTripleActionRepository: OneClickTripleActionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<VideoDetailUiEffect>()
    val uiEvent = _uiEffect.asSharedFlow()
    private var detailStateJob: Job? = null
    private var followingStateJob: Job? = null
    private var followUpdating = false
    private var initParams: InitParams? = null
    private var accountDataLoadedAid: Long? = null
    private val locallyModifiedActions = ConcurrentHashMap.newKeySet<VideoAction>()

    init {
        userRepository.followingChanges
            .onEach { change ->
                val detail = _uiState.value.videoDetailState
                if (
                    detail == null ||
                    detail.aid != initParams?.aid ||
                    detail.author.mid != change.mid
                ) return@onEach
                followingStateJob?.cancel()
                _uiState.update {
                    it.copy(
                        isFollowingUp = change.following,
                        followingStateLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun init(aid: Long) {
        if (initParams?.aid == aid && detailStateJob?.isActive == true) return

        initParams = InitParams(aid)
        accountDataLoadedAid = null
        locallyModifiedActions.clear()
        detailStateJob?.cancel()
        followingStateJob?.cancel()
        followUpdating = false

        _uiState.update {
            it.copy(
                isLoggedIn = authRepository.isLoggedIn,
                isFollowingUp = null,
                followingStateLoading = false,
                isSelfAuthor = false
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

                _uiState.update { currentState ->
                    val currentDetail = currentState.videoDetailState
                    val mergedState = newState.copy(
                        isLiked = currentDetail?.isLiked
                            ?.takeIf { VideoAction.Like in locallyModifiedActions }
                            ?: newState.isLiked,
                        isCoined = currentDetail?.isCoined
                            ?.takeIf { VideoAction.Coin in locallyModifiedActions }
                            ?: newState.isCoined,
                        isFavorite = currentDetail?.isFavorite
                            ?.takeIf { VideoAction.Favorite in locallyModifiedActions }
                            ?: newState.isFavorite
                    )
                    currentState.copy(
                        videoDetailState = mergedState,
                        isSelfAuthor = authRepository.mid == newState.author.mid,
                        loadingState = VideoInfoState.Success
                    )
                }

                if (authRepository.isLoggedIn && accountDataLoadedAid != aid) {
                    accountDataLoadedAid = aid
                    if (authRepository.mid != newState.author.mid) updateFollowingState()
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
        val currentState = _uiState.value
        val detail = currentState.videoDetailState ?: return
        if (
            !currentState.isLoggedIn ||
            currentState.followingStateLoading ||
            followUpdating
        ) return
        val aid = detail.aid
        val userMid = detail.author.mid

        followUpdating = true
        viewModelScope.launch {
            try {
                val queriedFollowing = currentState.isFollowingUp ?: withContext(Dispatchers.IO) {
                    userRepository.checkIsFollowing(mid = userMid)
                }
                if (!isCurrentAuthor(aid, userMid)) return@launch
                val currentFollowing = _uiState.value.isFollowingUp ?: queriedFollowing
                if (currentFollowing == null) {
                    _uiEffect.emit(VideoDetailUiEffect.ShowToast("获取关注状态失败"))
                    return@launch
                }
                _uiState.update {
                    if (it.isFollowingUp == currentFollowing) {
                        it
                    } else {
                        it.copy(isFollowingUp = currentFollowing)
                    }
                }
                if (currentFollowing == follow) return@launch

                val success = withContext(Dispatchers.IO) {
                    if (follow) {
                        userRepository.followUser(mid = userMid)
                    } else {
                        userRepository.unfollowUser(mid = userMid)
                    }
                }
                if (!isCurrentAuthor(aid, userMid)) return@launch
                if (success) {
                    _uiState.update {
                        if (it.isFollowingUp == follow) it else it.copy(isFollowingUp = follow)
                    }
                } else {
                    _uiEffect.emit(
                        VideoDetailUiEffect.ShowToast(
                            if (follow) "关注失败" else "取消关注失败"
                        )
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (isCurrentAuthor(aid, userMid)) {
                    _uiEffect.emit(
                        VideoDetailUiEffect.ShowToast(
                            if (follow) "关注失败" else "取消关注失败"
                        )
                    )
                }
            } finally {
                if (isCurrentAuthor(aid, userMid)) {
                    followUpdating = false
                }
            }
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
                if (_uiState.value.videoDetailState?.aid != videoDetail.aid) return@onSuccess
                locallyModifiedActions.add(VideoAction.Favorite)
                _uiState.update { currentState ->
                    currentState.copy(
                        videoDetailState = currentState.videoDetailState?.copy(
                            isFavorite = folderIds.isNotEmpty()
                        ),
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

    suspend fun awaitHistory(aid: Long): VideoDetailState = uiState
        .mapNotNull { it.videoDetailState }
        .first { it.aid == aid && it.historyResolved }

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
                if (_uiState.value.videoDetailState?.aid != currentDetail.aid) return@onSuccess
                locallyModifiedActions.add(VideoAction.Like)
                _uiState.update { currentState ->
                    currentState.copy(
                        videoDetailState = currentState.videoDetailState?.copy(isLiked = like)
                    )
                }
            }.onFailure { throwable ->
                _uiEffect.emit(VideoDetailUiEffect.ShowToast("点赞失败:${throwable.message ?: "unknown error"}"))
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
                    if (_uiState.value.videoDetailState?.aid != currentDetail.aid) return@onSuccess
                    locallyModifiedActions.addAll(VideoAction.entries)
                    _uiState.update { currentState ->
                        val defaultFolderId = getDefaultFavoriteFolderId()

                        currentState.copy(
                            videoDetailState = currentState.videoDetailState?.copy(
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
        val currentState = _uiState.value
        if (!currentState.isLoggedIn || currentState.isSelfAuthor) return
        val detail = currentState.videoDetailState ?: return
        val aid = detail.aid
        val userMid = detail.author.mid
        followingStateJob?.cancel()
        _uiState.update {
            it.copy(followingStateLoading = true)
        }
        followingStateJob = viewModelScope.launch {
            val isFollowing = withContext(Dispatchers.IO) {
                userRepository.checkIsFollowing(mid = userMid)
            }
            if (!isCurrentAuthor(aid, userMid)) return@launch
            _uiState.update {
                it.copy(
                    isFollowingUp = isFollowing,
                    followingStateLoading = false
                )
            }
            if (isFollowing == null) {
                _uiEffect.emit(VideoDetailUiEffect.ShowToast("获取关注状态失败"))
            }
        }
    }

    private fun isCurrentAuthor(aid: Long, userMid: Long): Boolean {
        val detail = _uiState.value.videoDetailState
        return detail?.aid == aid && detail.author.mid == userMid
    }

    override fun onCleared() {
        videoInfoRepository.reset()
    }

    private enum class VideoAction {
        Like,
        Coin,
        Favorite
    }
}

enum class VideoInfoState {
    Loading,
    Success,
    Error
}
