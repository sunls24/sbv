package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.video.VideoDetail
import dev.sunls24.biliapi.entity.video.UserActions
import dev.sunls24.biliapi.entity.video.season.SeasonDetail
import dev.sunls24.biliapi.http.BiliHttpApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class VideoDetailRepository(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val likeRepository: LikeRepository
) {
    suspend fun getVideoDetail(
        aid: Long,
        includeUserActions: Boolean = true
    ): VideoDetail = coroutineScope {
        val detail = getVideoDetailBase(aid)
        val history = async { getVideoHistory(detail) }
        val userActions = async { getVideoUserActions(aid, includeUserActions) }
        detail.copy(
            history = history.await(),
            userActions = userActions.await()
        )
    }

    suspend fun getVideoDetailBase(aid: Long): VideoDetail = withContext(Dispatchers.IO) {
        BiliHttpApi.getVideoDetail(
            av = aid,
            sessData = authRepository.sessionData.orEmpty()
        ).getResponseData().let(VideoDetail::fromVideoDetail)
    }

    suspend fun getVideoUserActions(
        aid: Long,
        includeUserActions: Boolean = true
    ): UserActions = withContext(Dispatchers.IO) {
        if (!includeUserActions || !authRepository.isLoggedIn) return@withContext UserActions()
        coroutineScope {
            val isFavoured = async {
                runCatching { favoriteRepository.checkVideoFavoured(aid) }.getOrDefault(false)
            }
            val isLiked = async {
                runCatching { likeRepository.checkVideoLiked(aid) }.getOrDefault(false)
            }
            UserActions(
                favorite = isFavoured.await(),
                like = isLiked.await()
            )
        }
    }

    suspend fun getVideoHistory(detail: VideoDetail): VideoDetail.History =
        withContext(Dispatchers.IO) {
            runCatching {
                BiliHttpApi.getVideoMoreInfo(
                    avid = detail.aid,
                    cid = detail.cid,
                    sessData = authRepository.sessionData.orEmpty(),
                    buvid3 = authRepository.buvid3.orEmpty()
                ).getResponseData().let {
                    VideoDetail.History(
                        progress = it.lastPlayTime / 1000,
                        lastPlayedCid = it.lastPlayCid
                    )
                }
            }.getOrDefault(VideoDetail.History(0, 0))
        }

    suspend fun getPgcVideoDetail(
        epid: Int? = null,
        seasonId: Int? = null
    ): SeasonDetail {
        val data = BiliHttpApi.getWebSeasonInfo(
            epId = epid,
            seasonId = seasonId,
            sessData = authRepository.sessionData.orEmpty()
        ).getResponseData()
        val detail = SeasonDetail.fromSeasonData(data)
        val firstEp = data.episodes.firstOrNull() ?: return detail
        detail.playerIcon = runCatching {
            BiliHttpApi.getVideoMoreInfo(
                avid = firstEp.aid,
                cid = firstEp.cid,
                sessData = authRepository.sessionData.orEmpty(),
                buvid3 = authRepository.buvid3.orEmpty()
            ).getResponseData().playerIcon.let(VideoDetail.PlayerIcon::fromPlayerIcon)
        }.getOrNull()
        return detail
        }
}
