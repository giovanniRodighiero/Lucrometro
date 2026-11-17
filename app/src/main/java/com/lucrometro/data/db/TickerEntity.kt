package com.lucrometro.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.lucrometro.data.Quote
import com.lucrometro.data.SavedQuote
import com.lucrometro.data.StoredTicker

/**
 * One watchlist row: the code, when it was added (the list order) and its last successful quote.
 * The quote columns are null until a quote is saved; [price] being null means "no quote".
 */
@Entity(tableName = "watchlist")
data class TickerEntity(
    @PrimaryKey val code: String,
    val addedAt: Long,
    val price: Double? = null,
    val currency: String? = null,
    val exchange: String? = null,
    val name: String? = null,
    val previousClose: Double? = null,
    val priceDecimals: Int? = null,
    /** Epoch seconds of the last trade, as the source reported it. */
    val marketTime: Long? = null,
    /** Epoch millis when the quote was fetched. */
    val fetchedAt: Long? = null,
)

internal fun TickerEntity.toStored(): StoredTicker {
    val saved = if (price != null && fetchedAt != null) {
        SavedQuote(
            Quote(
                symbol = code,
                price = price,
                currency = currency,
                exchange = exchange,
                marketTimeEpoch = marketTime ?: 0,
                name = name,
                previousClose = previousClose,
                priceDecimals = priceDecimals,
            ),
            fetchedAt,
        )
    } else {
        null
    }
    return StoredTicker(code, saved)
}

internal fun newTickerEntity(code: String, addedAt: Long, saved: SavedQuote) = TickerEntity(
    code = code,
    addedAt = addedAt,
    price = saved.quote.price,
    currency = saved.quote.currency,
    exchange = saved.quote.exchange,
    name = saved.quote.name,
    previousClose = saved.quote.previousClose,
    priceDecimals = saved.quote.priceDecimals,
    marketTime = saved.quote.marketTimeEpoch,
    fetchedAt = saved.fetchedAt,
)
