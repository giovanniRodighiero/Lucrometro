package com.lucrometro.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt")
    fun observeAll(): Flow<List<TickerEntity>>

    @Query("SELECT code FROM watchlist ORDER BY addedAt")
    suspend fun codes(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE code = :code)")
    suspend fun contains(code: String): Boolean

    /** Returns the new row id, or -1 if [ticker]'s code is already stored. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ticker: TickerEntity): Long

    @Query(
        """
        UPDATE watchlist SET price = :price, currency = :currency, exchange = :exchange, name = :name,
            previousClose = :previousClose, priceDecimals = :priceDecimals, marketTime = :marketTime,
            fetchedAt = :fetchedAt
        WHERE code = :code
        """,
    )
    suspend fun saveQuote(
        code: String,
        price: Double,
        currency: String?,
        exchange: String?,
        name: String?,
        previousClose: Double?,
        priceDecimals: Int?,
        marketTime: Long,
        fetchedAt: Long,
    )
}
