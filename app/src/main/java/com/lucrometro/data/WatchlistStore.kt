package com.lucrometro.data

import kotlinx.coroutines.flow.Flow

/**
 * A stored ticker: its code, its last successful quote (if one was ever saved), its place in the list
 * and when it was added (epoch millis). [position], [addedAt] and [average] (the owner's average price; filled only by
 * [WatchlistStore.remove]) let an undone removal come back unchanged.
 */
data class StoredTicker(
    val code: String,
    val saved: SavedQuote?,
    val position: Long = 0,
    val addedAt: Long = 0,
    val average: Position? = null,
    /** The ticker's trades; filled only by [WatchlistStore.remove]. */
    val trades: List<Trade> = emptyList(),
)

/**
 * Persistent storage for the watchlist (PRD section 5): ticker codes in order of addition, each with
 * its last successful quote. Room implements it in `data/db`; tests use a fake.
 */
interface WatchlistStore {
    /** All tickers in order of addition; emits again on every change. */
    fun observe(): Flow<List<StoredTicker>>

    suspend fun codes(): List<String>

    suspend fun contains(code: String): Boolean

    /** The stored ticker [code], or null if it is not stored. */
    suspend fun get(code: String): StoredTicker?

    /** Adds [code] at the end of the list with its first quote. False if it was already stored. */
    suspend fun add(code: String, saved: SavedQuote): Boolean

    /** Replaces the last successful quote of [code]. Does nothing if [code] is not stored. */
    suspend fun saveQuote(code: String, saved: SavedQuote)

    /** The average price of [code] (decision 0040), or null; emits again on every change. */
    fun observePosition(code: String): Flow<Position?>

    /** Saves the average price of [code], replacing the old one. False if [code] is not stored (removed meanwhile). */
    suspend fun savePosition(code: String, position: Position): Boolean

    /** The trades of [code], in no order; emits again on every change. */
    fun observeTrades(code: String): Flow<List<Trade>>

    suspend fun trades(code: String): List<Trade>

    /** Every stored ticker's trades, in no order; emits again on every change (the portfolio, decision 0049). */
    fun observeAllTrades(): Flow<List<Trade>>

    /** Every typed average price by ticker code; emits again on every change. */
    fun observePositions(): Flow<Map<String, Position>>

    /** Adds (id 0) or replaces (same id) [trades] of [code] at once. False if [code] is not stored. */
    suspend fun saveTrades(code: String, trades: List<Trade>): Boolean

    suspend fun deleteTrade(id: Long)

    /** Deletes [code] with its saved quote, average price and trades. Returns what was deleted, for undo; null if it wasn't stored. */
    suspend fun remove(code: String): StoredTicker?

    /** Puts back a removed ticker at its old place, with its average price and trades. False if its code was added again meanwhile. */
    suspend fun restore(ticker: StoredTicker): Boolean
}
