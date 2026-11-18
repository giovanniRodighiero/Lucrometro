package com.lucrometro.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.allCaps
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucrometro.R
import com.lucrometro.data.WatchlistEntry
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
fun WatchlistScreen(viewModel: WatchlistViewModel = viewModel(factory = WatchlistViewModel.Factory)) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val addSheet by viewModel.addSheet.collectAsStateWithLifecycle()
    val undo by viewModel.undo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // Undo snackbar for the latest removal (PRD 3). A newer removal replaces it; rotation shows it again.
    LaunchedEffect(undo) {
        val removed = undo ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = resources.getString(R.string.ticker_removed, removed.code),
            actionLabel = resources.getString(R.string.undo),
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed && !viewModel.undoRemove(removed)) {
            snackbar.showSnackbar(resources.getString(R.string.undo_failed, removed.code))
        }
        viewModel.undoDismissed(removed) // last: it changes the key, which would cancel the message above
    }

    // PRD: auto-refresh when the app is opened (the ViewModel skips it if it refreshed a moment ago).
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenStarted()
        onStopOrDispose {}
    }

    WatchlistContent(
        entries = entries,
        refreshing = refreshing,
        snackbar = snackbar,
        onRefresh = viewModel::refresh,
        onRemove = viewModel::remove,
        onAddClick = viewModel::openAddSheet,
    )

    addSheet?.let { sheet ->
        AddTickerSheet(
            state = sheet,
            onInputEdited = viewModel::onInputEdited,
            onSubmit = viewModel::submit,
            onClose = viewModel::closeAddSheet,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistContent(
    entries: List<WatchlistEntry>?,
    refreshing: Boolean,
    snackbar: SnackbarHostState,
    onRefresh: () -> Unit,
    onRemove: suspend (String) -> Boolean,
    onAddClick: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    // Doop TopAppBar: "Updated 14:32" stamp at the end of the bar.
                    // The newest saved quote: what "updated" means even after a restart or a failed refresh.
                    entries?.mapNotNull { it.saved?.fetchedAt }?.maxOrNull()?.let {
                        Text(
                            stringResource(R.string.watchlist_updated, formatTime(LocalContext.current, it)),
                            modifier = Modifier.padding(end = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add_ticker))
            }
        },
    ) { padding ->
        if (entries == null) {
            // Still loading from storage: a blank frame for a moment beats a flash of the empty state.
        } else if (entries.isEmpty()) {
            EmptyWatchlist(Modifier.padding(padding))
        } else {
            val formats = rememberQuoteFormats()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Room below the last row for the FAB (56dp + its 16dp margin + 16dp air).
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 88.dp),
                ) {
                    items(entries, key = { it.code }) { entry ->
                        // No fade-out: the swiped row has already left the screen, so it just goes.
                        RemovableQuoteRow(entry, formats, onRemove, Modifier.animateItem(fadeOutSpec = null))
                    }
                }
            }
        }
    }
}

/**
 * A row that can be swiped from the end to remove it (PRD 3), with a "Remove" accessibility action for
 * TalkBack, where swiping is not available. If storage fails, the row slides back.
 */
@Composable
private fun RemovableQuoteRow(
    entry: WatchlistEntry,
    formats: QuoteFormats,
    onRemove: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    // Plain remember, not rememberSwipeToDismissBoxState: that one is saveable, and LazyColumn restores
    // saved item state by key. An undo brings the same code back, which would get its "dismissed"
    // state back and be removed again at once (seen on the emulator).
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    val scope = rememberCoroutineScope()
    val remove: () -> Unit = { scope.launch { if (!onRemove(entry.code)) state.reset() } }
    val removeLabel = stringResource(R.string.remove_ticker)
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { remove() },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        QuoteRow(
            entry,
            formats,
            Modifier
                .background(MaterialTheme.colorScheme.surface) // covers the red background at rest
                .semantics { customActions = listOf(CustomAccessibilityAction(removeLabel) { remove(); true }) },
        )
    }
}

@Composable
private fun EmptyWatchlist(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.watchlist_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.watchlist_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTickerSheet(
    state: AddTickerState,
    onInputEdited: () -> Unit,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit,
) {
    val input = rememberTextFieldState()
    val submit = { onSubmit(input.text.toString()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Animate the sheet away, then drop it from the state.
    val hideThenClose: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() }
    }

    // Skip the initial (empty) value: only real edits clear the error.
    LaunchedEffect(input) { snapshotFlow { input.text.toString() }.drop(1).collect { onInputEdited() } }
    LaunchedEffect(state.added) { if (state.added) hideThenClose() }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
            Text(stringResource(R.string.add_ticker), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.add_ticker_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                state = input,
                modifier = Modifier.fillMaxWidth(),
                readOnly = state.checking,
                lineLimits = TextFieldLineLimits.SingleLine,
                inputTransformation = TickerInput,
                label = { Text(stringResource(R.string.add_ticker_label)) },
                supportingText = { Text(state.error?.message() ?: stringResource(R.string.add_ticker_hint)) },
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                onKeyboardAction = { submit() },
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = hideThenClose) { Text(stringResource(R.string.cancel)) }
                Button(onClick = submit, enabled = input.text.isNotBlank() && !state.checking) {
                    if (state.checking) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
}

/**
 * Ticker codes have no whitespace and are shown uppercase: drop whitespace as it comes in (pasted
 * trailing newlines, stray spaces) and uppercase the rest. Done as an input transformation so the
 * keyboard's composing text stays consistent.
 */
private val TickerInput = InputTransformation {
    for (i in length - 1 downTo 0) if (charAt(i).isWhitespace()) replace(i, i + 1, "")
}.allCaps(Locale("en"))

@Composable
private fun AddTickerError.message(): String = when (kind) {
    AddTickerError.Kind.Empty -> stringResource(R.string.add_ticker_error_empty)
    AddTickerError.Kind.InvalidFormat -> stringResource(R.string.add_ticker_error_format)
    AddTickerError.Kind.Duplicate -> stringResource(R.string.add_ticker_error_duplicate, code)
    AddTickerError.Kind.NotFound -> stringResource(R.string.add_ticker_error_not_found, code)
    AddTickerError.Kind.NoPrice -> stringResource(R.string.add_ticker_error_no_price, code)
    AddTickerError.Kind.CheckFailed -> stringResource(R.string.add_ticker_error_check_failed, code)
    AddTickerError.Kind.SaveFailed -> stringResource(R.string.add_ticker_error_save_failed, code)
}
