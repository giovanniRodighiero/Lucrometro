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
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlin.coroutines.CoroutineContext

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
            candles = listOf(Candle(noon - 600, 10.0), Candle(noon - 300, last)),
            currentSession = session,
            exchangeZone = "UTC",
        ),
    )

    private val DetailsViewModel.lastPrice get() = (chart.value as ChartState.Ready).data.points.last().price

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aChartBehindTheHeaderIsFetchedOnceAndKeptOnFailure() = runTest(main) {
        fun at(time: Long, price: Double) = day(price).map { it.copy(lastTradeEpoch = time) }
        // The second answer is no newer than the first, the third fails.
        val failure = Result.failure<History>(IllegalStateException("429"))
        val source = ScriptedSource(ArrayDeque(listOf(at(noon - 300, 10.5), at(noon - 300, 10.5), failure)))
        val gate = SourceGate(Dispatchers.Unconfined)
        val watchlist = WatchlistRepository(source, FakeWatchlistStore(), gate) { noon * 1000 }
        val vm = DetailsViewModel(watchlist, HistoryRepository(source, gate, watchlist) { noon * 1000 }, "SMH.MI") { noon * 1000 }
        advanceTimeBy(1_000)

        vm.catchUp(noon - 300) // not behind
        advanceTimeBy(1_000)
        assertEquals(1, source.calls)

        repeat(3) { // the header is newer: one request, even though the answer is still behind
            vm.catchUp(noon)
            advanceTimeBy(1_000)
        }
        assertEquals(2, source.calls)

        vm.catchUp(noon + 60)
        advanceTimeBy(1_000)
        vm.catchUp(noon + 60) // no retry
        advanceTimeBy(1_000)
        assertEquals(3, source.calls)
        assertEquals(10.5, vm.lastPrice, 0.0) // the chart stays
    }

    private fun at(time: Long, price: Double) = day(price).map { it.copy(lastTradeEpoch = time) }

    /** Runs the source's requests at once, or keeps them in flight while [hold] is true. */
    private class HeldDispatcher : CoroutineDispatcher() {
        var hold = false
        private val held = mutableListOf<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (hold) held += block else block.run()
        }
        fun release() {
            hold = false
            held.toList().also { held.clear() }.forEach { it.run() }
        }
    }

    /** SMH.MI tracked, so its charts are cached. */
    private suspend fun trackedStore() =
        FakeWatchlistStore().apply { add("SMH.MI", SavedQuote(Quote("SMH.MI", 10.5, "EUR", null, noon), noon * 1000)) }

    private fun viewModel(source: QuoteSource, store: FakeWatchlistStore, dispatcher: CoroutineDispatcher): DetailsViewModel {
        val gate = SourceGate(dispatcher)
        val watchlist = WatchlistRepository(source, store, gate) { noon * 1000 }
        return DetailsViewModel(watchlist, HistoryRepository(source, gate, watchlist) { noon * 1000 }, "SMH.MI") { noon * 1000 }
    }

    @Test
    fun noCatchUpForTheQuoteAFailedPollLeft() = runTest(main) {
        val failure = Result.failure<History>(IllegalStateException("429"))
        val source = ScriptedSource(ArrayDeque(listOf(at(noon - 300, 10.5), at(noon - 300, 10.5), failure, at(noon + 60, 12.0))))
        val store = FakeWatchlistStore().apply { add("SMH.MI", SavedQuote(Quote("SMH.MI", 10.5, "EUR", null, noon), noon * 1000)) }
        val vm = viewModel(source, store, Dispatchers.Unconfined)
        advanceTimeBy(1_000)
        vm.selectRange(HistoryRange.DAYS_5)
        advanceTimeBy(1_000)

        vm.setAutoRefresh(true)
        advanceTimeBy(AUTO_REFRESH_MILLIS + 1)
        assertFalse(vm.autoRefresh.value.on)
        vm.catchUp(noon)
        advanceTimeBy(1_000)
        assertEquals(3, source.calls)

        vm.catchUp(noon + 60) // a later quote was fetched successfully
        advanceTimeBy(1_000)
        assertEquals(4, source.calls)
    }

    @Test
    fun aRangeSelectedDuringACatchUpCatchesUpToo() = runTest(main) {
        val source = ScriptedSource(ArrayDeque(listOf(at(noon - 300, 10.0), at(noon - 300, 11.0), at(noon, 12.0), at(noon, 13.0))))
        val dispatcher = HeldDispatcher()
        val vm = viewModel(source, trackedStore(), dispatcher)
        advanceTimeBy(1_000)
        vm.selectRange(HistoryRange.DAYS_5)
        advanceTimeBy(1_000)
        vm.selectRange(HistoryRange.DAY_1) // from the cache

        dispatcher.hold = true
        vm.catchUp(noon)
        advanceTimeBy(1_000) // 1D in flight
        vm.selectRange(HistoryRange.DAYS_5) // from the cache, behind
        vm.catchUp(noon)
        advanceTimeBy(1_000)
        dispatcher.release()
        advanceTimeBy(1_000)

        assertEquals(4, source.calls)
        assertEquals(13.0, vm.lastPrice, 0.0)
    }

    @Test
    fun aCatchUpWithTooFewBarsKeepsTheChart() = runTest(main) {
        val short = at(noon, 12.0).map { it.copy(candles = it.candles.take(1)) }
        val source = ScriptedSource(ArrayDeque(listOf(at(noon - 300, 10.0), at(noon - 300, 11.0), short)))
        val vm = viewModel(source, trackedStore(), Dispatchers.Unconfined)
        advanceTimeBy(1_000)
        vm.selectRange(HistoryRange.DAYS_5)
        advanceTimeBy(1_000)

        vm.catchUp(noon)
        advanceTimeBy(1_000)

        assertEquals(3, source.calls)
        assertEquals(11.0, vm.lastPrice, 0.0)
    }

    @Test
    fun aReplyHeldForAScrubIsDroppedByAReload() = runTest(main) {
        val source = ScriptedSource(ArrayDeque(listOf(at(noon - 300, 10.0), at(noon - 300, 11.0), at(noon, 12.0))))
        val vm = viewModel(source, trackedStore(), Dispatchers.Unconfined)
        advanceTimeBy(1_000)
        vm.selectRange(HistoryRange.DAYS_5)
        advanceTimeBy(1_000)
        vm.onScrubbing(true)
        vm.catchUp(noon)
        advanceTimeBy(1_000)

        vm.onOpened() // reopened with the drag never ended: back to 1D
        vm.onScrubbing(false)

        assertEquals(10.0, vm.lastPrice, 0.0)
    }

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
