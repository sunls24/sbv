package dev.sunls24.sbv.viewmodel.video

import dev.sunls24.biliapi.entity.FavoriteFolderMetadata
import dev.sunls24.biliapi.entity.user.Author
import dev.sunls24.biliapi.entity.video.Tag
import dev.sunls24.biliapi.entity.video.VideoDetail.Stat
import dev.sunls24.biliapi.entity.video.VideoPage
import dev.sunls24.biliapi.entity.video.season.UgcSeason
import dev.sunls24.sbv.entity.carddata.VideoCardData
import java.util.Date

data class VideoDetailUiState(
    val videoDetailState: VideoDetailState? = null,
    val loadingState: VideoInfoState = VideoInfoState.Loading,
    val errorTip: String = "",
    val isFollowingUp: Boolean? = null,
    val followingStateLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isSelfAuthor: Boolean = false,
    val favoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    val videoFavoriteFolderIds: Set<Long> = emptySet()
) {
    val shouldShowLoading: Boolean
        get() = loadingState == VideoInfoState.Loading ||
                videoDetailState?.redirectToEp == true
}

data class VideoDetailState(
    val aid: Long = 0,
    val bvid: String? = null,
    val cid: Long,
    val epid: Int? = null,
    val cover: String,
    val title: String,
    val publishDate: Date,
    val stat: Stat,
    val author: Author,
    val tags: List<Tag>,
    val isUpowerExclusive: Boolean = false,
    val redirectToEp: Boolean,
    val argueTip: String?,
    val description: String,
    val pages: List<VideoPage>,
    val relatedVideos: List<VideoCardData>,
    val ugcSeason: UgcSeason?,
    val lastPlayedCid: Long,
    val lastPlayedTime: Int,
    val isLiked: Boolean,
    val isCoined: Boolean,
    val isFavorite: Boolean,
    val historyResolved: Boolean = false,
)
