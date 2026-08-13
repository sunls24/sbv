package dev.sunls24.sbv.screen.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.sbv.R
import dev.sunls24.sbv.activities.video.UpInfoActivity
import dev.sunls24.sbv.component.LoadingTip
import dev.sunls24.sbv.component.TvLazyVerticalGrid
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.util.requestFocus
import dev.sunls24.sbv.viewmodel.user.FollowViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FollowScreen(
    modifier: Modifier = Modifier,
    followViewModel: FollowViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultFocusRequester = remember { FocusRequester() }

    LaunchedEffect(followViewModel.followedUsers.isNotEmpty(), followViewModel.loadFailed) {
        val shouldFocusFirstItem = followViewModel.followedUsers.isNotEmpty() &&
                !followViewModel.loadFailed
        val shouldFocusRetry = followViewModel.followedUsers.isEmpty() &&
                followViewModel.loadFailed
        if (shouldFocusFirstItem || shouldFocusRetry) {
            defaultFocusRequester.requestFocus(scope)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(
                    start = 48.dp,
                    top = 24.dp,
                    bottom = 8.dp,
                    end = 48.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.user_homepage_follow),
                        fontSize = 24.sp
                    )
                    Text(
                        text = stringResource(
                            R.string.load_data_count,
                            followViewModel.followedUsers.size
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        TvLazyVerticalGrid(
            modifier = Modifier.padding(innerPadding),
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (followViewModel.followedUsers.isNotEmpty()) {
                itemsIndexed(items = followViewModel.followedUsers) { index, up ->
                    val upCardModifier =
                        if (index == 0) Modifier.focusRequester(defaultFocusRequester) else Modifier
                    UpCard(
                        modifier = upCardModifier,
                        face = up.avatar,
                        sign = up.sign,
                        username = up.name,
                        onFocusChange = {
                            if (it && index >= followViewModel.followedUsers.size - 10) {
                                followViewModel.loadMore()
                            }
                        },
                        onClick = {
                            UpInfoActivity.actionStart(
                                context = context,
                                mid = up.mid,
                                name = up.name
                            )
                        }
                    )
                }
                if (followViewModel.updating) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingTip()
                        }
                    }
                } else if (followViewModel.loadFailed) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            onClick = followViewModel::loadMore
                        ) {
                            EmptyTip(text = stringResource(R.string.load_failed_retry))
                        }
                    }
                }
            } else if (followViewModel.updating) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingTip()
                    }
                }
            } else if (followViewModel.loadFailed) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        modifier = Modifier.focusRequester(defaultFocusRequester),
                        onClick = followViewModel::loadMore
                    ) {
                        EmptyTip(text = stringResource(R.string.load_failed_retry))
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyTip()
                }
            }
        }
    }
}

@Composable
fun UpCard(
    modifier: Modifier = Modifier,
    face: String,
    sign: String,
    username: String,
    onFocusChange: (hasFocus: Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .size(280.dp, 80.dp),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            pressedContainerColor = MaterialTheme.colorScheme.surface
        ),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.large),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .padding(start = 12.dp, end = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape),
                color = Color.White
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    model = face,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
            }
            Column {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
fun UpCardPreview() {
    SBVTheme {
        UpCard(
            face = "",
            sign = "一只业余做翻译的Klei迷，动态区UP（自称），缺氧官中反馈可私信",
            username = "username",
            onFocusChange = {},
            onClick = {}
        )
    }
}
