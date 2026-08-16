package dev.sunls24.sbv.screen.user

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.biliapi.entity.user.UpProfile
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.video.VideoInfoActivity
import dev.sunls24.sbv.activities.video.VideoPlayerV3Activity
import dev.sunls24.sbv.component.LazyGridLoadMoreEffect
import dev.sunls24.sbv.component.LoadingTip
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.component.videocard.SmallVideoCard
import dev.sunls24.sbv.ui.effect.UiEffect
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.ImageSize
import dev.sunls24.sbv.util.firstRowActionFocus
import dev.sunls24.sbv.util.resizedImageUrl
import dev.sunls24.sbv.util.toWanString
import dev.sunls24.sbv.util.toast
import dev.sunls24.sbv.viewmodel.user.ToViewViewModel
import dev.sunls24.sbv.viewmodel.user.UpInfoViewModel
import org.koin.androidx.compose.koinViewModel

private const val UpSpaceColumnCount = 4

@Composable
fun UpSpaceScreen(
    modifier: Modifier = Modifier,
    upInfoViewModel: UpInfoViewModel = koinViewModel(),
    toViewViewModel: ToViewViewModel = koinViewModel()
) {
    val gridState = rememberLazyGridState()
    val firstVideoFocusRequester = remember { FocusRequester() }
    val followFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val showFollowButton = upInfoViewModel.isLoggedIn && !upInfoViewModel.isSelf

    LaunchedEffect(Unit) {
        val intent = (context as Activity).intent
        if (intent.hasExtra("mid")) {
            upInfoViewModel.init(
                mid = intent.getLongExtra("mid", 0),
                fallbackName = intent.getStringExtra("name").orEmpty()
            )
        } else {
            context.finish()
        }
    }

    LaunchedEffect(Unit) {
        upInfoViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> event.message.toast(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        toViewViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEffect.ShowToast -> event.message.toast(context)
            }
        }
    }

    LazyGridLoadMoreEffect(
        gridState = gridState,
        itemCount = upInfoViewModel.spaceVideos.size,
        contentKey = upInfoViewModel.upMid,
        onLoadMore = upInfoViewModel::update
    )

    TvLazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(UpSpaceColumnCount),
        state = gridState,
        contentPadding = PaddingValues(
            horizontal = SBVSpacing.xxxl,
            vertical = SBVSpacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(SBVSpacing.xl),
        horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
    ) {
        item(key = "profile", span = { GridItemSpan(maxLineSpan) }) {
            UpProfileHeader(
                fallbackName = upInfoViewModel.upName,
                profile = upInfoViewModel.profile,
                profileLoading = upInfoViewModel.profileLoading,
                profileLoadFailed = upInfoViewModel.profileLoadFailed,
                showFollowButton = showFollowButton,
                isFollowing = upInfoViewModel.isFollowing,
                relationLoading = upInfoViewModel.relationLoading,
                focusRequester = followFocusRequester,
                downFocusRequester = when {
                    upInfoViewModel.spaceVideos.isNotEmpty() -> firstVideoFocusRequester
                    upInfoViewModel.videoLoadFailed -> retryFocusRequester
                    else -> null
                },
                onSetFollowing = upInfoViewModel::setFollowing
            )
        }

        item(key = "video-header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.up_homepage_videos),
                    style = MaterialTheme.typography.titleLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SBVSpacing.sm)) {
                    Text(
                        text = stringResource(
                            R.string.up_homepage_loaded_count,
                            upInfoViewModel.spaceVideos.size
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedVisibility(
                        visible = upInfoViewModel.noMore &&
                            upInfoViewModel.initialVideoLoadFinished
                    ) {
                        Text(
                            text = stringResource(R.string.load_data_no_more),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (upInfoViewModel.spaceVideos.isNotEmpty()) {
            itemsIndexed(
                items = upInfoViewModel.spaceVideos,
                key = { _, video -> video.avid }
            ) { index, video ->
                Box(contentAlignment = Alignment.Center) {
                    SmallVideoCard(
                        focusModifier = Modifier
                            .then(
                                if (index == 0) {
                                    Modifier.focusRequester(firstVideoFocusRequester)
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (showFollowButton && !upInfoViewModel.relationLoading) {
                                    Modifier.firstRowActionFocus(
                                        index = index,
                                        columns = UpSpaceColumnCount,
                                        focusRequester = followFocusRequester
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        data = video,
                        onClick = {
                            VideoPlayerV3Activity.play(context = context, aid = video.avid)
                        },
                        onAddWatchLater = {
                            toViewViewModel.addToView(video.avid)
                        },
                        onGoToDetailPage = {
                            VideoInfoActivity.showDetail(context = context, aid = video.avid)
                        },
                    )
                }
            }

            if (upInfoViewModel.videoLoading) {
                item(key = "load-more", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SBVSpacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            }
        } else {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                when {
                    upInfoViewModel.videoLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SBVSpacing.xxxl),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingTip()
                        }
                    }
                    upInfoViewModel.videoLoadFailed -> {
                        Surface(
                            modifier = Modifier.focusRequester(retryFocusRequester),
                            onClick = upInfoViewModel::retryVideos
                        ) {
                            EmptyTip(text = stringResource(R.string.load_failed_retry))
                        }
                    }
                    upInfoViewModel.initialVideoLoadFinished -> {
                        EmptyTip(text = stringResource(R.string.up_homepage_no_videos))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpProfileHeader(
    fallbackName: String,
    profile: UpProfile?,
    profileLoading: Boolean,
    profileLoadFailed: Boolean,
    showFollowButton: Boolean,
    isFollowing: Boolean?,
    relationLoading: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    onSetFollowing: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .padding(SBVSpacing.xl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = profile?.avatar
                    ?.takeIf(String::isNotBlank)
                    ?.resizedImageUrl(ImageSize.Avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(SBVSpacing.xl))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SBVSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SBVSpacing.sm)
            ) {
                Text(
                    text = profile?.name
                        ?.takeIf(String::isNotBlank)
                        ?: fallbackName.ifBlank {
                            stringResource(R.string.up_homepage_unknown_user)
                        },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                profile?.level?.let {
                    Text(
                        text = "LV$it",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                profile?.officialTitle?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            when {
                profile != null -> {
                    Text(
                        text = profile.sign.ifBlank {
                            stringResource(R.string.up_homepage_no_sign)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    UpProfileStats(profile)
                }
                profileLoading -> {
                    Text(
                        text = stringResource(R.string.up_homepage_profile_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                profileLoadFailed -> {
                    Text(
                        text = stringResource(R.string.up_homepage_profile_failed),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showFollowButton) {
            Spacer(modifier = Modifier.width(SBVSpacing.xl))
            Button(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .then(
                        downFocusRequester?.let { requester ->
                            Modifier.focusProperties { down = requester }
                        } ?: Modifier
                    ),
                enabled = !relationLoading,
                onClick = { onSetFollowing(isFollowing != true) }
            ) {
                when {
                    relationLoading -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    isFollowing == true -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_symbol_done_filled),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(SBVSpacing.xs))
                        Text(text = stringResource(R.string.video_info_followed))
                    }
                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_symbol_add_filled),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(SBVSpacing.xs))
                        Text(text = stringResource(R.string.video_info_follow))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpProfileStats(profile: UpProfile) {
    Row(horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)) {
        UpProfileStat(
            label = stringResource(R.string.up_homepage_following),
            value = profile.followingCount.toWanString()
        )
        UpProfileStat(
            label = stringResource(R.string.up_homepage_followers),
            value = profile.followerCount.toWanString()
        )
        UpProfileStat(
            label = stringResource(R.string.up_homepage_likes),
            value = profile.likeCount.toWanString()
        )
        UpProfileStat(
            label = stringResource(R.string.up_homepage_archives),
            value = profile.archiveCount.toWanString()
        )
    }
}

@Composable
private fun UpProfileStat(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xs)) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            modifier = Modifier.alignByBaseline(),
            text = value,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
