package com.lucrometro.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Year ends being fetched: [done] of [total] tickers. */
data class YearEndProgress(val done: Int, val total: Int)

/**
 * The portfolio (decisions 0049-0052): the stored tickers added up with their saved year ends. Year ends are fetched once
 * per ticker (daily bars, one request each, [spacingMillis] apart, through the [gate] shared with the other repositories),
 * and again only when a new year has closed or older trades need earlier years. A 429 stops the fetching for the rest of
 * the process; any other failure leaves that ticker out until the app is opened again ([retryFailed]).
 */
class PortfolioRepository(
    private val source: QuoteSource,
    private val gate: SourceGate,
    private val store: WatchlistStore,
    private val yearStore: YearEndStore,
    watchlist: WatchlistRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val spacingMillis: Long = 5_000,
) {
    private val years = MutableStateFlow<Map<String, TickerYears>>(emptyMap())

    private val _progress = MutableStateFlow<YearEndProgress?>(null)

    /** Non-null while year ends are being fetched. */
    val progress: StateFlow<YearEndProgress?> = _progress.asStateFlow()

    private val failed = MutableStateFlow<Set<String>>(emptySet())

    @Volatile private var rateLimited = false

    /** When the last history request was sent: a restarted loop still waits [spacingMillis] after it. */
    private var lastRequestAt: Long? = null

    private val tickers: Flow<List<PortfolioTicker>> =
        combine(watchlist.entries, store.observeAllTrades(), store.observePositions()) { entries, trades, positions ->
            val byCode = trades.groupBy { it.code }
            entries.map { PortfolioTicker(it, byCode[it.code].orEmpty(), positions[it.code]) }
        }

    val portfolio: Flow<Portfolio> = combine(tickers, years) { list, saved -> portfolio(list, saved, today()) }

    /**
     * The year ends each counted ticker needs: from the year before its first trade (the base of that year's market
     * return). Only EUR tickers are counted (0049).
     */
    val needs: Flow<Map<String, Int>> = tickers.map { list ->
        list.filter { it.trades.isNotEmpty() && it.entry.saved?.quote?.currency == "EUR" }
            .associate { t -> t.entry.code to t.trades.minOf { it.date }.year - 1 }
    }.distinctUntilChanged()

    /** Tickers whose year ends could not be fetched in this run of the app: their years are left out until a retry. */
    val failedCodes: StateFlow<Set<String>> = failed.asStateFlow()

    /**
     * Tickers whose year ends are settled for now: saved, or failed. A year missing only settled tickers' prices is left
     * out; one missing others' is still loading.
     */
    val settledCodes: Flow<Set<String>> = combine(years, failed) { saved, failedNow -> saved.keys + failedNow }

    private val retries = MutableStateFlow(0)

    /** The app was opened again: tickers that failed (not after a 429) get another try. */
    fun retryFailed() {
        // Nothing failed: don't restart a loop that is fine (it would cancel a fetch half-way).
        if (failed.value.isEmpty()) return
        failed.value = emptySet()
        retries.update { it + 1 }
    }

    /** Keeps the year ends of the counted tickers loaded while collected: again when the trades or [retryFailed] change. */
    suspend fun keepYearEndsLoaded() = combine(needs, retries) { needs, _ -> needs }.collectLatest { ensureYearEnds(it) }

    /** Loads the saved year ends of [needs] and fetches the ones missing or out of date, one at a time. */
    suspend fun ensureYearEnds(needs: Map<String, Int>) {
        for (code in needs.keys - years.value.keys) {
            storeCall { yearStore.get(code) }?.let { saved -> years.update { it + (code to saved) } }
        }
        val lastClosed = today().year - 1
        val due = needs.filter { (code, from) ->
            val saved = years.value[code]
            code !in failed.value && (saved == null || saved.fromYear > from || saved.throughYear < lastClosed)
        }
        if (due.isEmpty()) return
        if (rateLimited) {
            failed.update { it + due.keys }
            return
        }
        _progress.value = YearEndProgress(0, due.size)
        try {
            for ((i, entry) in due.entries.withIndex()) {
                val (code, from) = entry
                // Every request, the first included: [keepYearEndsLoaded] may have just cancelled a loop mid-fetch.
                lastRequestAt?.let { delay(it + spacingMillis - clock()) }
                lastRequestAt = clock()
                // From 1 Dec of the year before the first trade: its last bars, past any holiday.
                val since = LocalDate.of(from, 12, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                val result = gate.request { source.dailyHistory(code, since) }
                val error = result.exceptionOrNull()
                if (error is RateLimitedException || error is IOException) {
                    // A 429 stops for good (CLAUDE.md); a dead connection would make each ticker wait its own timeout.
                    if (error is RateLimitedException) rateLimited = true
                    failed.update { it + due.keys.drop(i) }
                    break
                }
                val history = result.getOrNull()
                val saved = history?.let { yearEnds(code, it, from, clock()) }
                if (saved == null || storeCall { yearStore.save(saved) } == null) {
                    failed.update { it + code }
                } else {
                    years.update { it + (code to saved) }
                }
                _progress.value = YearEndProgress(i + 1, due.size)
            }
        } finally {
            _progress.value = null
        }
    }

    /** Today in the phone's clock: the current year, and the end of its "so far". */
    private fun today(): LocalDate = Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate()
}
