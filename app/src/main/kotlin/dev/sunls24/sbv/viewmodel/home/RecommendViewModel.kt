package dev.sunls24.sbv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.home.RecommendPage
import dev.sunls24.biliapi.entity.ugc.UgcItem
import dev.sunls24.biliapi.repositories.RecommendVideoRepository
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class RecommendViewModel(
    private val recommendVideoRepository: RecommendVideoRepository
) : ViewModel() {
    val recommendVideoList = mutableStateListOf<UgcItem>()
    private val loadedAids = mutableSetOf<Long>()

    private var nextPage = RecommendPage()
    var loading by mutableStateOf(false)
        private set
    private var loadJob: Job? = null
    private var requestVersion = 0
    private var initialized = false

    fun ensureLoaded() {
        if (!initialized) loadMore()
    }

    fun loadMore() {
        if (loading) return
        startLoad()
    }

    fun refresh() {
        requestVersion++
        loadJob?.cancel()
        recommendVideoList.clear()
        loadedAids.clear()
        nextPage = RecommendPage()
        initialized = false
        loading = false
        startLoad()
    }

    private fun startLoad() {
        val version = ++requestVersion
        loading = true
        loadJob = viewModelScope.launch {
            try {
                var requestCount = 0
                var newItems = emptyList<UgcItem>()
                while (requestCount < MAX_PAGE_REQUESTS_PER_LOAD && newItems.isEmpty()) {
                    val page = nextPage
                    val data = withContext(Dispatchers.IO) {
                        recommendVideoRepository.getRecommendVideos(page)
                    }
                    if (version != requestVersion) return@launch
                    nextPage = data.nextPage
                    newItems = data.items.filter { loadedAids.add(it.aid) }
                    requestCount++
                }
                recommendVideoList.addAll(newItems)
                initialized = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                "加载推荐视频失败: ${error.localizedMessage}".toast(SBVApp.context)
            } finally {
                if (version == requestVersion) {
                    loading = false
                    loadJob = null
                }
            }
        }
    }

    private companion object {
        const val MAX_PAGE_REQUESTS_PER_LOAD = 2
    }
}
