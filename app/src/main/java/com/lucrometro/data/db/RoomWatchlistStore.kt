package com.lucrometro.data.db

import com.lucrometro.data.Position
import com.lucrometro.data.SavedQuote
import com.lucrometro.data.StoredTicker
import com.lucrometro.data.Trade
import com.lucrometro.data.WatchlistStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWatchlistStore(private val dao: WatchlistDao) : WatchlistStore {
    override fun observe(): Flow<List<StoredTicker>> = dao.observeAll().map { rows -> rows.map { it.toStored() } }

    override suspend fun codes(): List<String> = dao.codes()

    override suspend fun contains(code: String): Boolean = dao.contains(code)

    override suspend fun get(code: String): StoredTicker? = dao.get(code)?.toStored()

    // Added at the moment its first quote was fetched: one clock, the repository's.
    override suspend fun add(code: String, saved: SavedQuote): Boolean =
        dao.insertLast(code, addedAt = saved.fetchedAt, quote = saved.toColumns())

    override suspend fun saveQuote(code: String, saved: SavedQuote) = dao.saveQuote(QuoteUpdate(code, saved.toColumns()))

    override fun observePosition(code: String): Flow<Position?> = dao.observePosition(code).map { it?.toPosition() }

    override suspend fun savePosition(code: String, position: Position): Boolean = dao.savePosition(position.toEntity(code))

    override suspend fun remove(code: String): StoredTicker? = dao.take(code)?.let { removed ->
        removed.ticker.toStored().copy(average = removed.position?.toPosition(), trades = removed.trades.map { it.toTrade() })
    }

    override suspend fun restore(ticker: StoredTicker): Boolean = dao.putBack(
        TickerEntity(ticker.code, ticker.position, ticker.addedAt, ticker.saved?.toColumns()),
        ticker.average?.toEntity(ticker.code),
        ticker.trades.map { it.toEntity() },
    )

    override fun observeTrades(code: String): Flow<List<Trade>> = dao.observeTrades(code).map { rows -> rows.map { it.toTrade() } }

    override suspend fun trades(code: String): List<Trade> = dao.trades(code).map { it.toTrade() }

    override suspend fun saveTrades(code: String, trades: List<Trade>): Boolean = dao.saveTrades(code, trades.map { it.toEntity() })

    override suspend fun deleteTrade(id: Long) = dao.deleteTrade(id)
}
