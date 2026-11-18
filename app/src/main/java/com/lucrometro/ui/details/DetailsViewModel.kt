package com.lucrometro.ui.details

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.DataGraph
import com.lucrometro.data.WatchlistEntry
import com.lucrometro.data.WatchlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The details screen of one ticker. For now it only shows the saved quote: it makes no request. */
class DetailsViewModel(repository: WatchlistRepository, code: String) : ViewModel() {
    /** The ticker's entry, or null while the list loads (or if the ticker is gone). */
    val entry: StateFlow<WatchlistEntry?> = repository.entries
        .map { list -> list.firstOrNull { it.code == code } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(code: String) = viewModelFactory {
            initializer {
                DetailsViewModel(DataGraph.watchlist(this[APPLICATION_KEY] as Application), code)
            }
        }
    }
}
