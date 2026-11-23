package com.lucrometro.ui.trades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.CsvError
import com.lucrometro.data.CsvParse
import com.lucrometro.data.DataGraph
import com.lucrometro.data.ImportCheck
import com.lucrometro.data.Position
import com.lucrometro.data.Trade
import com.lucrometro.data.TradeChange
import com.lucrometro.data.TradeRepository
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import com.lucrometro.data.checkImport
import com.lucrometro.data.parseTradeCsv
import com.lucrometro.ui.common.marketZone
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface ImportState {
    data class Confirm(val fileName: String?, val check: ImportCheck.Ready) : ImportState
    data class Failed(val problem: ImportProblem) : ImportState
}

/** Why an import saved nothing (decision 0042). */
sealed interface ImportProblem {
    data object Unreadable : ImportProblem
    data class Csv(val error: CsvError) : ImportProblem
    data object NoRows : ImportProblem

    /** [trade] would take the shares below 0. */
    data class BelowZero(val trade: Trade) : ImportProblem

    /** [trade] is dated after today. */
    data class Future(val trade: Trade) : ImportProblem
    data object SaveFailed : ImportProblem
}

sealed interface ImportNotice {
    data object NothingNew : ImportNotice
    data class Imported(val count: Int) : ImportNotice
}

/** One ticker's trades screen (decision 0042): the list, add, edit, delete with undo. Private view only. */
class TradesViewModel(
    private val trades: TradeRepository,
    watchlist: WatchlistRepository,
    val code: String,
) : ViewModel() {
    /** The trades in no order, or null while they load. */
    val list: StateFlow<List<Trade>?> = trades.trades(code).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The ticker's entry, for its currency, decimals and zone. */
    val entry: StateFlow<WatchlistEntry?> = watchlist.entries
        .map { list -> list.firstOrNull { it.code == code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The typed average: the import says the trades' one replaces it. */
    val typed: StateFlow<Position?> = watchlist.position(code).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _deleted = MutableStateFlow<Trade?>(null)

    /** The last deleted trade while its undo snackbar is up. */
    val deleted: StateFlow<Trade?> = _deleted.asStateFlow()

    // In the ViewModel's scope: closing the sheet, or the row leaving the list, must not cancel the write.
    suspend fun save(trade: Trade): TradeChange = viewModelScope.async { trades.save(trade) }.await()

    suspend fun delete(trade: Trade): TradeChange = viewModelScope.async {
        trades.delete(trade).also { if (it == TradeChange.Saved) _deleted.value = trade }
    }.await()

    /** Puts [trade] back with its id. False if that failed. */
    suspend fun undoDelete(trade: Trade): Boolean = viewModelScope.async { trades.save(trade) == TradeChange.Saved }.await()

    fun undoDismissed(trade: Trade) {
        _deleted.compareAndSet(trade, null)
    }

    private val _import = MutableStateFlow<ImportState?>(null)

    /** The import dialog: a confirmation or an error; null when none is up. */
    val import: StateFlow<ImportState?> = _import.asStateFlow()

    private val _notice = MutableStateFlow<ImportNotice?>(null)

    /** A plain message after an import (snackbar): nothing new, or how many were saved. */
    val notice: StateFlow<ImportNotice?> = _notice.asStateFlow()

    /** A file was picked: [text] is its content, or null if it could not be read. Checks it and asks before saving. */
    fun onFileRead(fileName: String?, text: String?) {
        if (text == null) {
            _import.value = ImportState.Failed(ImportProblem.Unreadable)
            return
        }
        val rows = when (val parsed = parseTradeCsv(text)) {
            is CsvParse.Bad -> {
                _import.value = ImportState.Failed(ImportProblem.Csv(parsed.error))
                return
            }
            is CsvParse.Ok -> parsed.rows
        }
        val today = LocalDate.now(marketZone(entry.value?.saved?.quote?.exchangeZone))
        when (val check = checkImport(code, rows, list.value.orEmpty(), today)) {
            is ImportCheck.Ready -> _import.value = ImportState.Confirm(fileName, check)
            ImportCheck.NoRows -> _import.value = ImportState.Failed(ImportProblem.NoRows)
            is ImportCheck.BelowZero -> _import.value = ImportState.Failed(ImportProblem.BelowZero(check.replay.trade))
            is ImportCheck.Future -> _import.value = ImportState.Failed(ImportProblem.Future(check.trade))
            ImportCheck.NothingNew -> _notice.value = ImportNotice.NothingNew
        }
    }

    /** Saves the confirmed trades, after the same check against what is stored now. */
    fun confirmImport() {
        val confirm = _import.value as? ImportState.Confirm ?: return
        _import.value = null
        viewModelScope.launch {
            when (val result = trades.addAll(code, confirm.check.new)) {
                TradeChange.Saved -> _notice.value = ImportNotice.Imported(confirm.check.new.size)
                is TradeChange.BelowZero -> _import.value = ImportState.Failed(ImportProblem.BelowZero(result.replay.trade))
                TradeChange.Failed -> _import.value = ImportState.Failed(ImportProblem.SaveFailed)
            }
        }
    }

    fun dismissImport() {
        _import.value = null
    }

    fun noticeShown(notice: ImportNotice) {
        _notice.compareAndSet(notice, null)
    }

    companion object {
        fun factory(code: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                TradesViewModel(DataGraph.trades(app), DataGraph.watchlist(app), code)
            }
        }
    }
}
