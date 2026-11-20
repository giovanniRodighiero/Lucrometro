package com.lucrometro.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.AddTickerResult
import com.lucrometro.data.DataGraph
import com.lucrometro.data.StoredTicker
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The add-ticker sheet. The typed text lives in the sheet's TextFieldState, not here. */
data class AddTickerState(
    val checking: Boolean = false,
    /** Why the input was refused: any result but [AddTickerResult.Added]. */
    val error: AddTickerResult? = null,
    /** Set when the ticker was saved: the screen animates the sheet away, then calls [WatchlistViewModel.closeAddSheet]. */
    val added: Boolean = false,
)

class WatchlistViewModel(private val repository: WatchlistRepository) : ViewModel() {
    /** The list, or null while it is still loading from storage (so the empty state does not flash). */
    val entries: StateFlow<List<WatchlistEntry>?> =
        repository.entries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val refreshing: StateFlow<Boolean> = repository.refreshing

    private val _undo = MutableStateFlow<StoredTicker?>(null)

    /** The last removed ticker while its undo snackbar is up; null otherwise. */
    val undo: StateFlow<StoredTicker?> = _undo.asStateFlow()

    /**
     * Removes [code] (swipe or accessibility action) and offers undo. False if storage failed, so the
     * screen can put the swiped row back.
     */
    suspend fun remove(code: String): Boolean = viewModelScope.async {
        // In the ViewModel's scope, not the caller's: once the row is deleted the list drops it, which
        // cancels the row's own scope, and the undo must still be set.
        val removed = repository.remove(code) ?: return@async false
        _undo.value = removed // replaces an older pending undo: only the latest removal can be undone
        true
    }.await()

    /** Puts [ticker] back. False if that failed (e.g. its code was added again meanwhile). */
    suspend fun undoRemove(ticker: StoredTicker): Boolean = viewModelScope.async { repository.restore(ticker) }.await()

    /** The undo is over, used or not: forget it. */
    fun undoDismissed(ticker: StoredTicker) {
        _undo.compareAndSet(ticker, null)
    }

    /** Pull-to-refresh: always fetches. */
    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    /** The app was opened or brought back, on either screen: refresh unless one finished in the last minute. */
    fun onAppStarted() {
        viewModelScope.launch { repository.refreshUnlessRecent(AUTO_REFRESH_MIN_GAP_MS) }
    }

    private val _addSheet = MutableStateFlow<AddTickerState?>(null)

    /** The add-ticker sheet, or null when it is closed. */
    val addSheet: StateFlow<AddTickerState?> = _addSheet.asStateFlow()

    private var checkJob: Job? = null

    fun openAddSheet() {
        _addSheet.value = AddTickerState()
    }

    fun closeAddSheet() {
        checkJob?.cancel()
        _addSheet.value = null
    }

    /** Editing the code clears the previous error. */
    fun onInputEdited() {
        _addSheet.update { if (it?.error == null) it else it.copy(error = null) }
    }

    fun submit(input: String) {
        val sheet = _addSheet.value ?: return
        if (sheet.checking || sheet.added) return
        _addSheet.value = sheet.copy(checking = true, error = null)
        checkJob = viewModelScope.launch {
            val result = repository.add(input)
            _addSheet.update { current ->
                current?.copy(checking = false, error = result.takeIf { it !is AddTickerResult.Added }, added = result is AddTickerResult.Added)
            }
        }
    }

    companion object {
        private const val AUTO_REFRESH_MIN_GAP_MS = 60_000L

        val Factory = viewModelFactory {
            initializer { WatchlistViewModel(DataGraph.watchlist(this[APPLICATION_KEY]!!)) }
        }
    }
}
