package dev.sunls24.sbv.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import dev.sunls24.sbv.AppInitializationState
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.R
import dev.sunls24.sbv.screen.MainScreen
import dev.sunls24.sbv.ui.theme.SBVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            SBVApp.initializationState.value == AppInitializationState.Loading
        }
        super.onCreate(savedInstanceState)

        setContent {
            SBVTheme {
                val initializationState by SBVApp.initializationState.collectAsState()
                when (val state = initializationState) {
                    AppInitializationState.Loading -> Unit
                    AppInitializationState.Ready -> MainScreen()
                    is AppInitializationState.Error -> InitializationError(
                        message = state.message,
                        onRetry = { (application as SBVApp).retryInitialization() },
                    )
                }
            }
        }
    }
}

@Composable
private fun InitializationError(message: String, onRetry: () -> Unit) {
    val retryFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { retryFocusRequester.requestFocus() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.initialization_failed))
        Text(message)
        Button(
            modifier = Modifier.focusRequester(retryFocusRequester),
            onClick = onRetry,
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}
