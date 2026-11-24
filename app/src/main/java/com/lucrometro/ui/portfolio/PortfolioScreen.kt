package com.lucrometro.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucrometro.R
import com.lucrometro.data.HoldingLine
import com.lucrometro.data.Portfolio
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.YearGrowth
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.MainNavBar
import com.lucrometro.ui.common.MainTab
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.common.WalletButton
import com.lucrometro.ui.common.changeLabel
import com.lucrometro.ui.common.rememberQuoteFormats
import com.lucrometro.ui.common.rememberTimeFormats
import com.lucrometro.ui.common.signedMoney

private const val EUR = "EUR"

/**
 * The Portfolio tab (decisions 0049, 0050, 0051): the total card, growth by year, and in private the composition, the
 * tickers not counted and the realized line. Public shows percentages only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel,
    private: Boolean,
    onPrivateChange: (Boolean) -> Unit,
    onOpenTicker: (String) -> Unit,
    onOpenYear: (Int) -> Unit,
    onSelectTab: (MainTab) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onShown() }
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val times = rememberTimeFormats()
    val formats = rememberQuoteFormats()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.portfolio_title)) },
                actions = {
                    state?.updatedAt?.let {
                        Text(
                            stringResource(R.string.watchlist_updated, times.phoneStamp(it)),
                            modifier = Modifier.padding(end = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    WalletButton(private, onPrivateChange)
                },
            )
        },
        bottomBar = { MainNavBar(MainTab.Portfolio, onSelectTab) },
    ) { padding ->
        val current = state ?: return@Scaffold // loading from storage: blank for a moment, no empty-state flash
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
            ) {
                content(current, private, formats, times, onOpenTicker, onOpenYear)
            }
        }
    }
}

private fun LazyListScope.content(
    state: PortfolioState,
    private: Boolean,
    formats: QuoteFormats,
    times: TimeFormats,
    onOpenTicker: (String) -> Unit,
    onOpenYear: (Int) -> Unit,
) {
    val portfolio = state.portfolio
    if (portfolio.counted.isEmpty()) {
        item { EmptyCard() }
        if (private) notCounted(portfolio, onOpenTicker)
        return
    }
    item { SummaryCard(portfolio, private, formats, times) }
    item { SectionHeader(stringResource(R.string.growth_title), stringResource(R.string.growth_subtitle)) }
    val loading = state.loading
    if (loading != null) {
        item { GrowthLoading(loading) }
    } else {
        items(portfolio.years, key = { "year-${it.year}" }) { year ->
            YearRow(year, private, formats, times, onClick = { onOpenYear(year.year) })
        }
        if (portfolio.missingYears.isNotEmpty()) item { MissingYears(portfolio, state.failed) }
    }
    if (!private) {
        item {
            Text(
                stringResource(R.string.portfolio_public_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            )
        }
        return
    }
    if (portfolio.holdings.isNotEmpty()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    stringResource(R.string.composition_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(
                    pluralStringResource(R.plurals.composition_count, portfolio.holdings.size, portfolio.holdings.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(portfolio.holdings, key = { "holding-${it.code}" }) { HoldingRow(it, formats, onClick = { onOpenTicker(it.code) }) }
    }
    notCounted(portfolio, onOpenTicker)
    portfolio.realized?.let { realized -> item { RealizedLine(realized, formats) } }
}

/** Tickers left out of the total: typed average only (0049), or another currency (0049). */
private fun LazyListScope.notCounted(portfolio: Portfolio, onOpenTicker: (String) -> Unit) {
    if (portfolio.averageOnly.isEmpty() && portfolio.otherCurrency.isEmpty()) return
    item {
        Text(
            stringResource(R.string.not_counted_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp).semantics { heading() },
        )
    }
    for ((entries, note) in listOf(
        portfolio.averageOnly to R.string.not_counted_average_only,
        portfolio.otherCurrency to R.string.not_counted_other_currency,
    )) {
        if (entries.isEmpty()) continue
        item {
            Text(
                stringResource(note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp),
            )
        }
        items(entries, key = { "not-counted-${it.code}" }) { TickerLink(it, onClick = { onOpenTicker(it.code) }) }
    }
}

