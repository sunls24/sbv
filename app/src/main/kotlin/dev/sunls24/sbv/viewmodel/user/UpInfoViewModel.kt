package dev.sunls24.sbv.viewmodel.user

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.user.SpaceVideoPage
import dev.sunls24.biliapi.entity.user.UpProfile
import dev.sunls24.biliapi.repositories.AuthRepository
import dev.sunls24.biliapi.repositories.UserRepository
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.ui.effect.UiEffect
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toWanString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UpInfoViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var upName by mutableStateOf("")
        private set
    var upMid by mutableLongStateOf(0L)
        private set
    var profile by mutableStateOf<UpProfile?>(null)
        private set
    var profileLoading by mutableStateOf(false)
        private set
    var profileLoadFailed by mutableStateOf(false)
        private set
    var videoLoading by mutableStateOf(false)
        private set
    var initialVideoLoadFinished by mutableStateOf(false)
        private set
    var videoLoadFailed by mutableStateOf(false)
        private set
    var isFollowing by mutableStateOf<Boolean?>(null)
        private set
    var relationLoading by mutableStateOf(false)
        private set
    private var followUpdating = false

    val spaceVideos = mutableStateListOf<VideoCardData>()
    val isLoggedIn get() = authRepository.isLoggedIn
    val isSelf get() = authRepository.mid == upMid
    val noMore get() = !page.hasNext

    private val _uiEvent = MutableSharedFlow<UiEffect>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var page = SpaceVideoPage()
    private var loadJob: Job? = null
    private var profileJob: Job? = null
    private var relationJob: Job? = null
    private var requestVersion = 0
    private var profileRequestVersion = 0

    init {
        userRepository.followingChanges
            .onEach { change ->
                if (change.mid != upMid || isFollowing == change.following) return@onEach
                relationJob?.cancel()
                relationLoading = false
                isFollowing = change.following
                loadProfile()
            }
            .launchIn(viewModelScope)
    }

    fun init(mid: Long, fallbackName: String) {
        if (mid == 0L || (upMid == mid && requestVersion != 0)) return

        requestVersion++
        profileJob?.cancel()
        relationJob?.cancel()
        loadJob?.cancel()

        upMid = mid
        upName = fallbackName
        profile = null
        profileLoading = false
        profileLoadFailed = false
        isFollowing = null
        relationLoading = false
        followUpdating = false
        page = SpaceVideoPage()
        spaceVideos.clear()
        videoLoading = false
        initialVideoLoadFinished = false
        videoLoadFailed = false

        loadProfile()
        loadFollowingState()
        update()
    }

    fun update() {
        if (loadJob?.isActive == true || noMore || upMid == 0L) return
        val requestedMid = upMid
        val requestedPage = page
        val version = requestVersion
        val initialLoad = spaceVideos.isEmpty() && !initialVideoLoadFinished

        videoLoading = true
        videoLoadFailed = false
        loadJob = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    userRepository.getSpaceVideos(
                        mid = requestedMid,
                        page = requestedPage,
                    )
                }
                if (version != requestVersion || requestedMid != upMid) return@launch

                spaceVideos.addAll(data.videos.map { item ->
                    VideoCardData(
                        avid = item.aid,
                        title = item.title,
                        // TODO 合集样式封面仍缺少可验证的 App API 样本。
                        cover = item.cover,
                        upName = item.author,
                        playString = item.play.takeIf { it != -1 }.toWanString(),
                        danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        pubTime = item.pubTime
                    )
                })
                page = data.page
                initialVideoLoadFinished = true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (version == requestVersion && requestedMid == upMid) {
                    videoLoadFailed = initialLoad
                    initialVideoLoadFinished = true
                    if (!initialLoad) _uiEvent.emit(UiEffect.ShowToast("加载更多失败"))
                }
            } finally {
                if (version == requestVersion && requestedMid == upMid) {
                    videoLoading = false
                    loadJob = null
                }
            }
        }
    }

    fun retryVideos() {
        if (spaceVideos.isEmpty()) {
            page = SpaceVideoPage()
            initialVideoLoadFinished = false
            videoLoadFailed = false
        }
        update()
    }

    fun setFollowing(following: Boolean) {
        if (!isLoggedIn || isSelf || relationLoading || followUpdating) return
        val requestedMid = upMid
        val version = requestVersion
        followUpdating = true

        viewModelScope.launch {
            try {
                val queriedFollowing = isFollowing ?: withContext(Dispatchers.IO) {
                    userRepository.checkIsFollowing(requestedMid)
                }
                if (version != requestVersion || requestedMid != upMid) return@launch
                val currentFollowing = isFollowing ?: queriedFollowing
                if (currentFollowing == null) {
                    _uiEvent.emit(UiEffect.ShowToast("获取关注状态失败"))
                    return@launch
                }
                isFollowing = currentFollowing
                if (currentFollowing == following) return@launch

                val success = withContext(Dispatchers.IO) {
                    if (following) {
                        userRepository.followUser(requestedMid)
                    } else {
                        userRepository.unfollowUser(requestedMid)
                    }
                }
                if (version != requestVersion || requestedMid != upMid) return@launch
                if (success) {
                    if (isFollowing != following) {
                        isFollowing = following
                        loadProfile()
                    }
                } else {
                    _uiEvent.emit(UiEffect.ShowToast(if (following) "关注失败" else "取消关注失败"))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (version == requestVersion && requestedMid == upMid) {
                    _uiEvent.emit(UiEffect.ShowToast(if (following) "关注失败" else "取消关注失败"))
                }
            } finally {
                if (version == requestVersion && requestedMid == upMid) followUpdating = false
            }
        }
    }

    private fun loadProfile() {
        val requestedMid = upMid
        val version = requestVersion
        val profileVersion = ++profileRequestVersion
        profileJob?.cancel()
        profileLoading = true
        profileLoadFailed = false
        profileJob = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    userRepository.getUpProfile(requestedMid)
                }
                if (
                    version != requestVersion ||
                    profileVersion != profileRequestVersion ||
                    requestedMid != upMid
                ) return@launch
                profile = data
                if (data.name.isNotBlank()) upName = data.name
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("UpInfo", "Failed to load profile: mid=$requestedMid", error)
                if (
                    version == requestVersion &&
                    profileVersion == profileRequestVersion &&
                    requestedMid == upMid
                ) profileLoadFailed = true
            } finally {
                if (
                    version == requestVersion &&
                    profileVersion == profileRequestVersion &&
                    requestedMid == upMid
                ) profileLoading = false
            }
        }
    }

    private fun loadFollowingState() {
        if (!isLoggedIn || isSelf) return
        val requestedMid = upMid
        val version = requestVersion
        relationLoading = true
        relationJob = viewModelScope.launch {
            try {
                val following = withContext(Dispatchers.IO) {
                    userRepository.checkIsFollowing(requestedMid)
                }
                if (version != requestVersion || requestedMid != upMid) return@launch
                if (following == null) {
                    _uiEvent.emit(UiEffect.ShowToast("获取关注状态失败"))
                } else {
                    isFollowing = following
                }
            } finally {
                if (version == requestVersion && requestedMid == upMid) relationLoading = false
            }
        }
    }

}
