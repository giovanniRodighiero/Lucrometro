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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * One watchlist row (Doop component QuoteRow): code and name on the left, price and day change on
 * the right. States: fresh; stale (saved quote shown with its fetch time: the last fetch failed, or it
 * predates this session); error (failed with no saved quote: "Couldn't fetch", no pill); pending (no
 * quote yet, not failed). Error and pending can't happen while add always saves a quote; they are kept
 * for tickers stored without one (planned import and holdings flows).
 */
@Composable
fun QuoteRow(entry: WatchlistEntry, formats: QuoteFormats, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val quote = entry.saved?.quote
    val stale = entry.stale && entry.saved != null
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
                        formats.price(quote),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (stale) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    quote.dayChangePercent?.let { ChangeBadge(it, formats) }
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
fun ChangeBadge(
    percent: Double,
    formats: QuoteFormats,
    modifier: Modifier = Modifier,
    /** The change in price units, shown before the percent ("+0.86 (+2.14%)"); null for the percent alone. */
    absolute: AmountChange? = null,
) {
    val roundedPercent = round(percent * 100) / 100
    // The direction follows what is shown: the percent, or the amount when only that is not zero after rounding.
    val rounded = if (roundedPercent == 0.0 && absolute != null) absolute.rounded else roundedPercent
    val colors = MaterialTheme.gainLoss
    val magnitude = formats.changeMagnitude(roundedPercent)
    val (container, content, icon) = when {
        rounded > 0 -> Triple(colors.gainContainer, colors.onGainContainer, R.drawable.ic_change_up)
        rounded < 0 -> Triple(colors.lossContainer, colors.onLossContainer, R.drawable.ic_change_down)
        else -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.drawable.ic_change_flat,
        )
    }
    val text = when {
        absolute != null -> when {
            rounded > 0 -> stringResource(R.string.change_up_abs, absolute.text, magnitude)
            rounded < 0 -> stringResource(R.string.change_down_abs, absolute.text, magnitude)
            else -> stringResource(R.string.change_flat_abs, absolute.text, magnitude)
        }
        rounded > 0 -> stringResource(R.string.change_up, magnitude)
        rounded < 0 -> stringResource(R.string.change_down, magnitude)
        else -> stringResource(R.string.change_flat, magnitude)
    }
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(PaddingValues(start = 6.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            // Doop: body-medium size, medium weight, tabular figures.
            Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

/** A change in price units: [rounded] (signed, to the price's decimals) and its unsigned [text]. */
class AmountChange(val rounded: Double, val text: String)

/**
 * Number formats for the list, in one locale, created once per list (not per row and recomposition).
 * The text around the numbers (currency position, sign, percent) comes from strings.xml.
 * NumberFormat is not thread-safe: use from composition only.
 */
class QuoteFormats(private val locale: Locale) {
    private val prices = HashMap<Int, NumberFormat>()
    private val change = fixed(2)

    private fun fixed(decimals: Int) = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }

    private fun priceFormat(quote: Quote): NumberFormat {
        val decimals = quote.priceDecimals ?: 2
        return prices.getOrPut(decimals) { fixed(decimals) }
    }

    /** The price with the source's decimals (2 if unknown), followed by its currency when known. */
    @Composable
    fun price(quote: Quote): String = price(quote, quote.price)

    /** [value] in the quote's decimals and currency (e.g. the first price of a range). */
    @Composable
    fun price(quote: Quote, value: Double): String {
        val number = priceFormat(quote).format(value)
        return quote.currency?.let { stringResource(R.string.quote_price, number, it) } ?: number
    }

    /** A price (e.g. an axis label) with the quote's decimals, without the currency. */
    fun number(quote: Quote, value: Double): String = priceFormat(quote).format(value)

    /** A change in price units with the quote's own decimals, rounded the way it is shown. */
    fun amountChange(quote: Quote, value: Double): AmountChange {
        val format = priceFormat(quote)
        val scale = Math.pow(10.0, format.maximumFractionDigits.toDouble())
        return AmountChange(round(value * scale) / scale, format.format(abs(value)))
    }

    /** The size of a change in percent, unsigned, 2 decimals: the sign comes from the string. */
    fun changeMagnitude(rounded: Double): String = change.format(abs(rounded))
}

@Composable
fun rememberQuoteFormats(): QuoteFormats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { QuoteFormats(locale) }
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
