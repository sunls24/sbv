package dev.sunls24.sbv.player

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun SBVVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: SBVPlayer?,
) {
    if (videoPlayer != null) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = videoPlayer.player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    useController = false
                }
            },
            update = { playerView ->
                playerView.player = videoPlayer.player
            },
            onRelease = { playerView ->
                playerView.player = null
            }
        )
    }
}
