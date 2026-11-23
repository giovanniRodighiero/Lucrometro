package com.lucrometro.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.lucrometro.R
import com.lucrometro.data.Quote
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * Number formats in one locale, created once per screen (not per row and recomposition).
 * The text around the numbers (currency position, sign, percent) comes from strings.xml.
 * NumberFormat is not thread-safe: use from composition only.
 */
class QuoteFormats(private val locale: Locale) {
    private val prices = HashMap<Int, NumberFormat>()
    private val change = fixed(2)

    private fun fixed(decimals: Int) = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }

    private fun priceFormat(quote: Quote): NumberFormat {
        val decimals = quote.priceDecimals ?: 2
        return prices.getOrPut(decimals) { fixed(decimals) }
    }

    /** The price with the source's decimals (2 if unknown), followed by its currency when known. */
    @Composable
    fun price(quote: Quote): String = price(quote, quote.price)

    /** [value] in the quote's decimals and currency (e.g. the first price of a range). */
    @Composable
    fun price(quote: Quote, value: Double): String {
        val number = priceFormat(quote).format(value)
        return quote.currency?.let { stringResource(R.string.quote_price, number, it) } ?: number
    }

    /** A price (e.g. an axis label) with the quote's decimals, without the currency. */
    fun number(quote: Quote, value: Double): String = priceFormat(quote).format(value)

    private val exact = HashMap<Int, NumberFormat>()

    /**
     * [value] with at least the quote's decimals and up to 6 in total, without the currency: a price the owner typed (the
     * broker's average, decision 0033), shown as typed.
     */
    fun exact(quote: Quote, value: Double): String {
        val decimals = quote.priceDecimals ?: 2
        return exact.getOrPut(decimals) {
            NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = decimals
                maximumFractionDigits = maxOf(decimals, 6)
            }
        }.format(value)
    }

    private val shares = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 6 }

    /** A number of shares as typed: "120", "2.5". */
    fun shares(value: Double): String = shares.format(value)

    /** A change in price units with the quote's own decimals, rounded the way it is shown. */
    fun amountChange(quote: Quote, value: Double): AmountChange {
        val format = priceFormat(quote)
        val scale = 10.0.pow(format.maximumFractionDigits)
        return AmountChange(round(value * scale) / scale, format.format(abs(value)))
    }

    /** The size of a change in percent, unsigned, 2 decimals: the sign comes from the string. */
    fun changeMagnitude(percent: Double): String = change.format(abs(roundPercent(percent)))
}

@Composable
fun rememberQuoteFormats(): QuoteFormats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { QuoteFormats(locale) }
}
