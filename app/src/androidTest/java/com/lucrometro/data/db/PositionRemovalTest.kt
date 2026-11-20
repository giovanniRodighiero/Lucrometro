package com.lucrometro.data.db

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.data.SavedQuote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Removing a ticker deletes its position entries in the same transaction, and undo brings them back in order
 * (decision 0033). Runs on the real Room database, where the transactions live; the unit tests use a fake.
 */
@RunWith(AndroidJUnit4::class)
class PositionRemovalTest {
    private val db = Room.inMemoryDatabaseBuilder<LucrometroDatabase>(InstrumentationRegistry.getInstrumentation().targetContext)
        .setDriver(AndroidSQLiteDriver())
        .build()
    private val store = RoomWatchlistStore(db.watchlist())
    private val quote = SavedQuote(Quote("SMH.MI", 41.27, "EUR", "MIL", marketTimeEpoch = 0), fetchedAt = 0)
    private val first = Position(40.70, 120.0, LocalDate.of(2026, 10, 28), savedAt = 1)
    private val second = Position(41.02, 140.0, LocalDate.of(2026, 11, 14), savedAt = 2)

    @After
    fun close() = db.close()

    @Test
    fun removeDeletesEntriesAndUndoRestoresThem() = runBlocking {
        store.add("SMH.MI", quote)
        assertTrue(store.addPosition("SMH.MI", first))
        assertTrue(store.addPosition("SMH.MI", second))
        assertEquals(second, store.observePosition("SMH.MI").first())

        val removed = store.remove("SMH.MI")!!
        assertEquals(listOf(first, second), removed.positions)
        assertNull(store.observePosition("SMH.MI").first())
        assertTrue(db.watchlist().positions("SMH.MI").isEmpty())

        assertTrue(store.restore(removed))
        assertEquals(listOf(first, second), db.watchlist().positions("SMH.MI").map { it.toPosition() })
        assertEquals(second, store.observePosition("SMH.MI").first())
    }

    @Test
    fun noEntryForATickerThatIsNotStored() = runBlocking {
        assertFalse(store.addPosition("SMH.MI", first))
        assertTrue(db.watchlist().positions("SMH.MI").isEmpty())
    }
}
