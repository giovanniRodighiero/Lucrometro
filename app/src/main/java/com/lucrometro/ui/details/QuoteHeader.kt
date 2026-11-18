package com.lucrometro.ui.details

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.ui.watchlist.ChangeBadge
import com.lucrometro.ui.watchlist.QuoteFormats
import com.lucrometro.ui.watchlist.formatTime
import java.util.Date

/**
 * Doop component QuoteHeader (decision 0023): the name, the price on its own row, then the day change
 * pill on the left and the stamp on the right. The stamp wraps below the pill when both do not fit
 * (large font scale). Stale (a saved quote, last fetch failed or from an earlier session): muted
 * price, and "Stale since HH:mm" in the tertiary colour as the stamp.
 */
@Composable
fun QuoteHeader(entry: WatchlistEntry, formats: QuoteFormats, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val saved = entry.saved
    val stale = entry.stale
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (saved == null) {
            Text(
                stringResource(R.string.quote_fetch_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }
        val quote = saved.quote
        quote.name?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            formats.price(quote),
            style = MaterialTheme.typography.displaySmall,
            color = if (stale) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            quote.dayChangePercent?.let { percent ->
                val diff = quote.previousClose?.let { quote.price - it }
                ChangeBadge(
                    percent,
                    formats,
                    Modifier.padding(end = 8.dp),
                    absolute = diff?.let { formats.amountChange(quote, it) },
                )
            }
            Text(
                if (stale) {
                    stringResource(R.string.quote_stale_since, formatTime(context, saved.fetchedAt))
                } else {
                    quoteStamp(context, quote.marketTimeEpoch * 1000)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = if (stale) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** The quote's own time: "Today, 14:32", or the session day and time when it is not from today. */
@Composable
private fun quoteStamp(context: Context, epochMillis: Long): String {
    val time = DateFormat.getTimeFormat(context).format(Date(epochMillis))
    return if (DateUtils.isToday(epochMillis)) {
        stringResource(R.string.quote_stamp_today, time)
    } else {
        val day = DateUtils.formatDateTime(
            context,
            epochMillis,
            DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL,
        )
        stringResource(R.string.quote_stamp_day, day, time)
    }
}
