package dev.sunls24.sbv.viewmodel.search

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.repositories.SearchFilterDuration
import dev.sunls24.biliapi.repositories.SearchFilterOrderType
import dev.sunls24.biliapi.repositories.SearchRepository
import dev.sunls24.biliapi.repositories.SearchType
import dev.sunls24.biliapi.repositories.SearchTypePage
import dev.sunls24.biliapi.repositories.SearchTypeResult
import dev.sunls24.sbv.util.Partition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SearchResultViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {
    var keyword by mutableStateOf("")
    var searchType by mutableStateOf(SearchType.Video)

    private var states by mutableStateOf(
        SearchType.entries.associateWith { SearchTypeState(result = SearchResult(it)) }
    )

    var selectedOrder by mutableStateOf(SearchFilterOrderType.ComprehensiveSort)
        private set
    var selectedDuration by mutableStateOf(SearchFilterDuration.All)
        private set
    var selectedPartition: Partition? by mutableStateOf(null)
        private set
    var selectedChildPartition: Partition? by mutableStateOf(null)
        private set

    private val jobs = mutableMapOf<SearchType, Job>()

    fun loadState(type: SearchType): SearchLoadState = state(type).loadState

    fun result(type: SearchType): SearchResult = state(type).result

    fun updateKeyword(value: String) {
        if (keyword == value) {
            ensureLoaded(searchType)
            return
        }
        keyword = value
        update()
    }

    fun update() {
        if (keyword.isBlank()) return
        SearchType.entries.forEach(::reset)
        ensureLoaded(searchType)
    }

    fun ensureLoaded(type: SearchType) {
        val currentState = state(type)
        if (currentState.initialized || currentState.updating) return
        loadMore(type)
    }

    fun selectOrder(order: SearchFilterOrderType) {
        if (selectedOrder == order) return
        selectedOrder = order
        reloadVideoSearch()
    }

    fun selectDuration(duration: SearchFilterDuration) {
        if (selectedDuration == duration) return
        selectedDuration = duration
        reloadVideoSearch()
    }

    fun selectPartition(partition: Partition?) {
        if (selectedPartition == partition && selectedChildPartition == null) return
        selectedPartition = partition
        selectedChildPartition = null
        reloadVideoSearch()
    }

    fun selectChildPartition(partition: Partition?) {
        if (selectedChildPartition == partition) return
        selectedChildPartition = partition
        reloadVideoSearch()
    }

    private fun reloadVideoSearch() {
        if (keyword.isBlank()) return
        reset(SearchType.Video)
        loadMore(SearchType.Video)
    }

    fun loadMore(type: SearchType) {
        if (keyword.isBlank()) return
        val currentState = state(type)
        if (!currentState.hasMore || currentState.updating) return

        val version = currentState.requestVersion
        val page = currentState.result.page
        val keyword = keyword
        val tid = selectedChildPartition?.tid ?: selectedPartition?.tid
        val order = selectedOrder
        val duration = selectedDuration

        updateState(type) {
            it.copy(loadState = SearchLoadState.Loading, updating = true)
        }
        jobs[type] = viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    searchRepository.searchType(
                        keyword = keyword,
                        type = type,
                        page = page,
                        tid = tid,
                        order = order,
                        duration = duration,
                    )
                }
                if (version != state(type).requestVersion) return@launch
                updateState(type) {
                    it.copy(
                        result = it.result.append(response),
                        loadState = SearchLoadState.Idle,
                        initialized = true,
                        hasMore = response.itemCount > 0
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("SearchResult", "Failed to load $type search results", error)
                if (version == state(type).requestVersion) {
                    updateState(type) { it.copy(loadState = SearchLoadState.Error) }
                }
            } finally {
                if (version == state(type).requestVersion) {
                    updateState(type) { it.copy(updating = false) }
                    jobs.remove(type)
                }
            }
        }
    }

    private fun reset(type: SearchType) {
        jobs.remove(type)?.cancel()
        val nextVersion = state(type).requestVersion + 1
        updateState(type) {
            SearchTypeState(
                result = SearchResult(type),
                requestVersion = nextVersion
            )
        }
    }

    private fun state(type: SearchType): SearchTypeState = states.getValue(type)

    private fun updateState(type: SearchType, update: (SearchTypeState) -> SearchTypeState) {
        states = states + (type to update(state(type)))
    }

    private data class SearchTypeState(
        val result: SearchResult,
        val loadState: SearchLoadState = SearchLoadState.Idle,
        val initialized: Boolean = false,
        val hasMore: Boolean = true,
        val requestVersion: Int = 0,
        val updating: Boolean = false
    )

    data class SearchResult(
        val type: SearchType,
        val videos: List<SearchTypeResult.Video> = emptyList(),
        val mediaBangumis: List<SearchTypeResult.Pgc> = emptyList(),
        val mediaFts: List<SearchTypeResult.Pgc> = emptyList(),
        val biliUsers: List<SearchTypeResult.User> = emptyList(),
        val page: SearchTypePage = SearchTypePage()
    ) {
        val count get() = videos.size + mediaBangumis.size + mediaFts.size + biliUsers.size

        fun append(result: SearchTypeResult): SearchResult = when (type) {
            SearchType.Video -> copy(videos = videos + result.videos, page = result.page)
            SearchType.MediaBangumi -> copy(
                mediaBangumis = mediaBangumis + result.pgcs,
                page = result.page
            )
            SearchType.MediaFt -> copy(mediaFts = mediaFts + result.pgcs, page = result.page)
            SearchType.BiliUser -> copy(biliUsers = biliUsers + result.users, page = result.page)
        }
    }
}

enum class SearchLoadState {
    Idle,
    Loading,
    Error
}
