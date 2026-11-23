package com.lucrometro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReplayTest {
    private fun t(side: Side, date: String, qty: Double, price: Double, time: String? = null) =
        Trade(code = "ENI.MI", side = side, date = LocalDate.parse(date), time = time?.let(LocalTime::parse), quantity = qty, price = price)

    private fun ok(trades: List<Trade>) = (replay(trades) as Replay.Ok).holding

    @Test
    fun `buys average by weight and a sell keeps the average`() {
        val h = ok(listOf(t(Side.BUY, "2026-01-02", 10.0, 10.0), t(Side.BUY, "2026-02-02", 30.0, 14.0), t(Side.SELL, "2026-03-02", 20.0, 20.0)))
        assertEquals(20.0, h.shares, 1e-9)
        assertEquals(13.0, h.average!!, 1e-9)
    }

    @Test
    fun `selling out resets the average for later buys`() {
        val h = ok(listOf(t(Side.BUY, "2026-01-02", 2.5, 10.0), t(Side.SELL, "2026-01-03", 2.5, 12.0), t(Side.BUY, "2026-01-04", 4.0, 7.0)))
        assertEquals(7.0, h.average!!, 1e-9)
        assertNull(ok(listOf(t(Side.BUY, "2026-01-02", 0.1, 1.0), t(Side.BUY, "2026-01-02", 0.2, 1.0), t(Side.SELL, "2026-01-03", 0.3, 1.0))).average)
    }

    @Test
    fun `a sell beyond the shares held is refused with what was held`() {
        val r = replay(listOf(t(Side.BUY, "2026-01-02", 10.0, 10.0), t(Side.SELL, "2026-01-03", 11.0, 10.0)))
        assertTrue(r is Replay.BelowZero)
        assertEquals(10.0, (r as Replay.BelowZero).sharesBefore, 1e-9)
    }

    @Test
    fun `an untimed buy counts before a timed sell on the same day`() {
        val h = ok(listOf(t(Side.SELL, "2026-01-02", 5.0, 10.0, "10:00"), t(Side.BUY, "2026-01-02", 5.0, 9.0)))
        assertEquals(0.0, h.shares, 1e-9)
    }
}
