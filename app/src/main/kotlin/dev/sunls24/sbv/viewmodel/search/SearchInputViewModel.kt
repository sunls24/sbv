package dev.sunls24.sbv.viewmodel.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.search.Hotword
import dev.sunls24.biliapi.repositories.SearchRepository
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.util.swapListWithMainContext
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
    val hotwords = mutableStateListOf<Hotword>()
    val suggests = mutableStateListOf<String>()
    val searchHistories = mutableStateListOf<String>()
    private var suggestJob: Job? = null

    init {
        updateHotwords()
        loadSearchHistories()
    }

    private fun updateHotwords() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { searchRepository.getSearchHotwords(limit = 50) }
                .onSuccess { hotwords.swapListWithMainContext(it) }
                .onFailure {
                    withContext(Dispatchers.Main) { "bilibili 热搜加载失败".toast(SBVApp.context) }
                }
        }
    }

    fun updateSuggests() {
        suggestJob?.cancel()
        val requestedKeyword = keyword.trim()
        if (requestedKeyword.isEmpty()) {
            suggests.clear()
            return
        }

        suggestJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(250)
                val result = searchRepository.getSearchSuggest(requestedKeyword)
                if (keyword.trim() == requestedKeyword) {
                    suggests.swapListWithMainContext(result)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    "bilibili 搜索建议加载失败".toast(SBVApp.context)
                }
            }
        }
    }

    private fun loadSearchHistories() {
        val histories = runCatching {
            Json.decodeFromString<List<String>>(Prefs.searchHistoryJson)
        }.getOrDefault(emptyList())
        searchHistories.clear()
        searchHistories.addAll(histories)
    }

    private fun saveSearchHistories() {
        Prefs.searchHistoryJson = Json.encodeToString(searchHistories.toList())
    }

    fun addSearchHistory(keyword: String) {
        val normalized = keyword.trim()
        if (normalized.isEmpty()) return
        searchHistories.remove(normalized)
        searchHistories.add(0, normalized)
        while (searchHistories.size > 20) searchHistories.removeAt(searchHistories.lastIndex)
        saveSearchHistories()
    }

    fun deleteSearchHistory(history: String) {
        searchHistories.remove(history)
        saveSearchHistories()
    }

    fun deleteAllSearchHistories() {
        searchHistories.clear()
        saveSearchHistories()
    }
}
