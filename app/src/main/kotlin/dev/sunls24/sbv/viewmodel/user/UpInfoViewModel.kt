package dev.sunls24.sbv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.user.SpaceVideoPage
import dev.sunls24.biliapi.repositories.UserRepository
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.util.formatHourMinSec
import dev.sunls24.sbv.util.toWanString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class UpInfoViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    var upName by mutableStateOf("")
    var upMid by mutableLongStateOf(0L)
    val spaceVideos = mutableStateListOf<VideoCardData>()

    private var page = SpaceVideoPage()
    private var loadJob: Job? = null
    private var requestVersion = 0
    val noMore get() = !page.hasNext

    fun update() {
        if (loadJob?.isActive == true || noMore || upMid == 0L) return
        val requestedMid = upMid
        val requestedPage = page
        val version = ++requestVersion

        loadJob = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    userRepository.getSpaceVideos(
                        mid = requestedMid,
                        page = requestedPage,
                    )
                }
                if (version != requestVersion || requestedMid != upMid) return@launch

                spaceVideos.addAll(data.videos.map { item ->
                    VideoCardData(
                        avid = item.aid,
                        title = item.title,
                        // TODO 合集样式封面仍缺少可验证的 App API 样本。
                        cover = item.cover,
                        upName = item.author,
                        playString = item.play.takeIf { it != -1 }.toWanString(),
                        danmakuString = item.danmaku.takeIf { it != -1 }.toWanString(),
                        timeString = (item.duration * 1000L).formatHourMinSec(),
                        pubTime = item.pubTime
                    )
                })
                page = data.page
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // 保持当前页，下一次触发时重试。
            } finally {
                if (version == requestVersion) loadJob = null
            }
        }
    }
}
