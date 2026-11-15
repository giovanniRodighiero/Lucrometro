package com.financeapp.data

data class Quote(
    val symbol: String,
    val price: Double,
    val currency: String?,
    val exchange: String?,
    /** Epoch seconds of the last trade as reported by the source. */
    val marketTimeEpoch: Long,
)

/** Abstraction so the data provider can be swapped if Yahoo blocks or changes. */
interface QuoteSource {
    /** Blocking; call from a background thread. */
    fun quote(symbol: String): Result<Quote>
}
