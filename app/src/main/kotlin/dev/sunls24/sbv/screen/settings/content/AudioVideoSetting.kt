package dev.sunls24.sbv.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Text
import dev.sunls24.sbv.R
import dev.sunls24.sbv.component.settings.SettingListItem
import dev.sunls24.sbv.component.settings.SettingSwitchListItem
import dev.sunls24.sbv.entity.Audio
import dev.sunls24.sbv.entity.PlaybackEndAction
import dev.sunls24.sbv.entity.PlaybackSpeed
import dev.sunls24.sbv.entity.Resolution
import dev.sunls24.sbv.entity.VideoCodec
import dev.sunls24.sbv.screen.settings.SettingsMenuNavItem
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.ui.theme.SBVPageTitle
import dev.sunls24.sbv.ui.theme.SBVSpacing

@Composable
fun AudioVideoSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var dialog by remember { mutableStateOf<AudioVideoSettingDialog?>(null) }

    var selectedResolution by remember { mutableStateOf(Prefs.defaultQuality) }
    var selectedVideoCodec by remember { mutableStateOf(Prefs.defaultVideoCodec) }
    var selectedAudioCodec by remember { mutableStateOf(Prefs.defaultAudio) }
    var selectedPlaySpeed by remember { mutableStateOf(Prefs.defaultPlaySpeed) }
    var selectedActionAfterPlay by remember { mutableStateOf(Prefs.actionAfterPlay) }

    var enableSoftwareVideoRenderer by remember { mutableStateOf(Prefs.enableSoftwareVideoDecoder) }
    var enableIncognitoMode by remember { mutableStateOf(Prefs.incognitoMode) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = SBVSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SBVSpacing.md)
    ) {
        Text(
            text = SettingsMenuNavItem.AudioVideo.getDisplayName(context),
            style = SBVPageTitle
        )
        Spacer(modifier = Modifier.height(SBVSpacing.md))
        SettingListItem(
            title = "默认分辨率",
            supportText = "当前：${selectedResolution.getDisplayName(context)}",
            onClick = { dialog = AudioVideoSettingDialog.Resolution }
        )
        SettingListItem(
            title = "默认视频编码",
            supportText = "当前：${selectedVideoCodec.getDisplayName(context)}",
            onClick = { dialog = AudioVideoSettingDialog.VideoCodec }
        )
        SettingListItem(
            title = "默认音频轨道",
            supportText = "当前：${selectedAudioCodec.getDisplayName(context)}",
            onClick = { dialog = AudioVideoSettingDialog.AudioCodec }
        )
        SettingListItem(
            title = "默认播放速度",
            supportText = "当前：${selectedPlaySpeed.getDisplayName(context)}",
            onClick = { dialog = AudioVideoSettingDialog.PlaySpeed }
        )
        SettingListItem(
            title = "播放结束动作",
            supportText = "当前：${selectedActionAfterPlay.displayName}",
            onClick = { dialog = AudioVideoSettingDialog.ActionAfterPlay }
        )
        SettingSwitchListItem(
            title = stringResource(R.string.settings_media_software_video_renderer_title),
            supportText = stringResource(R.string.settings_media_software_video_renderer_text),
            checked = enableSoftwareVideoRenderer,
            onCheckedChange = {
                enableSoftwareVideoRenderer = it
                Prefs.enableSoftwareVideoDecoder = it
            }
        )
        SettingSwitchListItem(
            title = "隐身播放",
            supportText = "不会向 Bilibili 上传播放进度",
            checked = enableIncognitoMode,
            onCheckedChange = {
                enableIncognitoMode = it
                Prefs.incognitoMode = it
            }
        )
    }
    when (dialog) {
        AudioVideoSettingDialog.Resolution -> OptionDialog(
            options = Resolution.entries.toTypedArray(),
            selectedOption = selectedResolution,
            onDismiss = { dialog = null },
            onSelect = {
                Prefs.defaultQuality = it
                selectedResolution = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )

        AudioVideoSettingDialog.VideoCodec -> OptionDialog(
            options = VideoCodec.entries.toTypedArray(),
            selectedOption = selectedVideoCodec,
            onDismiss = { dialog = null },
            onSelect = {
                Prefs.defaultVideoCodec = it
                selectedVideoCodec = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )

        AudioVideoSettingDialog.AudioCodec -> OptionDialog(
            options = Audio.entries.toTypedArray(),
            selectedOption = selectedAudioCodec,
            onDismiss = { dialog = null },
            onSelect = {
                Prefs.defaultAudio = it
                selectedAudioCodec = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )

        AudioVideoSettingDialog.PlaySpeed -> OptionDialog(
            options = PlaybackSpeed.entries.toTypedArray(),
            selectedOption = selectedPlaySpeed,
            onDismiss = { dialog = null },
            onSelect = {
                Prefs.defaultPlaySpeed = it
                selectedPlaySpeed = it
            },
            getDisplayName = { it.getDisplayName(context) }
        )

        AudioVideoSettingDialog.ActionAfterPlay -> OptionDialog(
            options = PlaybackEndAction.entries.toTypedArray(),
            selectedOption = selectedActionAfterPlay,
            onDismiss = { dialog = null },
            onSelect = {
                Prefs.actionAfterPlay = it
                selectedActionAfterPlay = it
            },
            getDisplayName = { it.displayName }
        )

        null -> Unit
    }
}

private enum class AudioVideoSettingDialog {
    Resolution,
    VideoCodec,
    AudioCodec,
    PlaySpeed,
    ActionAfterPlay
}
