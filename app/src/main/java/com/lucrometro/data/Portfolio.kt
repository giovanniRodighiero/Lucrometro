package com.lucrometro.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** One stored ticker as the portfolio sees it: its watchlist entry, its trades and its typed average (or null). */
data class PortfolioTicker(val entry: WatchlistEntry, val trades: List<Trade>, val typed: Position?)

/** A held ticker in the composition (decision 0051): [weight] in % of the total value. */
data class HoldingLine(val code: String, val quote: Quote, val value: Double, val weight: Double)

/**
 * A ticker held at some point of a year, for the year detail: [price] is the year-end close (today's price in the current
 * year), [marketReturn] the market's return over the year in %, dividends included; null when a year end is missing (the
 * ticker was listed during the year).
 */
data class TickerYear(val code: String, val quote: Quote, val price: Double?, val marketReturn: Double?)

/**
 * The portfolio's growth in one calendar year (decisions 0050, 0052), money-weighted (modified Dietz) from [start] to
 * [end]: the first year starts at the first buy, the current year ends today. [added] is buys minus sells; [percent] is
 * null when nothing was invested in the year.
 */
data class YearGrowth(
    val year: Int,
    val start: LocalDate,
    val end: LocalDate,
    /** The first year, when the first buy was after 1 Jan: "From 12 Mar". */
    val fromFirstBuy: Boolean,
    val current: Boolean,
    val startValue: Double,
    val added: Double,
    val endValue: Double,
    val gain: Double,
    val percent: Double?,
    /** Sorted by market return, highest first; unknown returns last. */
    val tickers: List<TickerYear>,
)

data class Portfolio(
    /** EUR tickers with trades: what the total counts. Empty is the "Nothing to add up yet" state. */
    val counted: List<String>,
    /** Held tickers, by value, largest first. */
    val holdings: List<HoldingLine>,
    val value: Double,
    /** What the shares still held cost, at their averages. */
    val invested: Double,
    val dayChange: Double?,
    val dayChangePercent: Double?,
    /** Gross realized gains of every counted ticker, sold-out ones included; null with no sells. */
    val realized: Double?,
    /** How many held tickers' prices are stale, and the oldest fetch among them (epoch millis). */
    val staleCount: Int,
    val staleSince: Long?,
    /** Newest first. Years whose prices are missing are left out (in [missingYears]). */
    val years: List<YearGrowth>,
    /** Years left out, newest first, each with the tickers whose year-end prices are missing. */
    val missingYears: Map<Int, Set<String>>,
    /** Tickers with a typed average and no trades: listed as not counted. */
    val averageOnly: List<WatchlistEntry>,
    /** Tickers with trades in another currency (or none known): not counted, no conversion yet (0049). */
    val otherCurrency: List<WatchlistEntry>,
) {
    val sinceBought: Double get() = value - invested
    val sinceBoughtPercent: Double? get() = if (invested > 0) sinceBought / invested * 100 else null
}

private const val EPSILON = 1e-9

/** The shares held at the end of [date], from trades oldest first (a stored log never goes below 0). */
internal fun sharesAt(trades: List<Trade>, date: LocalDate): Double {
    val shares = trades.filter { !it.date.isAfter(date) }.sumOf { if (it.side == Side.BUY) it.quantity else -it.quantity }
    return if (abs(shares) < EPSILON) 0.0 else shares
}

/**
 * Adds up the portfolio (decisions 0049, 0050, 0051, 0052) from the stored tickers and their saved year ends, on [today].
 * shortcut: trades are in real share counts but Yahoo's closes are split-adjusted, so a split after a trade misstates every
 * year before it (as it does the rest of the app); handle splits when a held ticker first has one.
 */
