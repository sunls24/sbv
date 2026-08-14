package dev.sunls24.sbv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.season.FollowingSeason
import dev.sunls24.biliapi.repositories.AuthRepository
import dev.sunls24.biliapi.repositories.SeasonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FollowingSeasonViewModel(
    private val seasonRepository: SeasonRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val followingSeasons = mutableStateListOf<FollowingSeason>()

    private var pageNumber = 1
    private val pageSize = 20
    private var noMore = false

    private var updateJob: Job? = null
    private var requestVersion = 0
    private var initialized = false

    fun ensureLoaded() {
        if (!initialized) loadMore()
    }

    fun clearData() {
        requestVersion++
        updateJob?.cancel()
        updateJob = null
        pageNumber = 1
        noMore = false
        initialized = false
        followingSeasons.clear()
    }

    fun loadMore() {
        if (!authRepository.isLoggedIn || updateJob?.isActive == true || noMore) return
        val requestedPage = pageNumber
        val version = ++requestVersion

        updateJob = viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    seasonRepository.getFollowingSeasons(
                        pageNumber = requestedPage,
                        pageSize = pageSize,
                    )
                }
                if (version != requestVersion) return@launch

                noMore = pageSize * requestedPage >= response.total
                pageNumber = requestedPage + 1
                followingSeasons.addAll(response.list)
                initialized = true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // 保持当前页码，重新进入或继续翻页时重试。
            } finally {
                if (version == requestVersion) updateJob = null
            }
        }
    }
}