@Composable
private fun EmptyCard() {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.portfolio_empty_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.portfolio_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Doop component PortfolioSummary rev 3: private with the € figures, public with the percentages only. */
@Composable
private fun SummaryCard(portfolio: Portfolio, private: Boolean, formats: QuoteFormats, times: TimeFormats) {
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
            if (private) {
                Text(stringResource(R.string.portfolio_total_value), style = label, color = variant)
                Text(formats.money(portfolio.value, EUR), style = MaterialTheme.typography.displaySmall)
            } else {
                Text(stringResource(R.string.portfolio_yours), style = label, color = variant)
                Text(
                    pluralStringResource(R.plurals.portfolio_holdings, portfolio.holdings.size, portfolio.holdings.size),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        portfolio.dayChange?.let { change ->
            Row(
                Modifier.padding(top = 8.dp).heightIn(min = 32.dp).semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val percent = portfolio.dayChangePercent
                if (private) {
                    ChangeBadge(percent, formats, absolute = formats.moneyChange(change))
                } else if (percent != null) {
                    ChangeBadge(percent, formats)
                }
                Text(stringResource(R.string.portfolio_today), style = label, color = variant)
            }
        }
        val since = portfolio.staleSince
        if (portfolio.staleCount > 0 && since != null) {
            Text(
                pluralStringResource(R.plurals.portfolio_stale, portfolio.staleCount, portfolio.staleCount, times.phoneStamp(since)),
                style = label,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        HorizontalDivider(Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (private) {
                // Its own width, not half the card: the badge beside it needs the rest to stay on one line.
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(stringResource(R.string.portfolio_invested), style = label, color = variant)
                    Text(formats.money(portfolio.invested, EUR), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(stringResource(R.string.position_since_average), style = label, color = variant)
                Spacer(Modifier.height(2.dp))
                val percent = portfolio.sinceBoughtPercent
                if (private) {
                    ChangeBadge(percent, formats, absolute = formats.moneyChange(portfolio.sinceBought))
                } else if (percent != null) {
                    ChangeBadge(percent, formats)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** One progress indicator for the whole section while the year ends are fetched (decision 0052). */
@Composable
private fun GrowthLoading(progress: Float) {
    val description = stringResource(R.string.growth_loading)
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
            .semantics { contentDescription = description },
    )
}

/** Doop component YearGrowthRow: private shows amount and % and opens the year detail; public shows the % only. */
@Composable
private fun YearRow(year: YearGrowth, private: Boolean, formats: QuoteFormats, times: TimeFormats, onClick: () -> Unit) {
    val openLabel = stringResource(R.string.growth_open_year)
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (private) Modifier.clickable(onClickLabel = openLabel, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(year.year.toString(), style = MaterialTheme.typography.bodyLarge)
            val note = when {
                year.current -> stringResource(R.string.growth_so_far)
                year.fromFirstBuy -> stringResource(R.string.growth_from, times.date("dMMM", year.start))
                else -> null
            }
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (private) {
            ChangeBadge(year.percent, formats, absolute = formats.moneyChange(year.gain))
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.offset(x = 4.dp),
            )
        } else {
            year.percent?.let { ChangeBadge(it, formats) }
        }
    }
}

/**
 * Years left out (decision 0052): those missing a ticker whose fetch failed (tried again on the next app open), and those
 * whose saved prices have no bar for the year (Yahoo has none; not fetched again).
 */
@Composable
private fun MissingYears(portfolio: Portfolio, failed: Set<String>) {
    val retried = portfolio.missingYears.filterValues { codes -> codes.any { it in failed } }
    val absent = portfolio.missingYears - retried.keys
    if (retried.isNotEmpty()) MissingNote(R.string.growth_missing, retried.keys, retried.values.flatten().filter { it in failed })
    if (absent.isNotEmpty()) MissingNote(R.string.growth_unavailable, absent.keys, absent.values.flatten())
}

@Composable
private fun MissingNote(text: Int, years: Collection<Int>, codes: Collection<String>) {
    Text(
        stringResource(text, years.joinToString(", "), codes.toSortedSet().joinToString(", ")),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Doop component HoldingRow: weight in % of the total, value in EUR and a primary bar for the weight. */
@Composable
private fun HoldingRow(holding: HoldingLine, formats: QuoteFormats, onClick: () -> Unit) {
    val openLabel = stringResource(R.string.open_details)
    val medium = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = openLabel, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(holding.code, style = medium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.portfolio_weight, formats.weight(holding.weight)), style = medium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                holding.quote.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = variant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(formats.money(holding.value, EUR), style = MaterialTheme.typography.bodyMedium, color = variant)
        }
        LinearProgressIndicator(
            progress = { (holding.weight / 100).toFloat() },
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp).clearAndSetSemantics {},
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** A not-counted ticker: code and name, opens its details. */
@Composable
private fun TickerLink(entry: WatchlistEntry, onClick: () -> Unit) {
    val openLabel = stringResource(R.string.open_details)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = openLabel, onClick = onClick)
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.code, style = MaterialTheme.typography.bodyLarge)
            entry.name?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Realized gains as one quiet line at the very end (decision 0050): text only, no badge, no colour. */
@Composable
private fun RealizedLine(realized: Double, formats: QuoteFormats) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.padding(top = 16.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val style = MaterialTheme.typography.bodySmall
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(stringResource(R.string.portfolio_realized), style = style, color = color, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.quote_price, signedMoney(realized, formats), EUR), style = style, color = color)
        }
    }
}
