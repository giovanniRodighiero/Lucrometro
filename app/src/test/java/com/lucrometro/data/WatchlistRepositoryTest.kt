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
        override fun history(symbol: String, range: HistoryRange) = Result.success(emptyList<Candle>())
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
            override fun history(symbol: String, range: HistoryRange) = Result.success(emptyList<Candle>())
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
            override fun history(symbol: String, range: HistoryRange) = Result.success(emptyList<Candle>())
        }
        var now = 1_000L
        val repo = WatchlistRepository(source, store, Dispatchers.Unconfined) { now }
        assertTrue(repo.entries.first().single().stale)

        now = 1_500L
        repo.refresh()
        assertFalse(repo.entries.first().single().stale)
    }
}
