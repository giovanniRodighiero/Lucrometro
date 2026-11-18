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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ensureActive
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

/** The details screen of one ticker: its saved quote and the chart of the (for now fixed) 1M range. */
class DetailsViewModel(private val repository: WatchlistRepository, private val code: String) : ViewModel() {
    /** The ticker's entry, or null while the list loads (or if the ticker is gone). */
    val entry: StateFlow<WatchlistEntry?> = repository.entries
        .map { list -> list.firstOrNull { it.code == code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _chart = MutableStateFlow<ChartState>(ChartState.Loading)
    val chart: StateFlow<ChartState> = _chart.asStateFlow()

    private var loading: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        loading?.cancel()
        _chart.value = ChartState.Loading
        loading = viewModelScope.launch {
            val candles = repository.history(code, HistoryRange.MONTH_1).getOrNull()
            // A superseded load (retry) can come back as an ordinary failure when its request was interrupted:
            // don't let it overwrite the state of the newer one.
            ensureActive()
            _chart.value = candles?.toChartData()?.let { ChartState.Ready(it) } ?: ChartState.Failed
        }
    }

    companion object {
        fun factory(code: String) = viewModelFactory {
            initializer {
                DetailsViewModel(DataGraph.watchlist(this[APPLICATION_KEY] as Application), code)
            }
        }
    }
}

/** Null when there are fewer than two bars: there is no line to draw. */
private fun List<Candle>.toChartData(): ChartData? {
    if (size < 2) return null
    // All adjusted or none: a bar without an adjusted close among adjusted ones would be a jump in the line.
    val adjusted = all { it.adjClose != null }
    return ChartData(map { ChartPoint(it.epochSeconds, if (adjusted) it.adjClose!! else it.close) }, first().close)
}
