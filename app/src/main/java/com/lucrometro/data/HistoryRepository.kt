package com.lucrometro.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Chart history of the tracked tickers. Requests take their turn on the source through the [gate] shared with the
 * [watchlist]. A 1D response carries the current quote, which is handed to the [watchlist] so the header agrees with
 * the chart's last bar at no extra request (decision 0029).
 *
 * Session cache (PRD 5), in memory only: a range fetched for a ticker is reused, so reopening it or switching back to
 * it makes no request. Entries expire: the last bar is the live price, so a long-lived process must not show a chart
 * that disagrees with a refreshed header. Intraday ranges after [INTRADAY_TTL_MILLIS], the others after
 * [HISTORY_TTL_MILLIS]. Removing a ticker drops its entries (PRD 3).
 */
class HistoryRepository(
    private val source: QuoteSource,
    private val gate: SourceGate,
    private val watchlist: WatchlistRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class CachedHistory(val history: History, val fetchedAt: Long)

    private val cache = ConcurrentHashMap<Pair<String, HistoryRange>, CachedHistory>()

    init {
        watchlist.onRemoved { code -> cache.keys.removeIf { it.first == code } }
    }

    /** The cached history for [code] and [range], or null if not fetched (or expired) in this session. */
    fun cached(code: String, range: HistoryRange): History? {
        val ttl = if (range.isIntraday) INTRADAY_TTL_MILLIS else HISTORY_TTL_MILLIS
        return cache[code to range]?.takeIf { clock() - it.fetchedAt < ttl }?.history
    }

    /** Candles for the details chart: from the session cache unless [bypassCache] (retry), otherwise fetched and cached. */
    suspend fun history(code: String, range: HistoryRange, bypassCache: Boolean = false): Result<History> {
        if (!bypassCache) cached(code, range)?.let { return Result.success(it) }
        val (result, fetchedNow) = gate.requestUnless(
            reuse = { if (bypassCache) null else cached(code, range)?.let { Result.success(it) } },
        ) { source.history(code, range) }
        val history = result.getOrNull()
        // Not if the ticker was removed while the request ran: that would bring its data back (PRD 3).
        if (history != null && history.isCacheable(range) && watchlist.isTracked(code)) {
            cache[code to range] = CachedHistory(history, clock())
            // A removal can land between the check and the write, and its listener has already run: undo the write.
            if (!watchlist.isTracked(code)) cache.remove(code to range)
        }
        // Only a response fetched just now: a cached one is older than the saved quote may be.
        val fresh = history?.quote
        if (fetchedNow && range == HistoryRange.DAY_1 && fresh != null) watchlist.saveIfNewer(code, fresh)
        return result
    }

    /** Usable results only (2+ bars); a 1D with none or one bar yet (before the first trades) is a real answer for 2 minutes. */
    private fun History.isCacheable(range: HistoryRange) = candles.size >= 2 || range == HistoryRange.DAY_1

    companion object {
        const val HISTORY_TTL_MILLIS = 60 * 60 * 1000L
        const val INTRADAY_TTL_MILLIS = 2 * 60 * 1000L
    }
}
