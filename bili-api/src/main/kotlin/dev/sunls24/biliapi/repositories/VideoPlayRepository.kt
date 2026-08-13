package dev.sunls24.biliapi.repositories

import bilibili.app.playerunite.v1.PlayerGrpcKt
import bilibili.app.playerunite.v1.playViewUniteReq
import bilibili.pgc.gateway.player.v2.playViewReq
import bilibili.playershared.videoVod
import dev.sunls24.biliapi.entity.CodeType
import dev.sunls24.biliapi.entity.PlayData
import dev.sunls24.biliapi.entity.video.HeartbeatVideoType
import dev.sunls24.biliapi.entity.video.Subtitle
import dev.sunls24.biliapi.http.BiliHttpApi
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.Single
import bilibili.pgc.gateway.player.v2.PlayURLGrpcKt as PgcPlayURLGrpcKt

@Single
class VideoPlayRepository(
    private val authRepository: AuthRepository,
    private val channelRepository: ChannelRepository
) {
    private val playerStub
        get() = PlayerGrpcKt.PlayerCoroutineStub(
            checkNotNull(channelRepository.defaultChannel) { "gRPC channel is not initialized" }
        )

    private val pgcPlayUrlStub
        get() = PgcPlayURLGrpcKt.PlayURLCoroutineStub(
            checkNotNull(channelRepository.defaultChannel) { "gRPC channel is not initialized" }
        )

    suspend fun getPlayData(
        aid: Long,
        cid: Long,
        preferCodec: CodeType = CodeType.Code264
    ): PlayData = webOrGrpc(
        web = {
            BiliHttpApi.getVideoPlayUrl(
                av = aid,
                cid = cid,
                fnval = 4048,
                qn = 127,
                fnver = 0,
                fourk = 1,
                sessData = authRepository.sessionData,
                dedeUserID = authRepository.mid
            ).getResponseData().let(PlayData::fromPlayUrlData)
        },
        grpc = {
            firstPlayable(codecCandidates(preferCodec)) { codec ->
                playerStub.playViewUnite(playViewUniteReq {
                    vod = videoVod {
                        this.aid = aid
                        this.cid = cid
                        fnval = 4048
                        qn = 127
                        fourk = true
                        forceHost = 2
                        preferCodecType = codec.toPlayerSharedCodeType()
                    }
                }).let(PlayData::fromPlayViewUniteReply)
            }
        }
    )

    suspend fun getPgcPlayData(
        aid: Long?,
        cid: Long?,
        epid: Int,
        preferCodec: CodeType = CodeType.Code264
    ): PlayData = webOrGrpc(
        web = {
            BiliHttpApi.getPgcVideoPlayUrlV2(
                av = aid,
                cid = cid,
                epid = epid,
                fnval = 4048,
                qn = 127,
                fnver = 0,
                fourk = 1,
                sessData = authRepository.sessionData
            ).getResponseData().let(PlayData::fromPlayUrlV2Data)
        },
        grpc = {
            firstPlayable(codecCandidates(preferCodec)) { codec ->
                pgcPlayUrlStub.playView(playViewReq {
                    this.epid = epid.toLong()
                    cid?.let { this.cid = it }
                    qn = 127
                    fnval = 4048
                    fourk = true
                    preferCodecType = codec.toPgcPlayUrlCodeType()
                }).let(PlayData::fromPgcPlayViewReply)
            }
        }
    )

    suspend fun getSubtitle(aid: Long, cid: Long): List<Subtitle> =
        BiliHttpApi.getVideoMoreInfo(
            avid = aid,
            cid = cid,
            sessData = authRepository.sessionData.orEmpty(),
            buvid3 = authRepository.buvid3.orEmpty()
        ).getResponseData().subtitle?.subtitles
            ?.map(Subtitle::fromSubtitleItem)
            .orEmpty()

    suspend fun sendHeartbeat(
        aid: Long,
        cid: Long,
        time: Int,
        type: HeartbeatVideoType = HeartbeatVideoType.Video,
        subType: Int? = null,
        epid: Int? = null,
        seasonId: Int? = null
    ) {
        BiliHttpApi.sendHeartbeat(
            avid = aid,
            cid = cid,
            playedTime = time,
            type = type.value,
            subType = subType,
            epid = epid,
            sid = seasonId,
            csrf = authRepository.biliJct,
            sessData = authRepository.sessionData.orEmpty()
        )
    }

    private suspend fun webOrGrpc(
        web: suspend () -> PlayData,
        grpc: suspend () -> PlayData
    ): PlayData {
        val webError = try {
            return web().requirePlayable()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            error
        }
        if (authRepository.accessToken.isNullOrBlank()) throw webError

        return try {
            authRepository.refreshAppTokenIfNeeded()
            grpc().requirePlayable()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            webError.addSuppressed(error)
            throw webError
        }
    }

    private suspend fun firstPlayable(
        codecs: List<CodeType>,
        request: suspend (CodeType) -> PlayData
    ): PlayData {
        var lastError: Throwable? = null
        for (codec in codecs) {
            try {
                return request(codec).requirePlayable()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                lastError = error
            }
        }
        throw IllegalStateException("Unable to get a playable gRPC stream", lastError)
    }

    private fun codecCandidates(preferCodec: CodeType): List<CodeType> =
        listOf(preferCodec.takeUnless { it == CodeType.NoCode } ?: CodeType.Code264, CodeType.Code264)
            .distinct()

    private fun PlayData.requirePlayable(): PlayData = apply {
        check(dashVideos.isNotEmpty()) { "Play response contains no video stream" }
    }
}
