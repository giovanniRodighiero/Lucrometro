package com.lucrometro.ui.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.data.Realized
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats

/**
 * Doop component PositionCard rev 10, compact layout (decisions 0033, 0040, 0042, 0047), under the range bar.
 * Public: empty ("Add average price"), avg (typed average, pencil) or avg-traded (from the trades, no pencil); hidden
 * when the trades are sold out (decision 0043). Private adds shares, value and the whole position's gain when there are
 * trades, the realized total (gross) once there are sells, and a footer that opens the trades screen. Sold out
 * (private): "No shares held" and the realized total.
 */
@Composable
fun PositionCard(
    state: PositionState,
    private: Boolean,
    quote: Quote,
    formats: QuoteFormats,
    times: TimeFormats,
    onEdit: () -> Unit,
    onOpenTrades: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val holding = state.holding
    val average = state.average
    if (!private && state.soldOut) return
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.position_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // Only a typed average is edited here; one from the trades is changed by editing them (in private).
            if (holding == null && average != null) {
                // The 48dp target reaches into the padding, so the icon lines up with the card's 16dp inset.
                IconButton(onClick = onEdit, modifier = Modifier.offset(x = 12.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        stringResource(R.string.position_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        when {
            state.soldOut -> {
                Text(
                    stringResource(R.string.position_no_shares),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                holding?.realized?.let { RealizedField(it, quote, formats, Modifier.padding(bottom = 16.dp)) }
            }
            average == null -> {
                Text(
                    stringResource(if (private) R.string.position_empty_private else R.string.position_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onEdit, modifier = Modifier.offset(x = (-12).dp).padding(top = 4.dp, bottom = 8.dp)) {
                    Text(stringResource(R.string.position_add))
                }
            }
            // Compact (decision 0047): avg, shares and value in a row of three, then "Since avg" and "Realized" halves.
            private && holding != null -> {
                val gain = (quote.price - average.averagePrice) * holding.shares
                Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AverageField(state, quote, formats, Modifier.weight(1f))
                    Field(stringResource(R.string.position_shares), Modifier.weight(1f)) {
                        Text(formats.shares(holding.shares), style = MaterialTheme.typography.bodyMedium)
                    }
                    Field(stringResource(R.string.position_value), Modifier.weight(1f)) {
                        Text(formats.price(quote, quote.price * holding.shares), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(stringResource(R.string.position_since_average), Modifier.weight(1f)) {
                        ChangeBadge(
                            (quote.price - average.averagePrice) / average.averagePrice * 100, formats,
                            absolute = formats.amountChange(quote, gain),
                        )
                    }
                    val realized = holding.realized
                    if (realized != null) RealizedField(realized, quote, formats, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
                }
            }
            else -> {
                val perShare = quote.price - average.averagePrice
                Row(Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AverageField(state, quote, formats, Modifier.weight(1f))
                    Field(stringResource(R.string.position_since_average), Modifier.weight(1f)) {
                        ChangeBadge(perShare / average.averagePrice * 100, formats, absolute = formats.amountChange(quote, perShare))
                    }
                }
            }
        }
        if (private) {
            val footer = if (holding == null) {
                stringResource(R.string.position_no_trades)
            } else {
                val lastBuy = holding.lastBuy?.let { times.date("dMMMyyyy", it.date) }.orEmpty()
                pluralStringResource(R.plurals.position_trades, holding.count, holding.count, lastBuy)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClickLabel = stringResource(R.string.position_open_trades), onClick = onOpenTrades),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tint = MaterialTheme.colorScheme.onSurfaceVariant
                Icon(painterResource(R.drawable.ic_receipt), null, Modifier.size(16.dp), tint = tint)
                Text(footer, style = MaterialTheme.typography.bodySmall, color = tint, modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = tint)
            }
        }
    }
}

/** A typed average is shown as typed; one from the trades in the quote's decimals, like a price. */
@Composable
private fun AverageField(state: PositionState, quote: Quote, formats: QuoteFormats, modifier: Modifier = Modifier) {
    val average = state.average?.averagePrice ?: return
    Field(stringResource(R.string.position_average), modifier, swatch = true) {
        Text(
            if (state.holding != null) formats.price(quote, average) else price(quote, formats.exact(quote, average)),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** A price the owner typed, with the quote's currency when known. */
@Composable
internal fun price(quote: Quote, number: String): String =
    quote.currency?.let { stringResource(R.string.quote_price, number, it) } ?: number

/** The ticker's gross realized total (decision 0047): signed amount and percent. */
@Composable
private fun RealizedField(realized: Realized, quote: Quote, formats: QuoteFormats, modifier: Modifier = Modifier) {
    Field(stringResource(R.string.position_realized), modifier) {
        ChangeBadge(realized.percent, formats, absolute = formats.amountChange(quote, realized.gain))
    }
}

/**
 * A label over its value, read together by TalkBack; [swatch] puts the chart's dotted average line before the label.
 * Values wrap, never cut (decision 0047).
 */
@Composable
private fun Field(label: String, modifier: Modifier = Modifier, swatch: Boolean = false, value: @Composable () -> Unit) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (swatch) {
                val color = MaterialTheme.colorScheme.tertiary
                Canvas(Modifier.size(14.dp, 4.dp)) {
                    drawLine(
                        color, Offset(2.dp.toPx(), center.y), Offset(13.dp.toPx(), center.y), 1.5.dp.toPx(), StrokeCap.Round,
                        PathEffect.dashPathEffect(floatArrayOf(0.1f, 4.dp.toPx())),
                    )
                }
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(2.dp))
        value()
    }
}
