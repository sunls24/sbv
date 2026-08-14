package dev.sunls24.sbv.viewmodel.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sunls24.biliapi.entity.video.season.SeasonDetail
import dev.sunls24.biliapi.repositories.UserRepository
import dev.sunls24.biliapi.repositories.VideoDetailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SeasonDetailViewModel(
    private val videoDetailRepository: VideoDetailRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeasonDetailUiState())
    val uiState = _uiState.asStateFlow()
    private var seasonId: Int? = null
    private var epId: Int? = null
    private var loadJob: Job? = null
    private var progressJob: Job? = null

    fun load(seasonId: Int?, epId: Int?) {
        this.seasonId = seasonId
        this.epId = epId
        loadJob?.cancel()
        progressJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val season = videoDetailRepository.getPgcVideoDetail(
                    seasonId = seasonId,
                    epid = epId,
                )
                if (this@SeasonDetailViewModel.seasonId != seasonId ||
                    this@SeasonDetailViewModel.epId != epId
                ) return@launch
                _uiState.value = SeasonDetailUiState(
                    season = season,
                    progress = season.userStatus.progress,
                    isFollowing = season.userStatus.follow
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (this@SeasonDetailViewModel.seasonId != seasonId ||
                    this@SeasonDetailViewModel.epId != epId
                ) return@launch
                _uiState.update { it.copy(message = error.localizedMessage ?: "未知错误") }
            }
        }
    }

    fun refreshProgress() {
        val seasonId = seasonId
        val epId = epId
        progressJob?.cancel()
        progressJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                videoDetailRepository.getPgcVideoDetail(
                    seasonId = seasonId,
                    epid = epId,
                ).userStatus.progress
            }.onSuccess { progress ->
                if (this@SeasonDetailViewModel.seasonId != seasonId ||
                    this@SeasonDetailViewModel.epId != epId
                ) return@onSuccess
                _uiState.update { it.copy(progress = progress) }
            }
        }
    }

    suspend fun resolveCid(aid: Long, cid: Long): Long = cid.takeIf { it != 0L }
        ?: videoDetailRepository.getVideoDetail(aid, includeUserActions = false).cid

    suspend fun setFollowing(follow: Boolean): Result<String> = runCatching {
        val currentSeasonId = requireNotNull(_uiState.value.season?.seasonId)
        if (follow) userRepository.addSeasonFollow(currentSeasonId)
        else userRepository.delSeasonFollow(currentSeasonId)
    }.onSuccess {
        _uiState.update { state -> state.copy(isFollowing = follow) }
    }
}

data class SeasonDetailUiState(
    val season: SeasonDetail? = null,
    val progress: SeasonDetail.UserStatus.Progress? = null,
    val isFollowing: Boolean = false,
    val message: String = "Loading..."
)
