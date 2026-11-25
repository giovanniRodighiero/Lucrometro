package com.lucrometro.data

/** What kind of instrument a ticker is (decision 0057). Any other kind the source reports is null. */
enum class AssetType { ETF, STOCK }

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
    /** ETF or stock; null when the source reports another kind, or none (decision 0057). */
    val assetType: AssetType? = null,
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
    val close: Double,
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

/** The price before the regular session opens (decision 0055). Never saved. */
data class PreMarket(
    val price: Double,
    /** Change from the last regular close, in percent. */
    val changePercent: Double,
    /** Epoch seconds of the pre-market price. */
    val timeEpoch: Long,
    /** When the regular session it comes before opens: not shown from then on. */
    val opensEpoch: Long = 0,
)

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
    /** The pre-market hours of the current session, only for tickers the source has pre-market prices for. */
    val preSession: TradingPeriod? = null,
    /** Epoch seconds of the last trade when this was fetched, for every range: how recent the bars are (decision 0056). */
    val lastTradeEpoch: Long? = null,
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
 * The owner's average price in a ticker as their broker shows it (decisions 0033, 0040): typed in, never computed, one
 * per ticker. Public: it has no amounts.
 */
data class Position(
    /** In the ticker's currency, as the broker shows it. */
    val averagePrice: Double,
)

/**
 * A ticker's last daily bar of a calendar year (decision 0051): [close] for the portfolio's values, [adjClose] (dividends
 * and splits included, as of the fetch that saved it) for the market return. [date] is the bar's day in the exchange's clock.
 */
data class YearEnd(val year: Int, val date: java.time.LocalDate, val close: Double, val adjClose: Double?)

/**
 * The year ends saved for a ticker from one daily-history fetch: every year from [fromYear] through [throughYear] (the
 * last year that had closed when it was fetched) that has a bar. All [ends] come from the same fetch, so their adjusted
 * closes share one basis and can be divided by each other.
 */
data class TickerYears(val code: String, val fromYear: Int, val throughYear: Int, val ends: List<YearEnd>) {
    fun end(year: Int): YearEnd? = ends.firstOrNull { it.year == year }
}
