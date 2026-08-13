package dev.sunls24.sbv.activities.video

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.sunls24.sbv.screen.user.UpSpaceScreen
import dev.sunls24.sbv.ui.theme.SBVTheme

class UpInfoActivity : ComponentActivity() {
    companion object {
        fun actionStart(context: Context, mid: Long, name: String) {
            context.startActivity(
                Intent(context, UpInfoActivity::class.java).apply {
                    putExtra("mid", mid)
                    putExtra("name", name)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SBVTheme {
                UpSpaceScreen()
            }
        }
    }
}
