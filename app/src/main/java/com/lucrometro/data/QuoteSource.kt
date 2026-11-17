package com.lucrometro.data

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

/** The source answered, and the symbol does not exist there (as opposed to a network or server failure). */
class TickerNotFoundException(symbol: String) : Exception("Ticker not found: $symbol")

/** The source knows the symbol but has no current price for it (suspended, delisted, never traded). */
class NoPriceException(symbol: String) : Exception("No price for: $symbol")

/** Abstraction so the data provider can be swapped if Yahoo blocks or changes. */
interface QuoteSource {
    /**
     * Blocking; call from a background thread. Fails with [TickerNotFoundException] when the source
     * does not know [symbol] and [NoPriceException] when it knows it but has no price; any other
     * failure means the check itself did not complete.
     */
    fun quote(symbol: String): Result<Quote>

    /** Daily candles, oldest first. Blocking; call from a background thread. */
    fun history(symbol: String, range: HistoryRange): Result<List<Candle>>
}
