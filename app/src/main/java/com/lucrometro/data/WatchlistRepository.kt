package com.lucrometro.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
 * The tracked tickers, in order of addition, with their last successful quote. Codes and quotes are
 * persisted in [store]; which tickers failed their latest fetch is kept in memory only, so after a
 * restart a ticker shows its saved quote until the first refresh says otherwise.
 */
class WatchlistRepository(
    private val source: QuoteSource,
    private val store: WatchlistStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val failed = MutableStateFlow<Set<String>>(emptySet())

    val entries: Flow<List<WatchlistEntry>> = combine(store.observe(), failed) { stored, failedCodes ->
        stored.map { WatchlistEntry(it.code, it.saved, failed = it.code in failedCodes) }
    }

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshedAt = MutableStateFlow<Long?>(null)

    /** When the last refresh finished with at least one ticker updated (epoch millis); null before that. */
    val refreshedAt: StateFlow<Long?> = _refreshedAt.asStateFlow()

    /** Normalizes [input], validates it with a live quote and saves it only if the quote resolves. */
    suspend fun add(input: String): AddTickerResult {
        val code = normalizeTicker(input)
        if (code.isEmpty()) return AddTickerResult.Empty
        if (!TICKER_FORMAT.matches(code)) return AddTickerResult.InvalidFormat(code)
        if (store.contains(code)) return AddTickerResult.Duplicate(code)

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

        // The store rejects a code added meanwhile (e.g. twice in a row), so this stays a duplicate.
        return if (store.add(code, saved)) AddTickerResult.Added(code) else AddTickerResult.Duplicate(code)
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
            for (code in store.codes()) {
                val result = runInterruptible(io) { source.quote(code) }
                val quote = result.getOrNull()
                if (quote != null) {
                    store.saveQuote(code, SavedQuote(quote, clock()))
                    failed.update { it - code }
                    updated = true
                } else {
                    failed.update { it + code }
                }
            }
            if (updated) _refreshedAt.value = clock()
        } finally {
            _refreshing.value = false
        }
    }
}
