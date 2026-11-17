package com.lucrometro.data

import kotlinx.coroutines.flow.Flow

/**
 * A stored ticker: its code, its last successful quote (if one was ever saved), its place in the list
 * and when it was added (epoch millis). [position] and [addedAt] let an undone removal come back unchanged.
 */
data class StoredTicker(val code: String, val saved: SavedQuote?, val position: Long = 0, val addedAt: Long = 0)

/**
 * Persistent storage for the watchlist (PRD section 5): ticker codes in order of addition, each with
 * its last successful quote. Room implements it in `data/db`; tests use a fake.
 */
interface WatchlistStore {
    /** All tickers in order of addition; emits again on every change. */
    fun observe(): Flow<List<StoredTicker>>

    suspend fun codes(): List<String>

    suspend fun contains(code: String): Boolean

    /** Adds [code] at the end of the list with its first quote. False if it was already stored. */
    suspend fun add(code: String, saved: SavedQuote): Boolean

    /** Replaces the last successful quote of [code]. Does nothing if [code] is not stored. */
    suspend fun saveQuote(code: String, saved: SavedQuote)

    /** Deletes [code] with its saved quote. Returns what was deleted, for undo; null if it wasn't stored. */
    suspend fun remove(code: String): StoredTicker?

    /** Puts back a removed ticker at its old position. False if its code was added again meanwhile. */
    suspend fun restore(ticker: StoredTicker): Boolean
}
