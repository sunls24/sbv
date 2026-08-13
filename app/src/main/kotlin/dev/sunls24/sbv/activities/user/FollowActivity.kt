package dev.sunls24.sbv.activities.user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.sunls24.sbv.screen.user.FollowScreen
import dev.sunls24.sbv.ui.theme.SBVTheme

class FollowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SBVTheme {
                FollowScreen()
            }
        }
    }
}
