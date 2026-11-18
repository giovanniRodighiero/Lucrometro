package com.lucrometro.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.TradingPeriod
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.theme.gainLoss
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Whether the exchange is open, in the exchange's own clock (decision 0023). */
sealed interface MarketStatus {
    data class Open(val closesAt: LocalTime) : MarketStatus
    data class OpensToday(val at: LocalTime) : MarketStatus
    data class OpensOn(val day: DayOfWeek, val at: LocalTime) : MarketStatus
}

/**
 * The status at [nowEpoch] (seconds) from the exchange's [session] (`currentTradingPeriod.regular`) in its [zone].
 * It walks the weekdays from the session's own day with the same opening hours, so a session fetched earlier (a screen
 * left open overnight) still gives the right answer: inside a window it is open, before one it opens then (today, or on
 * its weekday), after it the next weekday is tried. Exchange holidays are not known (Yahoo gives no calendar), so a
 * holiday reads as "opens tomorrow" until the day has passed; see the roadmap.
 */
fun marketStatus(nowEpoch: Long, session: TradingPeriod, zone: ZoneId): MarketStatus {
    val now = Instant.ofEpochSecond(nowEpoch).atZone(zone)
    val opens = Instant.ofEpochSecond(session.startEpoch).atZone(zone)
    val closes = Instant.ofEpochSecond(session.endEpoch).atZone(zone)
    val today = now.toLocalDate()
    var date = opens.toLocalDate()
    // A session is a day: two weeks of weekdays is far more than any stale session can need.
    repeat(14) {
        if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
            val windowStart = date.atTime(opens.toLocalTime()).atZone(zone)
            val windowEnd = date.atTime(closes.toLocalTime()).atZone(zone)
            if (now < windowStart) return opensOn(date, opens.toLocalTime(), today)
            if (now < windowEnd) return MarketStatus.Open(closes.toLocalTime())
        }
        date = date.plusDays(1)
    }
    return opensOn(date, opens.toLocalTime(), today)
}

private fun opensOn(date: LocalDate, at: LocalTime, today: LocalDate): MarketStatus =
    if (date == today) MarketStatus.OpensToday(at) else MarketStatus.OpensOn(date.dayOfWeek, at)

/** "Open · closes 17:30" / "Closed · opens Mon 09:00" with a filled (open) or hollow (closed) dot (Doop DetailsTopBar). */
@Composable
fun MarketStatusLabel(market: MarketInfo, times: TimeFormats) {
    val now by produceState(System.currentTimeMillis() / 1000, market) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis() / 1000
        }
    }
    val status = marketStatus(now, market.session, market.zone)
    val text = when (status) {
        is MarketStatus.Open -> stringResource(R.string.market_open_closes, times.time(status.closesAt))
        is MarketStatus.OpensToday -> stringResource(R.string.market_opens_today, times.time(status.at))
        is MarketStatus.OpensOn -> stringResource(R.string.market_opens_on, times.weekday(status.day), times.time(status.at))
    }
    Row(
        modifier = Modifier.padding(end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The text carries the meaning; the dot only supports it.
        Box(
            Modifier.size(8.dp).then(
                if (status is MarketStatus.Open) {
                    Modifier.background(MaterialTheme.gainLoss.gain, CircleShape)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            ),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
