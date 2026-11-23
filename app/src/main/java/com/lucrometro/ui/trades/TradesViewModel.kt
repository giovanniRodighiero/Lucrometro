package com.lucrometro.ui.trades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.DataGraph
import com.lucrometro.data.Position
import com.lucrometro.data.Trade
import com.lucrometro.data.TradeChange
import com.lucrometro.data.TradeRepository
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

    suspend fun import(code: String, new: List<Trade>): TradeChange = viewModelScope.async { trades.addAll(code, new) }.await()

    companion object {
        fun factory(code: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                TradesViewModel(DataGraph.trades(app), DataGraph.watchlist(app), code)
            }
        }
    }
}
