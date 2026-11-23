package com.lucrometro.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucrometro.R
import com.lucrometro.data.HistoryRange
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.data.Trade
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.ChangeDirection
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.common.WalletButton
import com.lucrometro.ui.common.changeDirection
import com.lucrometro.ui.common.changeLabel
import com.lucrometro.ui.common.color
import com.lucrometro.ui.common.rememberQuoteFormats
import com.lucrometro.ui.common.rememberTimeFormats
import kotlin.math.pow

/**
 * Details of one ticker (decision 0023): top bar with the market marker and auto-refresh (decision 0035), quote header, readout, chart (with the
 * scrub, decision 0028), range bar and the owner's position (decision 0033).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    code: String,
    private: Boolean,
    onPrivateChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenTrades: () -> Unit,
    viewModel: DetailsViewModel = viewModel(key = code, factory = DetailsViewModel.factory(code)),
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val market by viewModel.market.collectAsStateWithLifecycle()
    val positionState by viewModel.position.collectAsStateWithLifecycle()
    val position = positionState?.average
    var editingPosition by rememberSaveable { mutableStateOf(false) }
    val autoRefresh by viewModel.autoRefresh.collectAsStateWithLifecycle()
    var showingMarket by rememberSaveable { mutableStateOf(false) }
    val now = rememberNowSeconds()
    // Hidden when the hours are unknown or the chart could not be fetched (decisions 0023, 0035).
    val shownMarket = market?.takeIf { chart != ChartState.Failed }
    val status = shownMarket?.let { marketStatus(now, it.session, it.zone) }
    // Auto-refresh stops when the screen or the app stops (decision 0035), and when its icon is hidden.
    LifecycleStartEffect(viewModel) { onStopOrDispose { viewModel.setAutoRefresh(false) } }
    LaunchedEffect(status == null) { if (status == null) viewModel.setAutoRefresh(false) }
    val formats = rememberQuoteFormats()
    val times = rememberTimeFormats()
    // Once per opening of the screen: saved with the composition, so a rotation does not count as opening it again.
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!opened) {
            opened = true
            viewModel.onOpened()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                // One line: the market status keeps its width, a long symbol is cut with an ellipsis (decision 0032).
                title = { Text(code, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    if (status != null) {
                        val open = status is MarketStatus.Open
                        MarketMarker(open, onClick = { showingMarket = true })
                        AutoRefreshButton(on = autoRefresh.on, enabled = open, onToggle = { viewModel.setAutoRefresh(!autoRefresh.on) })
                    }
                    // Stays when the marker and sync are hidden (chart failed).
                    WalletButton(private, onPrivateChange)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        // Scrolls: at a large font scale the header, chart and range bar do not fit on one screen.
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            val quote = entry?.saved?.quote
            entry?.let { QuoteHeader(it, formats, times, fallbackZone = market?.zone) }
            // The chart needs the ticker's currency and decimals, so it waits for the saved quote.
            if (quote != null) {
                // The halo: polling runs and the 1D chart (what a poll refreshes) is shown.
                val live = autoRefresh.on && status is MarketStatus.Open && range == HistoryRange.DAY_1
                // Private only, and only while shares are held (decision 0043).
                val lastBuy = positionState?.holding?.lastBuy?.takeIf { private && positionState?.soldOut == false }
                ChartSection(
                    code, quote, position, lastBuy, chart, range, formats, times, live,
                    viewModel::selectRange, viewModel::retry, viewModel::onScrubbing,
                )
                positionState?.let {
                    PositionCard(
                        it, private, quote, formats, times,
                        onEdit = { editingPosition = true },
                        onOpenTrades = onOpenTrades,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                if (showingMarket && shownMarket != null && status != null) {
                    MarketSheet(code, shownMarket, status, now, quote, autoRefresh, times, onClose = { showingMarket = false })
                }
                if (editingPosition) {
                    PositionSheet(positionState?.typed, quote, formats, viewModel::savePosition, onClose = { editingPosition = false })
                }
            }
        }
    }
}

/** Readout, chart and range bar; loading, empty and failed states below the readout. */
@Composable
private fun ChartSection(
    code: String,
    quote: Quote,
    position: Position?,
    lastBuy: Trade?,
    chart: ChartState,
    range: HistoryRange,
    formats: QuoteFormats,
    times: TimeFormats,
    live: Boolean,
    onSelectRange: (HistoryRange) -> Unit,
    onRetry: () -> Unit,
    onScrubbing: (Boolean) -> Unit,
) {
    val data = (chart as? ChartState.Ready)?.data
    // The point under the finger while scrubbing; forgotten when another range or a new load replaces the data.
    var scrubIndex by remember(data) { mutableStateOf<Int?>(null) }
    val scrubbing = scrubIndex != null
    LaunchedEffect(scrubbing) { onScrubbing(scrubbing) }
    val scrub = data?.scrubAt(scrubIndex)
    val scrubDate = if (data != null && scrub != null) scrubLabel(data, scrub.point, times) else null
    val buyMark = if (data != null && lastBuy != null) tradeFraction(data, lastBuy.date, lastBuy.time)?.let { BuyMark(it, lastBuy.price) } else null
    val buyReadout = if (buyMark != null && lastBuy != null) lastBuyReadout(lastBuy, quote, formats, times) else null
    val onBuy = buyMark != null && scrubIndex == data?.points?.size

    if (onBuy && buyReadout != null && lastBuy != null) {
        // On the last buy: when and what, and the change from it to now.
        val change = quote.price - lastBuy.price
        ChartReadout(label = buyReadout.first, value = buyReadout.second, scrubbing = true) {
            ChangeBadge(change / lastBuy.price * 100, formats, absolute = formats.amountChange(quote, change))
        }
    } else if (scrub != null && scrubDate != null) {
        // Scrub mode: the date and price at the finger, and the change from the range start (1D: the previous close).
        ChartReadout(label = scrubDate, value = formats.price(quote, scrub.point.price), scrubbing = true) {
            scrub.percent?.let { ChangeBadge(it, formats, absolute = formats.amountChange(quote, scrub.change)) }
        }
    } else {
        SummaryReadout(data, quote, range, formats)
    }

    when (chart) {
        ChartState.Loading -> Box(Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ChartState.Empty -> ChartMessage(
            title = stringResource(R.string.chart_empty_title),
            body = stringResource(R.string.chart_empty_body),
            retryLabel = R.string.refresh,
            onRetry = onRetry,
        )
        ChartState.Failed -> ChartMessage(
            title = stringResource(R.string.chart_failed_title),
            body = stringResource(R.string.chart_failed_body),
            icon = R.drawable.ic_cloud_off,
            onRetry = onRetry,
        )
        is ChartState.Ready -> {
            val points = chart.data.points
            val layout = remember(chart.data, times) { chartLayout(chart.data, times) }
            val percent = chart.data.changePercent
            PriceChart(
                points = points,
                layout = layout,
                baseline = chart.data.baseline,
                color = (percent?.let { changeDirection(it) } ?: ChangeDirection.Flat).color(),
                formatPrice = { formats.number(quote, it) },
                scrubIndex = scrubIndex,
                onScrub = { scrubIndex = it },
                scrubDescription = if (scrub != null && scrubDate != null) {
                    val value = formats.price(quote, scrub.point.price)
                    scrub.percent?.let { stringResource(R.string.chart_scrub_description, scrubDate, value, changeLabel(it, formats)) }
                        ?: stringResource(R.string.chart_scrub_description_no_change, scrubDate, value)
                } else {
                    null
                },
                stepLabels = StepLabels(
                    stringResource(R.string.chart_action_previous),
                    stringResource(R.string.chart_action_next),
                    stringResource(R.string.chart_action_latest),
                ),
                minTickStep = 10.0.pow(-(quote.priceDecimals ?: 2)),
                description = stringResource(
                    R.string.chart_description,
                    code,
                    stringResource(range.readoutLabel),
                    formats.price(quote, chart.data.baseline ?: points.first().price),
                    formats.price(quote, points.last().price),
                    percent?.let { changeLabel(it, formats) }.orEmpty(),
                ) + positionDescription(position, points, quote, formats) +
                    (buyReadout?.takeIf { buyMark != null }?.let { ". ${it.first}, ${it.second}" }.orEmpty()),
                average = position?.let {
                    AverageMark(it.averagePrice, stringResource(R.string.chart_average), formats.number(quote, it.averagePrice))
                },
                lastBuy = buyMark,
                live = live,
            )
        }
    }
    RangeBar(HistoryRange.entries, range, onSelectRange, Modifier.padding(top = 8.dp))
}

/** The sync icon (decision 0035): plain when off, tonal when on, disabled (38%) when the market is closed. */
@Composable
private fun AutoRefreshButton(on: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val description = stringResource(
        when {
            !enabled -> R.string.auto_refresh_unavailable
            on -> R.string.auto_refresh_stop
            else -> R.string.auto_refresh_start
        },
    )
    // One button whose colours change, not two buttons: TalkBack keeps its focus on it when it is toggled.
    IconButton(
        onClick = onToggle,
        enabled = enabled,
        colors = if (on) IconButtonDefaults.filledTonalIconButtonColors() else IconButtonDefaults.iconButtonColors(),
    ) {
        Icon(painterResource(R.drawable.ic_sync), description)
    }
}

/** "Your last buy · 6 Nov, 10:41" and "10 shares at 42.85 EUR" (decision 0042); the time only when known. */
@Composable
private fun lastBuyReadout(trade: Trade, quote: Quote, formats: QuoteFormats, times: TimeFormats): Pair<String, String> {
    val day = times.date("dMMM", trade.date)
    val label = stringResource(R.string.chart_last_buy, trade.time?.let { "$day, ${times.time(it)}" } ?: day)
    val shares = pluralStringResource(R.plurals.trade_shares, if (trade.quantity == 1.0) 1 else 2, formats.shares(trade.quantity))
    return label to stringResource(R.string.chart_last_buy_value, shares, price(quote, formats.exact(quote, trade.price)))
}

/** TalkBack: the average and where it is (decision 0033). */
@Composable
private fun positionDescription(position: Position?, points: List<ChartPoint>, quote: Quote, formats: QuoteFormats): String {
    if (position == null) return ""
    val average = price(quote, formats.exact(quote, position.averagePrice))
    val prices = points.map { it.price }
    val where = when {
        position.averagePrice < prices.min() -> R.string.chart_average_below
        position.averagePrice > prices.max() -> R.string.chart_average_above
        else -> R.string.chart_average_on
    }
    return stringResource(where, average)
}

/**
 * The readout when not scrubbing. 1D: the previous close, no pill (the header already has the day change). Other
 * ranges: where they start, and the change. Empty while the chart loads, so the layout does not move.
 */
@Composable
private fun SummaryReadout(data: ChartData?, quote: Quote, range: HistoryRange, formats: QuoteFormats) {
    val previousClose = data?.baseline
    ChartReadout(
        label = stringResource(if (previousClose != null) R.string.readout_previous_close else range.readoutLabel),
        value = when {
            data == null -> ""
            previousClose != null -> formats.price(quote, previousClose)
            else -> stringResource(R.string.readout_from, formats.price(quote, data.fromPrice))
        },
    ) {
        val percent = data?.changePercent
        if (data != null && percent != null && previousClose == null) {
            ChangeBadge(percent, formats, absolute = formats.amountChange(quote, data.change))
        }
    }
}

/** The chart area when there is nothing to draw: a card with a title, a line, and optionally an icon and Retry (PRD 4). */
@Composable
private fun ChartMessage(
    title: String,
    body: String,
    icon: Int? = null,
    retryLabel: Int = R.string.retry,
    onRetry: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().height(ChartHeight).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            icon?.let { Icon(painterResource(it), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text(stringResource(retryLabel)) }
            }
        }
    }
}