fun portfolio(tickers: List<PortfolioTicker>, years: Map<String, TickerYears>, today: LocalDate): Portfolio {
    class Counted(val code: String, val quote: Quote, val stale: Boolean, val fetchedAt: Long, val trades: List<Trade>, val holding: Holding)

    val averageOnly = tickers.filter { it.trades.isEmpty() && it.typed != null }.map { it.entry }
    val withTrades = tickers.filter { it.trades.isNotEmpty() && it.entry.saved != null }
    val otherCurrency = withTrades.filter { it.entry.saved?.quote?.currency != "EUR" }.map { it.entry }
    val counted = withTrades.filter { it.entry.saved?.quote?.currency == "EUR" }.mapNotNull { t ->
        val saved = t.entry.saved ?: return@mapNotNull null
        // A stored log always replays: every change is checked when it is saved.
        val holding = (replay(t.trades) as? Replay.Ok)?.holding ?: return@mapNotNull null
        Counted(t.entry.code, saved.quote, t.entry.stale, saved.fetchedAt, t.trades, holding)
    }

    val held = counted.filter { it.holding.shares > 0 }
    val value = held.sumOf { it.holding.shares * it.quote.price }
    val holdings = held
        .map { HoldingLine(it.code, it.quote, it.holding.shares * it.quote.price, 0.0) }
        .map { it.copy(weight = if (value > 0) it.value / value * 100 else 0.0) }
        .sortedWith(compareByDescending<HoldingLine> { it.value }.thenBy { it.code })
    val invested = held.sumOf { it.holding.shares * (it.holding.average ?: 0.0) }
    val withPrevious = held.filter { it.quote.previousClose != null }
    val dayChange = withPrevious.takeIf { it.isNotEmpty() }?.sumOf { it.holding.shares * (it.quote.price - it.quote.previousClose!!) }
    val dayBase = withPrevious.sumOf { it.holding.shares * it.quote.previousClose!! }
    val realized = counted.mapNotNull { it.holding.realized?.gain }.takeIf { it.isNotEmpty() }?.sum()
    val stale = held.filter { it.stale }

    val growth = mutableListOf<YearGrowth>()
    val missing = linkedMapOf<Int, Set<String>>()
    val firstDate = counted.flatMap { it.trades }.minOfOrNull { it.date }
    if (firstDate != null) {
        for (year in today.year downTo firstDate.year) {
            val first = year == firstDate.year
            val current = year == today.year
            val start = if (first) firstDate else LocalDate.of(year, 1, 1)
            val end = if (current) today else LocalDate.of(year, 12, 31)
            val lastYearEnd = LocalDate.of(year - 1, 12, 31)
            val yearEnd = LocalDate.of(year, 12, 31)
            val days = ChronoUnit.DAYS.between(start, end) + 1
            var startValue = 0.0
            var endValue = 0.0
            var added = 0.0
            var weighted = 0.0
            val lacking = mutableSetOf<String>()
            val rows = mutableListOf<TickerYear>()
            for (t in counted) {
                val sharesStart = if (first) 0.0 else sharesAt(t.trades, lastYearEnd)
                val inYear = t.trades.filter { it.date.year == year }
                if (sharesStart == 0.0 && inYear.isEmpty()) continue
                val saved = years[t.code]
                val before = saved?.end(year - 1)
                val after = saved?.end(year)
                if (sharesStart > 0) {
                    if (before == null) lacking += t.code else startValue += sharesStart * before.close
                }
                if (current) {
                    endValue += t.holding.shares * t.quote.price
                } else {
                    val sharesEnd = sharesAt(t.trades, yearEnd)
                    if (sharesEnd > 0) {
                        if (after == null) lacking += t.code else endValue += sharesEnd * after.close
                    }
                }
                for (trade in inYear) {
                    val flow = if (trade.side == Side.BUY) trade.amount else -trade.amount
                    added += flow
                    // The share of the period the money was in: a buy on the first day counts in full.
                    weighted += flow * (ChronoUnit.DAYS.between(trade.date, end) + 1) / days
                }
                val price = if (current) t.quote.price else after?.close
                val from = before?.adjClose
                val to = if (current) t.quote.price else after?.adjClose
                val marketReturn = if (from != null && to != null && from > 0) (to / from - 1) * 100 else null
                rows += TickerYear(t.code, t.quote, price, marketReturn)
            }
            if (lacking.isNotEmpty()) {
                missing[year] = lacking
                continue
            }
            val gain = endValue - startValue - added
            val base = startValue + weighted
            growth += YearGrowth(
                year, start, end, first && start != LocalDate.of(year, 1, 1), current, startValue, added, endValue, gain,
                if (base > EPSILON) gain / base * 100 else null,
                rows.sortedWith(compareByDescending<TickerYear, Double?>(nullsFirst()) { it.marketReturn }.thenBy { it.code }),
            )
        }
    }

    return Portfolio(
        counted = counted.map { it.code },
        holdings = holdings,
        value = value,
        invested = invested,
        dayChange = dayChange,
        dayChangePercent = dayChange?.let { if (dayBase > 0) it / dayBase * 100 else null },
        realized = realized,
        staleCount = stale.size,
        staleSince = stale.minOfOrNull { it.fetchedAt },
        years = growth,
        missingYears = missing,
        averageOnly = averageOnly,
        otherCurrency = otherCurrency,
    )
}

/**
 * The year ends in a daily [history] of [code] (decision 0052): the last bar of each calendar year from [fromYear] up to
 * the last year that has closed at [nowMillis], in the exchange's clock.
 */
fun yearEnds(code: String, history: History, fromYear: Int, nowMillis: Long): TickerYears {
    val zone = history.exchangeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneOffset.UTC
    val throughYear = Instant.ofEpochMilli(nowMillis).atZone(zone).year - 1
    val ends = history.candles
        .groupBy { Instant.ofEpochSecond(it.epochSeconds).atZone(zone).year }
        .filterKeys { it in fromYear..throughYear }
        .map { (year, bars) ->
            val last = bars.maxBy { it.epochSeconds }
            YearEnd(year, Instant.ofEpochSecond(last.epochSeconds).atZone(zone).toLocalDate(), last.close, last.adjClose)
        }
        .sortedBy { it.year }
    return TickerYears(code, fromYear, throughYear, ends)
}
