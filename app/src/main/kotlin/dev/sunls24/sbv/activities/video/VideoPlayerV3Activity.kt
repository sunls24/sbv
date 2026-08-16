package dev.sunls24.sbv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.sunls24.biliapi.entity.user.Author
import dev.sunls24.sbv.screen.VideoPlayerV3Screen
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.viewmodel.player.VideoPlayerV3ViewModel
import dev.sunls24.sbv.viewmodel.player.DirectPlaybackInitResult
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideoPlayerV3Activity : ComponentActivity() {
    private val playerViewModel: VideoPlayerV3ViewModel by viewModel()

    companion object {
        private var currentInstance: VideoPlayerV3Activity? = null

        fun play(context: Context, aid: Long, epid: Int? = null) {
            if (epid != null) {
                SeasonInfoActivity.actionStart(context = context, epId = epid)
                return
            }
            currentInstance?.finish()
            context.startActivity(
                Intent(context, VideoPlayerV3Activity::class.java).apply {
                    putExtra("direct_aid", aid)
                }
            )
        }

        fun actionStart(
            context: Context,
            avid: Long,
            cid: Long,
            title: String,
            played: Int,
            fromSeason: Boolean,
            subType: Int? = null,
            epid: Int? = null,
            seasonId: Int? = null,
            author: Author? = null
        ) {
            currentInstance?.finish()
            context.startActivity(
                Intent(context, VideoPlayerV3Activity::class.java).apply {
                    putExtra("avid", avid)
                    putExtra("cid", cid)
                    putExtra("title", title)
                    putExtra("played", played)
                    putExtra("fromSeason", fromSeason)
                    putExtra("subType", subType)
                    putExtra("epid", epid)
                    putExtra("seasonId", seasonId)
                    putExtra("author_mid", author?.mid)
                    putExtra("author_name", author?.name)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentInstance = this

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val directAid = intent.getLongExtra("direct_aid", 0L)
        setContent {
            SBVTheme {
                VideoPlayerV3Screen(
                    onRetryInitialization = {
                        retryDirectPlayback(directAid)
                    }
                )
            }
        }

        if (directAid != 0L) {
            if (playerViewModel.isInitialized) {
                startPlayer()
            } else {
                initializeDirectPlayback(directAid)
            }
        } else if (initViewModelFromIntent()) {
            startPlayer()
        }
    }

    override fun onResume() {
        super.onResume()

        // 视频全屏播放，隐藏状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        super.onPause()

        // 恢复状态栏
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())

        if (!isChangingConfigurations) {
            playerViewModel.videoPlayer?.pause()
            playerViewModel.danmakuPlayer?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance === this) {
            currentInstance = null
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isFinishing) {
            playerViewModel.detachPlayer()
            playerViewModel.releaseDanmakuPlayer()
        }
    }

    private fun initViewModelFromIntent(): Boolean {
        if (intent.hasExtra("avid")) {
            val aid = intent.getLongExtra("avid", 170001)
            val cid = intent.getLongExtra("cid", 170001)
            val title = intent.getStringExtra("title") ?: "Unknown Title"
            val played = intent.getIntExtra("played", 0)
            val fromSeason = intent.getBooleanExtra("fromSeason", false)
            val subType = intent.getIntExtra("subType", 0)
            val epid = intent.getIntExtra("epid", 0)
            val seasonId = intent.getIntExtra("seasonId", 0)
            val author_mid = intent.getLongExtra("author_mid", 0)
            val author_name = intent.getStringExtra("author_name")

            return playerViewModel.init(
                aid = aid,
                cid = cid,
                epid = epid.takeIf { it != 0 },
                title = title,
                lastPlayed = played,
                fromSeason = fromSeason,
                subType = subType,
                seasonId = seasonId,
                authorMid = author_mid,
                authorName = author_name ?: ""
            )
        } else {
            return false
        }
    }

    private fun retryDirectPlayback(directAid: Long) {
        if (directAid == 0L || !playerViewModel.beginInitializationRetry()) return
        initializeDirectPlayback(directAid)
    }

    private fun initializeDirectPlayback(directAid: Long) {
        lifecycleScope.launch {
            when (val result = playerViewModel.initDirectPlayback(directAid)) {
                DirectPlaybackInitResult.Ready -> startPlayer()
                is DirectPlaybackInitResult.RedirectToSeason -> {
                    SeasonInfoActivity.actionStart(
                        context = this@VideoPlayerV3Activity,
                        epId = result.epid
                    )
                    finish()
                }
                is DirectPlaybackInitResult.Failure -> {
                    playerViewModel.showInitializationError(result.message)
                }
            }
        }
    }

    private fun startPlayer() {
        if (playerViewModel.videoPlayer != null) return
        playerViewModel.initVideoPlayer(applicationContext)
        playerViewModel.initDanmakuPlayer()
        playerViewModel.loadVideoWithResources()
    }
}
