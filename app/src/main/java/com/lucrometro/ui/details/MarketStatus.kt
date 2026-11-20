package com.lucrometro.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
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
import java.time.ZonedDateTime

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

/** The moment [status] names (a close or an opening), in the exchange's [zone], given [now]. */
fun MarketStatus.eventAt(now: ZonedDateTime, zone: ZoneId): ZonedDateTime {
    val today = now.toLocalDate()
    return when (this) {
        is MarketStatus.Open -> today.atTime(closesAt)
        is MarketStatus.OpensToday -> today.atTime(at)
        // Always later than today: the next date with that weekday.
        is MarketStatus.OpensOn -> generateSequence(today.plusDays(1)) { it.plusDays(1) }.first { it.dayOfWeek == day }.atTime(at)
    }.atZone(zone)
}

/** Whether the label names the weekday: only for an opening that is not tomorrow in the exchange's calendar (decision 0032). */
fun MarketStatus.showsWeekday(now: ZonedDateTime, zone: ZoneId): Boolean =
    this is MarketStatus.OpensOn && eventAt(now, zone).toLocalDate() != now.toLocalDate().plusDays(1)

/**
 * [exchangeTime] (a time of day in the exchange's clock) with the same moment on the phone's clock in parentheses:
 * "09:30 EST (15:30)", or "09:30 EST (Mon 01:00)" when the phone's day differs. The exchange's time carries its zone
 * name; the phone's does not (it is taken for granted). Nothing is added when both clocks read the same (phone in
 * the exchange's zone). Decision 0032.
 */
private fun withPhoneClock(exchangeTime: String, event: ZonedDateTime, times: TimeFormats): String {
    val phone = event.withZoneSameInstant(ZoneId.systemDefault())
    if (phone.toLocalDateTime() == event.toLocalDateTime()) return exchangeTime
    val local = times.format(times.timeSkeleton, event.toEpochSecond(), phone.zone)
    val shown = if (phone.toLocalDate() == event.toLocalDate()) local else "${times.weekday(phone.dayOfWeek)} $local"
    return "$exchangeTime ${times.zoneName(event.toEpochSecond(), event.zone)} ($shown)"
}

/**
 * "Open · closes 17:30" / "Closed · opens Mon 09:00" at [now] (epoch seconds), with the phone's clock when it differs
 * (decision 0032). Shown in the market sheet since decision 0035.
 */
@Composable
fun marketStatusText(status: MarketStatus, now: Long, market: MarketInfo, times: TimeFormats): String {
    val nowInZone = Instant.ofEpochSecond(now).atZone(market.zone)
    val event = status.eventAt(nowInZone, market.zone)
    fun time(at: LocalTime) = withPhoneClock(times.time(at), event, times)
    return when (status) {
        is MarketStatus.Open -> stringResource(R.string.market_open_closes, time(status.closesAt))
        is MarketStatus.OpensToday -> stringResource(R.string.market_opens_today, time(status.at))
        // "opens_today" is the plain "Closed · opens <time>" text: used for today and for tomorrow.
        is MarketStatus.OpensOn -> if (status.showsWeekday(nowInZone, market.zone)) {
            stringResource(R.string.market_opens_on, times.weekday(status.day), time(status.at))
        } else {
            stringResource(R.string.market_opens_today, time(status.at))
        }
    }
}

/** The current time in epoch seconds, ticking every 30 s: what the market status is read at. */
@Composable
fun rememberNowSeconds(): Long {
    val now by produceState(System.currentTimeMillis() / 1000) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis() / 1000
        }
    }
    return now
}

/** Filled gain dot for open, hollow outline dot for closed: never red, which means loss (decision 0035). */
@Composable
fun MarketDot(open: Boolean) {
    Box(
        Modifier.size(8.dp).then(
            if (open) {
                Modifier.background(MaterialTheme.gainLoss.gain, CircleShape)
            } else {
                Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            },
        ),
    )
}

/** The top bar's market marker (Doop DetailsTopBar rev 5): a dot and "Open" or "Closed"; a tap opens the market sheet. */
@Composable
fun MarketMarker(open: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = stringResource(R.string.market_details), onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The word carries the meaning; the dot only supports it.
        MarketDot(open)
        Text(
            stringResource(if (open) R.string.market_open else R.string.market_closed),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
