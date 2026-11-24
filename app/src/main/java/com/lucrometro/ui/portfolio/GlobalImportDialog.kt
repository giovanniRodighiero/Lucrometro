package com.lucrometro.ui.portfolio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.lucrometro.R
import com.lucrometro.data.ImportCheck
import com.lucrometro.data.Quote
import com.lucrometro.data.Side
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.common.csvErrorText
import java.time.LocalDate

/** The global import (decision 0054): the full-screen review before saving, or why nothing was saved. */
@Composable
fun GlobalImportDialog(
    state: GlobalImport,
    formats: QuoteFormats,
    times: TimeFormats,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is GlobalImport.Review -> Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            // The dialog's own window starts with light status and navigation bar icons: match the theme like the activity.
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            val dark = isSystemInDarkTheme()
            SideEffect {
                window?.let {
                    WindowCompat.getInsetsController(it, it.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
            }
            Review(state, formats, times, onConfirm, onDismiss)
        }
        is GlobalImport.Failed -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.import_error_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(problemText(state.problem, times))
                    Text(stringResource(R.string.global_import_nothing_saved))
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Review(state: GlobalImport.Review, formats: QuoteFormats, times: TimeFormats, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val check = state.check
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close))
                    }
                },
                title = {
                    Column {
                        Text(stringResource(R.string.global_import_title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        state.fileName?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = variant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                actions = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_action)) } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp)) {
            item {
                val new = check.tickers.flatMap { it.second.new }
                Text(
                    summary(new.size, check.tickers.size, new.minOf { it.date }, new.maxOf { it.date }, times),
                    style = MaterialTheme.typography.bodyMedium,
                    color = variant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SubHeader(stringResource(R.string.global_import_will), stringResource(R.string.global_import_after))
            }
            items(check.tickers, key = { it.first }) { (code, ready) ->
                TickerRow(code, ready, state.quotes[code], state.replaces[code], formats, times)
            }
            if (check.notInWatchlist.isEmpty() && check.alreadySaved == 0) return@LazyColumn
            item { SubHeader(stringResource(R.string.import_skipped), null) }
            items(check.notInWatchlist.toList(), key = { "skipped-${it.first}" }) { (code, count) ->
                Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp).semantics(mergeDescendants = true) {}) {
                    Text(code, style = MaterialTheme.typography.bodyLarge)
                    Text(pluralStringResource(R.plurals.global_import_not_in_watchlist, count, count), style = MaterialTheme.typography.bodyMedium, color = variant)
                }
            }
            item {
                if (check.alreadySaved > 0) {
                    Text(
                        pluralStringResource(R.plurals.global_import_already_saved, check.alreadySaved, check.alreadySaved),
                        style = MaterialTheme.typography.bodyMedium,
                        color = variant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    )
                }
                Text(
                    stringResource(R.string.global_import_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = variant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun summary(trades: Int, tickers: Int, first: LocalDate, last: LocalDate, times: TimeFormats): String {
    val count = pluralStringResource(R.plurals.global_import_trades, trades, trades)
    val forTickers = pluralStringResource(R.plurals.global_import_tickers, tickers, tickers)
    val from = times.date("dMMMyyyy", first)
    return if (first == last) {
        stringResource(R.string.global_import_summary_one_day, count, forTickers, from)
    } else {
        stringResource(R.string.global_import_summary, count, forTickers, from, times.date("dMMMyyyy", last))
    }
}

/** A list subheader: label large, on-surface-variant, with an optional label at the end. */
@Composable
private fun SubHeader(text: String, end: String?) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.weight(1f).semantics { heading() })
        end?.let { Text(it, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal), color = color) }
    }
}

/** Doop component ImportTickerRow (kind import): what the file adds to the ticker, and its shares and average after. */
@Composable
private fun TickerRow(code: String, ready: ImportCheck.Ready, quote: Quote?, replaces: Double?, formats: QuoteFormats, times: TimeFormats) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    val buys = ready.new.count { it.side == Side.BUY }
    val sells = ready.new.size - buys
    val first = times.date("dMMMyyyy", ready.new.minOf { it.date })
    val last = times.date("dMMMyyyy", ready.new.maxOf { it.date })
    val detail = listOfNotNull(
        buys.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.import_buys, it, it) },
        sells.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.import_sells, it, it) },
    ).joinToString(", ") + " · " + if (first == last) first else "$first – $last"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(code, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = variant)
            if (replaces != null && quote != null) {
                val typed = formats.exact(quote, replaces).let { n -> quote.currency?.let { stringResource(R.string.quote_price, n, it) } ?: n }
                Text(
                    stringResource(R.string.global_import_replaces, typed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            val average = ready.after.average
            if (average == null) {
                Text(stringResource(R.string.import_after_sold_out), style = MaterialTheme.typography.bodyMedium)
            } else {
                val shares = ready.after.shares
                Text(
                    pluralStringResource(R.plurals.trade_shares, if (shares == 1.0) 1 else 2, formats.shares(shares)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                quote?.let {
                    Text(stringResource(R.string.global_import_avg, formats.price(it, average)), style = MaterialTheme.typography.bodySmall, color = variant)
                }
            }
        }
    }
}

@Composable
private fun problemText(problem: GlobalImportProblem, times: TimeFormats): String = when (problem) {
    GlobalImportProblem.Unreadable -> stringResource(R.string.import_error_read)
    GlobalImportProblem.NoWatchlistRows -> stringResource(R.string.global_import_no_rows)
    GlobalImportProblem.SaveFailed -> stringResource(R.string.import_save_failed)
    is GlobalImportProblem.BelowZero ->
        stringResource(R.string.global_import_below_zero, problem.trade.code, times.date("dMMMyyyy", problem.trade.date))
    is GlobalImportProblem.Future ->
        stringResource(R.string.global_import_future, problem.trade.code, times.date("dMMMyyyy", problem.trade.date))
    is GlobalImportProblem.Csv -> csvErrorText(problem.error)
}
