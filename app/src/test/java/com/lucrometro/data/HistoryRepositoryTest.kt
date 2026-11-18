package com.lucrometro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HistoryRepositoryTest {
    private class CountingHistory : QuoteSource {
        var calls = 0
        override fun quote(symbol: String) = Result.success(Quote(symbol, 10.0, "EUR", null, 0))
        override fun history(symbol: String, range: HistoryRange): Result<History> {
            calls++
            return Result.success(History(listOf(Candle(1, null, null, null, 10.0, null), Candle(2, null, null, null, 11.0, null))))
        }
    }

    // PRD 5: a range fetched once is reused for the session; retry bypasses it; removing a ticker drops it.
    @Test fun historyIsCachedPerTickerAndRangeUntilBypassedOrRemoved() = runBlocking {
        val source = CountingHistory()
        val store = FakeWatchlistStore().apply { add("ENI.MI", SavedQuote(Quote("ENI.MI", 10.0, "EUR", null, 0), 0)) }
        var now = 1_000L
        val gate = SourceGate(Dispatchers.Unconfined)
        val watchlist = WatchlistRepository(source, store, gate) { now }
        val history = HistoryRepository(source, gate, watchlist) { now }

        history.history("ENI.MI", HistoryRange.MONTH_1)
        history.history("ENI.MI", HistoryRange.MONTH_1)
        assertEquals(1, source.calls)
        now += HistoryRepository.HISTORY_TTL_MILLIS // the last bar is the live price: a stale chart is refetched
        history.history("ENI.MI", HistoryRange.MONTH_1)
        assertEquals(2, source.calls)
        // Intraday ranges expire much sooner: 2 minutes, not an hour.
        history.history("ENI.MI", HistoryRange.DAY_1)
        now += HistoryRepository.INTRADAY_TTL_MILLIS
        history.history("ENI.MI", HistoryRange.DAY_1)
        assertEquals(4, source.calls)
        source.calls = 1
        history.history("ENI.MI", HistoryRange.YEAR_1) // another range is its own entry
        assertEquals(2, source.calls)
        history.history("ENI.MI", HistoryRange.MONTH_1, bypassCache = true)
        assertEquals(3, source.calls)

        watchlist.remove("ENI.MI")
        assertEquals(null, history.cached("ENI.MI", HistoryRange.MONTH_1))
        assertEquals(null, history.cached("ENI.MI", HistoryRange.YEAR_1))
    }

    // The 1D chart response carries the current quote: the header's saved quote follows it, without another request.
    @Test fun aFetched1DChartUpdatesTheSavedQuote() = runBlocking {
        val newer = Quote("ENI.MI", 12.5, "EUR", null, marketTimeEpoch = 200)
        val source = object : QuoteSource {
            override fun quote(symbol: String) = Result.success(newer)
            override fun history(symbol: String, range: HistoryRange) = Result.success(
                History(listOf(Candle(1, null, null, null, 12.0, null), Candle(2, null, null, null, 12.5, null)), quote = newer),
            )
        }
        val store = FakeWatchlistStore().apply { add("ENI.MI", SavedQuote(Quote("ENI.MI", 10.0, "EUR", null, 100), fetchedAt = 500)) }
        var now = 1_000L
        val gate = SourceGate(Dispatchers.Unconfined)
        val watchlist = WatchlistRepository(source, store, gate) { now }
        val history = HistoryRepository(source, gate, watchlist) { now }

        history.history("ENI.MI", HistoryRange.MONTH_1) // another range: the saved quote is left alone
        assertEquals(10.0, watchlist.entries.first().single().saved!!.quote.price, 0.0)

        history.history("ENI.MI", HistoryRange.DAY_1)
        val entry = watchlist.entries.first().single()
        assertEquals(12.5, entry.saved!!.quote.price, 0.0)
        assertEquals(1_000L, entry.saved.fetchedAt)
        assertFalse(entry.stale)

        now = 2_000L
        history.history("ENI.MI", HistoryRange.DAY_1) // served from the cache: not saved again
        assertEquals(1_000L, watchlist.entries.first().single().saved!!.fetchedAt)
    }

    // A slow chart response must not put an older price over a newer one saved by a refresh meanwhile.
    @Test fun anOlderChartQuoteDoesNotOverwriteANewerSavedOne() = runBlocking {
        val older = Quote("ENI.MI", 11.0, "EUR", null, marketTimeEpoch = 200)
        val source = object : QuoteSource {
            override fun quote(symbol: String) = Result.success(older)
            override fun history(symbol: String, range: HistoryRange) = Result.success(
                History(listOf(Candle(1, null, null, null, 11.0, null), Candle(2, null, null, null, 11.0, null)), quote = older),
            )
        }
        val store = FakeWatchlistStore().apply { add("ENI.MI", SavedQuote(Quote("ENI.MI", 12.0, "EUR", null, 300), fetchedAt = 500)) }
        val gate = SourceGate(Dispatchers.Unconfined)
        val watchlist = WatchlistRepository(source, store, gate) { 1_000L }
        val history = HistoryRepository(source, gate, watchlist) { 1_000L }
        history.history("ENI.MI", HistoryRange.DAY_1)
        assertEquals(12.0, watchlist.entries.first().single().saved!!.quote.price, 0.0)
    }
}
