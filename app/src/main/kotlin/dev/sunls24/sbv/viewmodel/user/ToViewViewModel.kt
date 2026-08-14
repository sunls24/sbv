package dev.sunls24.sbv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.http.entity.AuthFailureException
import dev.sunls24.biliapi.repositories.ToViewRepository
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.R
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.repository.UserRepository
import dev.sunls24.sbv.ui.effect.UiEffect
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ToViewViewModel(
    private val userRepository: UserRepository,
    private val toViewRepository: ToViewRepository
) : ViewModel() {
    private val _uiEffect = MutableSharedFlow<UiEffect>()
    val uiEvent = _uiEffect.asSharedFlow()

    val histories = mutableStateListOf<VideoCardData>()
    private var updateJob: Job? = null
    private var initialized = false

    fun ensureLoaded() {
        if (!initialized) update()
    }

    fun update() {
        if (!userRepository.isLogin) return
        updateJob?.cancel()
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            loadToView()
        }
    }

    fun addToView(aid: Long, bvid: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                toViewRepository.addToView(aid = aid, bvid = bvid)
            }.onSuccess {
                _uiEffect.emit(UiEffect.ShowToast("添加到稍后再看"))
            }.onFailure {
                _uiEffect.emit(UiEffect.ShowToast("添加到稍后再看失败"))
            }
        }
    }
    fun delToView(aid: Long, viewed: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                toViewRepository.delToView(viewed = viewed, aid = aid)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    histories.removeAll { it.avid == aid }
                }
                _uiEffect.emit(UiEffect.ShowToast("删除稍后再看"))
            }.onFailure {
                _uiEffect.emit(UiEffect.ShowToast("删除稍后再看失败"))
            }
        }
    }

    fun clearData() {
        updateJob?.cancel()
        updateJob = null
        histories.clear()
        initialized = false
    }

    private suspend fun loadToView(context: Context = SBVApp.context) {
        try {
            val histories = toViewRepository.getToView().map { toViewItem ->
                VideoCardData(
                    avid = toViewItem.oid,
                    title = toViewItem.title,
                    cover = toViewItem.cover,
                    upName = toViewItem.author,
                    upMid = toViewItem.mid,
                    timeString = if (toViewItem.progress == -1) {
                        context.getString(R.string.play_time_finish)
                    } else {
                        context.getString(
                            R.string.play_time_history,
                            (toViewItem.progress * 1000L).formatHourMinSec(),
                            (toViewItem.duration * 1000L).formatHourMinSec()
                        )
                    }
                )
            }
            withContext(Dispatchers.Main) {
                this@ToViewViewModel.histories.clear()
                this@ToViewViewModel.histories.addAll(histories)
                initialized = true
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            when (error) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        SBVApp.context.getString(R.string.exception_auth_failure)
                            .toast(SBVApp.context)
                    }
                    userRepository.logout()
                }

                else -> {}
            }
        }
    }
}
