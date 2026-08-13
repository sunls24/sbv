package dev.sunls24.sbv.component.settings

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.tv.material3.ListItem
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import dev.sunls24.sbv.ui.theme.SBVTheme

@Composable
fun SettingsMenuSelectItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(text = text) },
        trailingContent = {
            RadioButton(
                modifier = Modifier.focusable(false),
                selected = selected,
                onClick = { },
            )
        },
        onClick = onClick,
        selected = selected
    )
}

private class SettingsMenuSelectItemPreviewParameterProvider :
    PreviewParameterProvider<SettingsMenuSelectItemData> {
    override val values = sequenceOf(
        SettingsMenuSelectItemData(text = "This is a text", selected = false),
        SettingsMenuSelectItemData(text = "This is a text", selected = true),
    )
}

private data class SettingsMenuSelectItemData(
    val text: String,
    val selected: Boolean
)

@Preview
@Composable
private fun SettingsMenuSelectItemPreview(
    @PreviewParameter(SettingsMenuSelectItemPreviewParameterProvider::class) data: SettingsMenuSelectItemData
) {
    SBVTheme {
        SettingsMenuSelectItem(
            text = data.text,
            selected = data.selected,
            onClick = {}
        )
    }
}
