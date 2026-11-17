package com.lucrometro.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

/** The app database: only the watchlist and each ticker's last quote (PRD section 5). */
@Database(entities = [TickerEntity::class], version = 1, exportSchema = true)
abstract class LucrometroDatabase : RoomDatabase() {
    abstract fun watchlist(): WatchlistDao

    companion object {
        fun create(context: Context): LucrometroDatabase =
            Room.databaseBuilder<LucrometroDatabase>(context.applicationContext, "lucrometro.db")
                // The platform SQLite: no need to bundle a native copy for one small table.
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
