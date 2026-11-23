package com.lucrometro.ui.trades

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucrometro.R
import com.lucrometro.data.Holding
import com.lucrometro.data.Quote
import com.lucrometro.data.Realized
import com.lucrometro.data.Replay
import com.lucrometro.data.Side
import com.lucrometro.data.TRADE_ORDER
import com.lucrometro.data.Trade
import com.lucrometro.data.TradeChange
import com.lucrometro.data.replay
import com.lucrometro.ui.common.AmountChange
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.common.changeLabel
import com.lucrometro.ui.common.rememberQuoteFormats
import com.lucrometro.ui.common.rememberTimeFormats
import kotlin.math.abs
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** No sheet open. */
private const val NO_SHEET = -1L

/** The sheet for a new trade (trades start at id 1). */
private const val NEW_TRADE = 0L

/**
 * One ticker's trades (decision 0042), private view only: a summary line, the trades newest first under year headers,
 * tap to edit, swipe to delete with undo, + to add, and the CSV import in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradesScreen(
    code: String,
    onBack: () -> Unit,
    viewModel: TradesViewModel = viewModel(key = "trades-$code", factory = TradesViewModel.factory(code)),
) {
    val trades by viewModel.list.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val quote = entry?.saved?.quote
    val formats = rememberQuoteFormats()
    val times = rememberTimeFormats()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val importState by viewModel.import.collectAsStateWithLifecycle()
    val typed by viewModel.typed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The system file picker; the file is read and checked before anything is saved.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { readPickedFile(context, uri).let { (name, text) -> viewModel.onFileRead(name, text) } }
    }
    // CSV files come with different types depending on the app that saved them.
    val openCsv = { pickFile.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel")) }
    // Taken as soon as it arrives: the ViewModel outlives this screen, and an unconsumed notice would show again.
    LaunchedEffect(viewModel) {
        viewModel.notice.filterNotNull().collect { shown ->
            viewModel.noticeShown(shown)
            snackbar.showSnackbar(
                when (shown) {
                    ImportNotice.NothingNew -> resources.getString(R.string.import_nothing_new, code)
                    is ImportNotice.Imported -> resources.getQuantityString(R.plurals.import_done, shown.count, shown.count)
                },
            )
        }
    }
    // The trade the sheet edits (by id), NEW_TRADE for a new one, NO_SHEET when closed; kept on rotation.
    var sheet by rememberSaveable { mutableStateOf(NO_SHEET) }

    LaunchedEffect(deleted) {
        val trade = deleted ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = resources.getString(
                R.string.trade_deleted,
                times.date("dMMM", trade.date),
                resources.getQuantityString(R.plurals.trade_shares, shareCount(trade.quantity), formats.shares(trade.quantity)),
            ),
            actionLabel = resources.getString(R.string.undo),
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed && !viewModel.undoDelete(trade)) {
            snackbar.showSnackbar(resources.getString(R.string.trade_error_save))
        }
        viewModel.undoDismissed(trade) // last: it changes the key, which would cancel the message above
    }

    val delete: suspend (Trade) -> Boolean = { trade ->
        when (val result = viewModel.delete(trade)) {
            TradeChange.Saved -> true
            is TradeChange.BelowZero -> {
                scope.launch {
                    snackbar.showSnackbar(resources.getString(R.string.trade_delete_refused, times.date("dMMMyyyy", result.replay.trade.date)))
                }
                false
            }
            TradeChange.Failed -> {
                scope.launch { snackbar.showSnackbar(resources.getString(R.string.trade_delete_failed)) }
                false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.trades_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            code,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = openCsv) {
                        Icon(
                            painterResource(R.drawable.ic_upload_file),
                            contentDescription = stringResource(R.string.trades_import),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (!trades.isNullOrEmpty() && quote != null) {
                FloatingActionButton(onClick = { sheet = NEW_TRADE }) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.trades_add))
                }
            }
        },
    ) { padding ->
        val list = trades
        if (list == null || quote == null) return@Scaffold // loading: blank for a moment, no empty-state flash
        if (list.isEmpty()) {
            EmptyTrades(onAdd = { sheet = NEW_TRADE }, onImport = openCsv, modifier = Modifier.padding(padding))
        } else {
            val newestFirst = remember(list) { list.sortedWith(TRADE_ORDER.reversed()) }
            val holding = remember(list) { (replay(list) as? Replay.Ok)?.holding }
            LazyColumn(
                Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                // Room below the last row for the FAB.
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 88.dp),
            ) {
                holding?.let { item { Summary(it, quote, formats) } }
                newestFirst.groupBy { it.date.year }.forEach { (year, inYear) ->
                    item(key = "year-$year") {
                        Text(
                            year.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
                        )
                    }
                    items(inYear, key = { it.id }) { trade ->
                        RemovableTradeRow(trade, holding?.sells?.get(trade.id), quote, formats, times, delete, onOpen = { sheet = trade.id }, Modifier.animateItem(fadeOutSpec = null))
                    }
                }
            }
        }
        val editing = list.firstOrNull { it.id == sheet }
        // A trade deleted meanwhile has nothing to edit: no sheet.
        if (sheet == NEW_TRADE || editing != null) {
            TradeSheet(
                code = code,
                editing = editing,
                others = list.filter { it.id != sheet },
                quote = quote,
                formats = formats,
                times = times,
                onSave = viewModel::save,
                onDelete = { trade -> scope.launch { delete(trade) } },
                onClose = { sheet = NO_SHEET },
            )
        }
        importState?.let {
            ImportDialog(
                it, code, typed, hasTrades = list.isNotEmpty(), quote, formats, times,
                onConfirm = viewModel::confirmImport,
                onDismiss = viewModel::dismissImport,
            )
        }
    }
}

/** "130 shares · avg 40.70 EUR · 13 trades" under the top bar, with a divider; sold out, the realized total (0047). */
@Composable
private fun Summary(holding: Holding, quote: Quote, formats: QuoteFormats) {
    val average = holding.average
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            if (average == null) {
                val realized = holding.realized?.gain ?: 0.0
                // The sign from the badge's rounding, the text as a price with its currency: "+631.60 EUR".
                val amount = AmountChange(formats.amountChange(quote, realized).rounded, formats.price(quote, abs(realized)))
                val signed = changeLabel(null, formats, amount)
                pluralStringResource(R.plurals.trades_summary_sold_out, holding.count, holding.count, signed)
            } else {
                pluralStringResource(
                    R.plurals.trades_summary, holding.count,
                    formats.shares(holding.shares), formats.price(quote, average), holding.count,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** A swipe from the end deletes (with a "Delete" TalkBack action); a refused or failed delete slides the row back. */
@Composable
private fun RemovableTradeRow(
    trade: Trade,
    realized: Realized?,
    quote: Quote,
    formats: QuoteFormats,
    times: TimeFormats,
    onDelete: suspend (Trade) -> Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Not saveable: an undo brings back the same key, which would restore its "dismissed" state (see the watchlist).
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    val scope = rememberCoroutineScope()
    val delete: () -> Unit = { scope.launch { if (!onDelete(trade)) state.reset() } }
    val deleteLabel = stringResource(R.string.trade_delete)
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { delete() },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        TradeRow(
            trade, realized, quote, formats, times,
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpen)
                .semantics { customActions = listOf(CustomAccessibilityAction(deleteLabel) { delete(); true }) },
        )
    }
}

/**
 * Doop component TradeRow rev 3: a 72dp row with the date tile, "Buy · 10 shares" (the side in tertiary or primary,
 * never gain or loss colours: a sell is not a loss), the time when known, the price and the amount (no fees). A sell
 * shows its [realized] gain in place of the amount (decision 0047).
 */
@Composable
private fun TradeRow(trade: Trade, realized: Realized?, quote: Quote, formats: QuoteFormats, times: TimeFormats, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(trade.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                times.date("LLL", trade.date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f)) {
            val buy = trade.side == Side.BUY
            Row {
                Text(
                    stringResource(if (buy) R.string.trade_buy else R.string.trade_sell),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (buy) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
                Text(
                    " · " + pluralStringResource(R.plurals.trade_shares, shareCount(trade.quantity), formats.shares(trade.quantity)),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trade.time?.let {
                Text(times.time(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(withCurrency(quote, formats.exact(quote, trade.price)), style = MaterialTheme.typography.bodyLarge)
            if (realized != null) {
                ChangeBadge(realized.percent, formats, Modifier.padding(top = 2.dp), absolute = formats.amountChange(quote, realized.gain))
            } else {
                Text(
                    formats.price(quote, trade.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyTrades(onAdd: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_receipt), null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.trades_empty_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.trades_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onImport) { Text(stringResource(R.string.trades_import_button)) }
            Button(onClick = onAdd) { Text(stringResource(R.string.trades_add)) }
        }
    }
}

/** A price as typed, with the quote's currency when known. */
@Composable
internal fun withCurrency(quote: Quote, number: String): String =
    quote.currency?.let { stringResource(R.string.quote_price, number, it) } ?: number

/** The plural form for [quantity] shares: "1 share", "2.5 shares". */
internal fun shareCount(quantity: Double): Int = if (quantity == 1.0) 1 else 2
