package dev.sunls24.biliapi.repositories

import dev.sunls24.biliapi.entity.video.VideoDetail
import dev.sunls24.biliapi.entity.video.season.SeasonDetail
import dev.sunls24.biliapi.http.BiliHttpApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class VideoDetailRepository(
    private val authRepository: AuthRepository,
    private val favoriteRepository: FavoriteRepository,
    private val likeRepository: LikeRepository,
    private val coinRepository: CoinRepository
) {
    suspend fun getVideoDetail(
        aid: Long,
        includeUserActions: Boolean = true
    ): VideoDetail = withContext(Dispatchers.IO) {
                    val videoDetailWithoutUserActions = async {
                        val httpVideoDetail = BiliHttpApi.getVideoDetail(
                            av = aid,
                            sessData = authRepository.sessionData ?: ""
                        ).getResponseData()
                        VideoDetail.fromVideoDetail(httpVideoDetail)
                    }

                    val shouldLoadUserActions = includeUserActions && authRepository.isLoggedIn

                    val isFavoured = async {
                        if (!shouldLoadUserActions) return@async false
                        runCatching {
                            favoriteRepository.checkVideoFavoured(aid)
                        }.getOrDefault(false)
                    }

                    val isLiked = async {
                        if (!shouldLoadUserActions) return@async false
                        runCatching {
                            likeRepository.checkVideoLiked(
                                aid = aid,
                            )
                        }.getOrDefault(false)
                    }

                    val isCoined = async {
                        if (!shouldLoadUserActions) return@async false
                        runCatching {
                            coinRepository.checkVideoCoined(
                                aid = aid,
                            )
                        }.getOrDefault(false)
                    }


                    val historyAndPlayerIcon = async {
                        runCatching {
                            val videoModeInfo = BiliHttpApi.getVideoMoreInfo(
                                avid = aid,
                                cid = videoDetailWithoutUserActions.await().cid,
                                sessData = authRepository.sessionData ?: "",
                                buvid3 = authRepository.buvid3 ?: ""
                            ).getResponseData()
                            val history = VideoDetail.History(
                                progress = videoModeInfo.lastPlayTime / 1000,
                                lastPlayedCid = videoModeInfo.lastPlayCid
                            )
                            history
                        }.getOrDefault(VideoDetail.History(0, 0))
                    }

                    videoDetailWithoutUserActions.await().let { detail ->
                        val newUserActions = detail.userActions.copy(
                            favorite = isFavoured.await(),
                            like = isLiked.await(),
                            coin = isCoined.await()
                        )
                        val newHistory = historyAndPlayerIcon.await()
                        detail.copy(
                            userActions = newUserActions,
                            history = newHistory
                        )
                    }
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
