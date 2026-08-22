package dev.sunls24.sbv.viewmodel.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.repositories.SearchRepository
import dev.sunls24.biliapi.repositories.SearchType
import dev.sunls24.biliapi.repositories.SearchTypePage
import dev.sunls24.biliapi.repositories.SearchTypeResult
import dev.sunls24.sbv.entity.carddata.SeasonCardData
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.removeHtmlTags
import dev.sunls24.sbv.util.toWanString
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

    fun loadMore(type: SearchType) {
        if (keyword.isBlank()) return
        val currentState = state(type)
        if (!currentState.hasMore || currentState.updating) return

        val version = currentState.requestVersion
        val page = currentState.result.page
        val keyword = keyword
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
                    )
                }
                val uiItems = withContext(Dispatchers.Default) {
                    response.toUiItems(type)
                }
                if (version != state(type).requestVersion) return@launch
                updateState(type) {
                    val nextResult = it.result.append(response.page, uiItems)
                    it.copy(
                        result = nextResult,
                        loadState = SearchLoadState.Idle,
                        initialized = true,
                        hasMore = response.itemCount > 0 && nextResult.count > it.result.count
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
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
        val items: List<SearchResultUiItem> = emptyList(),
        val page: SearchTypePage = SearchTypePage()
    ) {
        val count get() = items.size

        fun append(nextPage: SearchTypePage, newItems: List<SearchResultUiItem>): SearchResult {
            val keys = items.asSequence().mapTo(hashSetOf()) { it.key }
            val uniqueItems = newItems.filter { keys.add(it.key) }
            return copy(
                items = items + uniqueItems,
                page = nextPage,
            )
        }
    }
}

@Immutable
sealed interface SearchResultUiItem {
    val key: String
    val contentType: String

    @Immutable
    data class Video(
        val card: VideoCardData,
        val aid: Long,
        val mid: Long,
        val author: String,
    ) : SearchResultUiItem {
        override val key = "video:$aid"
        override val contentType = "video"
    }

    @Immutable
    data class Pgc(
        val card: SeasonCardData,
    ) : SearchResultUiItem {
        override val key = "pgc:${card.seasonId}"
        override val contentType = "pgc"
    }

    @Immutable
    data class User(
        val mid: Long,
        val name: String,
        val avatar: String,
        val sign: String,
    ) : SearchResultUiItem {
        override val key = "user:$mid"
        override val contentType = "user"
    }
}

private fun SearchTypeResult.toUiItems(type: SearchType): List<SearchResultUiItem> = when (type) {
    SearchType.Video -> videos.map { video ->
        SearchResultUiItem.Video(
            card = VideoCardData(
                avid = video.aid,
                title = video.title.removeHtmlTags(),
                cover = video.cover,
                playString = video.play.takeIf { it != -1 }.toWanString(),
                danmakuString = video.danmaku.takeIf { it != -1 }.toWanString(),
                timeString = (video.duration * 1000L).formatHourMinSec(),
                upName = video.author,
                pubTime = video.pubTime,
            ),
            aid = video.aid,
            mid = video.mid,
            author = video.author,
        )
    }

    SearchType.MediaBangumi, SearchType.MediaFt -> pgcs.map { pgc ->
        SearchResultUiItem.Pgc(
            card = SeasonCardData(
                seasonId = pgc.seasonId,
                title = pgc.title.removeHtmlTags(),
                cover = pgc.cover,
                rating = String.format(java.util.Locale.ROOT, "%.1f", pgc.star),
            )
        )
    }

    SearchType.BiliUser -> users.map { user ->
        SearchResultUiItem.User(
            mid = user.mid,
            name = user.name,
            avatar = user.avatar,
            sign = user.sign,
        )
    }
}

enum class SearchLoadState {
    Idle,
    Loading,
    Error
}
