package com.lucrometro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WatchlistRepositoryTest {
    private fun quote(symbol: String, price: Double = 10.0) = Quote(symbol, price, "EUR", null, 0)

    /** Blocks every request until interrupted. Synchronized like YahooQuoteSource, whose monitor caused the bug. */
    private class HangingSource : QuoteSource {
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        @Synchronized override fun quote(symbol: String): Result<Quote> {
            started.countDown()
            return try {
                Thread.sleep(30_000)
                Result.failure(IllegalStateException("not interrupted"))
            } catch (e: InterruptedException) {
                interrupted.countDown()
                Result.failure(e)
            }
        }
        override fun history(symbol: String, range: HistoryRange) = Result.success(History(emptyList()))
    }

    // Closing the add sheet must abort a stuck request, or it holds the source until the network timeout.
    @Test fun cancellingAddInterruptsTheBlockingRequest() = runBlocking {
        val source = HangingSource()
        val store = FakeWatchlistStore()
        val repo = WatchlistRepository(source, store, Dispatchers.IO)
        val job = launch(Dispatchers.Default) { repo.add("ENI.MI") }
        assertTrue(source.started.await(5, TimeUnit.SECONDS))
        job.cancel()
        assertTrue("request was not interrupted", source.interrupted.await(5, TimeUnit.SECONDS))
        assertEquals(emptyList<StoredTicker>(), store.rows.value)
    }

    // An add waiting for the source while a refresh holds it must still cancel at once.
    @Test fun addWaitingBehindARefreshCancelsAtOnce() = runBlocking {
        val source = HangingSource()
        val store = FakeWatchlistStore().apply { add("ISP.MI", SavedQuote(quote("ISP.MI"), 0)) }
        val repo = WatchlistRepository(source, store, Dispatchers.IO)
        val refresh = launch(Dispatchers.Default) { repo.refresh() }
        assertTrue(source.started.await(5, TimeUnit.SECONDS))

        val add = launch(Dispatchers.Default) { repo.add("ENI.MI") }
        Thread.sleep(200) // let it reach the lock
        add.cancel()
        withTimeout(1_000) { add.join() } // would hang until the refresh's request ended
        assertFalse(store.rows.value.any { it.code == "ENI.MI" })
        refresh.cancel()
    }

    // PRD: a failed fetch keeps the last good quote, marked stale; it never replaces or drops it.
    @Test fun failedRefreshKeepsTheLastQuoteAndMarksItStale() = runBlocking {
        var price = 10.0
        var fail = false
        var now = 1_000L
        val source = object : QuoteSource {
            override fun quote(symbol: String): Result<Quote> =
                if (fail) Result.failure(IllegalStateException("offline")) else Result.success(quote(symbol, price))
            override fun history(symbol: String, range: HistoryRange) = Result.success(History(emptyList()))
        }
        val repo = WatchlistRepository(source, FakeWatchlistStore(), Dispatchers.Unconfined) { now }
        repo.add("ENI.MI")
        assertFalse(repo.entries.first().single().stale)

        fail = true; now = 2_000L
        repo.refresh()
        val stale = repo.entries.first().single()
        assertTrue(stale.stale)
        assertEquals(10.0, stale.saved!!.quote.price, 0.0)
        assertEquals(1_000L, stale.saved.fetchedAt)

        fail = false; price = 11.0; now = 3_000L
        repo.refresh()
        val fresh = repo.entries.first().single()
        assertFalse(fresh.stale)
        assertEquals(11.0, fresh.saved!!.quote.price, 0.0)
    }

    // After a restart, saved quotes are not current until this session fetches them again.
    @Test fun quoteFromAnEarlierSessionIsStale() = runBlocking {
        val store = FakeWatchlistStore().apply { add("ENI.MI", SavedQuote(quote("ENI.MI"), fetchedAt = 500)) }
        val source = object : QuoteSource {
            override fun quote(symbol: String) = Result.success(quote(symbol, 12.0))
            override fun history(symbol: String, range: HistoryRange) = Result.success(History(emptyList()))
        }
        var now = 1_000L
        val repo = WatchlistRepository(source, store, Dispatchers.Unconfined) { now }
        assertTrue(repo.entries.first().single().stale)

        now = 1_500L
        repo.refresh()
        assertFalse(repo.entries.first().single().stale)
    }

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
        val repo = WatchlistRepository(source, store, Dispatchers.Unconfined) { now }

        repo.history("ENI.MI", HistoryRange.MONTH_1)
        repo.history("ENI.MI", HistoryRange.MONTH_1)
        assertEquals(1, source.calls)
        now += WatchlistRepository.HISTORY_TTL_MILLIS // the last bar is the live price: a stale chart is refetched
        repo.history("ENI.MI", HistoryRange.MONTH_1)
        assertEquals(2, source.calls)
        // Intraday ranges expire much sooner: 2 minutes, not an hour.
        repo.history("ENI.MI", HistoryRange.DAY_1)
        now += WatchlistRepository.INTRADAY_TTL_MILLIS
        repo.history("ENI.MI", HistoryRange.DAY_1)
        assertEquals(4, source.calls)
        source.calls = 1
        repo.history("ENI.MI", HistoryRange.YEAR_1) // another range is its own entry
        assertEquals(2, source.calls)
        repo.history("ENI.MI", HistoryRange.MONTH_1, bypassCache = true)
        assertEquals(3, source.calls)

        repo.remove("ENI.MI")
        assertEquals(null, repo.cachedHistory("ENI.MI", HistoryRange.MONTH_1))
        assertEquals(null, repo.cachedHistory("ENI.MI", HistoryRange.YEAR_1))
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
        val repo = WatchlistRepository(source, store, Dispatchers.Unconfined) { now }

        repo.history("ENI.MI", HistoryRange.MONTH_1) // another range: the saved quote is left alone
        assertEquals(10.0, repo.entries.first().single().saved!!.quote.price, 0.0)

        repo.history("ENI.MI", HistoryRange.DAY_1)
        val entry = repo.entries.first().single()
        assertEquals(12.5, entry.saved!!.quote.price, 0.0)
        assertEquals(1_000L, entry.saved.fetchedAt)
        assertFalse(entry.stale)

        now = 2_000L
        repo.history("ENI.MI", HistoryRange.DAY_1) // served from the cache: not saved again
        assertEquals(1_000L, repo.entries.first().single().saved!!.fetchedAt)
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
        val repo = WatchlistRepository(source, store, Dispatchers.Unconfined) { 1_000L }
        repo.history("ENI.MI", HistoryRange.DAY_1)
        assertEquals(12.0, repo.entries.first().single().saved!!.quote.price, 0.0)
    }
}
