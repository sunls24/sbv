package dev.sunls24.sbv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.user.HistoryItem
import dev.sunls24.biliapi.http.entity.AuthFailureException
import dev.sunls24.biliapi.repositories.HistoryRepository
import dev.sunls24.sbv.R
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.repository.UserRepository
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HistoryViewModel(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    val histories = mutableStateListOf<VideoCardData>()
    var noMore by mutableStateOf(false)
        private set

    private var cursor = 0L
    private var updateJob: Job? = null
    private var requestVersion = 0
    private var initialized = false

    fun ensureLoaded() {
        if (!initialized) update()
    }

    fun update() {
        if (!userRepository.isLogin || updateJob?.isActive == true || noMore) return
        val requestedCursor = cursor
        val version = ++requestVersion
        updateJob = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    historyRepository.getHistories(cursor = requestedCursor)
                }
                if (version != requestVersion) return@launch

                histories.addAll(data.data.map(::toVideoCardData))
                cursor = data.cursor
                noMore = cursor == 0L
                initialized = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (version != requestVersion) return@launch
                if (error is AuthFailureException) {
                    SBVApp.context.getString(R.string.exception_auth_failure)
                        .toast(SBVApp.context)
                    userRepository.logout()
                }
            } finally {
                if (version == requestVersion) updateJob = null
            }
        }
    }

    fun clearData() {
        requestVersion++
        updateJob?.cancel()
        updateJob = null
        histories.clear()
        cursor = 0
        noMore = false
        initialized = false
    }

    private fun toVideoCardData(
        historyItem: HistoryItem,
        context: Context = SBVApp.context
    ) = VideoCardData(
        avid = historyItem.oid,
        title = historyItem.title,
        cover = historyItem.cover,
        upName = historyItem.author,
        upMid = historyItem.mid,
        timeString = if (historyItem.progress == -1) {
            context.getString(R.string.play_time_finish)
        } else {
            context.getString(
                R.string.play_time_history,
                (historyItem.progress * 1000L).formatHourMinSec(),
                (historyItem.duration * 1000L).formatHourMinSec()
            )
        }
    )
}
