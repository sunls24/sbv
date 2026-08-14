package dev.sunls24.sbv.component.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import dev.sunls24.biliapi.entity.FavoriteFolderMetadata
import dev.sunls24.sbv.R
import dev.sunls24.sbv.tv.component.TvAlertDialog
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.util.swapList

@Composable
fun FavoriteButton(
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onAddToDefaultFavoriteFolder: () -> Unit,
    onUpdateFavoriteFolders: (List<Long>) -> Unit
) {
    var showFavoriteDialog by remember { mutableStateOf(false) }

    Button(
        modifier = modifier,
        onClick = {
            if (showFavoriteDialog) return@Button
            if (isFavorite) {
                showFavoriteDialog = true
            } else onAddToDefaultFavoriteFolder()
        }
    ) {
        VideoActionContent(
            icon = if (isFavorite) R.drawable.ic_symbol_star_filled else R.drawable.ic_symbol_star,
            label = stringResource(R.string.video_info_action_favorite),
        )
    }

    FavoriteDialog(
        show = showFavoriteDialog,
        onHideDialog = { showFavoriteDialog = false },
        userFavoriteFolders = userFavoriteFolders,
        favoriteFolderIds = favoriteFolderIds,
        onUpdateFavoriteFolders = onUpdateFavoriteFolders
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
private fun FavoriteDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit,
    userFavoriteFolders: List<FavoriteFolderMetadata> = emptyList(),
    favoriteFolderIds: List<Long> = emptyList(),
    onUpdateFavoriteFolders: (List<Long>) -> Unit
) {
    val selectedFavoriteFolderIds = remember { mutableStateListOf<Long>() }
    val defaultFocusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            selectedFavoriteFolderIds.swapList(favoriteFolderIds)
            defaultFocusRequester.requestFocus()
        }
    }

    if (show) {
        TvAlertDialog(
            modifier = modifier,
            onDismissRequest = onHideDialog,
            confirmButton = {},
            title = { Text(text = stringResource(R.string.favorite_dialog_title)) },
            text = {
                FlowRow(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    userFavoriteFolders.forEachIndexed { index, userFavoriteFolder ->
                        val selected = selectedFavoriteFolderIds.contains(userFavoriteFolder.id)
                        var hasFocus by remember { mutableStateOf(false) }

                        val itemModifier =
                            if (index == 0) Modifier.focusRequester(defaultFocusRequester)
                            else Modifier

                        FilterChip(
                            modifier = itemModifier.onFocusChanged { hasFocus = it.hasFocus },
                            selected = selected,
                            onClick = {
                                if (selectedFavoriteFolderIds.contains(userFavoriteFolder.id)) {
                                    selectedFavoriteFolderIds.remove(userFavoriteFolder.id)
                                } else {
                                    selectedFavoriteFolderIds.add(userFavoriteFolder.id)
                                }
                                onUpdateFavoriteFolders(selectedFavoriteFolderIds)
                            },
                            leadingIcon = {
                                Row {
                                    AnimatedVisibility(visible = selected) {
                                        Icon(
                                            modifier = Modifier.size(20.dp),
                                            painter = painterResource(R.drawable.ic_symbol_done_filled),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        ) {
                            Text(text = userFavoriteFolder.title)
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun FavoriteButtonEnablePreview() {
    SBVTheme {
        FavoriteButton(
            isFavorite = true,
            onAddToDefaultFavoriteFolder = {},
            onUpdateFavoriteFolders = {}
        )
    }
}

@Preview
@Composable
fun FavoriteButtonDisablePreview() {
    SBVTheme {
        FavoriteButton(
            isFavorite = false,
            onAddToDefaultFavoriteFolder = {},
            onUpdateFavoriteFolders = {}
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun FavoriteDialogPreview() {
    val userFavoriteFolders = listOf(
        FavoriteFolderMetadata(0, 0, 0, "收藏夹1", null, false, 0),
        FavoriteFolderMetadata(1, 1, 0, "收藏夹2", null, false, 0),
        FavoriteFolderMetadata(2, 2, 0, "收藏夹3", null, false, 0),
        FavoriteFolderMetadata(3, 3, 0, "收藏夹4", null, false, 0),
        FavoriteFolderMetadata(4, 4, 0, "收藏夹5", null, false, 0),
        FavoriteFolderMetadata(5, 5, 0, "收藏夹6", null, false, 0),
        FavoriteFolderMetadata(6, 6, 0, "收藏夹7", null, false, 0),
        FavoriteFolderMetadata(7, 7, 0, "收藏夹8", null, false, 0),
        FavoriteFolderMetadata(8, 8, 0, "收藏夹9", null, false, 0),
        FavoriteFolderMetadata(9, 9, 0, "收藏夹10", null, false, 0),
    )
    SBVTheme {
        FavoriteDialog(
            show = true,
            onHideDialog = {},
            userFavoriteFolders = userFavoriteFolders,
            onUpdateFavoriteFolders = {}
        )
    }
}
