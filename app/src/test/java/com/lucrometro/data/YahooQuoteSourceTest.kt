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

    private fun candles(adjclose: String?) = HttpResult(
        200,
        """{"chart":{"result":[{"meta":{},"timestamp":[1,2,3],"indicators":{"quote":[{"close":[10.0,null,12.0]}]""" +
            (adjclose?.let { ""","adjclose":[{"adjclose":$it}]""" } ?: "") + "}}],\"error\":null}}",
        emptyList(),
    )

    @Test fun historyCarriesAdjustedClosesAndDropsEmptyBars() {
        val bars = YahooQuoteSource(FakeHttp(candles("[9.5,9.8,11.5]"))).history("X.MI", HistoryRange.MONTH_1).getOrThrow()
        assertEquals(listOf(10.0, 12.0), bars.map { it.close })
        assertEquals(listOf(9.5, 11.5), bars.map { it.adjClose }) // aligned with the kept bars
    }

    @Test fun historyWithoutAdjustedClosesLeavesThemNull() {
        val bars = YahooQuoteSource(FakeHttp(candles(null))).history("X.MI", HistoryRange.MONTH_1).getOrThrow()
        assertEquals(listOf(null, null), bars.map { it.adjClose })
    }
}
