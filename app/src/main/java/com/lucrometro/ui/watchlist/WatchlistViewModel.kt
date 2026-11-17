package com.lucrometro.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.AddTickerResult
import com.lucrometro.data.DataGraph
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why the add-ticker sheet refused the input. [code] is the normalized code, when there is one. */
data class AddTickerError(val kind: Kind, val code: String = "") {
    enum class Kind { Empty, InvalidFormat, Duplicate, NotFound, NoPrice, CheckFailed }
}

/** The add-ticker sheet. The typed text lives in the sheet's TextFieldState, not here. */
data class AddTickerState(
    val checking: Boolean = false,
    val error: AddTickerError? = null,
    /** Set when the ticker was saved: the screen animates the sheet away, then calls [WatchlistViewModel.closeAddSheet]. */
    val added: Boolean = false,
)

class WatchlistViewModel(private val repository: WatchlistRepository) : ViewModel() {
    val tickers: StateFlow<List<String>> = repository.tickers

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
                current?.copy(checking = false, error = result.toError(), added = result is AddTickerResult.Added)
            }
        }
    }

    private fun AddTickerResult.toError(): AddTickerError? = when (this) {
        is AddTickerResult.Added -> null
        AddTickerResult.Empty -> AddTickerError(AddTickerError.Kind.Empty)
        is AddTickerResult.InvalidFormat -> AddTickerError(AddTickerError.Kind.InvalidFormat, code)
        is AddTickerResult.Duplicate -> AddTickerError(AddTickerError.Kind.Duplicate, code)
        is AddTickerResult.NotFound -> AddTickerError(AddTickerError.Kind.NotFound, code)
        is AddTickerResult.NoPrice -> AddTickerError(AddTickerError.Kind.NoPrice, code)
        is AddTickerResult.CheckFailed -> AddTickerError(AddTickerError.Kind.CheckFailed, code)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WatchlistViewModel(DataGraph.watchlist(this[APPLICATION_KEY]!!)) }
        }
    }
}
