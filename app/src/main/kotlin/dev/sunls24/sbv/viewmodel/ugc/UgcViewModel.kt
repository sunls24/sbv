package dev.sunls24.sbv.viewmodel.ugc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.ugc.UgcItem
import dev.sunls24.biliapi.entity.ugc.region.UgcFeedPage
import dev.sunls24.biliapi.repositories.UgcRepository
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.component.UgcTopNavItem
import dev.sunls24.sbv.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UgcViewModel(private val ugcRepository: UgcRepository) : ViewModel() {
    private val _states = MutableStateFlow<Map<UgcTopNavItem, UgcRegionState>>(emptyMap())
    val states = _states.asStateFlow()
    private val jobs = mutableMapOf<UgcTopNavItem, Job>()

    fun ensureLoaded(item: UgcTopNavItem) {
        if (_states.value[item] != null) return
        _states.update { it + (item to UgcRegionState()) }
        load(item, refresh = true)
    }

    fun reloadAll(item: UgcTopNavItem) {
        jobs.remove(item)?.cancel()
        _states.update {
            val requestVersion = (it[item]?.requestVersion ?: 0) + 1
            it + (item to UgcRegionState(requestVersion = requestVersion))
        }
        load(item, refresh = true)
    }

    fun loadMoreData(item: UgcTopNavItem) {
        load(item, refresh = false)
    }

    private fun load(item: UgcTopNavItem, refresh: Boolean) {
        val state = _states.value[item] ?: return
        if (!state.hasMore || state.updating || jobs[item]?.isActive == true) return
        val requestVersion = state.requestVersion + 1
        _states.update { states ->
            states + (item to state.copy(updating = true, requestVersion = requestVersion))
        }
        jobs[item] = viewModelScope.launch {
            try {
                val current = _states.value[item] ?: return@launch
                val feed = withContext(Dispatchers.IO) {
                    ugcRepository.getRegionFeedRcmd(item.ugcTypeV2, current.nextPage)
                }
                if (_states.value[item]?.requestVersion != requestVersion) return@launch
                _states.update { states ->
                    val latest = states[item] ?: return@update states
                    states + (
                        item to latest.copy(
                            items = if (refresh) feed.items else latest.items + feed.items,
                            nextPage = feed.nextPage,
                            hasMore = feed.items.isNotEmpty(),
                            updating = false
                        )
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (_states.value[item]?.requestVersion != requestVersion) return@launch
                _states.update { states ->
                    val latest = states[item] ?: return@update states
                    states + (item to latest.copy(updating = false))
                }
                val action = if (refresh) "加载" else "加载更多"
                (action + " " + item.ugcTypeV2 + " 数据失败: " + error.message)
                    .toast(SBVApp.context)
            } finally {
                if (_states.value[item]?.requestVersion == requestVersion) jobs.remove(item)
            }
        }
    }
}

data class UgcRegionState(
    val items: List<UgcItem> = emptyList(),
    val nextPage: UgcFeedPage = UgcFeedPage(),
    val hasMore: Boolean = true,
    val updating: Boolean = false,
    val requestVersion: Int = 0
)
