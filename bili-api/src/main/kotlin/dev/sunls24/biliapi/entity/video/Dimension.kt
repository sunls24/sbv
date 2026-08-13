package dev.sunls24.biliapi.entity.video

data class Dimension(
    val width: Int,
    val height: Int,
    val isVertical: Boolean = width < height
) {
    companion object {
        fun fromDimension(dimension: dev.sunls24.biliapi.http.entity.video.Dimension) =
            Dimension(
                width = dimension.width,
                height = dimension.height
            )
    }
}
