package dev.sunls24.sbv.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.sbv.BuildConfig
import dev.sunls24.sbv.R
import dev.sunls24.sbv.screen.settings.content.AudioVideoSetting
import dev.sunls24.sbv.screen.settings.content.UISetting
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.requestFocus

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var currentMenu by remember { mutableStateOf(SettingsMenuNavItem.AudioVideo) }
    var focusInNav by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(
                    start = SBVSpacing.xxxl,
                    top = SBVSpacing.xl,
                    bottom = SBVSpacing.sm,
                    end = SBVSpacing.xxxl
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.title_activity_settings),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_version_current_version,
                            BuildConfig.VERSION_NAME
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier.padding(innerPadding)
        ) {
            SettingsNav(
                modifier = Modifier
                    .onFocusChanged { focusInNav = it.hasFocus }
                    .weight(3f)
                    .fillMaxHeight(),
                currentMenu = currentMenu,
                onMenuChanged = { currentMenu = it },
                isFocusing = focusInNav
            )
            SettingContent(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxSize(),
                onBackNav = { focusInNav = true },
                currentMenu = currentMenu
            )
        }
    }
}

@Composable
fun SettingsNav(
    modifier: Modifier = Modifier,
    currentMenu: SettingsMenuNavItem,
    onMenuChanged: (SettingsMenuNavItem) -> Unit,
    isFocusing: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocusing) {
        if (isFocusing) focusRequester.requestFocus(scope)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(SBVSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (item in SettingsMenuNavItem.entries) {
            val buttonModifier = if (currentMenu == item) Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
            else Modifier.fillMaxWidth()
            item {
                SettingsMenuButton(
                    modifier = buttonModifier,
                    text = item.getDisplayName(context),
                    selected = currentMenu == item,
                    onFocus = {
                        onMenuChanged(item)
                    }
                )
            }
        }
    }
}

enum class SettingsMenuNavItem(private val strRes: Int) {
    AudioVideo(R.string.settings_item_audio_video_settings),
    UI(R.string.settings_item_ui);

    fun getDisplayName(context: Context) = context.getString(strRes)
}

@Composable
fun SettingContent(
    modifier: Modifier = Modifier,
    onBackNav: () -> Unit,
    currentMenu: SettingsMenuNavItem
) {
    Box(
        modifier = modifier
            .padding(SBVSpacing.xl)
    ) {
        SettingsDetail(
            modifier = Modifier.fillMaxSize(),
            onFocusBackMenuList = {
                onBackNav()
            }
        ) {
            when (currentMenu) {
                SettingsMenuNavItem.AudioVideo -> AudioVideoSetting()
                SettingsMenuNavItem.UI -> UISetting()
            }
        }
    }
}

@Composable
fun SettingsMenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onFocus: () -> Unit,
    onLoseFocus: () -> Unit = {},
    onClick: () -> Unit = {},
    selected: Boolean
) {
    ListItem(
        modifier = modifier
            .onFocusChanged { if (it.hasFocus) onFocus() else onLoseFocus() },
        selected = selected,
        onClick = onClick,
        headlineContent = {
            Text(
                modifier = Modifier.padding(
                    horizontal = 16.dp
                ),
                text = text,
                style = MaterialTheme.typography.titleLarge
            )
        }
    )
}

@Preview
@Composable
fun SettingsMenuButtonPreview() {
    SBVTheme {
        Box(
            modifier = Modifier.size(200.dp, 100.dp)
        ) {
            SettingsMenuButton(
                modifier = Modifier.align(Alignment.Center),
                text = "This is button",
                selected = true,
                onFocus = {}
            )
        }
    }
}

@Composable
fun SettingsDetail(
    modifier: Modifier = Modifier,
    onFocusBackMenuList: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent {
                val result = it.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT
                if (result) onFocusBackMenuList()
                result
            }
    ) {
        content()
    }
}
