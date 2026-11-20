package com.lucrometro.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.Quote
import com.lucrometro.ui.common.TimeFormats
import java.time.Instant
import java.time.ZoneId

/**
 * The market details sheet behind the top bar's marker (decision 0035): the exchange, the full status line (decision
 * 0032), the session hours, the last quote, the delay and the auto-refresh state. A row the source gives no data for
 * is left out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSheet(
    code: String,
    market: MarketInfo,
    status: MarketStatus,
    now: Long,
    quote: Quote?,
    autoRefresh: AutoRefresh,
    times: TimeFormats,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
            Text(
                market.exchangeName ?: quote?.exchange ?: code,
                style = MaterialTheme.typography.titleLarge,
            )
            Row(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarketDot(status is MarketStatus.Open)
                Text(
                    marketStatusText(status, now, market, times),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetRow(stringResource(R.string.market_sheet_hours), sessionHours(market, times))
                quote?.let { SheetRow(stringResource(R.string.market_sheet_last_quote), times.marketStamp(it.marketTimeEpoch, market.zone)) }
                market.delayMinutes?.let {
                    SheetRow(stringResource(R.string.market_sheet_delay), pluralStringResource(R.plurals.market_sheet_delay_minutes, it, it))
                }
                SheetRow(
                    stringResource(R.string.market_sheet_auto_refresh),
                    when {
                        !autoRefresh.on -> stringResource(R.string.auto_refresh_off)
                        autoRefresh.checkedAt == null -> stringResource(R.string.auto_refresh_every)
                        else -> stringResource(
                            R.string.auto_refresh_every_checked,
                            times.format(times.timeSkeleton, autoRefresh.checkedAt / 1000, ZoneId.systemDefault()),
                        )
                    },
                )
            }
        }
    }
}

/** "09:00 – 17:30" in the exchange's clock, with its zone name when the phone's clock reads differently (decision 0032). */
@Composable
private fun sessionHours(market: MarketInfo, times: TimeFormats): String {
    val (start, end) = market.session.startEpoch to market.session.endEpoch
    val range = stringResource(
        R.string.market_sheet_hours_range,
        times.format(times.timeSkeleton, start, market.zone),
        times.format(times.timeSkeleton, end, market.zone),
    )
    val instant = Instant.ofEpochSecond(start)
    val sameClock = market.zone.rules.getOffset(instant) == ZoneId.systemDefault().rules.getOffset(instant)
    return if (sameClock) range else "$range ${times.zoneName(start, market.zone)}"
}

@Composable
private fun SheetRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
