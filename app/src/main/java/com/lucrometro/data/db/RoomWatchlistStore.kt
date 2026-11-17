package com.lucrometro.data.db

import com.lucrometro.data.SavedQuote
import com.lucrometro.data.StoredTicker
import com.lucrometro.data.WatchlistStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWatchlistStore(
    private val dao: WatchlistDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : WatchlistStore {
    override fun observe(): Flow<List<StoredTicker>> = dao.observeAll().map { rows -> rows.map { it.toStored() } }

    override suspend fun codes(): List<String> = dao.codes()

    override suspend fun contains(code: String): Boolean = dao.contains(code)

    override suspend fun add(code: String, saved: SavedQuote): Boolean =
        dao.insert(newTickerEntity(code, clock(), saved)) != -1L

    override suspend fun saveQuote(code: String, saved: SavedQuote) {
        val q = saved.quote
        dao.saveQuote(
            code, q.price, q.currency, q.exchange, q.name, q.previousClose, q.priceDecimals,
            q.marketTimeEpoch, saved.fetchedAt,
        )
    }
}
