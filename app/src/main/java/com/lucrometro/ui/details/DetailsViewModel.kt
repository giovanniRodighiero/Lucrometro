package com.lucrometro.ui.details

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.Candle
import com.lucrometro.data.DataGraph
import com.lucrometro.data.HistoryRange
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One point of the chart: the adjusted close (decision 0022), or the close when the source has no adjusted one. */
data class ChartPoint(val epochSeconds: Long, val price: Double)

/** A loaded range. Changes are measured on adjusted closes; [fromPrice] is the first bar's plain close. */
data class ChartData(val points: List<ChartPoint>, val fromPrice: Double) {
    val change: Double get() = points.last().price - points.first().price
    val changePercent: Double? get() = points.first().price.takeIf { it > 0 }?.let { change / it * 100 }
}

sealed interface ChartState {
    data object Loading : ChartState
    data class Ready(val data: ChartData) : ChartState
    data object Failed : ChartState
}

/** The ranges of the range bar, in order. 1D (the default, decision 0022) joins when the intraday chart is built. */
val DETAILS_RANGES = listOf(
    HistoryRange.DAYS_5, HistoryRange.MONTH_1, HistoryRange.MONTHS_3, HistoryRange.MONTHS_6,
    HistoryRange.YEAR_1, HistoryRange.YEARS_5, HistoryRange.MAX,
)

/** The details screen of one ticker: its saved quote and the chart of the selected range. */
class DetailsViewModel(private val repository: WatchlistRepository, private val code: String) : ViewModel() {
    /** The ticker's entry, or null while the list loads (or if the ticker is gone). */
    val entry: StateFlow<WatchlistEntry?> = repository.entries
        .map { list -> list.firstOrNull { it.code == code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _range = MutableStateFlow(HistoryRange.MONTH_1)
    val range: StateFlow<HistoryRange> = _range.asStateFlow()

    private val _chart = MutableStateFlow<ChartState>(ChartState.Loading)
    val chart: StateFlow<ChartState> = _chart.asStateFlow()

    /** The wait before a request is sent: a new selection cancels it, so tapping through the bar sends nothing. */
    private var debounce: Job? = null

    init {
        load(bypassCache = false)
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
        val cached = if (bypassCache) null else repository.cachedHistory(code, range)
        if (cached != null) {
            // Already fetched this session: show it at once, no request.
            _chart.value = cached.toChartState()
            return
        }
        _chart.value = ChartState.Loading
        debounce = viewModelScope.launch {
            if (!bypassCache) delay(SWITCH_DEBOUNCE_MILLIS)
            // A request, once sent, is not cancelled by a later selection: it finishes and fills the session cache,
            // so coming back to the range costs nothing. Only the screen ignores it if the range has changed.
            // Launched in the ViewModel's scope, not as a child of this job, which a later selection cancels.
            viewModelScope.launch {
                val candles = repository.history(code, range, bypassCache).getOrNull()
                if (_range.value == range) _chart.value = candles.toChartState()
            }
        }
    }

    companion object {
        private const val SWITCH_DEBOUNCE_MILLIS = 250L

        fun factory(code: String) = viewModelFactory {
            initializer {
                DetailsViewModel(DataGraph.watchlist(this[APPLICATION_KEY] as Application), code)
            }
        }
    }
}

/** Ready with the chart, or Failed when there is nothing to draw (the fetch failed, or fewer than two bars). */
private fun List<Candle>?.toChartState(): ChartState =
    this?.toChartData()?.let { ChartState.Ready(it) } ?: ChartState.Failed

private fun List<Candle>.toChartData(): ChartData? {
    if (size < 2) return null
    // All adjusted or none: a bar without an adjusted close among adjusted ones would be a jump in the line.
    val adjusted = all { it.adjClose != null }
    return ChartData(map { ChartPoint(it.epochSeconds, if (adjusted) it.adjClose!! else it.close) }, first().close)
}
