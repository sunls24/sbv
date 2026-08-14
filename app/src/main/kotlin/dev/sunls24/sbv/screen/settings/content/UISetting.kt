package dev.sunls24.sbv.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import dev.sunls24.sbv.R
import dev.sunls24.sbv.component.HomeTopNavItem
import dev.sunls24.sbv.component.PersonalTopNavItem
import dev.sunls24.sbv.component.settings.SettingListItem
import dev.sunls24.sbv.component.settings.SettingSwitchListItem
import dev.sunls24.sbv.screen.settings.SettingsMenuNavItem
import dev.sunls24.sbv.ui.theme.SBVPageTitle
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.Prefs

@Composable
fun UISetting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var activeDialog by remember { mutableStateOf<UiSettingDialog?>(null) }
    var selectedFirstHomeTopNavItem by remember { mutableStateOf(Prefs.firstHomeTopNavItem) }
    var selectedFirstPersonalTopNavItem by remember { mutableStateOf(Prefs.firstPersonalTopNavItem) }
    var showPersistentSeek by remember { mutableStateOf(Prefs.showPersistentSeek) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = SBVSpacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SBVSpacing.md)
        ) {
            Text(
                text = SettingsMenuNavItem.UI.getDisplayName(context),
                style = SBVPageTitle
            )
            Spacer(modifier = Modifier.height(SBVSpacing.md))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_homepage_title),
                        supportText = stringResource(R.string.settings_ui_homepage_text),
                        onClick = { activeDialog = UiSettingDialog.HomePage }
                    )
                }
                item {
                    SettingListItem(
                        title = stringResource(R.string.settings_ui_personal_page_title),
                        supportText = stringResource(R.string.settings_ui_personal_page_text),
                        onClick = { activeDialog = UiSettingDialog.PersonalPage }
                    )
                }
                item {
                    SettingSwitchListItem(
                        title = stringResource(R.string.settings_ui_show_persistent_seek_title),
                        supportText = stringResource(R.string.settings_ui_show_persistent_seek_text),
                        checked = showPersistentSeek,
                        onCheckedChange = {
                            showPersistentSeek = it
                            Prefs.showPersistentSeek = it
                        }
                    )
                }
            }
        }
    }

    when (activeDialog) {
        UiSettingDialog.HomePage -> OptionDialog(
            options = HomeTopNavItem.entries.toTypedArray(),
            selectedOption = selectedFirstHomeTopNavItem,
            onDismiss = { activeDialog = null },
            onSelect = {
                Prefs.firstHomeTopNavItem = it
                selectedFirstHomeTopNavItem = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
        UiSettingDialog.PersonalPage -> OptionDialog(
            options = PersonalTopNavItem.displayOrder.toTypedArray(),
            selectedOption = selectedFirstPersonalTopNavItem,
            onDismiss = { activeDialog = null },
            onSelect = {
                Prefs.firstPersonalTopNavItem = it
                selectedFirstPersonalTopNavItem = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )
        null -> Unit
    }
}

private enum class UiSettingDialog {
    HomePage,
    PersonalPage
}
