package com.lucrometro.data

import java.time.LocalDate

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
    /** IANA zone of the exchange (Europe/Rome): the clock the trade time is shown in (decision 0031). */
    val exchangeZone: String? = null,
) {
    /** Change since the previous close, in price units; null when the source gave no previous close. */
    val dayChange: Double?
        get() = previousClose?.let { price - it }

    /** Change since the previous close, in percent; null when the source gave no previous close. */
    val dayChangePercent: Double?
        get() = previousClose?.takeIf { it > 0 }?.let { (price - it) / it * 100 }
}

/** One bar. Prices are null-free: bars with a missing close are dropped by the source. */
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
 * Supported history windows. 1D is intraday (5-minute bars), 5D has half-hour bars, then daily bars up to 1Y, weekly
 * for 5Y and monthly for Max (decisions 0022, 0027). The source picks the bar size and may answer a finer one than
 * asked for a short history (VWCE.MI Max came back weekly), so callers must not assume it.
 */
enum class HistoryRange { DAY_1, DAYS_5, MONTH_1, MONTHS_3, MONTHS_6, YEAR_1, YEARS_5, MAX }

/** Intraday ranges change during the trading day, so they are cached briefly; the others for longer. */
val HistoryRange.isIntraday: Boolean
    get() = this == HistoryRange.DAY_1 || this == HistoryRange.DAYS_5

/** A regular trading session of the exchange, in epoch seconds. */
data class TradingPeriod(val startEpoch: Long, val endEpoch: Long)

/** The bars of a range, with what the details screen needs besides them. */
data class History(
    val candles: List<Candle>,
    /** `chartPreviousClose`: for the 1D range the close of the previous session, the base of the day change. */
    val previousClose: Double? = null,
    /** The regular sessions the range covers, oldest first (the 1D axis runs over the last one). */
    val sessions: List<TradingPeriod> = emptyList(),
    /** The exchange's current (or next) regular session: what the market status is read from. */
    val currentSession: TradingPeriod? = null,
    /** IANA zone of the exchange (Europe/Rome), for times shown as the exchange sees them. */
    val exchangeZone: String? = null,
    /** The current quote from the same response, for the 1D range only (the other ranges' previous close differs). */
    val quote: Quote? = null,
    /** The exchange's full name ("Milan"), when the source gives it: the market sheet's title (decision 0035). */
    val exchangeName: String? = null,
    /** How many minutes the source's quotes lag the exchange, when it says. */
    val delayMinutes: Int? = null,
)

/** The last quote fetched successfully for a ticker, and when (epoch millis). */
data class SavedQuote(val quote: Quote, val fetchedAt: Long)

/** One tracked ticker. */
data class WatchlistEntry(
    val code: String,
    /** Last successful quote; null if none was ever saved. */
    val saved: SavedQuote? = null,
    /** The latest fetch for this ticker failed. */
    val failed: Boolean = false,
    /**
     * [saved] can't be shown as current: the latest fetch failed, or it was fetched before this app
     * session and no fetch has replaced it yet (e.g. right after a restart, or offline).
     */
    val stale: Boolean = false,
)

/**
 * The owner's position in a ticker as their broker shows it (decision 0033): never computed by the app. Each save is a
 * new entry; the latest one is the position.
 */
data class Position(
    /** In the ticker's currency, fees included, as the broker shows it. */
    val averagePrice: Double,
    val shares: Double?,
    /** The day of the last buy (today by default, or earlier: buys are often logged later). */
    val lastBuy: LocalDate,
    /** Epoch millis when this entry was saved. */
    val savedAt: Long,
)
