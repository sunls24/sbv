package dev.sunls24.sbv.screen.search

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import dev.sunls24.sbv.activities.search.SearchResultActivity
import dev.sunls24.sbv.component.search.SearchKeyword
import dev.sunls24.sbv.component.search.SoftKeyboard
import dev.sunls24.sbv.tv.component.TvAlertDialog
import dev.sunls24.sbv.ui.theme.SBVTheme
import dev.sunls24.sbv.ui.theme.SBVSpacing
import dev.sunls24.sbv.util.Prefs
import dev.sunls24.sbv.viewmodel.search.SearchInputViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchInputScreen(
    modifier: Modifier = Modifier,
    defaultFocusRequester: FocusRequester,
    searchInputViewModel: SearchInputViewModel = koinViewModel()
) {
    val context = LocalContext.current

    val searchKeyword = searchInputViewModel.keyword
    val hotwords = searchInputViewModel.hotwords
    val searchHistories = searchInputViewModel.searchHistories
    val suggests = searchInputViewModel.suggests

    val onSearch: (String) -> Unit = { keyword ->
        SearchResultActivity.actionStart(context, keyword)
        searchInputViewModel.keyword = keyword
        searchInputViewModel.addSearchHistory(keyword)
    }

    LaunchedEffect(searchKeyword) {
        searchInputViewModel.updateSuggests()
    }

    SearchInputScreenContent(
        modifier = modifier,
        defaultFocusRequester = defaultFocusRequester,
        searchKeyword = searchKeyword,
        onSearchKeywordChange = { searchInputViewModel.keyword = it },
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
    onSearch: (String) -> Unit,
    hotwords: List<Hotword>,
    suggests: List<String>,
    histories: List<String>,
    onDeleteHistory: (String) -> Unit,
    onDeleteAllHistories: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(
                    start = SBVSpacing.xl,
                    top = SBVSpacing.lg,
                    bottom = SBVSpacing.sm,
                    end = SBVSpacing.xl,
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.search_input_title),
                        style = MaterialTheme.typography.displaySmall,
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .padding(vertical = SBVSpacing.sm)
                .padding(start = SBVSpacing.lg)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SBVSpacing.xl)
        ) {
            SearchInput(
                firstButtonFocusRequester = defaultFocusRequester,
                searchKeyword = searchKeyword,
                onSearchKeywordChange = onSearchKeywordChange,
                onSearch = { onSearch(searchKeyword) }
            )

            if (searchKeyword.isEmpty()) {
                SearchHotwords(
                    hotwords = hotwords,
                    onSearch = onSearch
                )
            } else {
                SearchSuggestion(
                    suggests = suggests,
                    onSearch = onSearch
                )
            }

            SearchHistory(
                modifier = Modifier
                    .padding(end = 10.dp),
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
    onSearch: (String) -> Unit
) {
    Box(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .focusGroup(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.width(258.dp),
                value = searchKeyword,
                onValueChange = onSearchKeywordChange,
                maxLines = 1,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(searchKeyword) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                )
            )
            SoftKeyboard(
                firstButtonFocusRequester = firstButtonFocusRequester,
                onClick = { onSearchKeywordChange(searchKeyword + it) },
                onClear = { onSearchKeywordChange("") },
                onDelete = {
                    if (searchKeyword.isNotEmpty()) {
                        onSearchKeywordChange(searchKeyword.dropLast(1))
                    }
                },
                onSearch = { onSearch(searchKeyword) }
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
            .width(250.dp)
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
                itemsIndexed(hotwords) { index, hotword ->
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
            .width(250.dp)
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
            itemsIndexed(suggests) { index, suggest ->
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
    histories: List<String>,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var deleteMode by remember { mutableStateOf(false) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(250.dp)
            .fillMaxHeight()
            .focusGroup(),
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
            Row {
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
            itemsIndexed(histories) { index, searchHistory ->
                SearchKeyword(
                    modifier = Modifier,
                    keyword = searchHistory,
                    leadingIcon = "",
                    onClick = {
                        if (deleteMode) {
                            if (index == histories.lastIndex) {
                                focusManager.moveFocus(FocusDirection.Up)
                            }
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
                Text(text = stringResource(R.string.search_input_history_delete_all_confirm_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.search_input_history_delete_all_confirm_dialog_text))
            },
            confirmButton = {
                Button(onClick = {
                    onDeleteAll()
                    showDeleteAllConfirmDialog = false
                    deleteMode = false
                }) {
                    Text(text = stringResource(R.string.search_input_history_delete_all_confirm_dialog_confirm_button))
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteAllConfirmDialog = false
                }) {
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
