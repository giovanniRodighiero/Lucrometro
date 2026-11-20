package com.lucrometro.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY position")
    fun observeAll(): Flow<List<TickerEntity>>

    @Query("SELECT code FROM watchlist ORDER BY position")
    suspend fun codes(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE code = :code)")
    suspend fun contains(code: String): Boolean

    @Query("SELECT IFNULL(MAX(position), 0) FROM watchlist")
    suspend fun lastPosition(): Long

    @Insert
    suspend fun insert(ticker: TickerEntity)

    /** Adds [code] after the last row, in one transaction. False if it is already stored. */
    @Transaction
    suspend fun insertLast(code: String, addedAt: Long, quote: QuoteColumns): Boolean {
        if (contains(code)) return false
        insert(TickerEntity(code, position = lastPosition() + 1, addedAt = addedAt, quote = quote))
        return true
    }

    @Query("SELECT * FROM watchlist WHERE code = :code")
    suspend fun get(code: String): TickerEntity?

    @Query("DELETE FROM watchlist WHERE code = :code")
    suspend fun delete(code: String)

    /** Deletes [code] with its position entries and returns them, in one transaction; null if it wasn't stored. */
    @Transaction
    suspend fun take(code: String): RemovedTicker? {
        val ticker = get(code) ?: return null
        val positions = positions(code)
        delete(code)
        deletePositions(code)
        return RemovedTicker(ticker, positions)
    }

    /** Moves every row at or after [position] one place down the list, to make room for a restored row. */
    @Query("UPDATE watchlist SET position = position + 1 WHERE position >= :position")
    suspend fun shiftFrom(position: Long)

    /**
     * Re-inserts a removed row at its old place. Tickers added since may hold that position (a new one
     * takes MAX + 1, which a removed last row had), so the rows from there on shift down by one first:
     * positions stay unique and the restored row comes back before them. False if its code is stored again.
     */
    @Transaction
    suspend fun putBack(ticker: TickerEntity, positions: List<PositionEntity>): Boolean {
        if (contains(ticker.code)) return false
        shiftFrom(ticker.position)
        insert(ticker)
        insertPositions(positions)
        return true
    }

    /** Replaces the quote columns of an existing row; a missing code is ignored. */
    @Update(entity = TickerEntity::class)
    suspend fun saveQuote(update: QuoteUpdate)

    @Query("SELECT * FROM position WHERE code = :code ORDER BY id DESC LIMIT 1")
    fun observeLatestPosition(code: String): Flow<PositionEntity?>

    @Query("SELECT * FROM position WHERE code = :code ORDER BY id")
    suspend fun positions(code: String): List<PositionEntity>

    @Insert
    suspend fun insertPositions(positions: List<PositionEntity>)

    @Query("DELETE FROM position WHERE code = :code")
    suspend fun deletePositions(code: String)

    /** Adds a position entry, in one transaction with the check that its ticker is stored. */
    @Transaction
    suspend fun addPosition(position: PositionEntity): Boolean {
        if (!contains(position.code)) return false
        insertPositions(listOf(position))
        return true
    }
}

/** A deleted watchlist row with its position entries, for undo. */
class RemovedTicker(val ticker: TickerEntity, val positions: List<PositionEntity>)
