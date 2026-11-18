package com.lucrometro.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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

/** The last quote fetched successfully for a ticker, and when (epoch millis). */
data class SavedQuote(val quote: Quote, val fetchedAt: Long)

/** One tracked ticker. */
data class WatchlistEntry(
    val code: String,
    /** Last successful quote; null if none was ever saved. */
    val saved: SavedQuote? = null,
    /** The latest fetch for this ticker failed. */
    val failed: Boolean = false,
    /**
     * [saved] can't be shown as current: the latest fetch failed, or it was fetched before this app
     * session and no fetch has replaced it yet (e.g. right after a restart, or offline).
     */
    val stale: Boolean = false,
)

/**
 * The tracked tickers, in order of addition, with their last successful quote. Codes and quotes are
 * persisted in [store]. Which tickers failed their latest fetch is kept in memory; a quote saved before
 * this session counts as stale until a fetch in this session replaces it.
 */
class WatchlistRepository(
    private val source: QuoteSource,
    private val store: WatchlistStore,
    private val io: CoroutineDispatcher = Dispatchers.IO,
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

    /**
     * One request to the source at a time (PRD), shared by add and refresh. A coroutine lock, not the
     * source's own monitor: waiting for it can be cancelled, so closing the add sheet while a refresh
     * holds the source abandons the check at once instead of waiting for the refresh's request.
     */
    private val sourceLock = Mutex()

    private suspend fun fetch(code: String): Result<Quote> =
        sourceLock.withLock { runInterruptible(io) { source.quote(code) } }

    /** Chart history of one ticker and range, and when it was fetched (the repository's clock). */
    private class CachedHistory(val candles: List<Candle>, val fetchedAt: Long)

    /**
     * Session cache of chart history (PRD 5), in memory only: a range fetched for a ticker is reused, so reopening
     * it or switching back to it makes no request. Entries expire after [HISTORY_TTL_MILLIS]: the last bar is the live
     * price, so a long-lived process must not show a chart that disagrees with a refreshed header.
     */
    private val historyCache = ConcurrentHashMap<Pair<String, HistoryRange>, CachedHistory>()

    /** The cached candles for [code] and [range], or null if not fetched (or expired) in this session. */
    fun cachedHistory(code: String, range: HistoryRange): List<Candle>? =
        historyCache[code to range]?.takeIf { clock() - it.fetchedAt < HISTORY_TTL_MILLIS }?.candles

    /**
     * Candles for the details chart. Served from the session cache unless [bypassCache] (retry); otherwise it takes its
     * turn on the source like every other request, and a usable result is cached.
     */
    suspend fun history(code: String, range: HistoryRange, bypassCache: Boolean = false): Result<List<Candle>> {
        if (!bypassCache) cachedHistory(code, range)?.let { return Result.success(it) }
        val result = sourceLock.withLock {
            // The request before this one may have fetched the same range while this one waited.
            val fetched = if (bypassCache) null else cachedHistory(code, range)
            if (fetched != null) Result.success(fetched) else runInterruptible(io) { source.history(code, range) }
        }
        val candles = result.getOrNull()
        // Not if the ticker was removed while the request ran: that would bring its data back (PRD 3).
        if (candles != null && candles.size >= 2 && storeCall { store.contains(code) } != false) {
            historyCache[code to range] = CachedHistory(candles, clock())
        }
        return result
    }

    /** Normalizes [input], validates it with a live quote and saves it only if the quote resolves. */
    suspend fun add(input: String): AddTickerResult {
        val code = normalizeTicker(input)
        if (code.isEmpty()) return AddTickerResult.Empty
        if (!TICKER_FORMAT.matches(code)) return AddTickerResult.InvalidFormat(code)
        val known = storeCall { store.contains(code) } ?: return AddTickerResult.SaveFailed(code)
        if (known) return AddTickerResult.Duplicate(code)

        // Interruptible: cancelling the caller (sheet closed) aborts the blocking request instead of
        // leaving it to hold the source until it times out.
        val saved = SavedQuote(fetch(code).getOrElse { e ->
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
     * Removes [code] and its saved quote (PRD 3). Returns the removed ticker so it can be [restore]d
     * (undo); null if it wasn't stored or storage failed.
     */
    suspend fun remove(code: String): StoredTicker? {
        val removed = storeCall { store.remove(code) } ?: return null
        historyCache.keys.removeIf { it.first == code } // PRD 3: removing also drops its cached data
        failed.update { it - code }
        return removed
    }

    /** Undoes [remove]: the ticker comes back at its old place with its saved quote. */
    suspend fun restore(ticker: StoredTicker): Boolean = storeCall { store.restore(ticker) } ?: false

    /**
     * Fetches a fresh quote for every ticker, one at a time (PRD). A failed fetch (or a failed save)
     * keeps the last saved quote and marks the entry failed, so the UI shows it as stale; it never
     * substitutes data. Does nothing if a refresh is already running.
     */
    suspend fun refresh() {
        if (!_refreshing.compareAndSet(expect = false, update = true)) return
        try {
            val codes = storeCall { store.codes() } ?: return
            for (code in codes) {
                val saved = fetch(code).getOrNull()?.let { SavedQuote(it, clock()) }
                // Removed while the request was running: don't mark it failed (an undo would bring it
                // back looking stale). A storage error here is not "removed", so it carries on.
                if (storeCall { store.contains(code) } == false) continue
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

    companion object {
        const val HISTORY_TTL_MILLIS = 60 * 60 * 1000L
    }

    /** Runs a storage call; a storage failure (I/O, disk full) becomes null instead of crashing the app. */
    private inline fun <T> storeCall(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
