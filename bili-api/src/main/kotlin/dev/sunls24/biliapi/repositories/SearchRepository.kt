package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.search.Hotword
import dev.sunls24.biliapi.http.BiliHttpApi
import dev.sunls24.biliapi.http.util.smartDate
import org.koin.core.annotation.Single

@Single
class SearchRepository(
    private val authRepository: AuthRepository
) {
    suspend fun getSearchHotwords(limit: Int = 30): List<Hotword> =
        BiliHttpApi.getWebSearchSquare(limit = limit)
            .getResponseData().trending.list
            .map(Hotword::fromHttpWebHotword)

    suspend fun getSearchSuggest(
        keyword: String
    ): List<String> = BiliHttpApi.getKeywordSuggest(
        term = keyword,
        buvid = authRepository.buvid.orEmpty()
    ).suggests.map { it.value }

    /** 按类型进行搜索 */
    suspend fun searchType(
        keyword: String,
        type: SearchType,
        page: SearchTypePage
    ): SearchTypeResult {
        val response = BiliHttpApi.searchType(
            keyword = keyword,
            type = type.httpTypeParam,
            page = page.nextPage,
            buvid3 = authRepository.buvid3.orEmpty(),
            sessData = authRepository.sessionData,
        ).getResponseData()
        return SearchTypeResult.fromSearchTypeResult(response)
    }
}

data class SearchTypePage(val nextPage: Int = 1)

enum class SearchType(val httpTypeParam: String) {
    Video("video"),
    MediaBangumi("media_bangumi"),
    MediaFt("media_ft"),
    BiliUser("bili_user")
}

data class SearchTypeResult(
    val videos: List<Video> = emptyList(),
    val pgcs: List<Pgc> = emptyList(),
    val users: List<User> = emptyList(),
    val page: SearchTypePage
) {
    val itemCount get() = videos.size + pgcs.size + users.size

    companion object {
        fun fromSearchTypeResult(result: dev.sunls24.biliapi.http.entity.search.SearchResultData): SearchTypeResult {
            val first = result.searchTypeResults.firstOrNull()
                ?: return SearchTypeResult(page = SearchTypePage(nextPage = result.page + 1))
            return when (first) {
                is dev.sunls24.biliapi.http.entity.search.SearchVideoResult -> {
                    SearchTypeResult(
                        videos = result.searchTypeResults.map { Video.fromSearchVideoResult(it as dev.sunls24.biliapi.http.entity.search.SearchVideoResult) },
                        page = SearchTypePage(nextPage = result.page + 1)
                    )
                }

                is dev.sunls24.biliapi.http.entity.search.SearchMediaResult -> {
                    SearchTypeResult(
                        pgcs = result.searchTypeResults.map { Pgc.fromSearchPgcResult(it as dev.sunls24.biliapi.http.entity.search.SearchMediaResult) },
                        page = SearchTypePage(nextPage = result.page + 1)
                    )
                }

                is dev.sunls24.biliapi.http.entity.search.SearchBiliUserResult -> {
                    SearchTypeResult(
                        users = result.searchTypeResults.map { User.fromSearchUserResult(it as dev.sunls24.biliapi.http.entity.search.SearchBiliUserResult) },
                        page = SearchTypePage(nextPage = result.page + 1)
                    )
                }

                else -> {
                    SearchTypeResult(page = SearchTypePage(nextPage = result.page + 1))
                }
            }
        }

    }

    interface SearchTypeResultItem

    data class Video(
        val aid: Long,
        val bvid: String,
        val title: String,
        val cover: String,
        val author: String,
        val mid: Long,
        val duration: Int,
        val play: Int,
        val danmaku: Int,
        val pubTime: String? = null
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchVideoResult(video: dev.sunls24.biliapi.http.entity.search.SearchVideoResult) =
                Video(
                    aid = video.aid,
                    bvid = video.bvid,
                    title = video.title,
                    cover = "https:${video.pic}",
                    author = video.author,
                    mid = video.mid,
                    duration = convertStringTimeToSeconds(video.duration),
                    play = video.play,
                    danmaku = video.danmaku,
                    pubTime = video.pubDate.smartDate
                )

        }
    }

    data class Pgc(
        val title: String,
        val cover: String,
        val star: Float,
        val seasonId: Int
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchPgcResult(pgc: dev.sunls24.biliapi.http.entity.search.SearchMediaResult) =
                Pgc(
                    title = pgc.title,
                    cover = pgc.cover,
                    star = pgc.mediaScore.score,
                    seasonId = pgc.seasonId
                )

        }
    }

    data class User(
        val mid: Long,
        val name: String,
        val avatar: String,
        val sign: String
    ) : SearchTypeResultItem {
        companion object {
            fun fromSearchUserResult(user: dev.sunls24.biliapi.http.entity.search.SearchBiliUserResult) =
                User(
                    mid = user.mid,
                    name = user.uname,
                    avatar = "https:${user.upic}",
                    sign = user.usign
                )

        }
    }
}

private fun convertStringTimeToSeconds(time: String): Int {
    val parts = time.split(":")
    val hours = if (parts.size == 3) parts[0].toInt() else 0
    val minutes = parts[parts.size - 2].toInt()
    val seconds = parts[parts.size - 1].toInt()
    return (hours * 3600) + (minutes * 60) + seconds
}
