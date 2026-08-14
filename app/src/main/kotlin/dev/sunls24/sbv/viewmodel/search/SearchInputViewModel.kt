package dev.sunls24.sbv.viewmodel.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.search.Hotword
import dev.sunls24.biliapi.repositories.SearchRepository
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SearchInputViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    var keyword by mutableStateOf("")
    var hotwords by mutableStateOf<List<Hotword>>(emptyList())
        private set
    var suggests by mutableStateOf<List<String>>(emptyList())
        private set
    var searchHistories by mutableStateOf<List<String>>(emptyList())
        private set
    private var suggestJob: Job? = null

    init {
        updateHotwords()
        loadSearchHistories()
    }

    private fun updateHotwords() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { searchRepository.getSearchHotwords(limit = 50) }
                .onSuccess { result ->
                    withContext(Dispatchers.Main) {
                        hotwords = result.distinctBy(Hotword::showName)
                    }
                }
                .onFailure {
                    withContext(Dispatchers.Main) { "bilibili 热搜加载失败".toast(SBVApp.context) }
                }
        }
    }

    fun updateSuggests() {
        suggestJob?.cancel()
        val requestedKeyword = keyword.trim()
        if (requestedKeyword.isEmpty()) {
            suggests = emptyList()
            return
        }

        suggestJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(300)
                val result = searchRepository.getSearchSuggest(requestedKeyword)
                if (keyword.trim() == requestedKeyword) {
                    withContext(Dispatchers.Main) { suggests = result.distinct() }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // 自动补全失败不打断输入，也不重复弹出提示。
            }
        }
    }

    private fun loadSearchHistories() {
        val histories = runCatching {
            Json.decodeFromString<List<String>>(Prefs.searchHistoryJson)
        }.getOrDefault(emptyList())
        searchHistories = histories
    }

    private fun saveSearchHistories() {
        Prefs.searchHistoryJson = Json.encodeToString(searchHistories.toList())
    }

    fun addSearchHistory(keyword: String) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return
        searchHistories = (listOf(normalized) + searchHistories.filterNot { it == normalized })
            .take(20)
        saveSearchHistories()
    }

    fun deleteSearchHistory(history: String) {
        searchHistories = searchHistories.filterNot { it == history }
        saveSearchHistories()
    }

    fun deleteAllSearchHistories() {
        searchHistories = emptyList()
        saveSearchHistories()
    }
}
