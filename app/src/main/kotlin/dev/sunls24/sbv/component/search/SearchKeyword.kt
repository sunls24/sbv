package dev.sunls24.sbv.component.search

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.sunls24.sbv.util.ImageSize
import dev.sunls24.sbv.util.resizedImageUrl

@Composable
fun SearchKeyword(
    modifier: Modifier = Modifier,
    keyword: String,
    leadingIcon: String,
    trailingIcon: @Composable() (() -> Unit)? = null,
    onClick: () -> Unit
) {
    if (leadingIcon.isNotBlank()) {
        DenseListItem(
            modifier = modifier,
            selected = false,
            onClick = onClick,
            headlineContent = {
                Text(
                    text = keyword,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                AsyncImage(
                    modifier = Modifier.size(16.dp),
                    model = leadingIcon.resizedImageUrl(ImageSize.Icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            },
            trailingContent = trailingIcon
        )
    } else {
        DenseListItem(
            modifier = modifier,
            selected = false,
            onClick = onClick,
            headlineContent = {
                Text(
                    text = keyword,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = trailingIcon
        )
    }
}
