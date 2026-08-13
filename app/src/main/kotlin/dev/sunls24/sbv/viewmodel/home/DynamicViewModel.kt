package dev.sunls24.sbv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.user.DynamicVideo
import dev.sunls24.biliapi.http.entity.AuthFailureException
import dev.sunls24.biliapi.repositories.UserRepository
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.BuildConfig
import dev.sunls24.sbv.R
import dev.sunls24.sbv.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.sunls24.sbv.repository.UserRepository as SBVUserRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class DynamicViewModel(
    private val sbvUserRepository: SBVUserRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    val dynamicList = mutableStateListOf<DynamicVideo>()

    private var currentPage = 0
    var loading by mutableStateOf(false)
        private set

    var hasMore by mutableStateOf(true)
        private set

    private var historyOffset: String? = null
    private var loadJob: Job? = null
    private var requestVersion = 0
    private var initialized = false
    val isLogin get() = sbvUserRepository.isLogin

    fun ensureLoaded() {
        if (!initialized) loadMore()
    }

    fun loadMore() {
        if (!loading) startLoad()
    }

    fun refresh() {
        clear()
        startLoad()
    }

    fun clear() {
        requestVersion++
        loadJob?.cancel()
        loadJob = null
        dynamicList.clear()
        currentPage = 0
        historyOffset = null
        hasMore = true
        initialized = false
        loading = false
    }

    private fun startLoad() {
        if (!hasMore || !sbvUserRepository.isLogin) return
        if (loading) return

        val version = ++requestVersion
        loading = true
        val nextPage = currentPage + 1
        val offset = historyOffset.orEmpty()
        loadJob = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    userRepository.getDynamicVideos(page = nextPage, offset = offset)
                }
                if (version != requestVersion) return@launch
                currentPage = nextPage
                dynamicList.addAll(data.videos)
                historyOffset = data.historyOffset
                hasMore = data.hasMore
                initialized = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                when (error) {
                    is AuthFailureException -> {
                        SBVApp.context.getString(R.string.exception_auth_failure)
                            .toast(SBVApp.context)
                        if (!BuildConfig.DEBUG) sbvUserRepository.logout()
                    }

                    else -> {
                        "加载动态失败: ${error.localizedMessage}".toast(SBVApp.context)
                    }
                }
            } finally {
                if (version == requestVersion) {
                    loading = false
                    loadJob = null
                }
            }
        }
    }
}
