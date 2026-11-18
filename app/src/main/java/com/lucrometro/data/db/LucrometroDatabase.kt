package com.lucrometro.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers

/** The app database: only the watchlist and each ticker's last quote (PRD section 5). */
@Database(entities = [TickerEntity::class], version = 3, exportSchema = true)
abstract class LucrometroDatabase : RoomDatabase() {
    abstract fun watchlist(): WatchlistDao

    companion object {
        fun create(context: Context): LucrometroDatabase =
            Room.databaseBuilder<LucrometroDatabase>(context.applicationContext, "lucrometro.db")
                // The platform SQLite: no need to bundle a native copy for one small table.
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}

/** v2 orders the list by an explicit `position` instead of `addedAt` (a wall clock that can go backwards). */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE watchlist ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        // Number the existing rows 1..n in their v1 order (addedAt, then code to break ties).
        connection.execSQL(
            """
            UPDATE watchlist SET position = (
                SELECT COUNT(*) FROM watchlist AS w
                WHERE w.addedAt < watchlist.addedAt
                   OR (w.addedAt = watchlist.addedAt AND w.code <= watchlist.code)
            )
            """.trimIndent(),
        )
    }
}

/** v3 stores the exchange's time zone with the quote (decision 0031). Existing quotes get it on their next fetch. */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE watchlist ADD COLUMN exchangeZone TEXT")
    }
}
