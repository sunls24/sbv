package dev.sunls24.sbv.tv.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import dev.sunls24.sbv.ui.theme.SBVTheme

@Composable
fun TvAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconContentColor: Color = MaterialTheme.colorScheme.onSurface,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = TvDialogDefaults.properties
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.tvDialogWidth(),
        dismissButton = dismissButton,
        icon = icon,
        title = (@Composable {
            ProvideTextStyle(
                value = MaterialTheme.typography.headlineSmall
            ) {
                title?.invoke()
            }
        }).takeIf { title != null },
        text = (@Composable {
            ProvideTextStyle(
                value = MaterialTheme.typography.bodyMedium
            ) {
                text?.invoke()
            }
        }).takeIf { text != null },
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}

@Preview
@Composable
private fun DialogPreview() {
    SBVTheme {
        TvAlertDialog(
            title = {
                Text(text = "Dialog Title")
            },
            text = {
                Column {
                    Text(text = "This is a sample dialog text. It can be used to display information or ask for user input.")
                    Text(
                        text = "This is a sample dialog text. It can be used to display information or ask for user input.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            onDismissRequest = {},
            confirmButton = {
                OutlinedButton(onClick = {}) {
                    Text(text = "Confirm")
                }
            },
        )
    }
}
