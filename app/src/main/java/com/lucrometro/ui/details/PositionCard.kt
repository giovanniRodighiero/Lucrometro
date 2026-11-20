package com.lucrometro.ui.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats

/**
 * Doop component PositionCard (decision 0033), under the range bar: empty (no [position] yet, an "Add average price"
 * button), average only (average, change since it, a hint to add shares) or full (average, shares, value, change).
 * The change is measured on the [quote]'s price; with shares it is the whole position's, otherwise one share's.
 */
@Composable
fun PositionCard(
    position: Position?,
    quote: Quote,
    formats: QuoteFormats,
    times: TimeFormats,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.position_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (position != null) {
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
        if (position == null) {
            Text(
                stringResource(R.string.position_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onEdit, modifier = Modifier.offset(x = (-12).dp).padding(top = 4.dp, bottom = 8.dp)) {
                Text(stringResource(R.string.position_add))
            }
            return@Column
        }

        val shares = position.shares
        val perShare = quote.price - position.averagePrice
        val percent = perShare / position.averagePrice * 100
        val change: @Composable () -> Unit = {
            Field(stringResource(R.string.position_since_average)) {
                ChangeBadge(percent, formats, absolute = formats.amountChange(quote, perShare * (shares ?: 1.0)))
            }
        }
        val average: @Composable () -> Unit = {
            Field(stringResource(R.string.position_average), swatch = true) {
                Value(price(quote, formats.exact(quote, position.averagePrice)))
            }
        }
        if (shares != null) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Cells(average) { Field(stringResource(R.string.position_shares)) { Value(formats.shares(shares)) } }
                Cells({ Field(stringResource(R.string.position_value)) { Value(formats.price(quote, shares * quote.price)) } }, change)
            }
        } else {
            Cells(average, change)
            Text(
                stringResource(R.string.position_hint_shares),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        HorizontalDivider(Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.position_last_buy, times.date(position.lastBuy)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A price the owner typed, with the quote's currency when known. */
@Composable
internal fun price(quote: Quote, number: String): String =
    quote.currency?.let { stringResource(R.string.quote_price, number, it) } ?: number

/** Two equal columns, 16dp apart. */
@Composable
private fun Cells(start: @Composable () -> Unit, end: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) { start() }
        Column(Modifier.weight(1f)) { end() }
    }
}

/** A label over its value; [swatch] puts the chart's dotted average line before the label. */
@Composable
private fun Field(label: String, swatch: Boolean = false, value: @Composable () -> Unit) {
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

@Composable
private fun Value(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
