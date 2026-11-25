package com.lucrometro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YahooQuoteSourceTest {
    /** Answers the cookie and crumb calls; chart calls get the next queued response. */
    private class FakeHttp(vararg chart: HttpResult) : HttpClient {
        private val queue = ArrayDeque(chart.toList())
        val chartUrls = mutableListOf<String>()
        var sessions = 0

        override fun get(url: String, headers: Map<String, String>): HttpResult = when {
            url.startsWith("https://fc.yahoo.com") -> {
                sessions++
                HttpResult(404, "", listOf("A3=s$sessions; Path=/"))
            }
            url.contains("/getcrumb") -> HttpResult(200, "cr/umb$sessions", emptyList())
            else -> {
                chartUrls += url
                queue.removeFirst()
            }
        }
    }

    private fun ok(meta: String) =
        HttpResult(200, """{"chart":{"result":[{"meta":$meta}],"error":null}}""", emptyList())

    private val eni = ok("""{"symbol":"ENI.MI","regularMarketPrice":14.86,"currency":"EUR","regularMarketTime":1}""")


    @Test fun missingPriceIsNoPrice() {
        val r = YahooQuoteSource(FakeHttp(ok("""{"symbol":"X.MI","regularMarketTime":1}"""))).quote("X.MI")
        assertTrue(r.exceptionOrNull() is NoPriceException)
        val empty = HttpResult(200, """{"chart":{"result":[],"error":null}}""", emptyList())
        assertTrue(YahooQuoteSource(FakeHttp(empty)).quote("X.MI").exceptionOrNull() is NoPriceException)
    }

    @Test fun expiredSessionIsRenewedAndRetriedOnce() {
        val http = FakeHttp(eni, HttpResult(401, "Invalid Crumb", emptyList()), eni)
        val source = YahooQuoteSource(http)
        source.quote("ENI.MI").getOrThrow() // opens session 1
        assertEquals(14.86, source.quote("ENI.MI").getOrThrow().price, 0.0) // 401, then session 2
        assertEquals(2, http.sessions)
        assertTrue(http.chartUrls.last().contains("crumb=cr%2Fumb2"))
    }

    @Test fun freshSessionRejectedIsNotRetried() {
        val http = FakeHttp(HttpResult(401, "Invalid Crumb", emptyList()))
        assertTrue(YahooQuoteSource(http).quote("ENI.MI").isFailure)
        assertEquals(1, http.chartUrls.size)
    }

    // A 429 is not an expired session: renewing it would turn one refused request into three.
    @Test fun rateLimitKeepsTheSession() {
        val http = FakeHttp(eni, HttpResult(429, "Too Many Requests", emptyList()), eni)
        val source = YahooQuoteSource(http)
        source.quote("ENI.MI").getOrThrow()
        assertTrue(source.quote("ENI.MI").exceptionOrNull() is RateLimitedException)
        source.quote("ENI.MI").getOrThrow()
        assertEquals(1, http.sessions)
    }

    private fun candles(adjclose: String?) = HttpResult(
        200,
        """{"chart":{"result":[{"meta":{},"timestamp":[1,2,3],"indicators":{"quote":[{"close":[10.0,null,12.0]}]""" +
            (adjclose?.let { ""","adjclose":[{"adjclose":$it}]""" } ?: "") + "}}],\"error\":null}}",
        emptyList(),
    )

    @Test fun historyCarriesAdjustedClosesAndDropsEmptyBars() {
        val bars = YahooQuoteSource(FakeHttp(candles("[9.5,9.8,11.5]"))).history("X.MI", HistoryRange.MONTH_1).getOrThrow().candles
        assertEquals(listOf(10.0, 12.0), bars.map { it.close })
        assertEquals(listOf(9.5, 11.5), bars.map { it.adjClose }) // aligned with the kept bars
    }

    @Test fun historyWithoutAdjustedClosesLeavesThemNull() {
        val bars = YahooQuoteSource(FakeHttp(candles(null))).history("X.MI", HistoryRange.MONTH_1).getOrThrow().candles
        assertEquals(listOf(null, null), bars.map { it.adjClose })
    }

    // Decision 0055: only a PRE market, and never an earlier day's pre-market price.
    @Test fun preMarketOnlyWhenCurrent() {
        fun answer(state: String, time: Long) = HttpResult(
            200,
            """{"quoteResponse":{"result":[{"marketState":"$state","preMarketPrice":408.95,
               "preMarketChangePercent":-4.17,"preMarketTime":$time,"regularMarketTime":100}],"error":null}}""",
            emptyList(),
        )
        fun fetch(r: HttpResult) = YahooQuoteSource(FakeHttp(r)).preMarket("SMH").getOrThrow()
        assertEquals(PreMarket(408.95, -4.17, 200), fetch(answer("PRE", 200)))
        assertEquals(null, fetch(answer("REGULAR", 200)))
        assertEquals(null, fetch(answer("PRE", 50)))
    }

    // A refused pre-market request is one request: no new session, and the chart keeps the one it has.
    @Test fun refusedPreMarketKeepsTheSessionAndIsNotRetried() {
        val http = FakeHttp(eni, HttpResult(401, "Unauthorized", emptyList()), eni)
        val source = YahooQuoteSource(http)
        source.quote("ENI.MI").getOrThrow()
        assertTrue(source.preMarket("SMH").isFailure)
        source.quote("ENI.MI").getOrThrow()
        assertEquals(1, http.sessions)
        assertEquals(3, http.chartUrls.size)
    }

    // Milan answers with a "pre" period too: without the flag it must not lead to a pre-market request.
    @Test fun preMarketHoursOnlyForTickersWithPreMarketData() {
        fun hours(flag: Boolean) = YahooQuoteSource(
            FakeHttp(
                HttpResult(
                    200,
                    """{"chart":{"result":[{"meta":{"hasPrePostMarketData":$flag,
                       "currentTradingPeriod":{"pre":{"start":100,"end":200}}},"indicators":{"quote":[{}]}}],"error":null}}""",
                    emptyList(),
                ),
            ),
        ).history("X", HistoryRange.DAY_1).getOrThrow().preSession
        assertEquals(TradingPeriod(100, 200), hours(true))
        assertEquals(null, hours(false))
    }

    // Decision 0052: a weekly bar can run past 31 Dec, so the year ends take daily bars or nothing.
    @Test fun dailyHistoryRefusesAnyOtherBarSize() {
        fun chart(granularity: String) = HttpResult(
            200,
            """{"chart":{"result":[{"meta":{"dataGranularity":"$granularity","exchangeTimezoneName":"Europe/Rome"},
               "timestamp":[1735545600],"indicators":{"quote":[{"close":[10.5]}],"adjclose":[{"adjclose":[10.1]}]}}],
               "error":null}}""",
            emptyList(),
        )
        assertTrue(YahooQuoteSource(FakeHttp(chart("1wk"))).dailyHistory("A.MI", 0).isFailure)
        val http = FakeHttp(chart("1d"))
        val daily = YahooQuoteSource(http).dailyHistory("A.MI", 1733011200).getOrThrow()
        assertEquals(listOf(Candle(1735545600, 10.5, 10.1)), daily.candles)
        assertTrue(http.chartUrls.single().contains("period1=1733011200&period2="))
        assertTrue(http.chartUrls.single().contains("&interval=1d&"))
    }
}
