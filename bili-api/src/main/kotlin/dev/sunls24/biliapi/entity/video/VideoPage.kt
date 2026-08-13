package dev.sunls24.biliapi.entity.video


data class VideoPage(
    var cid: Long,
    val index: Int,
    val title: String,
    val duration: Int,
    val dimension: Dimension
) {
    companion object {
        fun fromVideoPage(videoPage: dev.sunls24.biliapi.http.entity.video.VideoPage) =
            VideoPage(
                cid = videoPage.cid,
                index = videoPage.page,
                title = videoPage.part,
                duration = videoPage.duration,
                dimension = Dimension.fromDimension(videoPage.dimension)
            )
    }
}
