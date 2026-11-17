package com.lucrometro.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import java.util.Locale

/** Outcome of [WatchlistRepository.add]. */
sealed interface AddTickerResult {
    data class Added(val code: String) : AddTickerResult
    data object Empty : AddTickerResult
    data class InvalidFormat(val code: String) : AddTickerResult
    data class Duplicate(val code: String) : AddTickerResult
    data class NotFound(val code: String) : AddTickerResult
    data class NoPrice(val code: String) : AddTickerResult

    /** The check could not be completed (offline, timeout, source error). Nothing was saved. */
    data class CheckFailed(val code: String) : AddTickerResult
}

/** Trims and uppercases a pasted ticker code (PRD: "Normalize input"). */
fun normalizeTicker(input: String): String = input.trim().uppercase(Locale.ROOT)

// Yahoo symbols: letters, digits and . - ^ = (e.g. ENI.MI, ^FTSEMIB, EURUSD=X). Sources encode the
// code before putting it in a URL.
private val TICKER_FORMAT = Regex("[A-Z0-9.^=-]{1,20}")

/**
 * The list of tracked ticker codes, in order of addition. In memory for now: Room comes next
 * (roadmap, PRD section 5), behind this same API.
 */
class WatchlistRepository(
    private val source: QuoteSource,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _tickers = MutableStateFlow<List<String>>(emptyList())
    val tickers: StateFlow<List<String>> = _tickers.asStateFlow()

    /** Normalizes [input], validates it with a live quote and saves it only if the quote resolves. */
    suspend fun add(input: String): AddTickerResult {
        val code = normalizeTicker(input)
        if (code.isEmpty()) return AddTickerResult.Empty
        if (!TICKER_FORMAT.matches(code)) return AddTickerResult.InvalidFormat(code)
        if (code in _tickers.value) return AddTickerResult.Duplicate(code)

        // Interruptible: cancelling the caller (sheet closed) aborts the blocking request instead of
        // leaving it to hold the source until it times out.
        val quote = runInterruptible(io) { source.quote(code) }
        quote.exceptionOrNull()?.let { e ->
            return when (e) {
                is TickerNotFoundException -> AddTickerResult.NotFound(code)
                is NoPriceException -> AddTickerResult.NoPrice(code)
                else -> AddTickerResult.CheckFailed(code)
            }
        }

        var duplicate = false
        _tickers.update { list -> if (code in list) list.also { duplicate = true } else list + code }
        return if (duplicate) AddTickerResult.Duplicate(code) else AddTickerResult.Added(code)
    }
}
