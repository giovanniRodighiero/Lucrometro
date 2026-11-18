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

    /** Deletes [code] and returns the deleted row, in one transaction; null if it wasn't stored. */
    @Transaction
    suspend fun take(code: String): TickerEntity? = get(code)?.also { delete(code) }

    /** Moves every row at or after [position] one place down the list, to make room for a restored row. */
    @Query("UPDATE watchlist SET position = position + 1 WHERE position >= :position")
    suspend fun shiftFrom(position: Long)

    /**
     * Re-inserts a removed row at its old place. Tickers added since may hold that position (a new one
     * takes MAX + 1, which a removed last row had), so the rows from there on shift down by one first:
     * positions stay unique and the restored row comes back before them. False if its code is stored again.
     */
    @Transaction
    suspend fun putBack(ticker: TickerEntity): Boolean {
        if (contains(ticker.code)) return false
        shiftFrom(ticker.position)
        insert(ticker)
        return true
    }

    /** Replaces the quote columns of an existing row; a missing code is ignored. */
    @Update(entity = TickerEntity::class)
    suspend fun saveQuote(update: QuoteUpdate)
}
