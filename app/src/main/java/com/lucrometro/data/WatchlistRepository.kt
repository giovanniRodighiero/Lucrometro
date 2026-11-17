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

/** The last quote fetched successfully for a ticker, and when (epoch millis). */
data class SavedQuote(val quote: Quote, val fetchedAt: Long)

/** One tracked ticker. */
data class WatchlistEntry(
    val code: String,
    /** Last successful quote; null if none was ever fetched. */
    val saved: SavedQuote? = null,
    /** The latest fetch for this ticker failed, so [saved] (if any) is stale. */
    val failed: Boolean = false,
)

/**
 * The tracked tickers, in order of addition, with their last successful quote. In memory for now:
 * Room comes next (roadmap, PRD section 5), behind this same API.
 */
class WatchlistRepository(
    private val source: QuoteSource,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _entries = MutableStateFlow<List<WatchlistEntry>>(emptyList())
    val entries: StateFlow<List<WatchlistEntry>> = _entries.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshedAt = MutableStateFlow<Long?>(null)

    /** When the last refresh finished with at least one ticker updated (epoch millis); null before that. */
    val refreshedAt: StateFlow<Long?> = _refreshedAt.asStateFlow()

    private fun has(code: String) = _entries.value.any { it.code == code }

    /** Normalizes [input], validates it with a live quote and saves it only if the quote resolves. */
    suspend fun add(input: String): AddTickerResult {
        val code = normalizeTicker(input)
        if (code.isEmpty()) return AddTickerResult.Empty
        if (!TICKER_FORMAT.matches(code)) return AddTickerResult.InvalidFormat(code)
        if (has(code)) return AddTickerResult.Duplicate(code)

        // Interruptible: cancelling the caller (sheet closed) aborts the blocking request instead of
        // leaving it to hold the source until it times out.
        val quote = runInterruptible(io) { source.quote(code) }
        val saved = SavedQuote(quote.getOrElse { e ->
            return when (e) {
                is TickerNotFoundException -> AddTickerResult.NotFound(code)
                is NoPriceException -> AddTickerResult.NoPrice(code)
                else -> AddTickerResult.CheckFailed(code)
            }
        }, clock())

        var duplicate = false
        _entries.update { list ->
            if (list.any { it.code == code }) list.also { duplicate = true } else list + WatchlistEntry(code, saved)
        }
        return if (duplicate) AddTickerResult.Duplicate(code) else AddTickerResult.Added(code)
    }

    /**
     * Fetches a fresh quote for every ticker, one at a time (PRD). A failed fetch keeps the last saved
     * quote and marks the entry failed, so the UI shows it as stale; it never substitutes data.
     * Does nothing if a refresh is already running.
     */
    suspend fun refresh() {
        if (!_refreshing.compareAndSet(expect = false, update = true)) return
        try {
            var updated = false
            for (code in _entries.value.map { it.code }) {
                val result = runInterruptible(io) { source.quote(code) }
                val fetchedAt = clock()
                result.onSuccess { updated = true }
                _entries.update { list ->
                    list.map { e ->
                        if (e.code != code) e
                        else result.fold({ e.copy(saved = SavedQuote(it, fetchedAt), failed = false) }, { e.copy(failed = true) })
                    }
                }
            }
            if (updated) _refreshedAt.value = clock()
        } finally {
            _refreshing.value = false
        }
    }
}
