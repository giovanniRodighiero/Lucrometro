package com.lucrometro.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TradeRepositoryTest {
    private fun trade(code: String, side: Side, quantity: Double) =
        Trade(code = code, side = side, date = LocalDate.of(2026, 1, 2), time = null, quantity = quantity, price = 10.0)

    @Test
    fun `the global import saves every ticker or none`() = runBlocking {
        val store = FakeWatchlistStore()
        listOf("ENI.MI", "ISP.MI").forEach { store.add(it, SavedQuote(Quote(it, 10.0, "EUR", null, 0), 0)) }
        val repository = TradeRepository(store)

        val refused = repository.addAll(mapOf("ENI.MI" to listOf(trade("ENI.MI", Side.BUY, 5.0)), "ISP.MI" to listOf(trade("ISP.MI", Side.SELL, 5.0))))
        assertTrue(refused is TradeChange.BelowZero)
        assertEquals(emptyList<Trade>(), store.trades("ENI.MI") + store.trades("ISP.MI"))

        val saved = repository.addAll(mapOf("ENI.MI" to listOf(trade("ENI.MI", Side.BUY, 5.0)), "ISP.MI" to listOf(trade("ISP.MI", Side.BUY, 5.0))))
        assertEquals(TradeChange.Saved, saved)
        assertEquals(2, (store.trades("ENI.MI") + store.trades("ISP.MI")).size)
    }
}
