package dev.sunls24.sbv.repository

import android.util.Log
import dev.sunls24.biliapi.entity.video.HeartbeatVideoType
import dev.sunls24.biliapi.repositories.VideoPlayRepository
import dev.sunls24.sbv.util.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single

@Single
class PlaybackProgressReporter(
    private val videoInfoRepository: VideoInfoRepository,
    private val videoPlayRepository: VideoPlayRepository
) {
    private var reportJob: Job? = null

    fun report(
        scope: CoroutineScope,
        progress: PlaybackProgress,
        updateLocal: Boolean = true,
        timeoutMillis: Long? = null
    ) {
        if (updateLocal) {
            videoInfoRepository.updateHistory(progress.time, progress.cid)
        }
        if (Prefs.incognitoMode) return

        reportJob?.cancel()
        reportJob = scope.launch(Dispatchers.IO) {
            runCatching {
                if (timeoutMillis == null) upload(progress)
                else withTimeout(timeoutMillis) { upload(progress) }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.w("PlaybackProgress", "Failed to upload playback progress", error)
            }
        }
    }

    private suspend fun upload(progress: PlaybackProgress) {
        videoPlayRepository.sendHeartbeat(
            aid = progress.aid,
            cid = progress.cid,
            time = progress.time,
            type = if (progress.fromSeason) HeartbeatVideoType.Season
            else HeartbeatVideoType.Video,
            subType = progress.subType.takeIf { progress.fromSeason },
            epid = progress.epid.takeIf { progress.fromSeason },
            seasonId = progress.seasonId.takeIf { progress.fromSeason }
        )
    }
}

data class PlaybackProgress(
    val aid: Long,
    val cid: Long,
    val time: Int,
    val fromSeason: Boolean,
    val subType: Int,
    val epid: Int?,
    val seasonId: Int
)
