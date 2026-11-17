package com.lucrometro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WatchlistRepositoryTest {
    // Closing the add sheet must abort a stuck request, or it holds the source until the network timeout.
    @Test fun cancellingAddInterruptsTheBlockingRequest() = runBlocking {
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val hanging = object : QuoteSource {
            override fun quote(symbol: String): Result<Quote> {
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
        val repo = WatchlistRepository(hanging, Dispatchers.IO)
        val job = launch(Dispatchers.Default) { repo.add("ENI.MI") }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        job.cancel()
        assertTrue("request was not interrupted", interrupted.await(5, TimeUnit.SECONDS))
        assertEquals(emptyList<WatchlistEntry>(), repo.entries.value)
    }

    // PRD: a failed fetch keeps the last good quote, marked stale; it never replaces or drops it.
    @Test fun failedRefreshKeepsTheLastQuoteAndMarksItStale() = runBlocking {
        var price = 10.0
        var fail = false
        var now = 1_000L
        val source = object : QuoteSource {
            override fun quote(symbol: String): Result<Quote> =
                if (fail) Result.failure(IllegalStateException("offline"))
                else Result.success(Quote(symbol, price, "EUR", null, 0))
            override fun history(symbol: String, range: HistoryRange) = Result.success(emptyList<Candle>())
        }
        val repo = WatchlistRepository(source, Dispatchers.Unconfined) { now }
        repo.add("ENI.MI")

        fail = true; now = 2_000L
        repo.refresh()
        val stale = repo.entries.value.single()
        assertTrue(stale.failed)
        assertEquals(10.0, stale.saved!!.quote.price, 0.0)
        assertEquals(1_000L, stale.saved.fetchedAt)
        assertNull("nothing was updated", repo.refreshedAt.value)

        fail = false; price = 11.0; now = 3_000L
        repo.refresh()
        val fresh = repo.entries.value.single()
        assertFalse(fresh.failed)
        assertEquals(11.0, fresh.saved!!.quote.price, 0.0)
        assertEquals(3_000L, repo.refreshedAt.value)
    }
}
