package com.lucrometro.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.CsvError
import com.lucrometro.data.CsvParse
import com.lucrometro.data.DataGraph
import com.lucrometro.data.GlobalImportCheck
import com.lucrometro.data.Portfolio
import com.lucrometro.data.PortfolioRepository
import com.lucrometro.data.Quote
import com.lucrometro.data.Trade
import com.lucrometro.data.TradeChange
import com.lucrometro.data.TradeRepository
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import com.lucrometro.data.checkGlobalImport
import com.lucrometro.data.parseTradeCsv
import com.lucrometro.ui.common.marketZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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

/** The global import (decision 0054): the review before saving, or why nothing was saved. */
sealed interface GlobalImport {
    data class Review(
        val fileName: String?,
        val check: GlobalImportCheck.Ready,
        /** Each ticker's quote, for its currency and decimals. */
        val quotes: Map<String, Quote>,
        /** The typed average of each ticker whose first trades these are: the trades' one replaces it (0040). */
        val replaces: Map<String, Double>,
    ) : GlobalImport

    data class Failed(val problem: GlobalImportProblem) : GlobalImport
}

sealed interface GlobalImportProblem {
    data object Unreadable : GlobalImportProblem
    data class Csv(val error: CsvError) : GlobalImportProblem
    data object NoWatchlistRows : GlobalImportProblem

    /** [trade] would take its ticker's shares below 0. */
    data class BelowZero(val trade: Trade) : GlobalImportProblem

    /** [trade] is dated after today. */
    data class Future(val trade: Trade) : GlobalImportProblem
    data object SaveFailed : GlobalImportProblem
}

sealed interface GlobalImportNotice {
    data object NothingNew : GlobalImportNotice
    data class Imported(val trades: Int, val tickers: Int) : GlobalImportNotice
}

class PortfolioViewModel(
    private val repository: PortfolioRepository,
    private val watchlist: WatchlistRepository,
    private val trades: TradeRepository,
) : ViewModel() {
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

    private val _import = MutableStateFlow<GlobalImport?>(null)

    /** The review or the error dialog of the global import; null when none is up. */
    val import: StateFlow<GlobalImport?> = _import.asStateFlow()

    private val _notice = MutableStateFlow<GlobalImportNotice?>(null)

    /** A snackbar after an import: nothing new, or how many were saved. */
    val notice: StateFlow<GlobalImportNotice?> = _notice.asStateFlow()

    /** A file was picked: [text] is its content, or null if it could not be read. Checks it for every ticker. */
    fun onFileRead(fileName: String?, text: String?) {
        viewModelScope.launch {
            _import.value = check(fileName, text)
        }
    }

    private suspend fun check(fileName: String?, text: String?): GlobalImport? {
        text ?: return GlobalImport.Failed(GlobalImportProblem.Unreadable)
        val rows = when (val parsed = parseTradeCsv(text)) {
            is CsvParse.Bad -> return GlobalImport.Failed(GlobalImportProblem.Csv(parsed.error))
            is CsvParse.Ok -> parsed.rows
        }
        val entries = watchlist.currentEntries() ?: return GlobalImport.Failed(GlobalImportProblem.SaveFailed)
        val saved = trades.allTrades() ?: return GlobalImport.Failed(GlobalImportProblem.SaveFailed)
        val zones = entries.associate { it.code to it.saved?.quote?.exchangeZone }
        return when (val check = checkGlobalImport(entries.map { it.code }, rows, saved) { LocalDate.now(marketZone(zones[it])) }) {
            is GlobalImportCheck.Ready -> {
                val typed = trades.typedAverages().orEmpty()
                val withTrades = saved.mapTo(HashSet()) { it.code }
                GlobalImport.Review(
                    fileName,
                    check,
                    entries.mapNotNull { e -> e.saved?.quote?.let { e.code to it } }.toMap(),
                    check.tickers.mapNotNull { (code, _) -> typed[code]?.takeIf { code !in withTrades }?.let { code to it.averagePrice } }.toMap(),
                )
            }
            GlobalImportCheck.NoWatchlistRows -> GlobalImport.Failed(GlobalImportProblem.NoWatchlistRows)
            is GlobalImportCheck.BelowZero -> GlobalImport.Failed(GlobalImportProblem.BelowZero(check.replay.trade))
            is GlobalImportCheck.Future -> GlobalImport.Failed(GlobalImportProblem.Future(check.trade))
            GlobalImportCheck.NothingNew -> {
                _notice.value = GlobalImportNotice.NothingNew
                null
            }
        }
    }

    /** Saves every ticker's new trades in one go, after the same check against what is stored now. */
    fun confirmImport() {
        val review = _import.value as? GlobalImport.Review ?: return
        _import.value = null
        val new = review.check.tickers.associate { (code, ready) -> code to ready.new }
        viewModelScope.launch {
            when (val result = trades.addAll(new)) {
                TradeChange.Saved -> _notice.value = GlobalImportNotice.Imported(new.values.sumOf { it.size }, new.size)
                is TradeChange.BelowZero -> _import.value = GlobalImport.Failed(GlobalImportProblem.BelowZero(result.replay.trade))
                TradeChange.Failed -> _import.value = GlobalImport.Failed(GlobalImportProblem.SaveFailed)
            }
        }
    }

    fun dismissImport() {
        _import.value = null
    }

    fun noticeShown(notice: GlobalImportNotice) {
        _notice.compareAndSet(notice, null)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                PortfolioViewModel(DataGraph.portfolio(app), DataGraph.watchlist(app), DataGraph.trades(app))
            }
        }
    }
}

/** The not-counted rows: code and name, from the saved quote. */
internal val WatchlistEntry.name: String? get() = saved?.quote?.name
