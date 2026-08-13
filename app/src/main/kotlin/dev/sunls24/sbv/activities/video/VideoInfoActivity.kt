package dev.sunls24.sbv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.sunls24.sbv.screen.VideoInfoScreen
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.viewmodel.video.VideoDetailViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class VideoInfoActivity : ComponentActivity() {
    companion object {
        fun showDetail(
            context: Context,
            aid: Long,
            epid: Int? = null
        ) {
            if (epid != null) {
                SeasonInfoActivity.actionStart(
                    context = context,
                    epId = epid
                )
                return
            }

            context.startActivity(
                Intent(context, VideoInfoActivity::class.java).apply {
                    putExtra("aid", aid)
                }
            )
        }
    }

    private val videoDetailViewModel: VideoDetailViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SBVTheme {
                VideoInfoScreen()
            }
        }
        getParamsFromIntent()
    }

    private fun getParamsFromIntent() {
        if (intent.hasExtra("aid")) {
            val aid = intent.getLongExtra("aid", 170001)
            videoDetailViewModel.init(aid)
        }
    }
}
