package com.lucrometro.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.ui.common.ChangeBadge
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats

/**
 * One watchlist row (Doop component QuoteRow): code and name on the left, price and day change on
 * the right. States: fresh; stale (saved quote shown with its fetch time: the last fetch failed, or it
 * predates this session); error (failed with no saved quote: "Couldn't fetch", no pill); pending (no
 * quote yet, not failed). Error and pending can't happen while add always saves a quote; they are kept
 * for tickers stored without one (planned import and holdings flows).
 */
@Composable
fun QuoteRow(entry: WatchlistEntry, formats: QuoteFormats, times: TimeFormats, modifier: Modifier = Modifier) {
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
                    stringResource(R.string.quote_stale_since, times.phoneStamp(entry.saved.fetchedAt)),
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
