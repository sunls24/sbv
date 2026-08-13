package dev.sunls24.sbv.component.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text

@Composable
fun SettingListItem(
    modifier: Modifier = Modifier,
    title: String,
    supportText: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = modifier
            .padding(horizontal = 12.dp),
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = supportText) },
        onClick = onClick,
        selected = false
    )
}
