package com.lucrometro.ui.details

import com.lucrometro.data.Candle
import com.lucrometro.data.FakeWatchlistStore
import com.lucrometro.data.History
import com.lucrometro.data.HistoryRange
import com.lucrometro.data.HistoryRepository
import com.lucrometro.data.Quote
import com.lucrometro.data.QuoteSource
import com.lucrometro.data.SavedQuote
import com.lucrometro.data.SourceGate
import com.lucrometro.data.TradingPeriod
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

/**
 * What the view model does with each poll (decisions 0035, 0037): a poll that lands during a scrub waits for its end,
 * and a failed poll stops auto-refresh and marks the header stale, without retrying.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailsAutoRefreshTest {
    private val main = StandardTestDispatcher()

    // A Wednesday at noon, inside a session in UTC: the market is open whatever day the test runs.
    private val noon = ZonedDateTime.parse("2026-11-18T12:00:00Z").toEpochSecond()
    private val session = TradingPeriod(noon - 4 * 3600, noon + 4 * 3600)

    /** Answers each 1D request with the next result in [answers], counting the requests. */
    private inner class ScriptedSource(var answers: ArrayDeque<Result<History>>) : QuoteSource {
        var calls = 0
        override fun quote(symbol: String) = error("not used")
        override fun history(symbol: String, range: HistoryRange): Result<History> {
            calls++
            return answers.removeFirst()
        }
    }

    private fun day(last: Double) = Result.success(
        History(
            candles = listOf(Candle(noon - 600, null, null, null, 10.0, null), Candle(noon - 300, null, null, null, last, null)),
            currentSession = session,
            exchangeZone = "UTC",
        ),
    )

    private val DetailsViewModel.lastPrice get() = (chart.value as ChartState.Ready).data.points.last().price

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aPollWaitsForTheScrubAndAFailureStopsIt() = runTest(main) {
        val source = ScriptedSource(ArrayDeque(listOf(day(10.5), day(11.0), Result.failure(IllegalStateException("429")))))
        val store = FakeWatchlistStore().apply { add("SMH.MI", SavedQuote(Quote("SMH.MI", 10.5, "EUR", null, noon), noon * 1000)) }
        val gate = SourceGate(Dispatchers.Unconfined)
        val watchlist = WatchlistRepository(source, store, gate) { noon * 1000 }
        val history = HistoryRepository(source, gate, watchlist) { noon * 1000 }
        val vm = DetailsViewModel(watchlist, history, "SMH.MI") { noon * 1000 }

        advanceTimeBy(1_000) // the first load, after its debounce
        assertEquals(10.5, vm.lastPrice, 0.0)

        vm.setAutoRefresh(true)
        vm.onScrubbing(true)
        advanceTimeBy(AUTO_REFRESH_MILLIS + 1)
        assertEquals(2, source.calls)
        assertEquals(10.5, vm.lastPrice, 0.0) // the finger is on the chart: it does not change under it
        vm.onScrubbing(false)
        assertEquals(11.0, vm.lastPrice, 0.0)

        advanceTimeBy(AUTO_REFRESH_MILLIS + 1)
        runCurrent()
        assertEquals(3, source.calls)
        assertFalse(vm.autoRefresh.value.on)
        assertTrue(watchlist.entries.first().single().stale)
        assertEquals(11.0, vm.lastPrice, 0.0) // the chart keeps what it shows

        advanceTimeBy(5 * AUTO_REFRESH_MILLIS) // no retry
        assertEquals(3, source.calls)
    }
}
