package dev.sunls24.sbv.util

fun String.resizedImageUrl(size: ImageSize): String {
    return when (size) {
        ImageSize.Default -> this
        else -> "$this@${size.sizeString}.webp"
    }
}

enum class ImageSize(val sizeString: String) {
    Default(""),
    Cover("180h_288w_1c"),
    SmallVideoCardCover("400h_640w_1c"),
    SeasonCoverThumbnail("466h_622w"),
    Avatar("96h_96w_1c"),
    Icon("48h_48w_1c"),
    Background("720h_1280w_1c"),
}
