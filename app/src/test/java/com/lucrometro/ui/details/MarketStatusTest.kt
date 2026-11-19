package com.lucrometro.ui.details

import com.lucrometro.data.TradingPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class MarketStatusTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun epoch(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(rome).toEpochSecond()

    // Wed 18 Nov 2026, 09:00-17:30 Milan.
    private val wed = TradingPeriod(epoch(2026, 11, 18, 9), epoch(2026, 11, 18, 17, 30))

    @Test fun insideTheSessionIsOpenUntilTheClose() =
        assertEquals(MarketStatus.Open(LocalTime.of(17, 30)), marketStatus(epoch(2026, 11, 18, 12), wed, rome))

    @Test fun beforeTheOpenOpensTodayAtTheStart() =
        assertEquals(MarketStatus.OpensToday(LocalTime.of(9, 0)), marketStatus(epoch(2026, 11, 18, 7), wed, rome))

    @Test fun afterTheCloseOpensTheNextDay() =
        assertEquals(
            MarketStatus.OpensOn(DayOfWeek.THURSDAY, LocalTime.of(9, 0)),
            marketStatus(epoch(2026, 11, 18, 18), wed, rome),
        )

    // Fri 20 Nov after the close, and the whole weekend, open on Monday.
    @Test fun fridayEveningAndTheWeekendOpenOnMonday() {
        val fri = TradingPeriod(epoch(2026, 11, 20, 9), epoch(2026, 11, 20, 17, 30))
        val monday = MarketStatus.OpensOn(DayOfWeek.MONDAY, LocalTime.of(9, 0))
        assertEquals(monday, marketStatus(epoch(2026, 11, 20, 20), fri, rome))
        assertEquals(monday, marketStatus(epoch(2026, 11, 21, 12), fri, rome))
        assertEquals(monday, marketStatus(epoch(2026, 11, 22, 23), fri, rome))
    }

    // If the source already points at a later session (e.g. Monday's on a Saturday), that is the next opening.
    @Test fun aLaterSessionIsTheNextOpening() {
        val mon = TradingPeriod(epoch(2026, 11, 23, 9), epoch(2026, 11, 23, 17, 30))
        assertEquals(
            MarketStatus.OpensOn(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
            marketStatus(epoch(2026, 11, 21, 12), mon, rome),
        )
    }

    // A session fetched on Wednesday still gives the right answer when the screen is read the next morning.
    @Test fun aStaleSessionStillGivesTheRightStatus() {
        assertEquals(MarketStatus.Open(LocalTime.of(17, 30)), marketStatus(epoch(2026, 11, 19, 10), wed, rome))
        assertEquals(MarketStatus.OpensToday(LocalTime.of(9, 0)), marketStatus(epoch(2026, 11, 19, 7), wed, rome))
        // Friday's session read on Monday morning.
        val fri = TradingPeriod(epoch(2026, 11, 20, 9), epoch(2026, 11, 20, 17, 30))
        assertEquals(MarketStatus.Open(LocalTime.of(17, 30)), marketStatus(epoch(2026, 11, 23, 12), fri, rome))
    }

    // The times are the exchange's, whatever zone the phone is in.
    @Test fun timesAreInTheExchangeZone() {
        val newYork = ZoneId.of("America/New_York")
        val nyse = TradingPeriod(
            LocalDateTime.of(2026, 11, 18, 9, 30).atZone(newYork).toEpochSecond(),
            LocalDateTime.of(2026, 11, 18, 16, 0).atZone(newYork).toEpochSecond(),
        )
        val noon = LocalDateTime.of(2026, 11, 18, 12, 0).atZone(newYork).toEpochSecond()
        assertEquals(MarketStatus.Open(LocalTime.of(16, 0)), marketStatus(noon, nyse, newYork))
    }

    // The phone-clock equivalent is derived from the exchange's date, across a DST gap (US already back, Europe not yet).
    @Test fun eventAtGivesTheRealMoment() {
        val newYork = ZoneId.of("America/New_York")
        val now = LocalDateTime.of(2026, 11, 20, 20, 0).atZone(newYork)
        val event = MarketStatus.OpensOn(DayOfWeek.MONDAY, LocalTime.of(9, 30)).eventAt(now, newYork)
        assertEquals(LocalDateTime.of(2026, 11, 23, 9, 30), event.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 11, 23, 15, 30), event.withZoneSameInstant(rome).toLocalDateTime())
        // 2026-03-16: the US is on summer time, Italy is not yet: 14:30 in Milan.
        val gap = MarketStatus.OpensToday(LocalTime.of(9, 30)).eventAt(LocalDateTime.of(2026, 3, 16, 8, 0).atZone(newYork), newYork)
        assertEquals(LocalDateTime.of(2026, 3, 16, 14, 30), gap.withZoneSameInstant(rome).toLocalDateTime())
    }

    // The weekday is named only when the opening is not tomorrow (Friday evening, the weekend).
    @Test fun weekdayIsShownOnlyWhenTheOpeningIsNotTomorrow() {
        fun at(d: Int, h: Int) = LocalDateTime.of(2026, 11, d, h, 0).atZone(rome)
        val tomorrow = MarketStatus.OpensOn(DayOfWeek.THURSDAY, LocalTime.of(9, 0))
        val monday = MarketStatus.OpensOn(DayOfWeek.MONDAY, LocalTime.of(9, 0))
        assertEquals(false, tomorrow.showsWeekday(at(18, 20), rome))
        assertEquals(true, monday.showsWeekday(at(20, 20), rome))
        assertEquals(true, monday.showsWeekday(at(21, 12), rome))
        // Sunday: Monday is tomorrow.
        assertEquals(false, monday.showsWeekday(at(22, 12), rome))
    }
}
