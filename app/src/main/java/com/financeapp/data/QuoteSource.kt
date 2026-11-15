package com.financeapp.data

data class Quote(
    val symbol: String,
    val price: Double,
    val currency: String?,
    val exchange: String?,
    /** Epoch seconds of the last trade as reported by the source. */
    val marketTimeEpoch: Long,
)

/** One daily bar. Prices are null-free: bars with a missing close are dropped by the source. */
data class Candle(
    val epochSeconds: Long,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val close: Double,
    val volume: Long?,
)

/** Supported history windows (Yahoo `range` values), all with daily bars. */
enum class HistoryRange(val yahoo: String) { DAYS_5("5d"), MONTH_1("1mo"), MONTHS_3("3mo") }

/** Abstraction so the data provider can be swapped if Yahoo blocks or changes. */
interface QuoteSource {
    /** Blocking; call from a background thread. */
    fun quote(symbol: String): Result<Quote>

    /** Daily candles, oldest first. Blocking; call from a background thread. */
    fun history(symbol: String, range: HistoryRange): Result<List<Candle>>
}
