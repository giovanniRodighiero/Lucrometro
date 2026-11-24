package com.lucrometro.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.DataGraph
import com.lucrometro.data.Portfolio
import com.lucrometro.data.PortfolioRepository
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How the "Growth by year" section stands: still fetching year ends (one progress indicator), or settled. */
data class PortfolioState(
    val portfolio: Portfolio,
    /** Newest saved quote's fetch time (epoch millis): the "Updated" stamp, as on the watchlist. */
    val updatedAt: Long?,
    /** Fraction of the year ends fetched, while any year still waits for them; null once settled. */
    val loading: Float?,
    /** Tickers whose year ends failed in this run: their left-out years are tried again on the next app open. */
    val failed: Set<String>,
)

class PortfolioViewModel(private val repository: PortfolioRepository, private val watchlist: WatchlistRepository) : ViewModel() {
    val state: StateFlow<PortfolioState?> = combine(
        repository.portfolio,
        watchlist.entries,
        repository.progress,
        repository.settledCodes,
        repository.failedCodes,
    ) { portfolio, entries, progress, settled, failed ->
        val waiting = portfolio.missingYears.values.any { codes -> !settled.containsAll(codes) }
        PortfolioState(
            portfolio,
            entries.mapNotNull { it.saved?.fetchedAt }.maxOrNull(),
            when {
                progress != null -> progress.done.toFloat() / progress.total
                waiting -> 0f
                else -> null
            },
            failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val refreshing: StateFlow<Boolean> = watchlist.refreshing

    private var loader: Job? = null

    /**
     * The Portfolio tab was shown: from now on keep the year ends loaded. Not before, so an app that is only used for the
     * watchlist makes no history requests. In the ViewModel's scope (kept above the tabs): switching tabs does not stop
     * a fetch half-way.
     */
    fun onShown() {
        if (loader == null) loader = viewModelScope.launch { repository.keepYearEndsLoaded() }
    }

    /** The app was opened or brought back: year ends that failed get another try (not after a 429). */
    fun onAppStarted() = repository.retryFailed()

    /** Pull-to-refresh: the watchlist's quote refresh; the year ends are not fetched again. */
    fun refresh() {
        viewModelScope.launch { watchlist.refresh() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                PortfolioViewModel(DataGraph.portfolio(app), DataGraph.watchlist(app))
            }
        }
    }
}

/** The not-counted rows: code and name, from the saved quote. */
internal val WatchlistEntry.name: String? get() = saved?.quote?.name
