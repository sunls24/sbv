package dev.sunls24.sbv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.rank.PopularVideoPage
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
class PopularViewModel(
    private val recommendVideoRepository: RecommendVideoRepository
) : ViewModel() {
    val popularVideoList = mutableStateListOf<UgcItem>()

    private var nextPage = PopularVideoPage()
    var loading by mutableStateOf(false)
        private set
    private var loadJob: Job? = null
    private var requestVersion = 0
    private var initialized = false
    var hasMore by mutableStateOf(true)
        private set

    fun ensureLoaded() {
        if (!initialized) loadMore()
    }

    fun loadMore() {
        if (loading || !hasMore) return
        startLoad()
    }

    fun refresh() {
        requestVersion++
        loadJob?.cancel()
        popularVideoList.clear()
        nextPage = PopularVideoPage()
        initialized = false
        hasMore = true
        loading = false
        startLoad()
    }

    private fun startLoad() {
        val version = ++requestVersion
        loading = true
        loadJob = viewModelScope.launch {
            try {
                val page = nextPage
                val data = withContext(Dispatchers.IO) {
                    recommendVideoRepository.getPopularVideos(page)
                }
                if (version != requestVersion) return@launch
                nextPage = data.nextPage
                popularVideoList.addAll(data.list)
                hasMore = !data.noMore
                initialized = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                "加载热门视频失败: ${error.localizedMessage}".toast(SBVApp.context)
            } finally {
                if (version == requestVersion) {
                    loading = false
                    loadJob = null
                }
            }
        }
    }
}
