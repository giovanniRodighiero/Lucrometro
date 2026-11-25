package com.lucrometro.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.AssetType
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.ui.common.AssetTypeIcon
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.common.label
import com.lucrometro.ui.common.marketZone
import java.time.ZoneId

/**
 * Doop component QuoteHeader (decision 0023): the name, the price on its own row, then the day change
 * pill on the left and the stamp on the right. The stamp wraps below the pill when both do not fit
 * (large font scale). Stale (a saved quote, last fetch failed or from an earlier session): muted
 * price, and "Stale since HH:mm" in the tertiary colour as the stamp. The quote's own time is in the exchange's
 * clock, the fetch time ("Stale since") in the phone's (decision 0031).
 */
@Composable
fun QuoteHeader(
    entry: WatchlistEntry,
    formats: QuoteFormats,
    times: TimeFormats,
    modifier: Modifier = Modifier,
    /** The exchange's zone from the chart, for a quote saved before it carried its own (schema v2). */
    fallbackZone: ZoneId? = null,
    /** Less when the pre-market strip sits right under the header. */
    bottomPadding: Dp = 16.dp,
) {
    val saved = entry.saved
    val stale = entry.stale
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
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
        NameLine(quote.name, quote.assetType)
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
                ChangeBadge(
                    percent,
                    formats,
                    Modifier.padding(end = 8.dp),
                    absolute = quote.dayChange?.let { formats.amountChange(quote, it) },
                )
            }
            Text(
                if (stale) {
                    stringResource(R.string.quote_stale_since, times.phoneStamp(saved.fetchedAt))
                } else {
                    times.marketStamp(quote.marketTimeEpoch, marketZone(quote.exchangeZone, fallbackZone))
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = if (stale) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * The name line, starting with the type icon and word when known: where the watchlist icon is explained (decision 0057).
 * It wraps (decision 0058): "ETF · name" is one text, so the next lines start under the word; the icon stays on the first.
 */
@Composable
private fun NameLine(name: String?, type: AssetType?) {
    val word = type?.let { stringResource(it.label) }
    val text = if (word != null && name != null) stringResource(R.string.asset_name, word, name) else word ?: name ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // 2dp centres the 16dp icon on the first line.
        if (type != null) AssetTypeIcon(type, Modifier.padding(top = 2.dp), described = false)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
