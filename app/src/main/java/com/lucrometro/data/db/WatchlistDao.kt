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

    /** Replaces the quote columns of an existing row; a missing code is ignored. */
    @Update(entity = TickerEntity::class)
    suspend fun saveQuote(update: QuoteUpdate)
}
