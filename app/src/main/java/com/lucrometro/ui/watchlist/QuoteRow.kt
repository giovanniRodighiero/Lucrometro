package com.lucrometro.ui.watchlist

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.Quote
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.ui.theme.gainLoss
import java.text.NumberFormat
import java.util.Date
import kotlin.math.abs
import kotlin.math.round

/**
 * One watchlist row (Doop component QuoteRow): code and name on the left, price and day change on
 * the right. Three states: fresh, stale (last fetch failed, saved quote shown with its time) and
 * error (failed with no saved quote: "Couldn't fetch", no change pill).
 */
@Composable
fun QuoteRow(entry: WatchlistEntry, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val quote = entry.saved?.quote
    val stale = entry.failed && entry.saved != null
    // A plain Row, not ListItem: ListItem keeps its three-line height after the stale note goes away.
    // Layout per the Doop QuoteRow: min 72dp, 8dp/16dp padding, 16dp gap, both columns centred.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.code, style = MaterialTheme.typography.titleMedium)
            quote?.name?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (stale) {
                Text(
                    stringResource(R.string.quote_stale_since, formatTime(context, entry.saved.fetchedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            when {
                quote != null -> {
                    Text(
                        formatPrice(quote),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (stale) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    quote.dayChangePercent?.let { ChangeBadge(it) }
                }
                entry.failed -> Text(
                    stringResource(R.string.quote_fetch_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                // Not fetched yet (refresh still running).
                else -> Text(
                    stringResource(R.string.quote_pending),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A change in percent as a filled tonal pill (Doop component ChangeBadge, decision 0007): arrow plus
 * signed value on a gain, loss or neutral container. The sign and arrow carry the meaning too, never
 * the colour alone. Direction follows the value as shown, so a rounded 0.00% is flat.
 */
@Composable
fun ChangeBadge(percent: Double, modifier: Modifier = Modifier) {
    val rounded = round(percent * 100) / 100
    val colors = MaterialTheme.gainLoss
    val (container, content, icon) = when {
        rounded > 0 -> Triple(colors.gainContainer, colors.onGainContainer, R.drawable.ic_change_up)
        rounded < 0 -> Triple(colors.lossContainer, colors.onLossContainer, R.drawable.ic_change_down)
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.drawable.ic_change_flat,
        )
    }
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(PaddingValues(start = 6.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            // Doop: body-medium size, medium weight, tabular figures.
            Text(formatChange(rounded), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

/** "14.86 EUR": the source's decimals (2 if unknown), in the device locale, in the ticker's own currency. */
private fun formatPrice(quote: Quote): String {
    val decimals = quote.priceDecimals ?: 2
    val number = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(quote.price)
    return quote.currency?.let { "$number $it" } ?: number
}

/** "+1.24%", "-0.38%", "0.00%". */
private fun formatChange(rounded: Double): String {
    val number = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(abs(rounded))
    val sign = when {
        rounded > 0 -> "+"
        rounded < 0 -> "-"
        else -> ""
    }
    return "$sign$number%"
}

/** Time of day in the user's 12/24h setting; with the date too when it is not today. */
internal fun formatTime(context: Context, epochMillis: Long): String =
    if (DateUtils.isToday(epochMillis)) {
        DateFormat.getTimeFormat(context).format(Date(epochMillis))
    } else {
        DateUtils.formatDateTime(
            context,
            epochMillis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
        )
    }
