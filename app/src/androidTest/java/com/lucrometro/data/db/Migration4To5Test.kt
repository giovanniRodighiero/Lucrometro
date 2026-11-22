package com.lucrometro.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lucrometro.data.Position
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v4 to v5 (decision 0040, M1) keeps each ticker's latest average and drops the older entries: a wrong pick would
 * silently replace the owner's average. Builds a v4 file by hand (the v4 schema from `schemas/`), then opens it with
 * Room, which runs the migration and checks the result against the v5 schema.
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "migration-4-5-test.db"

    @After
    fun delete() {
        context.deleteDatabase(name)
    }

    @Test
    fun keepsTheLatestAveragePerTicker() = runBlocking {
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { v4 ->
            V4_SCHEMA.forEach(v4::execSQL)
            v4.execSQL("INSERT INTO watchlist (code, position, addedAt) VALUES ('SMH.MI', 1, 0), ('ENI.MI', 2, 0), ('ISP.MI', 3, 0)")
            // ENI.MI's latest entry (highest id) has a lower average and an earlier buy date than the one before it.
            v4.execSQL(
                """
                INSERT INTO position (id, code, averagePrice, shares, lastBuyDay, savedAt) VALUES
                (1, 'SMH.MI', 40.70, 120, 20389, 1), (2, 'ENI.MI', 14.20, NULL, 20390, 2),
                (3, 'SMH.MI', 41.02, 140, 20406, 3), (4, 'ENI.MI', 13.90, 50, 20380, 4)
                """.trimIndent(),
            )
            v4.version = 4
        }

        val db = Room.databaseBuilder<LucrometroDatabase>(context, name)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5)
            .build()
        try {
            val store = RoomWatchlistStore(db.watchlist())
            assertEquals(Position(41.02), store.observePosition("SMH.MI").first())
            assertEquals(Position(13.90), store.observePosition("ENI.MI").first())
            assertNull(store.observePosition("ISP.MI").first())
            assertEquals(listOf("SMH.MI", "ENI.MI", "ISP.MI"), store.codes())
        } finally {
            db.close()
        }
    }
}

/** Copied from `schemas/com.lucrometro.data.db.LucrometroDatabase/4.json`. */
private val V4_SCHEMA = listOf(
    "CREATE TABLE IF NOT EXISTS `watchlist` (`code` TEXT NOT NULL, `position` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, " +
        "`price` REAL, `currency` TEXT, `exchange` TEXT, `name` TEXT, `previousClose` REAL, `priceDecimals` INTEGER, " +
        "`marketTime` INTEGER, `fetchedAt` INTEGER, `exchangeZone` TEXT, PRIMARY KEY(`code`))",
    "CREATE TABLE IF NOT EXISTS `position` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, " +
        "`averagePrice` REAL NOT NULL, `shares` REAL, `lastBuyDay` INTEGER NOT NULL, `savedAt` INTEGER NOT NULL)",
    "CREATE INDEX IF NOT EXISTS `index_position_code` ON `position` (`code`)",
    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9f07e5c658dbca324fd9cf759e994917')",
)
