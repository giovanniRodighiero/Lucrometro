package com.lucrometro.ui.common

import com.lucrometro.data.Quote
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Change amounts from 10,000 up drop their decimals, checked after rounding (decision 0048). */
class QuoteFormatsTest {
    private val formats = QuoteFormats(Locale.US)
    private val quote = Quote("SMH.MI", 72.0, "EUR", null, 0, priceDecimals = 2)

    private fun check(value: Double, text: String, rounded: Double) {
        val amount = formats.amountChange(quote, value)
        assertEquals(text, amount.text)
        assertEquals(rounded, amount.rounded, 0.0)
    }

    @Test
    fun wholeFromTenThousand() {
        check(9_999.5, "9,999.50", 9_999.5)
        check(9_999.996, "10,000", 10_000.0)
        check(61_310.4, "61,310", 61_310.0)
        check(-10_000.4, "10,000", -10_000.0)
    }
}
