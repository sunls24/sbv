package dev.sunls24.sbv.activities.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.sunls24.sbv.screen.settings.SettingsScreen
import dev.sunls24.sbv.ui.theme.SBVTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SBVTheme {
                SettingsScreen()
            }
        }
    }
}
