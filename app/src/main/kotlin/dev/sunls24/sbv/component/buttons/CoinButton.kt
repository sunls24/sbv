package dev.sunls24.sbv.component.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import dev.sunls24.sbv.R

@Composable
fun CoinButton(
    modifier: Modifier = Modifier,
    isCoined: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
        VideoActionContent(
            icon = if (isCoined) R.drawable.ic_symbol_paid_filled else R.drawable.ic_symbol_paid,
            label = stringResource(R.string.video_info_action_coin),
        )
    }
}

@Preview
@Composable
fun CoinButtonPreview() {
    CoinButton(
        isCoined = false,
        onClick = {}
    )
}
