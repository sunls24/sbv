package dev.sunls24.sbv.component.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
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
        Icon(
            painter = painterResource(if (isCoined) R.drawable.ic_symbol_paid_filled else R.drawable.ic_symbol_paid),
            contentDescription = null
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
