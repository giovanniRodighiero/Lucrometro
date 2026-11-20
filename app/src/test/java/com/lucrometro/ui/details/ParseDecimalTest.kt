package com.lucrometro.ui.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The position sheet takes a dot or a comma as the decimal mark, and nothing else (decision 0033). */
class ParseDecimalTest {
    @Test
    fun acceptsEitherDecimalMark() {
        assertEquals(29.5, parseDecimal("29.50")!!, 0.0)
        assertEquals(29.5, parseDecimal("29,50")!!, 0.0)
        assertEquals(120.0, parseDecimal(" 120 ")!!, 0.0)
        assertEquals(0.5, parseDecimal(",5")!!, 0.0)
        assertEquals(29.0, parseDecimal("29.")!!, 0.0)
    }

    @Test
    fun rejectsEverythingElse() {
        listOf("", "1.234,56", "1,234.5", "-3", "1e5", "NaN", "Infinity", "12a", "0x1A", ".").forEach {
            assertNull(it, parseDecimal(it))
        }
    }
}
