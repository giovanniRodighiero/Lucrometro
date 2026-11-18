package com.lucrometro.ui.details

import com.lucrometro.data.TradingPeriod
import java.time.DayOfWeek
import java.time.Instant
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

private fun opensOn(date: java.time.LocalDate, at: LocalTime, today: java.time.LocalDate): MarketStatus =
    if (date == today) MarketStatus.OpensToday(at) else MarketStatus.OpensOn(date.dayOfWeek, at)
