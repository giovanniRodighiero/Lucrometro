package com.lucrometro.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [WatchlistStore] with the same rules as the Room one (order of addition, no duplicates). */
class FakeWatchlistStore : WatchlistStore {
    val rows = MutableStateFlow<List<StoredTicker>>(emptyList())

    override fun observe(): Flow<List<StoredTicker>> = rows
    override suspend fun codes() = rows.value.map { it.code }
    override suspend fun contains(code: String) = rows.value.any { it.code == code }

    override suspend fun add(code: String, saved: SavedQuote): Boolean {
        if (contains(code)) return false
        rows.update { it + StoredTicker(code, saved) }
        return true
    }

    override suspend fun saveQuote(code: String, saved: SavedQuote) {
        rows.update { list -> list.map { if (it.code == code) it.copy(saved = saved) else it } }
    }
}
