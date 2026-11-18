package com.lucrometro.ui.details

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucrometro.R
import com.lucrometro.data.HistoryRange
import com.lucrometro.data.Quote
import com.lucrometro.ui.theme.gainLoss
import com.lucrometro.ui.watchlist.ChangeBadge
import com.lucrometro.ui.watchlist.QuoteFormats
import com.lucrometro.ui.watchlist.rememberQuoteFormats
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.pow
import kotlin.math.round

/**
 * Details of one ticker (decision 0023): top bar with the market status, quote header, readout, chart and range
 * bar. Not built yet: the scrub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    code: String,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = viewModel(key = code, factory = DetailsViewModel.factory(code)),
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val market by viewModel.market.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(code) },
                actions = {
                    // Hidden when the hours are unknown or the chart could not be fetched (decision 0023).
                    market?.takeIf { chart != ChartState.Failed }?.let { MarketStatusLabel(it) }
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
            val formats = rememberQuoteFormats()
            val quote = entry?.saved?.quote
            entry?.let { QuoteHeader(it, formats) }
            // The chart needs the ticker's currency and decimals, so it waits for the saved quote.
            if (quote != null) {
                ChartSection(code, quote, chart, range, formats, onSelectRange = viewModel::selectRange, onRetry = viewModel::retry)
            }
        }
    }
}

/** Readout, chart and range bar; loading, empty and failed states below the readout. */
@Composable
private fun ChartSection(
    code: String,
    quote: Quote,
    chart: ChartState,
    range: HistoryRange,
    formats: QuoteFormats,
    onSelectRange: (HistoryRange) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.gainLoss
    val data = (chart as? ChartState.Ready)?.data
    val percent = data?.changePercent
    val rounded = percent?.let { round(it * 100) / 100 }
    val direction = when {
        rounded == null || rounded == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        rounded > 0 -> colors.gain
        else -> colors.loss
    }
    // 1D: the previous close, no pill (the header already has the day change). Other ranges: where they start, and the change.
    val previousClose = data?.baseline
    ChartReadout(
        label = stringResource(if (previousClose != null) R.string.readout_previous_close else range.readoutLabel),
        value = when {
            data == null -> ""
            previousClose != null -> formats.price(quote, previousClose)
            else -> stringResource(R.string.readout_from, formats.price(quote, data.fromPrice))
        },
    ) {
        if (data != null && percent != null && previousClose == null) {
            ChangeBadge(percent, formats, absolute = formats.amountChange(quote, data.change))
        }
    }
    when (chart) {
        ChartState.Loading -> Box(Modifier.fillMaxWidth().height(324.dp), contentAlignment = Alignment.Center) {
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
            val context = LocalContext.current
            val locale = LocalConfiguration.current.locales[0]
            val is24Hour = DateFormat.is24HourFormat(context)
            val layout = remember(chart.data, locale, is24Hour) { chartLayout(chart.data, locale, is24Hour) }
            val first = chart.data.points.first().price
            val last = chart.data.points.last().price
            PriceChart(
                points = chart.data.points,
                layout = layout,
                baseline = chart.data.baseline,
                color = direction,
                formatPrice = { formats.number(quote, it) },
                minTickStep = 10.0.pow(-(quote.priceDecimals ?: 2).toDouble()),
                description = stringResource(
                    R.string.chart_description,
                    code,
                    stringResource(range.readoutLabel),
                    formats.price(quote, chart.data.baseline ?: first),
                    formats.price(quote, last),
                    when {
                        rounded == null -> ""
                        rounded > 0 -> stringResource(R.string.change_up, formats.changeMagnitude(rounded))
                        rounded < 0 -> stringResource(R.string.change_down, formats.changeMagnitude(rounded))
                        else -> stringResource(R.string.change_flat, formats.changeMagnitude(rounded))
                    },
                ),
            )
        }
    }
    RangeBar(DETAILS_RANGES, range, onSelectRange, Modifier.padding(top = 8.dp))
}

/** "Open · closes 17:30" / "Closed · opens Mon 09:00" with a filled (open) or hollow (closed) dot (Doop DetailsTopBar). */
@Composable
private fun MarketStatusLabel(market: MarketInfo) {
    val now by produceState(System.currentTimeMillis() / 1000, market) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis() / 1000
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time = remember(locale, is24Hour) {
        val skeleton = if (is24Hour) "Hm" else "hm"
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }
    val status = marketStatus(now, market.session, market.zone)
    val open = status is MarketStatus.Open
    val text = when (status) {
        is MarketStatus.Open -> stringResource(R.string.market_open_closes, time.format(status.closesAt))
        is MarketStatus.OpensToday -> stringResource(R.string.market_opens_today, time.format(status.at))
        is MarketStatus.OpensOn -> stringResource(
            R.string.market_opens_on,
            status.day.getDisplayName(TextStyle.SHORT, locale),
            time.format(status.at),
        )
    }
    Row(
        modifier = Modifier.padding(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The text carries the meaning; the dot only supports it.
        Box(
            Modifier.size(8.dp).then(
                if (open) {
                    Modifier.background(MaterialTheme.gainLoss.gain, CircleShape)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            ),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Box(Modifier.fillMaxWidth().height(324.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
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
