package com.lucrometro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PortfolioTest {
    private var nextId = 1L
    private fun trade(code: String, side: Side, date: String, quantity: Double, price: Double) =
        Trade(nextId++, code, side, LocalDate.parse(date), null, quantity, price)

    private fun ticker(code: String, price: Double, trades: List<Trade>, currency: String = "EUR", typed: Position? = null) =
        PortfolioTicker(
            WatchlistEntry(code, SavedQuote(Quote(code, price, currency, null, 0, previousClose = price - 1), 0)),
            trades,
            typed,
        )

    private fun end(year: Int, date: String, close: Double, adj: Double = close) = YearEnd(year, LocalDate.parse(date), close, adj)

    private val today = LocalDate.parse("2026-11-24")

    // A: bought over two years, partly sold this year. B: bought in 2024, sold out in 2025. C: average only. D: in USD.
    private val tickers = listOf(
        ticker("A.MI", 125.0, listOf(
            trade("A.MI", Side.BUY, "2024-03-12", 10.0, 100.0),
            trade("A.MI", Side.BUY, "2025-06-01", 5.0, 110.0),
            trade("A.MI", Side.SELL, "2026-02-10", 3.0, 130.0),
        )),
        ticker("B.MI", 70.0, listOf(
            trade("B.MI", Side.BUY, "2024-07-01", 20.0, 50.0),
            trade("B.MI", Side.SELL, "2025-09-15", 20.0, 60.0),
        )),
        ticker("C.MI", 10.0, emptyList(), typed = Position(9.0)),
        ticker("D", 300.0, listOf(trade("D", Side.BUY, "2025-01-10", 1.0, 250.0)), currency = "USD"),
    )

    private val years = mapOf(
        "A.MI" to TickerYears("A.MI", 2023, 2025, listOf(
            end(2023, "2023-12-29", 95.0, 90.0), end(2024, "2024-12-30", 105.0, 100.0), end(2025, "2025-12-30", 120.0, 116.0),
        )),
        "B.MI" to TickerYears("B.MI", 2023, 2025, listOf(end(2024, "2024-12-30", 55.0), end(2025, "2025-12-30", 66.0))),
    )

    // Decision 0051: the yearly amounts add up to the gain since you bought plus the realized gains.
    @Test fun yearlyGainsAddUpToSinceYouBoughtPlusRealized() {
        val p = portfolio(tickers, years, today)
        assertEquals(listOf(2026, 2025, 2024), p.years.map { it.year })
        assertEquals(p.sinceBought + p.realized!!, p.years.sumOf { it.gain }, 1e-9)
        // Realized: B sold 20 at 60 over a 50 average, A sold 3 at 130 over (10x100 + 5x110) / 15.
        assertEquals(20 * 10.0 + 3 * (130.0 - 1550.0 / 15), p.realized!!, 1e-9)
        assertEquals(listOf("A.MI"), p.counted - "B.MI")
        assertEquals(listOf("C.MI"), p.averageOnly.map { it.code })
        assertEquals(listOf("D"), p.otherCurrency.map { it.code })
        assertEquals(12 * 125.0, p.value, 1e-9)
        assertEquals(100.0, p.holdings.single().weight, 1e-9)
    }

    // Modified Dietz (decision 0052): the first year runs from the first buy, each flow weighted by the days it was in.
    @Test fun firstYearRunsFromTheFirstBuyWithDayWeightedFlows() {
        val y2024 = portfolio(tickers, years, today).years.single { it.year == 2024 }
        assertTrue(y2024.fromFirstBuy)
        assertEquals(LocalDate.parse("2024-03-12"), y2024.start)
        assertEquals(0.0, y2024.startValue, 0.0)
        assertEquals(2000.0, y2024.added, 1e-9)
        assertEquals(10 * 105.0 + 20 * 55.0, y2024.endValue, 1e-9)
        // 12 Mar to 31 Dec 2024 is 295 days; B's buy on 1 Jul was in for 184 of them.
        assertEquals(150.0 / (1000.0 + 1000.0 * 184 / 295) * 100, y2024.percent!!, 1e-9)
        // Its market return: A's adjusted closes, 2023 end to 2024 end; B has no 2023 end.
        assertEquals(100.0 / 90.0 * 100 - 100, y2024.tickers.single { it.code == "A.MI" }.marketReturn!!, 1e-9)
        assertNull(y2024.tickers.single { it.code == "B.MI" }.marketReturn)
    }

    @Test fun currentYearEndsTodayAtTheLatestPrices() {
        val y2026 = portfolio(tickers, years, today).years.first()
        assertTrue(y2026.current)
        assertFalse(y2026.fromFirstBuy)
        assertEquals(15 * 120.0, y2026.startValue, 1e-9)
        assertEquals(-3 * 130.0, y2026.added, 1e-9)
        assertEquals(12 * 125.0, y2026.endValue, 1e-9)
        // B was sold out in 2025: not in this year's detail.
        assertEquals(listOf("A.MI"), y2026.tickers.map { it.code })
        assertEquals(125.0 / 116.0 * 100 - 100, y2026.tickers.single().marketReturn!!, 1e-9)
    }

    // A ticker's missing year end leaves out exactly the years that need it, never a guess.
    @Test fun yearsMissingAPriceAreLeftOut() {
        val withoutB2024 = years + ("B.MI" to TickerYears("B.MI", 2023, 2025, listOf(end(2025, "2025-12-30", 66.0))))
        val p = portfolio(tickers, withoutB2024, today)
        assertEquals(listOf(2026), p.years.map { it.year })
        assertEquals(mapOf(2025 to setOf("B.MI"), 2024 to setOf("B.MI")), p.missingYears)
    }

    @Test fun yearEndsAreTheLastDailyBarOfEachClosedYearInTheExchangeClock() {
        val rome = ZoneId.of("Europe/Rome")
        fun bar(at: String, close: Double) = Candle(LocalDateTime.parse(at).atZone(rome).toEpochSecond(), close, close - 1)
        val history = History(
            listOf(
                bar("2024-12-02T09:00", 9.0), bar("2024-12-30T09:00", 10.0),
                bar("2025-12-30T09:00", 12.0), bar("2026-01-02T09:00", 13.0),
            ),
            exchangeZone = "Europe/Rome",
        )
        val now = LocalDateTime.parse("2026-11-24T12:00").atZone(rome).toInstant().toEpochMilli()
        val saved = yearEnds("A.MI", history, 2024, now)
        assertEquals(2024, saved.fromYear)
        assertEquals(2025, saved.throughYear)
        assertEquals(listOf(end(2024, "2024-12-30", 10.0, 9.0), end(2025, "2025-12-30", 12.0, 11.0)), saved.ends)
    }
}
