package com.lucrometro.ui.details

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.DataGraph
import com.lucrometro.data.History
import com.lucrometro.data.HistoryRange
import com.lucrometro.data.TradingPeriod
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
import java.time.ZoneId

/** One point of the chart: the adjusted close (decision 0022), or the close when the source has no adjusted one. */
data class ChartPoint(val epochSeconds: Long, val price: Double)

/**
 * A loaded range. Changes are measured on adjusted closes; [fromPrice] is the first bar's plain close. For 1D the
 * change is measured from the previous close ([baseline], decision 0022) and the axis runs over the [session].
 */
data class ChartData(
    val range: HistoryRange,
    val points: List<ChartPoint>,
    val fromPrice: Double,
    /** 1D: the previous session's close, drawn as a dashed line; null for the other ranges. */
    val baseline: Double?,
    /** 1D: the trading session the axis runs over (the line stops at the last bar); null: the axis spans the points. */
    val session: TradingPeriod?,
    val zone: ZoneId?,
) {
    /** What changes are measured from: the previous close for 1D, the first bar otherwise. */
    val startPrice: Double get() = baseline ?: points.first().price
    val change: Double get() = points.last().price - startPrice
    val changePercent: Double? get() = startPrice.takeIf { it > 0 }?.let { change / it * 100 }
}

sealed interface ChartState {
    data object Loading : ChartState
    data class Ready(val data: ChartData) : ChartState

    /** 1D before the first trade of the day: nothing to draw yet, and nothing wrong. */
    data object Empty : ChartState
    data object Failed : ChartState
}

/** The exchange's current session and zone: what the market status in the top bar is read from. */
data class MarketInfo(val session: TradingPeriod, val zone: ZoneId)

/** The ranges of the range bar, in order. 1D is the default (decision 0022). */
val DETAILS_RANGES = listOf(
    HistoryRange.DAY_1, HistoryRange.DAYS_5, HistoryRange.MONTH_1, HistoryRange.MONTHS_3, HistoryRange.MONTHS_6,
    HistoryRange.YEAR_1, HistoryRange.YEARS_5, HistoryRange.MAX,
)

/** The details screen of one ticker: its saved quote and the chart of the selected range. */
class DetailsViewModel(private val repository: WatchlistRepository, private val code: String) : ViewModel() {
    /** The ticker's entry, or null while the list loads (or if the ticker is gone). */
    val entry: StateFlow<WatchlistEntry?> = repository.entries
        .map { list -> list.firstOrNull { it.code == code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _range = MutableStateFlow(HistoryRange.DAY_1)
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
                val candles = repository.history(code, range, bypassCache).getOrNull()
                if (_range.value == range) show(range, candles)
            }
        }
    }

    private fun show(range: HistoryRange, history: History?) {
        history?.currentSession?.let { session ->
            history.zoneId()?.let { _market.value = MarketInfo(session, it) }
        }
        _chart.value = history.toChartState(range)
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

private fun History.zoneId(): ZoneId? = exchangeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() }

/**
 * Ready with the chart; Empty for a 1D with no bars at all yet (a single bar is drawn as a point); otherwise Failed
 * (the fetch failed, or too few bars to draw a line).
 */
private fun History?.toChartState(range: HistoryRange): ChartState = when {
    this == null -> ChartState.Failed
    candles.size >= 2 || (range == HistoryRange.DAY_1 && candles.isNotEmpty()) -> ChartState.Ready(toChartData(range))
    range == HistoryRange.DAY_1 -> ChartState.Empty
    else -> ChartState.Failed
}

private fun History.toChartData(range: HistoryRange): ChartData {
    // All adjusted or none: a bar without an adjusted close among adjusted ones would be a jump in the line.
    // Intraday bars have none, so they are plain closes.
    val adjusted = candles.all { it.adjClose != null }
    val day = range == HistoryRange.DAY_1
    // The 1D axis runs over the session the bars belong to; bars outside it (cannot be placed on it) are left out. If
    // none of the bars is inside the session, the session is not theirs (e.g. a weekend): the axis spans the bars.
    val session = if (day) sessions.lastOrNull()?.takeIf { s -> candles.any { it.epochSeconds in s.startEpoch..s.endEpoch } } else null
    val bars = if (session != null) candles.filter { it.epochSeconds in session.startEpoch..session.endEpoch } else candles
    return ChartData(
        range = range,
        points = bars.map { ChartPoint(it.epochSeconds, if (adjusted) it.adjClose!! else it.close) },
        fromPrice = bars.first().close,
        baseline = if (day) previousClose else null,
        session = session,
        zone = zoneId(),
    )
}
