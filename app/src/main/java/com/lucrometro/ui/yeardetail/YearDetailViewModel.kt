package com.lucrometro.ui.yeardetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucrometro.data.DataGraph
import com.lucrometro.data.PortfolioRepository
import com.lucrometro.data.YearGrowth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One year of the portfolio (decision 0051), from the same sums as the Portfolio tab. */
class YearDetailViewModel(repository: PortfolioRepository, year: Int) : ViewModel() {
    /** Null while loading, or when the year can't be added up (its prices are missing). */
    val year: StateFlow<YearGrowth?> = repository.portfolio
        .map { portfolio -> portfolio.years.firstOrNull { it.year == year } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(year: Int) = viewModelFactory {
            initializer { YearDetailViewModel(DataGraph.portfolio(this[APPLICATION_KEY]!!), year) }
        }
    }
}
