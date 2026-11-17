package com.lucrometro.data.db

import com.lucrometro.data.SavedQuote
import com.lucrometro.data.StoredTicker
import com.lucrometro.data.WatchlistStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWatchlistStore(private val dao: WatchlistDao) : WatchlistStore {
    override fun observe(): Flow<List<StoredTicker>> = dao.observeAll().map { rows -> rows.map { it.toStored() } }

    override suspend fun codes(): List<String> = dao.codes()

    override suspend fun contains(code: String): Boolean = dao.contains(code)

    // Added at the moment its first quote was fetched: one clock, the repository's.
    override suspend fun add(code: String, saved: SavedQuote): Boolean =
        dao.insertLast(code, addedAt = saved.fetchedAt, quote = saved.toColumns())

    override suspend fun saveQuote(code: String, saved: SavedQuote) = dao.saveQuote(QuoteUpdate(code, saved.toColumns()))
}
