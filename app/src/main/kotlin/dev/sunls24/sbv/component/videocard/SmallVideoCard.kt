package dev.sunls24.sbv.component.videocard

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.sbv.R
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.UpIcon
import dev.sunls24.sbv.entity.carddata.VideoCardData
import dev.sunls24.sbv.ui.theme.SBVFocus
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.ImageSize
import dev.sunls24.sbv.util.resizedImageUrl

private const val TitleMarqueeFocusDelayMillis = 250L


@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    focusModifier: Modifier = Modifier,
    data: VideoCardData,
    delToView: Boolean = false,
    compactActions: Boolean = false,
    onClick: () -> Unit,
    onAddWatchLater: (() -> Unit)? = null,
    onGoToDetailPage: (() -> Unit)? = null,
    onGoToUpPage: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
) {
    var showActions by remember { mutableStateOf(false) }
    var cardHasFocus by remember { mutableStateOf(false) }
    var releaseLongPress by remember { mutableStateOf(false) }
    val firstButtonRequester = remember { FocusRequester() }
    val actionButtonSpacing = if (compactActions) SBVSpacing.xs else SBVSpacing.sm

    // 判断是否有任何操作按钮
    val hasAnyAction = onAddWatchLater != null || onGoToDetailPage != null || onGoToUpPage != null

    LaunchedEffect(showActions) {
        if (showActions && hasAnyAction) {
            firstButtonRequester.requestFocus()
        } else if (!showActions) {
            releaseLongPress = false
        }
    }

    fun runAction(isFirstAction: Boolean, action: () -> Unit) {
        // 长按打开操作区后焦点会落到首项，先吞掉同一次按键的释放事件。
        if (isFirstAction && !releaseLongPress) {
            releaseLongPress = true
            return
        }
        action()
    }

    fun firstActionModifier(isFirstAction: Boolean): Modifier =
        if (isFirstAction) Modifier.focusRequester(firstButtonRequester) else Modifier

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            onClick = { if (!showActions) onClick() },
            onLongClick = {
                if (hasAnyAction) showActions = true

            },
            modifier = focusModifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .onFocusChanged { focusState ->
                    cardHasFocus = focusState.hasFocus
                    if (!focusState.hasFocus) showActions = false
                },
            shape = CardDefaults.shape(MaterialTheme.shapes.large),
            scale = CardDefaults.scale(focusedScale = SBVFocus.focusedScale),
        ) {
            if (showActions) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = SBVSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = actionButtonSpacing,
                        alignment = Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onAddWatchLater?.let { action ->
                        val isFirstAction = true
                        VideoCardActionButton(
                            onClick = { runAction(isFirstAction, action) },
                            modifier = actionModifier.then(firstActionModifier(isFirstAction)),
                            compact = compactActions,
                            icon = if (delToView) R.drawable.remove_from_list else R.drawable.add_to_list,
                            label = stringResource(
                                if (delToView) {
                                    R.string.video_card_action_remove_watch_later
                                } else {
                                    R.string.video_card_action_watch_later
                                }
                            ),
                        )
                    }

                    onGoToDetailPage?.let { action ->
                        val isFirstAction = onAddWatchLater == null
                        VideoCardActionButton(
                            onClick = { runAction(isFirstAction, action) },
                            modifier = actionModifier.then(firstActionModifier(isFirstAction)),
                            compact = compactActions,
                            icon = R.drawable.info_24px,
                            label = stringResource(R.string.video_card_action_detail),
                        )
                    }

                    onGoToUpPage?.let { action ->
                        val isFirstAction = onAddWatchLater == null && onGoToDetailPage == null
                        VideoCardActionButton(
                            onClick = { runAction(isFirstAction, action) },
                            modifier = actionModifier.then(firstActionModifier(isFirstAction)),
                            compact = compactActions,
                            icon = R.drawable.ic_up,
                            label = stringResource(R.string.video_card_action_up_page),
                        )
                    }
                }
            } else {
                CardCover(
                    cover = data.cover,
                    play = data.playString,
                    danmaku = data.danmakuString,
                    time = data.timeString
                )
            }
        }

        CardInfo(
            modifier = Modifier.fillMaxWidth(),
            title = data.title,
            upName = data.upName,
            pubTime = data.pubTime,
            isFocused = cardHasFocus && !showActions,
        )
    }
}

@Composable
private fun VideoCardActionButton(
    modifier: Modifier = Modifier,
    compact: Boolean,
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    val width = if (compact) 60.dp else 72.dp
    val height = if (compact) 52.dp else 60.dp
    val iconSize = if (compact) 26.dp else 28.dp
    val labelStyle = if (compact) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelMedium
    }

    Surface(
        modifier = modifier
            .width(width)
            .height(height),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        ),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = 2.dp,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(icon),
                contentDescription = label,
                tint = LocalContentColor.current,
            )
            Text(
                text = label,
                style = labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Composable
fun CardCover(
    modifier: Modifier = Modifier,
    cover: String,
    play: String,
    danmaku: String,
    time: String
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large),
            model = cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // 播放数、弹幕数、时间
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (play.isNotBlank()) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_play_count),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = play,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(12.dp))
            }
            if (danmaku.isNotBlank()) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(id = R.drawable.ic_danmaku_count),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = danmaku,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = time,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CardInfo(
    modifier: Modifier = Modifier,
    title: String,
    upName: String,
    pubTime: String?,
    isFocused: Boolean = false,
) {
    var marqueeEnabled by remember(title) { mutableStateOf(false) }

    LaunchedEffect(isFocused, title) {
        marqueeEnabled = false
        if (isFocused) {
            kotlinx.coroutines.delay(TitleMarqueeFocusDelayMillis)
            marqueeEnabled = true
        }
    }

    val showMarquee = isFocused && marqueeEnabled

    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = if (showMarquee) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (showMarquee) {
                        Modifier.basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 0,
                        )
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UpIcon(
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = 1.dp)
            )
            Text(
                modifier = Modifier.weight(1f),
                text = upName,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pubTime ?: "",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Preview
@Composable
fun SmallVideoCardWithoutFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        cid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        playString = "2333",
        danmakuString = "666",
        timeString = "2333",
        pubTime = "1小时前"
    )
    SBVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCard(
                modifier = Modifier.padding(20.dp),
                onClick = {},
                data = data,
            )
        }
    }
}

@Preview
@Composable
fun SmallVideoCardWithFocusPreview() {
    val data = VideoCardData(
        avid = 0,
        cid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        upName = "bishi",
        playString = "2333",
        danmakuString = "666",
        timeString = "2333",
        pubTime = "1小时前"
    )
    SBVTheme {
        Surface(
            modifier = Modifier.width(300.dp)
        ) {
            SmallVideoCard(
                modifier = Modifier.padding(20.dp),
                onClick = {},
                data = data,
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
fun SmallVideoCardsPreview() {
    val data = VideoCardData(
        avid = 0,
        cid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        //cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        cover = "",
        upName = "bishi",
        playString = "2333",
        danmakuString = "666",
        timeString = "2333",
        pubTime = "1小时前"
    )
    SBVTheme {
        TvLazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(20) {
                item {
                    SmallVideoCard(
                        onClick = {},
                        data = data
                    )
                }
            }
        }
    }
}
