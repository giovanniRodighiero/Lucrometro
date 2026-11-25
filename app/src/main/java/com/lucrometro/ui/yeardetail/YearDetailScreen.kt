package com.lucrometro.ui.yeardetail

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucrometro.R
import com.lucrometro.data.TickerYear
import com.lucrometro.data.YearGrowth
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TickerCode
import com.lucrometro.ui.common.rememberQuoteFormats
import com.lucrometro.ui.common.rememberTimeFormats
import com.lucrometro.ui.common.signedMoney

/**
 * Year detail (decision 0051), private only: the portfolio's return in [year] with its start, added and end values, then
 * each ticker held that year with its market return over the year, highest first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearDetailScreen(
    year: Int,
    onBack: () -> Unit,
    viewModel: YearDetailViewModel = viewModel(key = "year-$year", factory = YearDetailViewModel.factory(year)),
) {
    val growth by viewModel.year.collectAsStateWithLifecycle()
    val formats = rememberQuoteFormats()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(year.toString()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val current = growth ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
        ) {
            item { YearCard(current, formats) }
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)) {
                    Text(
                        stringResource(if (current.current) R.string.year_holdings_title_current else R.string.year_holdings_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(if (current.current) R.string.year_holdings_body_current else R.string.year_holdings_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            items(current.tickers, key = { it.code }) { TickerRow(it, formats) }
        }
    }
}

@Composable
private fun YearCard(growth: YearGrowth, formats: QuoteFormats) {
    val times = rememberTimeFormats()
    val label = MaterialTheme.typography.bodySmall
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
            .padding(16.dp),
    ) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.year_yours, growth.year), style = label, color = variant)
            ChangeBadge(growth.percent, formats, Modifier.padding(top = 8.dp), absolute = formats.moneyChange(growth.gain))
        }
        HorizontalDivider(Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure(times.date("dMMM", growth.start), formats.money(growth.startValue), Modifier.weight(1f))
            Figure(stringResource(R.string.year_added), signedMoney(growth.added, formats), Modifier.weight(1f))
            Figure(
                if (growth.current) stringResource(R.string.year_today) else times.date("dMMM", growth.end),
                formats.money(growth.endValue),
                Modifier.weight(1f),
            )
        }
        Text(stringResource(R.string.year_note), style = label, color = variant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A watchlist-style row: code and name, the year-end price and the market return over the year. */
@Composable
private fun TickerRow(ticker: TickerYear, formats: QuoteFormats) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            TickerCode(ticker.code, ticker.quote.assetType, MaterialTheme.typography.titleMedium)
            ticker.quote.name?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            ticker.price?.let { Text(formats.price(ticker.quote, it), style = MaterialTheme.typography.bodyLarge) }
            ticker.marketReturn?.let { ChangeBadge(it, formats) }
        }
    }
}
