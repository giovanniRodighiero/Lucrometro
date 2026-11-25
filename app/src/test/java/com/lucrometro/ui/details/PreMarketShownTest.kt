package com.lucrometro.ui.details

import com.lucrometro.data.PreMarket
import com.lucrometro.data.Quote
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreMarketShownTest {
    // Decision 0055: a wrong rule would show an old price as the one coming, which the emulator rarely catches.
    @Test fun onlyWhileClosedBeforeItsOpeningAndNewerThanTheLastTrade() {
        val price = PreMarket(11.0, 1.0, timeEpoch = 150, opensEpoch = 200)
        val closed = MarketStatus.OpensToday(LocalTime.of(9, 30))
        fun quote(tradedAt: Long) = Quote("SMH", 10.0, "USD", null, tradedAt)

        assertTrue(price.isShown(199, closed, quote(100)))
        assertFalse(price.isShown(200, closed, quote(100))) // the opening, then closed again that evening
        assertFalse(price.isShown(199, MarketStatus.Open(LocalTime.of(16, 0)), quote(100)))
        assertFalse(price.isShown(199, null, quote(100))) // hours unknown
        assertFalse(price.isShown(199, closed, quote(160)))
    }
}
