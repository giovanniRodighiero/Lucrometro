package com.lucrometro.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.pow
import kotlin.math.round

/**
 * Details of one ticker (decision 0023). Iteration 2: top bar and quote header only; the market
 * status, chart, readout and range bar come next.
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(code) },
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

/** Readout, chart and range bar; loading and failed states below the readout. */
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
    ChartReadout(
        label = stringResource(range.readoutLabel),
        value = data?.let { stringResource(R.string.readout_from, formats.price(quote, data.fromPrice)) }.orEmpty(),
    ) {
        if (data != null && percent != null) {
            ChangeBadge(percent, formats, absolute = formats.amountChange(quote, data.change))
        }
    }
    when (chart) {
        ChartState.Loading -> Box(Modifier.fillMaxWidth().height(324.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ChartState.Failed -> ChartFailed(onRetry)
        is ChartState.Ready -> {
            val first = chart.data.points.first().price
            val last = chart.data.points.last().price
            PriceChart(
                points = chart.data.points,
                color = direction,
                formatPrice = { formats.number(quote, it) },
                dateSkeleton = range.dateSkeleton,
                minTickStep = 10.0.pow(-(quote.priceDecimals ?: 2).toDouble()),
                description = stringResource(
                    R.string.chart_description,
                    code,
                    stringResource(range.readoutLabel),
                    formats.price(quote, first),
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

/** The chart area when the fetch failed (PRD 4): no chart, never a stale one, and a retry. */
@Composable
private fun ChartFailed(onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(324.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(R.drawable.ic_cloud_off), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.chart_failed_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                stringResource(R.string.chart_failed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
