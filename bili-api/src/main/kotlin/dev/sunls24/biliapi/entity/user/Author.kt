package dev.sunls24.biliapi.entity.user

import dev.sunls24.biliapi.http.entity.video.VideoOwner

data class Author(
    val mid: Long,
    val name: String,
    val face: String
) {
    companion object {
        fun fromVideoOwner(videoOwner: VideoOwner) = Author(
            mid = videoOwner.mid,
            name = videoOwner.name,
            face = videoOwner.face
        )
    }
}
