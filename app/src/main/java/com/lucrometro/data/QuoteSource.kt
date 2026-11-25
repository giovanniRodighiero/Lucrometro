package com.lucrometro.data

/** The source answered, and the symbol does not exist there (as opposed to a network or server failure). */
class TickerNotFoundException(symbol: String) : Exception("Ticker not found: $symbol")

/** The source knows the symbol but has no current price for it (suspended, delisted, never traded). */
class NoPriceException(symbol: String) : Exception("No price for: $symbol")

/** The source refused the request as too many (HTTP 429): stop asking, no retries (CLAUDE.md). */
class RateLimitedException : Exception("Rate limited (HTTP 429)")

/** Abstraction so the data provider can be swapped if Yahoo blocks or changes. */
interface QuoteSource {
    /**
     * Blocking; call from a background thread. Fails with [TickerNotFoundException] when the source
     * does not know [symbol] and [NoPriceException] when it knows it but has no price; any other
     * failure means the check itself did not complete.
     */
    fun quote(symbol: String): Result<Quote>

    /**
     * The pre-market price, or null when the market is not in pre-market or has no price yet (decision 0055). One
     * request. Blocking; call from a background thread. The default is for test fakes that never ask for it.
     */
    fun preMarket(symbol: String): Result<PreMarket?> = Result.success(null)

    /** Candles of the range, oldest first. Blocking; call from a background thread. */
    fun history(symbol: String, range: HistoryRange): Result<History>

    /**
     * Daily bars from [fromEpochSeconds] to now, oldest first, with the exchange's zone (decision 0052). Fails if the
     * source answers with any other bar size. Blocking; call from a background thread. The default is for test fakes
     * that never ask for it.
     */
    fun dailyHistory(symbol: String, fromEpochSeconds: Long): Result<History> =
        Result.failure(UnsupportedOperationException("dailyHistory"))
}
