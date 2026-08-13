package dev.sunls24.biliapi.entity.video

data class Tag(
    val id: Int,
    val name: String
) {
    companion object {
        fun fromTag(tag: dev.sunls24.biliapi.http.entity.video.Tag) = Tag(
            id = tag.tagId,
            name = tag.tagName
        )

        fun fromTag(tag: dev.sunls24.biliapi.http.entity.video.VideoDetail.Tag) = Tag(
            id = tag.tagId,
            name = tag.tagName
        )
    }
}
