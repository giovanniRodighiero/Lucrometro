package com.lucrometro.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.DataGraph
import com.lucrometro.data.History
import com.lucrometro.data.HistoryRange
import com.lucrometro.data.HistoryRepository
import com.lucrometro.data.Position
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The details screen of one ticker: its saved quote and the chart of the selected range. */
class DetailsViewModel(
    private val watchlist: WatchlistRepository,
    private val history: HistoryRepository,
    private val code: String,
) : ViewModel() {
    /** The ticker's entry, or null while the list loads (or if the ticker is gone). */
    val entry: StateFlow<WatchlistEntry?> = watchlist.entries
        .map { list -> list.firstOrNull { it.code == code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The owner's latest position entry (decision 0033); null while it loads or if none was saved. */
    val position: StateFlow<Position?> = watchlist.position(code)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Saves a new position entry. False if it could not be saved. */
    suspend fun savePosition(averagePrice: Double, shares: Double?, lastBuy: LocalDate): Boolean =
        // In the ViewModel's scope: closing the sheet must not cancel the write.
        viewModelScope.async { watchlist.savePosition(code, averagePrice, shares, lastBuy) }.await()

    private val _range = MutableStateFlow(DEFAULT_RANGE)
    val range: StateFlow<HistoryRange> = _range.asStateFlow()

    private val _chart = MutableStateFlow<ChartState>(ChartState.Loading)
    val chart: StateFlow<ChartState> = _chart.asStateFlow()

    private val _market = MutableStateFlow<MarketInfo?>(null)

    /** The exchange's session from the latest chart response (every range carries it); null until one arrives. */
    val market: StateFlow<MarketInfo?> = _market.asStateFlow()

    /** The wait before a request is sent: a new selection cancels it, so tapping through the bar sends nothing. */
    private var debounce: Job? = null

    init {
        load(bypassCache = false)
    }

    /**
     * The screen was opened (not rotated, see `DetailsScreen`). The ViewModel outlives the screen (it is kept per ticker
     * for the activity), so a reopened ticker starts again on the default range. On that range it reloads only what needs
     * it: nothing while a load is running or the chart is fresh in the session cache, otherwise (failed, or expired) it
     * loads again.
     */
    fun onOpened() {
        if (_range.value != DEFAULT_RANGE) {
            _range.value = DEFAULT_RANGE
            load(bypassCache = false)
            return
        }
        val fresh = history.cached(code, DEFAULT_RANGE) != null
        if (_chart.value !is ChartState.Loading && !fresh) load(bypassCache = false)
    }

    fun selectRange(range: HistoryRange) {
        if (range == _range.value) return
        _range.value = range
        load(bypassCache = false)
    }

    /** Fetches the selected range again, ignoring the session cache (PRD 5). */
    fun retry() = load(bypassCache = true)

    private fun load(bypassCache: Boolean) {
        debounce?.cancel()
        val range = _range.value
        val cached = if (bypassCache) null else history.cached(code, range)
        if (cached != null) {
            // Already fetched this session: show it at once, no request.
            show(range, cached)
            return
        }
        _chart.value = ChartState.Loading
        debounce = viewModelScope.launch {
            if (!bypassCache) delay(SWITCH_DEBOUNCE_MILLIS)
            // A request, once sent, is not cancelled by a later selection: it finishes and fills the session cache,
            // so coming back to the range costs nothing. Only the screen ignores it if the range has changed.
            // Launched in the ViewModel's scope, not as a child of this job, which a later selection cancels.
            viewModelScope.launch {
                val result = history.history(code, range, bypassCache).getOrNull()
                if (_range.value == range) show(range, result)
            }
        }
    }

    private fun show(range: HistoryRange, history: History?) {
        history?.marketInfo()?.let { _market.value = it }
        _chart.value = history.toChartState(range)
    }

    companion object {
        private const val SWITCH_DEBOUNCE_MILLIS = 250L
        private val DEFAULT_RANGE = HistoryRange.DAY_1

        fun factory(code: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                DetailsViewModel(DataGraph.watchlist(app), DataGraph.history(app), code)
            }
        }
    }
}
