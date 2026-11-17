package com.lucrometro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        assertEquals(emptyList<String>(), repo.tickers.value)
    }
}
