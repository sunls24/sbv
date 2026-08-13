package dev.sunls24.sbv.component.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import dev.sunls24.sbv.ui.theme.SBVTheme

@Composable
fun SettingSwitchListItem(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = modifier
            .padding(horizontal = 12.dp),
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = supportText) },
        trailingContent = {
            Switch(
                modifier = Modifier.focusable(false),
                checked = checked,
                onCheckedChange = null,
            )
        },
        onClick = { onCheckedChange(!checked) },
        selected = false
    )
}

@Preview
@Composable
fun SettingSwitchListItemFocusedAndEnabledPreview() {
    SBVTheme {
        SettingSwitchListItem(
            title = "This is a title",
            supportText = "This is a support text",
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview
@Composable
fun SettingSwitchListItemFocusedAndDisabledPreview() {
    SBVTheme {
        SettingSwitchListItem(
            title = "This is a title",
            supportText = "This is a support text",
            checked = false,
            onCheckedChange = {}
        )
    }
}

@Preview
@Composable
fun SettingSwitchListItemNotFocusedAndEnabledPreview() {
    SBVTheme {
        SettingSwitchListItem(
            title = "This is a title",
            supportText = "This is a support text",
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview
@Composable
fun SettingSwitchListItemNotFocusedAndDisabledPreview() {
    SBVTheme {
        SettingSwitchListItem(
            title = "This is a title",
            supportText = "This is a support text",
            checked = false,
            onCheckedChange = {}
        )
    }
}
