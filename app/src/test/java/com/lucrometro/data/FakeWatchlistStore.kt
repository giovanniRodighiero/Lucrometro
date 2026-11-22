package com.lucrometro.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [WatchlistStore] with the same rules as the Room one (order of addition, no duplicates). */
class FakeWatchlistStore : WatchlistStore {
    val rows = MutableStateFlow<List<StoredTicker>>(emptyList())

    override fun observe(): Flow<List<StoredTicker>> = rows
    override suspend fun codes() = rows.value.map { it.code }
    override suspend fun contains(code: String) = rows.value.any { it.code == code }
    override suspend fun get(code: String) = rows.value.firstOrNull { it.code == code }

    override suspend fun add(code: String, saved: SavedQuote): Boolean {
        if (contains(code)) return false
        rows.update { it + StoredTicker(code, saved, position = (it.maxOfOrNull { r -> r.position } ?: 0) + 1) }
        return true
    }

    override suspend fun remove(code: String): StoredTicker? {
        val row = rows.value.firstOrNull { it.code == code } ?: return null
        rows.update { list -> list - row }
        val removed = positions.value[code]
        positions.update { it - code }
        return row.copy(average = removed)
    }

    override suspend fun restore(ticker: StoredTicker): Boolean {
        if (contains(ticker.code)) return false
        rows.update { list ->
            (list.map { if (it.position >= ticker.position) it.copy(position = it.position + 1) else it } + ticker.copy(average = null))
                .sortedBy { it.position }
        }
        ticker.average?.let { average -> positions.update { it + (ticker.code to average) } }
        return true
    }

    val positions = MutableStateFlow<Map<String, Position>>(emptyMap())

    override fun observePosition(code: String): Flow<Position?> = positions.map { it[code] }

    override suspend fun savePosition(code: String, position: Position): Boolean {
        if (!contains(code)) return false
        positions.update { it + (code to position) }
        return true
    }

    override suspend fun saveQuote(code: String, saved: SavedQuote) {
        rows.update { list -> list.map { if (it.code == code) it.copy(saved = saved) else it } }
    }
}
