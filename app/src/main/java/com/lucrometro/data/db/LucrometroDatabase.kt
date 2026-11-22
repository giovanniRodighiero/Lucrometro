package com.lucrometro.data.db

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers

/** The app database: the watchlist with each ticker's last quote (PRD section 5), and each one's average price (0040). */
@Database(
    entities = [TickerEntity::class, PositionEntity::class],
    version = 5,
    exportSchema = true,
    // v4 only adds the position table.
    autoMigrations = [AutoMigration(from = 3, to = 4)],
)
abstract class LucrometroDatabase : RoomDatabase() {
    abstract fun watchlist(): WatchlistDao

    companion object {
        fun create(context: Context): LucrometroDatabase =
            Room.databaseBuilder<LucrometroDatabase>(context.applicationContext, "lucrometro.db")
                // The platform SQLite: no need to bundle a native copy for one small table.
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5)
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

/**
 * v5 keeps one average per ticker (decision 0040, M1): each ticker's latest entry (highest id) survives, its shares and
 * last-buy date are dropped.
 */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE position_new (code TEXT NOT NULL, averagePrice REAL NOT NULL, PRIMARY KEY(code))",
        )
        connection.execSQL(
            """
            INSERT INTO position_new (code, averagePrice)
            SELECT code, averagePrice FROM position AS p WHERE id = (SELECT MAX(id) FROM position WHERE code = p.code)
            """.trimIndent(),
        )
        connection.execSQL("DROP TABLE position")
        connection.execSQL("ALTER TABLE position_new RENAME TO position")
    }
}
