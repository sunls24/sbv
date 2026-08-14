package dev.sunls24.sbv.screen.search

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.sunls24.biliapi.entity.search.Hotword
import dev.sunls24.sbv.R
import dev.sunls24.sbv.component.search.SearchKeyword
import dev.sunls24.sbv.component.search.SoftKeyboard
import dev.sunls24.sbv.tv.component.TvAlertDialog
import dev.sunls24.sbv.tv.component.tvDialogButtonHeight
import dev.sunls24.sbv.ui.theme.SBVMaterial3Typography
import dev.sunls24.sbv.ui.theme.SBVPageTitle
import dev.sunls24.sbv.ui.theme.SBVSize
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.viewmodel.search.SearchInputViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchInputScreen(
    modifier: Modifier = Modifier,
    defaultFocusRequester: FocusRequester,
    onSearchRequest: (String) -> Unit,
    searchInputViewModel: SearchInputViewModel = koinViewModel()
) {
    val searchKeyword = searchInputViewModel.keyword
    val hotwords = searchInputViewModel.hotwords
    val searchHistories = searchInputViewModel.searchHistories
    val suggests = searchInputViewModel.suggests

    val onKeywordChange: (String) -> Unit = remember(searchInputViewModel) {
        { searchInputViewModel.keyword = it }
    }
    val onAppendKeyword: (String) -> Unit = remember(searchInputViewModel) {
        { searchInputViewModel.keyword += it }
    }
    val onClearKeyword: () -> Unit = remember(searchInputViewModel) {
        { searchInputViewModel.keyword = "" }
    }
    val onDeleteKeyword: () -> Unit = remember(searchInputViewModel) {
        {
            if (searchInputViewModel.keyword.isNotEmpty()) {
                searchInputViewModel.keyword = searchInputViewModel.keyword.dropLast(1)
            }
        }
    }
    val onSearch: (String) -> Unit = remember(searchInputViewModel, onSearchRequest) {
        { keyword ->
            val normalized = keyword.trim()
            if (normalized.isNotEmpty()) {
                onSearchRequest(normalized)
                searchInputViewModel.keyword = normalized
                searchInputViewModel.addSearchHistory(normalized)
            }
        }
    }
    val onSubmitSearch: () -> Unit = remember(searchInputViewModel, onSearch) {
        { onSearch(searchInputViewModel.keyword) }
    }

    LaunchedEffect(Unit) {
        searchInputViewModel.keyword = ""
    }

    LaunchedEffect(searchKeyword) {
        searchInputViewModel.updateSuggests()
    }

    SearchInputScreenContent(
        modifier = modifier,
        defaultFocusRequester = defaultFocusRequester,
        searchKeyword = searchKeyword,
        onSearchKeywordChange = onKeywordChange,
        onAppendKeyword = onAppendKeyword,
        onClearKeyword = onClearKeyword,
        onDeleteKeyword = onDeleteKeyword,
        onSubmitSearch = onSubmitSearch,
        onSearch = onSearch,
        hotwords = hotwords,
        suggests = suggests,
        histories = searchHistories,
        onDeleteHistory = { searchInputViewModel.deleteSearchHistory(it) },
        onDeleteAllHistories = { searchInputViewModel.deleteAllSearchHistories() }
    )
}

@Composable
private fun SearchInputScreenContent(
    modifier: Modifier = Modifier,
    defaultFocusRequester: FocusRequester,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    onAppendKeyword: (String) -> Unit,
    onClearKeyword: () -> Unit,
    onDeleteKeyword: () -> Unit,
    onSubmitSearch: () -> Unit,
    onSearch: (String) -> Unit,
    hotwords: List<Hotword>,
    suggests: List<String>,
    histories: List<String>,
    onDeleteHistory: (String) -> Unit,
    onDeleteAllHistories: () -> Unit
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(
                    top = SBVSpacing.xxl,
                    bottom = SBVSpacing.sm,
                    start = SBVSpacing.xl,
                    end = SBVSpacing.xl,
                ),
            horizontalArrangement = Arrangement.spacedBy(
                space = SBVSpacing.xxl,
                alignment = Alignment.CenterHorizontally,
            )
        ) {
            SearchInput(
                modifier = Modifier.weight(1.15f),
                firstButtonFocusRequester = defaultFocusRequester,
                searchKeyword = searchKeyword,
                onSearchKeywordChange = onSearchKeywordChange,
                onAppendKeyword = onAppendKeyword,
                onClearKeyword = onClearKeyword,
                onDeleteKeyword = onDeleteKeyword,
                onSearch = onSubmitSearch,
            )

            if (searchKeyword.isEmpty()) {
                SearchHotwords(
                    modifier = Modifier.weight(1f),
                    hotwords = hotwords,
                    onSearch = onSearch
                )
            } else {
                SearchSuggestion(
                    modifier = Modifier.weight(1f),
                    suggests = suggests,
                    onSearch = onSearch
                )
            }

            SearchHistory(
                modifier = Modifier.weight(1f),
                fallbackFocusRequester = defaultFocusRequester,
                histories = histories,
                onSearch = onSearch,
                onDelete = onDeleteHistory,
                onDeleteAll = onDeleteAllHistories
            )
        }
    }
}

@Composable
private fun SearchInput(
    modifier: Modifier = Modifier,
    firstButtonFocusRequester: FocusRequester,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    onAppendKeyword: (String) -> Unit,
    onClearKeyword: () -> Unit,
    onDeleteKeyword: () -> Unit,
    onSearch: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .focusGroup(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = SBVSize.searchPanelWidth)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.search_input_title),
                style = SBVPageTitle,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                textStyle = SBVMaterial3Typography.bodyLarge,
                maxLines = 1,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                )
            )
            SoftKeyboard(
                firstButtonFocusRequester = firstButtonFocusRequester,
                onClick = onAppendKeyword,
                onClear = onClearKeyword,
                onDelete = onDeleteKeyword,
                onSearch = onSearch,
            )
        }
    }
}

