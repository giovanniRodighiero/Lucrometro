package com.lucrometro.data

data class Quote(
    val symbol: String,
    val price: Double,
    val currency: String?,
    val exchange: String?,
    /** Epoch seconds of the last trade as reported by the source. */
    val marketTimeEpoch: Long,
    /** Display name (e.g. "Eni S.p.A."), when the source has one. */
    val name: String? = null,
    /** Close of the previous session: the base of the day change. */
    val previousClose: Double? = null,
    /** Decimals the source uses for this price (e.g. 3 for 5.412), when it says. */
    val priceDecimals: Int? = null,
) {
    /** Change since the previous close, in percent; null when the source gave no previous close. */
    val dayChangePercent: Double?
        get() = previousClose?.takeIf { it > 0 }?.let { (price - it) / it * 100 }
}

/** One daily bar. Prices are null-free: bars with a missing close are dropped by the source. */
data class Candle(
    val epochSeconds: Long,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val close: Double,
    val volume: Long?,
    /** Close adjusted for dividends and splits (decision 0022); null when the source gave none for this bar. */
    val adjClose: Double? = null,
)

/**
 * Supported history windows: Yahoo `range` and the bar `interval` that fits it. Daily bars up to 1Y; 5Y is weekly
 * and Max monthly (decision 0022). Yahoo may answer a finer interval than asked for a short history (VWCE.MI Max
 * came back weekly), so callers must not assume the interval.
 */
enum class HistoryRange(val yahoo: String, val interval: String) {
    DAYS_5("5d", "1d"),
    MONTH_1("1mo", "1d"),
    MONTHS_3("3mo", "1d"),
    MONTHS_6("6mo", "1d"),
    YEAR_1("1y", "1d"),
    YEARS_5("5y", "1wk"),
    MAX("max", "1mo"),
}

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
