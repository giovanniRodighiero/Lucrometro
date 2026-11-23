package com.lucrometro.data.db

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lucrometro.data.Position
import com.lucrometro.data.Quote
import com.lucrometro.data.SavedQuote
import com.lucrometro.data.Side
import com.lucrometro.data.Trade
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
import java.time.LocalTime

/**
 * Removing a ticker deletes its average price and trades in the same transaction, and undo brings it back (decisions 0033, 0040).
 * Runs on the real Room database, where the transactions live; the unit tests use a fake.
 */
@RunWith(AndroidJUnit4::class)
class PositionRemovalTest {
    private val db = Room.inMemoryDatabaseBuilder<LucrometroDatabase>(InstrumentationRegistry.getInstrumentation().targetContext)
        .setDriver(AndroidSQLiteDriver())
        .build()
    private val store = RoomWatchlistStore(db.watchlist())
    private val quote = SavedQuote(Quote("SMH.MI", 41.27, "EUR", "MIL", marketTimeEpoch = 0), fetchedAt = 0)

    @After
    fun close() = db.close()

    @Test
    fun saveReplacesAndUndoRestoresTheAverage() = runBlocking {
        store.add("SMH.MI", quote)
        assertTrue(store.savePosition("SMH.MI", Position(40.70)))
        assertTrue(store.savePosition("SMH.MI", Position(41.02)))
        assertEquals(Position(41.02), store.observePosition("SMH.MI").first())

        val removed = store.remove("SMH.MI")!!
        assertEquals(Position(41.02), removed.average)
        assertNull(store.observePosition("SMH.MI").first())
        assertNull(db.watchlist().position("SMH.MI"))

        assertTrue(store.restore(removed))
        assertEquals(Position(41.02), store.observePosition("SMH.MI").first())
    }

    @Test
    fun removeTakesTheTradesAndUndoBringsThemBack() = runBlocking {
        store.add("SMH.MI", quote)
        val buy = Trade(code = "SMH.MI", side = Side.BUY, date = LocalDate.of(2026, 11, 6), time = LocalTime.of(10, 41), quantity = 2.5, price = 42.85)
        assertTrue(store.saveTrades("SMH.MI", listOf(buy)))
        val saved = store.trades("SMH.MI").single()

        val removed = store.remove("SMH.MI")!!
        assertEquals(listOf(saved), removed.trades)
        assertTrue(store.trades("SMH.MI").isEmpty())

        assertTrue(store.restore(removed))
        assertEquals(listOf(saved), store.trades("SMH.MI"))
    }

    @Test
    fun noAverageForATickerThatIsNotStored() = runBlocking {
        assertFalse(store.savePosition("SMH.MI", Position(40.70)))
        assertNull(db.watchlist().position("SMH.MI"))
    }
}