@Composable
private fun SearchHotwords(
    modifier: Modifier = Modifier,
    hotwords: List<Hotword>,
    onSearch: (String) -> Unit
) {
    var showHotword by remember { mutableStateOf(Prefs.showHotword) }

    Column(
        modifier = modifier
            .widthIn(max = SBVSize.searchPanelWidth)
            .fillMaxWidth()
            .fillMaxHeight()
            .focusGroup(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = stringResource(R.string.search_input_hotword),
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                onClick = {
                    showHotword = !showHotword
                    Prefs.showHotword = showHotword
                },
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            ) {
                if (showHotword) {
                    Icon(
                        painter = painterResource(id = R.drawable.expand_circle_up_24px),
                        contentDescription = null
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.expand_circle_down_24px),
                        contentDescription = null
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showHotword,
            enter = expandVertically(
                expandFrom = Alignment.Top
            ) + fadeIn(),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top
            ) + fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier,
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = hotwords,
                    key = { it.showName },
                ) { hotword ->
                    SearchKeyword(
                        modifier = Modifier,
                        keyword = hotword.showName,
                        leadingIcon = hotword.icon ?: "",
                        onClick = { onSearch(hotword.showName) }
                    )
                }
            }
        }
    }
}


@Composable
private fun SearchSuggestion(
    modifier: Modifier = Modifier,
    suggests: List<String>,
    onSearch: (String) -> Unit
) {
    Column(
        modifier = modifier
            .widthIn(max = SBVSize.searchPanelWidth)
            .fillMaxWidth()
            .fillMaxHeight()
            .focusGroup(),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = stringResource(R.string.search_input_suggest),
            style = MaterialTheme.typography.titleMedium
        )
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(
                items = suggests,
                key = { it },
            ) { suggest ->
                SearchKeyword(
                    modifier = Modifier,
                    keyword = suggest,
                    leadingIcon = "",
                    onClick = { onSearch(suggest) }
                )
            }
        }
    }
}

@Composable
private fun SearchHistory(
    modifier: Modifier = Modifier,
    fallbackFocusRequester: FocusRequester,
    histories: List<String>,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit
) {
    val firstHistoryFocusRequester = remember { FocusRequester() }
    val historyCount = histories.size
    val focusRestorerFallback = if (historyCount > 0) {
        firstHistoryFocusRequester
    } else {
        fallbackFocusRequester
    }
    var deleteMode by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .widthIn(max = SBVSize.searchPanelWidth)
            .fillMaxWidth()
            .fillMaxHeight()
            .focusGroup()
            .focusRestorer(
                fallback = focusRestorerFallback,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                text = stringResource(R.string.search_input_history),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(SBVSpacing.sm)
            ) {
                if (deleteMode) {
                    IconButton(
                        onClick = { showDeleteAllConfirmDialog = true },
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        )
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_symbol_delete_sweep_filled), contentDescription = null)
                    }
                }
                IconButton(
                    onClick = { deleteMode = !deleteMode },
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                ) {
                    if (deleteMode) {
                        Icon(painter = painterResource(R.drawable.ic_symbol_close_filled), contentDescription = null)
                    } else {
                        Icon(painter = painterResource(R.drawable.ic_symbol_delete_filled), contentDescription = null)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(
                items = histories,
                key = { it },
            ) { searchHistory ->
                SearchKeyword(
                    modifier = if (searchHistory == histories.firstOrNull()) {
                        Modifier.focusRequester(firstHistoryFocusRequester)
                    } else {
                        Modifier
                    },
                    keyword = searchHistory,
                    leadingIcon = "",
                    onClick = {
                        if (deleteMode) {
                            onDelete(searchHistory)
                        } else {
                            onSearch(searchHistory)
                        }
                    },
                    trailingIcon = (@Composable {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(R.drawable.ic_symbol_delete_filled),
                            contentDescription = null
                        )
                    }).takeIf { deleteMode }
                )
            }
        }
    }

    if (showDeleteAllConfirmDialog) {
        TvAlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.search_input_history_delete_all_confirm_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.search_input_history_delete_all_confirm_dialog_text),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                Button(
                    modifier = Modifier.tvDialogButtonHeight(),
                    onClick = {
                        onDeleteAll()
                        showDeleteAllConfirmDialog = false
                        deleteMode = false
                    }
                ) {
                    Text(text = stringResource(R.string.search_input_history_delete_all_confirm_dialog_confirm_button))
                }
            },
            dismissButton = {
                Button(
                    modifier = Modifier.tvDialogButtonHeight(),
                    onClick = {
                        showDeleteAllConfirmDialog = false
                    }
                ) {
                    Text(text = stringResource(R.string.search_input_history_delete_all_confirm_dialog_cancel_button))
                }
            }
        )
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchInputScreenContentPreview() {
    SBVTheme {
        Row {
            Spacer(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            SearchInputScreenContent(
                modifier = Modifier,
                defaultFocusRequester = FocusRequester.Default,
                searchKeyword = "",
                onSearchKeywordChange = {},
                onAppendKeyword = {},
                onClearKeyword = {},
                onDeleteKeyword = {},
                onSubmitSearch = {},
                onSearch = {},
                hotwords = listOf(
                    Hotword("热搜1", "热搜1", null),
                    Hotword("热搜2", "热搜2", null)
                ),
                suggests = listOf("建议1", "建议2"),
                histories = listOf("历史1", "历史2"),
                onDeleteHistory = {},
                onDeleteAllHistories = {}
            )
        }
    }
}
