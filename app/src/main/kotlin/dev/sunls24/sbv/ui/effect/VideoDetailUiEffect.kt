package dev.sunls24.sbv.ui.effect

sealed class VideoDetailUiEffect {
    data class ShowToast(val message: String) : VideoDetailUiEffect()
    data class LaunchSeasonInfoActivity(val seasonId: Int?, val epid: Int?) :
        VideoDetailUiEffect()
}
