package dev.sunls24.sbv.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SuggestionChip
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.biliapi.entity.FavoriteFolderMetadata
import dev.sunls24.biliapi.entity.video.Dimension
import dev.sunls24.biliapi.entity.video.Tag
import dev.sunls24.biliapi.entity.video.VideoPage
import dev.sunls24.biliapi.entity.video.season.Episode
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.video.SeasonInfoActivity
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.tv.component.TvAlertDialog
import dev.sunls24.sbv.component.UpIcon
import dev.sunls24.sbv.component.buttons.CoinButton
import dev.sunls24.sbv.component.buttons.FavoriteButton
import dev.sunls24.sbv.component.buttons.LikeButton
import dev.sunls24.sbv.component.ifElse
import dev.sunls24.sbv.component.videocard.VideosRow
import dev.sunls24.sbv.entity.VideoListItem
import dev.sunls24.sbv.ui.effect.UiEffect
import dev.sunls24.sbv.ui.effect.VideoDetailUiEffect
import dev.sunls24.sbv.ui.theme.SBVColorTokens
import dev.sunls24.sbv.ui.theme.SBVFocus
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.ui.theme.focusedTextColor
import dev.sunls24.sbv.util.focusedBorder
import dev.sunls24.sbv.util.formatPubTimeString
import dev.sunls24.sbv.util.ImageSize
import dev.sunls24.sbv.util.resizedImageUrl
import dev.sunls24.sbv.util.requestFocus
import dev.sunls24.sbv.util.toWanString
import dev.sunls24.sbv.util.toast
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import dev.sunls24.sbv.viewmodel.video.VideoDetailState
import dev.sunls24.sbv.viewmodel.video.VideoDetailViewModel
import dev.sunls24.sbv.viewmodel.video.VideoInfoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoInfoScreen(
    modifier: Modifier = Modifier,
    videoDetailViewModel: VideoDetailViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel(),
) {
    val context = (LocalContext.current) as Activity

    val defaultFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val uiState by videoDetailViewModel.uiState.collectAsState()

    var lastPlayedAid by remember(uiState.videoDetailState?.aid) {
        mutableLongStateOf(uiState.videoDetailState?.aid ?: 0L)
    }
    val containsVerticalScreenVideo by remember(uiState.videoDetailState) {
        derivedStateOf {
            uiState.videoDetailState?.pages?.any { it.dimension.isVertical } ?: false
        }
    }

    val bringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                val targetPosition = containerSize * 0.3f
                return offset - targetPosition
            }
        }
    }

    fun performLaunchPlayer(
        targetAid: Long,
        targetCid: Long,
        targetTitle: String
    ) {
        val videoDetailState = uiState.videoDetailState ?: return

        val playedTime = if (targetCid == videoDetailState.lastPlayedCid) {
            videoDetailState.lastPlayedTime * 1000
        } else {
            0
        }

        // 播放其他avid时更新视频详情
        if (lastPlayedAid != targetAid) {
            videoDetailViewModel.loadVideoDetail(targetAid, includeUserActions = false)
            lastPlayedAid = targetAid
        }

        VideoPlayerV3Activity.actionStart(
            context = context,
            avid = targetAid,
            cid = targetCid,
            title = targetTitle,
            played = playedTime,
            fromSeason = false,
            author = videoDetailState.author
        )
    }

    fun playCurrentVideo(cid: Long? = null) {
        val videoDetailState = uiState.videoDetailState ?: return
        val targetCid = cid ?: videoDetailState.cid

        // 1. 更新播放列表
        videoDetailViewModel.updateVideoList(
            listOf(
                VideoListItem(
                    aid = videoDetailState.aid,
                    cid = targetCid,
                    title = videoDetailState.title,
                )
            )
        )

        // 2. 启动播放器
        performLaunchPlayer(
            targetAid = videoDetailState.aid,
            targetCid = targetCid,
            targetTitle = videoDetailState.title
        )
    }

    LaunchedEffect(Unit) {
        videoDetailViewModel.uiEvent.collect { event ->
            when (event) {
                is VideoDetailUiEffect.ShowToast -> event.message.toast(context)
                is VideoDetailUiEffect.LaunchSeasonInfoActivity -> {
                    SeasonInfoActivity.actionStart(
                        context = context,
                        seasonId = event.seasonId,
                        epId = event.epid
                    )
                    context.finish()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> {
                    event.message.toast(context)
                }
            }
        }
    }

    LaunchedEffect(uiState.loadingState) {
        if (uiState.loadingState == VideoInfoState.Success && !uiState.shouldShowLoading) {
            defaultFocusRequester.requestFocus()
        }
    }


    when {
        uiState.shouldShowLoading -> {
            FullScreenMessage(message = "Loading...")
        }

        uiState.loadingState == VideoInfoState.Error -> {
            FullScreenMessage(message = uiState.errorTip)
        }

        else -> {
            CompositionLocalProvider(
                LocalBringIntoViewSpec provides bringIntoViewSpec
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Column(
                        modifier = modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(top = 16.dp, bottom = 64.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 视频提示
                        if (uiState.videoDetailState?.isUpowerExclusive == true) {
                            ArgueTip(text = stringResource(R.string.video_info_argue_tip_upower_exclusive))
                        }
                        if (containsVerticalScreenVideo) {
                            ArgueTip(text = stringResource(R.string.video_info_argue_tip_vertical_screen))
                        }
                        if (uiState.videoDetailState?.argueTip != null) {
                            ArgueTip(text = uiState.videoDetailState?.argueTip!!)
                        }

                        // 视频信息
                        val videoDetailState = uiState.videoDetailState
                        if (videoDetailState != null) {
                            VideoInfoData(
                                defaultFocusRequester = defaultFocusRequester,
                                videoDetail = videoDetailState,
                                isFollowing = uiState.isFollowingUp,
                                isLoggedIn = uiState.isLoggedIn,
                                isFavorite = videoDetailState.isFavorite,
                                isLiked = videoDetailState.isLiked,
                                isCoined = videoDetailState.isCoined,
                                userFavoriteFolders = uiState.favoriteFolders,
                                favoriteFolderIds = uiState.videoFavoriteFolderIds.toList(),
                                onClickCover = {
                                    // 点击封面播放当前视频
                                    playCurrentVideo(videoDetailState.lastPlayedCid.takeIf { it != 0L })
                                },
                                onClickUp = {
                                    UpInfoActivity.actionStart(
                                        context,
                                        mid = videoDetailState.author.mid,
                                        name = videoDetailState.author.name
                                    )
                                },
                                onAddFollow = {
                                    videoDetailViewModel.setFollow(true)
                                },
                                onDelFollow = {
                                    videoDetailViewModel.setFollow(false)
                                },
                                onAddToDefaultFavoriteFolder = {
                                    videoDetailViewModel.addVideoToDefaultFavoriteFolder()
                                },
                                onUpdateFavoriteFolders = {
                                    videoDetailViewModel.updateVideoFavoriteData(it)
                                },
                                onUpdateLiked = { liked ->
                                    videoDetailViewModel.updateVideoLiked(liked)
                                },
                                onSendVideoCoin = {
                                    videoDetailViewModel.sendVideoCoin()
                                },
                                onSendVideoOneClickTripleAction = {
                                    videoDetailViewModel.sendVideoOneClickTripleAction()
                                }
                            )

                            // 视频描述
                            if ((videoDetailState.description).isNotBlank()) {
                                VideoDescription(
                                    description = videoDetailState.description
                                )
                            }

                            // 视频分P
                            VideoPartRow(
                                pages = videoDetailState.pages,
                                lastPlayedCid = videoDetailState.lastPlayedCid,
                                lastPlayedTime = videoDetailState.lastPlayedTime,
                                enablePartListDialog =
                                (videoDetailState.pages.size > 5),
                                onClick = { cid ->
                                    // 播放当前视频的对应分P
                                    playCurrentVideo(cid)
                                }
                            )

                            // 合集
                            videoDetailState.ugcSeason?.let { season ->
                                season.sections.forEachIndexed { index, section ->
                                    VideoUgcSeasonRow(
                                        title = if (season.sections.size == 1) season.title else section.title,
                                        episodes = section.episodes,
                                        lastPlayedCid = videoDetailState.lastPlayedCid,
                                        lastPlayedTime = videoDetailState.lastPlayedTime,
                                        enableUgcListDialog = section.episodes.size > 5,
                                        onClick = { aid, cid ->

                                            // 1. 读取合集内视频
                                            videoDetailViewModel.updateVideoList(index)

                                            // 2. 解析标题
                                            val currentEpisode =
                                                section.episodes.find { it.cid == cid }
                                            val episodeTitle = currentEpisode?.title ?: ""

                                            // 3. 统一调用
                                            performLaunchPlayer(
                                                targetAid = aid,
                                                targetCid = cid,
                                                targetTitle = episodeTitle
                                            )
                                        }
                                    )
                                }
                            }

                            // 相关视频
                            val relatedVideos = videoDetailState.relatedVideos
                            if (relatedVideos.isNotEmpty()) {
                                VideosRow(
                                    header = stringResource(R.string.video_info_related_video_title),
                                    videos = relatedVideos,
                                    onVideoClicked = { videoData ->
                                        VideoPlayerV3Activity.play(context, videoData.avid)
                                    },
                                    onAddWatchLater = { aid ->
                                        toViewViewModel.addToView(aid)
                                    },
                                    onGoToDetailPage = { aid ->
                                        VideoInfoActivity.showDetail(
                                            context = context,
                                            aid = aid
                                        )
                                    },
                                    onGoToUpPage = { mid, upName ->
                                        UpInfoActivity.actionStart(context, mid, upName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = message
        )
    }
}

@Composable
fun ArgueTip(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp),
        colors = SurfaceDefaults.colors(
            containerColor = SBVColorTokens.warning.copy(alpha = 0.2f),
            contentColor = SBVColorTokens.warning
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_warning_filled),
                contentDescription = null,
                tint = SBVColorTokens.warning
            )
            Text(text = text)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoInfoData(
    modifier: Modifier = Modifier,
    defaultFocusRequester: FocusRequester,
    videoDetail: VideoDetailState,
    isFollowing: Boolean,
    isLoggedIn: Boolean,
    isFavorite: Boolean,
    isLiked: Boolean,
    isCoined: Boolean,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onClickCover: () -> Unit,
    onClickUp: () -> Unit,
    onAddFollow: () -> Unit,
    onDelFollow: () -> Unit,
    onAddToDefaultFavoriteFolder: () -> Unit,
    onUpdateFavoriteFolders: (List<Long>) -> Unit,
    onUpdateLiked: (Boolean) -> Unit,
    onSendVideoCoin: () -> Unit,
    onSendVideoOneClickTripleAction: () -> Unit
) {
    val localDensity = LocalDensity.current
    var heightIs by remember { mutableStateOf(0.dp) }

    Row(
        modifier = modifier
            .padding(horizontal = 50.dp, vertical = 16.dp),
    ) {
        Surface(
            modifier = Modifier
                .focusRequester(defaultFocusRequester)
                .weight(3f)
                .aspectRatio(1.6f)
                .onGloballyPositioned { coordinates ->
                    heightIs = with(localDensity) { coordinates.size.height.toDp() }
                },
            onClick = onClickCover,
            shape = ClickableSurfaceDefaults.shape(
                shape = MaterialTheme.shapes.large,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(
                        width = SBVFocus.borderWidth,
                        color = MaterialTheme.colorScheme.border
                    ),
                    shape = MaterialTheme.shapes.large
                )
            ),
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = videoDetail.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier
                .weight(7f)
                .heightIn(min = heightIs),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = videoDetail.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.labelMedium
                    ) {
                        Text(text = "发布于 ${videoDetail.publishDate.formatPubTimeString()}")
                        Text(text = "·")
                        Text(text = "播放量 ${(videoDetail.stat.view).toWanString()}")
                        Text(text = "·")
                        Text(text = "弹幕 ${(videoDetail.stat.danmaku).toWanString()}")
                        Text(text = "·")
                        Text(text = "点赞 ${videoDetail.stat.like.toWanString()}")
                        Text(text = "·")
                        Text(text = "投币 ${videoDetail.stat.coin.toWanString()}")
                        Text(text = "·")
                        Text(text = "收藏 ${videoDetail.stat.favorite.toWanString()}")
                    }
                }
            }
            UpButton(
                name = videoDetail.author.name,
                followed = isFollowing,
                isLoggedIn = isLoggedIn,
                onClickUp = onClickUp,
                onAddFollow = onAddFollow,
                onDelFollow = onDelFollow
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LikeButton(
                    isLiked = isLiked,
                    onClick = { onUpdateLiked(!isLiked) },
                    onLongClick = { onSendVideoOneClickTripleAction() })
                CoinButton(
                    isCoined = isCoined,
                    onClick = onSendVideoCoin,
                )
                FavoriteButton(
                    isFavorite = isFavorite,
                    userFavoriteFolders = userFavoriteFolders,
                    favoriteFolderIds = favoriteFolderIds,
                    onAddToDefaultFavoriteFolder = onAddToDefaultFavoriteFolder,
                    onUpdateFavoriteFolders = onUpdateFavoriteFolders
                )
            }
        }
    }
}

@Composable
private fun UpButton(
    modifier: Modifier = Modifier,
    name: String,
    followed: Boolean,
    isLoggedIn: Boolean,
    onClickUp: () -> Unit,
    onAddFollow: () -> Unit,
    onDelFollow: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                .focusedBorder(MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable { onClickUp() },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UpIcon(color = MaterialTheme.colorScheme.onSurface)
            Text(text = name, color = MaterialTheme.colorScheme.onSurface)
        }
        AnimatedVisibility(visible = isLoggedIn) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    .focusedBorder(MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clickable { if (followed) onDelFollow() else onAddFollow() }
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (followed) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_done_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.video_info_followed),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_add_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.video_info_follow),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun VideoDescription(
    modifier: Modifier = Modifier,
    description: String
) {
    var hasFocus by remember { mutableStateOf(false) }
    val titleColor = focusedTextColor(hasFocus)
    var showDescriptionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(horizontal = 50.dp),
    ) {
        Text(
            text = stringResource(R.string.video_info_description_title),
            style = MaterialTheme.typography.titleLarge,
            color = titleColor
        )
        Box(
            modifier = Modifier
                .padding(top = 15.dp)
                .onFocusChanged { hasFocus = it.hasFocus }
                .clip(MaterialTheme.shapes.medium)
                .focusedBorder(MaterialTheme.shapes.medium)
                .padding(8.dp)
                .clickable {
                    showDescriptionDialog = true
                }
        ) {
            Text(
                text = description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    VideoDescriptionDialog(
        show = showDescriptionDialog,
        onHideDialog = { showDescriptionDialog = false },
        description = description
    )
}

@Composable
fun VideoDescriptionDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    description: String
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = { onHideDialog() },
            title = {
                Text(
                    text = stringResource(R.string.video_info_description_title),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                LazyColumn {
                    item {
                        Text(text = description)
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun VideoPartButton(
    modifier: Modifier = Modifier,
    title: String,
    duration: Int,
    played: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        onClick = { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(200.dp, 64.dp)
        ) {
            //播放进度覆盖
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    .fillMaxHeight()
                    .fillMaxWidth(if (played < 0) 1f else (played / duration.toFloat()))
            )
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoPartRowButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.size(64.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun VideoPartRow(
    modifier: Modifier = Modifier,
    pages: List<VideoPage>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    enablePartListDialog: Boolean = false,
    onClick: (cid: Long) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showPartListDialog by remember { mutableStateOf(false) }
    val titleColor = focusedTextColor(hasFocus)

    Column(
        modifier = modifier
            .padding(start = 50.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.video_info_part_row_title),
            style = MaterialTheme.typography.titleLarge,
            color = titleColor
        )

        LazyRow(
            modifier = Modifier
                .padding(top = 15.dp)
                .focusRestorer(focusRequester),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (enablePartListDialog) {
                item {
                    VideoPartRowButton(
                        onClick = { showPartListDialog = true }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            painter = painterResource(R.drawable.ic_symbol_view_module_filled),
                            contentDescription = null
                        )
                    }
                }
            }

            val matchedPage = pages.find { it.cid == lastPlayedCid }
            if (matchedPage != null && pages.size > 1) {
                item {
                    // 分P历史播放按钮
                    VideoPartRowButton(
                        onClick = { onClick(matchedPage.cid) }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            painter = painterResource(R.drawable.ic_symbol_history_filled),
                            contentDescription = null
                        )
                    }
                }
            }

            itemsIndexed(items = pages, key = { _, page -> page.cid }) { index, page ->
                VideoPartButton(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                    title = page.title,
                    played = if (page.cid == lastPlayedCid) lastPlayedTime else 0,
                    duration = page.duration,
                    onClick = { onClick(page.cid) }
                )
            }
        }
    }

    PagedVideoGridDialog(
        show = showPartListDialog,
        onHideDialog = { showPartListDialog = false },
        items = pages,
        title = "分 P 列表",
        key = { it.cid }
    ) { itemModifier, page ->
        VideoPartButton(
            modifier = itemModifier,
            title = page.title,
            played = 0,
            duration = page.duration,
            onClick = { onClick(page.cid) }
        )
    }
}

@Composable
fun VideoUgcSeasonRow(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    lastPlayedCid: Long = 0,
    lastPlayedTime: Int = 0,
    enableUgcListDialog: Boolean = false,
    onClick: (avid: Long, cid: Long) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showUgcListDialog by remember { mutableStateOf(false) }
    val titleColor = focusedTextColor(hasFocus)

    Column(
        modifier = modifier
            .padding(start = 50.dp)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = titleColor
        )

        LazyRow(
            modifier = Modifier
                .padding(top = 15.dp)
                .focusRestorer(focusRequester),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (enableUgcListDialog) {
                item {
                    VideoPartRowButton(
                        onClick = { showUgcListDialog = true }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            painter = painterResource(R.drawable.ic_symbol_view_module_filled),
                            contentDescription = null
                        )
                    }
                }
            }

            val matchedEp = episodes.find { it.cid == lastPlayedCid }
            if (matchedEp != null && episodes.size > 1) {
                item {
                    // ugc分季历史播放按钮
                    VideoPartRowButton(
                        onClick = { onClick(matchedEp.aid, matchedEp.cid) }
                    ) {
                        Icon(
                            modifier = Modifier.size(36.dp),
                            painter = painterResource(R.drawable.ic_symbol_history_filled),
                            contentDescription = null
                        )
                    }
                }
            }

            itemsIndexed(
                items = episodes,
                key = { _, episode -> episode.cid },
            ) { index, episode ->
                VideoPartButton(
                    modifier = Modifier
                        .ifElse(index == 0, Modifier.focusRequester(focusRequester)),
                    title = episode.title,
                    played = if (episode.cid == lastPlayedCid) lastPlayedTime else 0,
                    duration = episode.duration,
                    onClick = { onClick(episode.aid, episode.cid) }
                )
            }
        }
    }

    PagedVideoGridDialog(
        show = showUgcListDialog,
        onHideDialog = { showUgcListDialog = false },
        items = episodes,
        title = "合集列表",
        key = { it.cid }
    ) { itemModifier, episode ->
        VideoPartButton(
            modifier = itemModifier,
            title = episode.title,
            played = 0,
            duration = episode.duration,
            onClick = { onClick(episode.aid, episode.cid) }
        )
    }
}

@Composable
private fun <T> PagedVideoGridDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    items: List<T>,
    onHideDialog: () -> Unit,
    key: (T) -> Any,
    itemContent: @Composable (Modifier, T) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember(items) { mutableIntStateOf(0) }
    val tabCount = (items.size + 19) / 20
    val selectedItems = items.drop(selectedTabIndex * 20).take(20)

    val tabRowFocusRequester = remember { FocusRequester() }
    val videoListFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyGridState()

    LaunchedEffect(show) {
        if (show && tabCount > 1) tabRowFocusRequester.requestFocus(scope)
        if (show && tabCount <= 1) videoListFocusRequester.requestFocus(scope)
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            title = { Text(text = title) },
            onDismissRequest = { onHideDialog() },
            confirmButton = {},
            text = {
                Column(
                    modifier = Modifier.size(600.dp, 330.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tabCount > 1) {
                        TabRow(
                            modifier = Modifier
                                .onFocusChanged {
                                    if (it.hasFocus) {
                                        scope.launch(Dispatchers.Main) {
                                            listState.scrollToItem(0)
                                        }
                                    }
                                },
                            selectedTabIndex = selectedTabIndex,
                            separator = { Spacer(modifier = Modifier.width(12.dp)) },
                        ) {
                            for (i in 0 until tabCount) {
                                Tab(
                                    modifier = if (i == 0) Modifier.focusRequester(
                                        tabRowFocusRequester
                                    ) else Modifier,
                                    selected = i == selectedTabIndex,
                                    onFocus = { selectedTabIndex = i },
                                ) {
                                    Text(
                                        text = "P${i * 20 + 1}-${(i + 1) * 20}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = LocalContentColor.current,
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 6.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    TvLazyVerticalGrid(
                        state = listState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = selectedItems,
                            key = { _, item -> key(item) }
                        ) { index, item ->
                            val buttonModifier =
                                if (index == 0) Modifier.focusRequester(videoListFocusRequester) else Modifier
                            itemContent(buttonModifier, item)
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun VideoPartButtonShortTextPreview() {
    SBVTheme {
        VideoPartButton(
            title = "这是一段短文字",
            duration = 100,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun VideoPartButtonLongTextPreview() {
    SBVTheme {
        VideoPartButton(
            title = "这可能是我这辈子距离梅西最近的一次",
            played = 23333,
            duration = 100,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun VideoPartRowPreview() {
    val pages = remember { mutableStateListOf<VideoPage>() }
    for (i in 0..10) {
        pages.add(
            VideoPage(
                cid = 1000L + i,
                index = i,
                title = "这可能是我这辈子距离梅西最近的一次",
                duration = 10,
                dimension = Dimension(0, 0)
            )
        )
    }
    SBVTheme {
        VideoPartRow(pages = pages, onClick = {})
    }
}

@Preview
@Composable
fun VideoDescriptionPreview() {
    SBVTheme {
        VideoDescription(description = "12435678")
    }
}

@Preview
@Composable
private fun UpButtonPreview() {
    var followed by remember { mutableStateOf(false) }
    SBVTheme {
        UpButton(
            name = "12435678",
            followed = followed,
            isLoggedIn = true,
            onClickUp = { followed = !followed },
            onAddFollow = {},
            onDelFollow = {}
        )
    }
}
