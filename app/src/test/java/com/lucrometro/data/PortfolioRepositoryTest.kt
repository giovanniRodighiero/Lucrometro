package com.lucrometro.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** The year-end fetch rules (decision 0052): Yahoo's limits are the risk, and the emulator can't show a 429. */
class PortfolioRepositoryTest {
    private class MemoryYearEnds : YearEndStore {
        val saved = mutableMapOf<String, TickerYears>()
        override suspend fun get(code: String) = saved[code]
        override suspend fun save(years: TickerYears) { saved[years.code] = years }
    }

    /** Answers daily history with one bar per year end 2023-2025, or [failure]; records when each request came. */
    private class Source(var failure: Exception? = null) : QuoteSource {
        val asked = mutableListOf<String>()
        val at = mutableListOf<Long>()
        override fun quote(symbol: String) = Result.success(Quote(symbol, 10.0, "EUR", null, 0))
        override fun history(symbol: String, range: HistoryRange) = Result.success(History(emptyList()))
        override fun dailyHistory(symbol: String, fromEpochSeconds: Long): Result<History> {
            asked += symbol
            at += System.currentTimeMillis()
            failure?.let { return Result.failure(it) }
            val bars = (2023..2025).map { y ->
                Candle(LocalDate.of(y, 12, 30).atStartOfDay(ZoneOffset.UTC).toEpochSecond(), 10.0 + y - 2023, 10.0)
            }
            return Result.success(History(bars))
        }
    }

    private val june2026 = LocalDate.of(2026, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun repo(source: Source, store: MemoryYearEnds, spacing: Long = 0, clock: () -> Long = { june2026 }) =
        FakeWatchlistStore().let { watchlist ->
            val gate = SourceGate(Dispatchers.Unconfined)
            PortfolioRepository(source, gate, watchlist, store, WatchlistRepository(source, watchlist, gate), clock, spacing)
        }

    @Test fun aRateLimitStopsEveryFetchForTheProcess() = runBlocking {
        val source = Source(RateLimitedException())
        val repo = repo(source, MemoryYearEnds())
        val needs = mapOf("A.MI" to 2023, "B.MI" to 2023)

        repo.ensureYearEnds(needs)
        assertEquals(listOf("A.MI"), source.asked)
        assertEquals(needs.keys, repo.failedCodes.value)

        // Opening the app again retries other failures, never a 429.
        source.failure = null
        repo.retryFailed()
        repo.ensureYearEnds(needs)
        assertEquals(listOf("A.MI"), source.asked)
        assertEquals(needs.keys, repo.failedCodes.value)
    }

    @Test fun aNetworkFailureLeavesTheRestOutUntilARetry() = runBlocking {
        val source = Source(java.io.IOException("offline"))
        val repo = repo(source, MemoryYearEnds())
        val needs = mapOf("A.MI" to 2023, "B.MI" to 2023)

        repo.ensureYearEnds(needs)
        assertEquals(listOf("A.MI"), source.asked)
        assertEquals(needs.keys, repo.failedCodes.value)

        source.failure = null
        repo.retryFailed()
        repo.ensureYearEnds(needs)
        assertEquals(listOf("A.MI", "A.MI", "B.MI"), source.asked)
        assertTrue(repo.failedCodes.value.isEmpty())
    }

    @Test fun savedYearEndsAreFetchedAgainOnlyWhenOutOfDate() = runBlocking {
        val source = Source()
        val store = MemoryYearEnds()
        store.saved["NEW.MI"] = TickerYears("NEW.MI", 2023, 2025, emptyList())
        store.saved["OLD.MI"] = TickerYears("OLD.MI", 2023, 2024, emptyList()) // 2025 closed since
        store.saved["LATE.MI"] = TickerYears("LATE.MI", 2024, 2025, emptyList()) // an older trade needs 2023

        repo(source, store).ensureYearEnds(mapOf("NEW.MI" to 2023, "OLD.MI" to 2023, "LATE.MI" to 2023))

        assertEquals(listOf("OLD.MI", "LATE.MI"), source.asked)
        assertEquals(2025, store.saved.getValue("OLD.MI").throughYear)
        assertEquals(2023, store.saved.getValue("LATE.MI").fromYear)
    }

    @Test fun aRestartedLoopStillWaitsAfterTheLastRequest() = runBlocking {
        val source = Source()
        val repo = repo(source, MemoryYearEnds(), spacing = 300, clock = System::currentTimeMillis)

        // Two loops back to back, as when collectLatest restarts one: each asks for its first ticker.
        repo.ensureYearEnds(mapOf("A.MI" to 2023))
        repo.ensureYearEnds(mapOf("B.MI" to 2023))

        assertEquals(listOf("A.MI", "B.MI"), source.asked)
        assertTrue("requests ${source.at[1] - source.at[0]} ms apart", source.at[1] - source.at[0] >= 300)
    }
}
