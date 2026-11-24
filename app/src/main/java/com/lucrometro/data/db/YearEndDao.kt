package com.lucrometro.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface YearEndDao {
    @Query("SELECT * FROM year_end_fetch WHERE code = :code")
    suspend fun fetch(code: String): YearEndFetchEntity?

    @Query("SELECT * FROM year_end WHERE code = :code ORDER BY year")
    suspend fun ends(code: String): List<YearEndEntity>

    @Query("DELETE FROM year_end WHERE code = :code")
    suspend fun deleteEnds(code: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFetch(fetch: YearEndFetchEntity)

    @Insert
    suspend fun insertEnds(ends: List<YearEndEntity>)

    /** Replaces everything saved for the ticker with one fetch's year ends, so their adjusted closes share one basis. */
    @Transaction
    suspend fun replace(fetch: YearEndFetchEntity, ends: List<YearEndEntity>) {
        deleteEnds(fetch.code)
        insertEnds(ends)
        insertFetch(fetch)
    }
}
