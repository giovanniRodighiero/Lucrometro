package com.lucrometro.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

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

    /** The quote resolved but storage failed (e.g. disk full). Nothing was saved. */
    data class SaveFailed(val code: String) : AddTickerResult
}

/** Trims and uppercases a pasted ticker code (PRD: "Normalize input"). */
fun normalizeTicker(input: String): String = input.trim().uppercase(Locale.ROOT)

// Yahoo symbols: letters, digits and . - ^ = (e.g. ENI.MI, ^FTSEMIB, EURUSD=X). Sources encode the
// code before putting it in a URL.
private val TICKER_FORMAT = Regex("[A-Z0-9.^=-]{1,20}")

/**
 * The tracked tickers, in order of addition, with their last successful quote. Codes and quotes are
 * persisted in [store]. Which tickers failed their latest fetch is kept in memory; a quote saved before
 * this session counts as stale until a fetch in this session replaces it. Requests take their turn on
 * the source through the [gate] shared with the [HistoryRepository].
 */
class WatchlistRepository(
    private val source: QuoteSource,
    private val store: WatchlistStore,
    /** Shared with every other repository that calls [source]: one gate, one request at a time. */
    private val gate: SourceGate,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** The repository lives as long as the process (DataGraph), so this is when the app started. */
    private val sessionStartedAt = clock()

    private val failed = MutableStateFlow<Set<String>>(emptySet())

    val entries: Flow<List<WatchlistEntry>> = combine(store.observe(), failed) { stored, failedCodes ->
        stored.map {
            val failed = it.code in failedCodes
            val stale = it.saved != null && (failed || it.saved.fetchedAt < sessionStartedAt)
            WatchlistEntry(it.code, it.saved, failed, stale)
        }
    }

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    @Volatile private var lastRefreshEndedAt: Long? = null

    private val removalListeners = CopyOnWriteArrayList<(String) -> Unit>()

    /** Calls [listener] with the code of every removed ticker, so data kept elsewhere for it is dropped too (PRD 3). */
    fun onRemoved(listener: (String) -> Unit) {
        removalListeners += listener
    }

    /** Whether [code] is stored. A storage error counts as stored: it is not a removal. */
    suspend fun isTracked(code: String): Boolean = storeCall { store.contains(code) } != false

    /**
     * Saves [fresh] as the quote of [code] unless the saved one is from a later trade (a refresh that finished first), and
     * clears the failed mark. Not for a ticker that is not stored (removed meanwhile).
     */
    suspend fun saveIfNewer(code: String, fresh: Quote) {
        val current = storeCall { store.get(code) } ?: return
        if (current.saved != null && current.saved.quote.marketTimeEpoch > fresh.marketTimeEpoch) return
        if (storeCall { store.saveQuote(code, SavedQuote(fresh, clock())) } != null) failed.update { it - code }
    }

    /** Marks [code]'s quote as failed, so it shows as stale: a fetch outside [refresh] failed (auto-refresh, decision 0035). */
    fun markFailed(code: String) = failed.update { it + code }

    /** Normalizes [input], validates it with a live quote and saves it only if the quote resolves. */
    suspend fun add(input: String): AddTickerResult {
        val code = normalizeTicker(input)
        if (code.isEmpty()) return AddTickerResult.Empty
        if (!TICKER_FORMAT.matches(code)) return AddTickerResult.InvalidFormat(code)
        val known = storeCall { store.contains(code) } ?: return AddTickerResult.SaveFailed(code)
        if (known) return AddTickerResult.Duplicate(code)

        // Cancelling the caller (sheet closed) aborts the request (see SourceGate).
        val saved = SavedQuote(gate.request { source.quote(code) }.getOrElse { e ->
            return when (e) {
                is TickerNotFoundException -> AddTickerResult.NotFound(code)
                is NoPriceException -> AddTickerResult.NoPrice(code)
                else -> AddTickerResult.CheckFailed(code)
            }
        }, clock())

        // The store rejects a code added meanwhile (e.g. twice in a row), so this stays a duplicate.
        return when (storeCall { store.add(code, saved) }) {
            true -> AddTickerResult.Added(code)
            false -> AddTickerResult.Duplicate(code)
            null -> AddTickerResult.SaveFailed(code)
        }
    }

    /**
     * Removes [code] with its saved quote and average price (PRD 3, decision 0040). Returns the removed ticker so it can be [restore]d
     * (undo); null if it wasn't stored or storage failed.
     */
    suspend fun remove(code: String): StoredTicker? {
        val removed = storeCall { store.remove(code) } ?: return null
        removalListeners.forEach { it(code) }
        failed.update { it - code }
        return removed
    }

    /** The average price of [code] (decision 0040), or null if none was saved. */
    fun position(code: String): Flow<Position?> = store.observePosition(code)

    /** Saves the average price of [code], replacing the old one. False if storage failed or [code] is no longer stored. */
    suspend fun savePosition(code: String, averagePrice: Double): Boolean =
        storeCall { store.savePosition(code, Position(averagePrice)) } == true

    /** Undoes [remove]: the ticker comes back at its old place with its saved quote and average price. */
    suspend fun restore(ticker: StoredTicker): Boolean = storeCall { store.restore(ticker) } ?: false

    /**
     * Fetches a fresh quote for every ticker, one at a time (PRD). A failed fetch (or a failed save)
     * keeps the last saved quote and marks the entry failed, so the UI shows it as stale; it never
     * substitutes data. A rate limit (429) stops it: the remaining entries are marked failed without a request. Does
     * nothing if a refresh is already running.
     */
    suspend fun refresh() {
        if (!_refreshing.compareAndSet(expect = false, update = true)) return
        try {
            val codes = storeCall { store.codes() } ?: return
            for ((i, code) in codes.withIndex()) {
                val result = gate.request { source.quote(code) }
                if (result.exceptionOrNull() is RateLimitedException) {
                    // Stop at the first 429: the rest keep their last quote, shown stale.
                    val rest = codes.drop(i).filter { isTracked(it) }
                    failed.update { it + rest }
                    break
                }
                val saved = result.getOrNull()?.let { SavedQuote(it, clock()) }
                // Removed while the request was running: don't mark it failed (an undo would bring it
                // back looking stale).
                if (!isTracked(code)) continue
                val ok = saved != null && storeCall { store.saveQuote(code, saved) } != null
                failed.update { if (ok) it - code else it + code }
            }
        } finally {
            lastRefreshEndedAt = clock()
            _refreshing.value = false
        }
    }

    /**
     * The automatic refresh (app opened or brought back): skipped if any refresh, manual or automatic,
     * finished less than [minGapMillis] ago, so rotation or a quick app switch doesn't refetch the list.
     */
    suspend fun refreshUnlessRecent(minGapMillis: Long) {
        val last = lastRefreshEndedAt
        if (last != null && clock() - last < minGapMillis) return
        refresh()
    }
}

/** Runs a storage call; a storage failure (I/O, disk full) becomes null instead of crashing the app. */
internal inline fun <T> storeCall(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
