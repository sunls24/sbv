package dev.sunls24.biliapi.http.entity.video

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoDetail(
    @SerialName("View")
    val view: VideoInfo,
    @SerialName("Tags")
    val tags: List<Tag>,
    //TODO 评论
    //@SerialName("Reply")
    //val reply:Any
    @SerialName("Related")
    val related: List<RelatedVideoInfo>?
    //@SerialName("is_old_user")
    //val isOldUser: Boolean
) {
    /**
     * 视频 TAG
     *
     * @param tagId TAG ID 当存在[musicId]时可能为 0
     * @param tagName TAG名称
     * @param musicId 音乐ID
     * @param tagType TAG类型 bgm old_channel topic
     * @param jumpUrl 跳转链接
     */
    @Serializable
    data class Tag(
        @SerialName("tag_id")
        val tagId: Int,
        @SerialName("tag_name")
        val tagName: String,
        @SerialName("music_id")
        val musicId: String,
        @SerialName("tag_type")
        val tagType: String,
        @SerialName("jump_url")
        val jumpUrl: String
    )
}
