package com.lucrometro.data.db

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.lucrometro.data.AssetType
import com.lucrometro.data.Quote
import com.lucrometro.data.SavedQuote
import com.lucrometro.data.StoredTicker

/**
 * One watchlist row: the code, its place in the list, when it was added and its last successful quote.
 * [quote] is null when no quote is saved (all its columns null).
 */
@Entity(tableName = "watchlist")
data class TickerEntity(
    @PrimaryKey val code: String,
    /** List order: 1, 2, 3... in order of addition. Not the clock, which can go backwards. */
    val position: Long,
    /** Epoch millis when the ticker was added. Informational; not used for ordering. */
    val addedAt: Long,
    @Embedded val quote: QuoteColumns?,
)

/**
 * The last successful quote, as stored. The one place that maps [SavedQuote] to columns: add and
 * refresh both write through it, so a new field cannot be saved by one and dropped by the other.
 */
data class QuoteColumns(
    val price: Double,
    val currency: String?,
    val exchange: String?,
    val name: String?,
    val previousClose: Double?,
    val priceDecimals: Int?,
    /** Epoch seconds of the last trade, as the source reported it. */
    val marketTime: Long,
    /** Epoch millis when the quote was fetched. */
    val fetchedAt: Long,
    /** IANA zone of the exchange; null for quotes saved before schema v3, until the next fetch. */
    val exchangeZone: String?,
    /** Null for quotes saved before schema v8, until the next fetch, and for other kinds of instrument. */
    val assetType: AssetType?,
)

/** Partial update of a row's quote columns (Room `@Update(entity = TickerEntity::class)`). */
data class QuoteUpdate(val code: String, @Embedded val quote: QuoteColumns)

internal fun SavedQuote.toColumns() = QuoteColumns(
    price = quote.price,
    currency = quote.currency,
    exchange = quote.exchange,
    name = quote.name,
    previousClose = quote.previousClose,
    priceDecimals = quote.priceDecimals,
    marketTime = quote.marketTimeEpoch,
    fetchedAt = fetchedAt,
    exchangeZone = quote.exchangeZone,
    assetType = quote.assetType,
)

internal fun TickerEntity.toStored() = StoredTicker(
    code = code,
    position = position,
    addedAt = addedAt,
    saved = quote?.let {
        SavedQuote(
            Quote(
                symbol = code,
                price = it.price,
                currency = it.currency,
                exchange = it.exchange,
                marketTimeEpoch = it.marketTime,
                name = it.name,
                previousClose = it.previousClose,
                priceDecimals = it.priceDecimals,
                exchangeZone = it.exchangeZone,
                assetType = it.assetType,
            ),
            it.fetchedAt,
        )
    },
)
