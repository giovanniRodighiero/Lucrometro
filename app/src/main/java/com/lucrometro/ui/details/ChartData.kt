package com.lucrometro.ui.details

import com.lucrometro.data.History
import com.lucrometro.data.HistoryRange
import com.lucrometro.data.TradingPeriod
import com.lucrometro.ui.common.marketZone
import com.lucrometro.ui.common.zoneOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One point of the chart: the adjusted close (decision 0022), or the close when the source has no adjusted one. */
data class ChartPoint(val epochSeconds: Long, val price: Double)

/**
 * A loaded range. Changes are measured on adjusted closes; [fromPrice] is the first bar's plain close. For 1D the
 * change is measured from the previous close ([baseline], decision 0022) and the axis runs over the [session].
 */
data class ChartData(
    val range: HistoryRange,
    val points: List<ChartPoint>,
    val fromPrice: Double,
    /** 1D: the previous session's close, drawn as a dashed line; null for the other ranges. */
    val baseline: Double?,
    /** 1D: the trading session the axis runs over (the line stops at the last bar); null: the axis spans the points. */
    val session: TradingPeriod?,
    /** The exchange's zone, which bar times are shown in (decision 0031); the phone's if the source gave none. */
    val zone: ZoneId,
) {
    /** What changes are measured from: the previous close for 1D, the first bar otherwise. */
    val startPrice: Double get() = baseline ?: points.first().price
    val change: Double get() = points.last().price - startPrice
    val changePercent: Double? get() = percentOf(change)

    /** The point at [index] (the scrub), or null if there is none there. */
    fun scrubAt(index: Int?): ScrubbedPoint? {
        val point = index?.let(points::getOrNull) ?: return null
        val change = point.price - startPrice
        return ScrubbedPoint(point, change, percentOf(change))
    }

    /**
     * The point a day falls on: the last one on or before it (a weekend buy lands on Friday, a weekly bar holds its
     * week). Null when the day is before the first point: outside the range (decision 0034).
     */
    fun indexOn(date: LocalDate): Int? {
        fun day(p: ChartPoint) = Instant.ofEpochSecond(p.epochSeconds).atZone(zone).toLocalDate()
        if (date < day(points.first())) return null
        return points.indexOfLast { day(it) <= date }
    }

    private fun percentOf(change: Double): Double? = startPrice.takeIf { it > 0 }?.let { change / it * 100 }
}

/** The point under the finger, and its change from where the range's changes are measured. */
class ScrubbedPoint(val point: ChartPoint, val change: Double, val percent: Double?)

sealed interface ChartState {
    data object Loading : ChartState
    data class Ready(val data: ChartData) : ChartState

    /** 1D before the first trade of the day: nothing to draw yet, and nothing wrong. */
    data object Empty : ChartState
    data object Failed : ChartState
}

/**
 * The exchange's current session and zone: what the market marker and sheet are read from (decision 0035), with the
 * exchange's name and the quotes' delay when the source gives them.
 */
data class MarketInfo(
    val session: TradingPeriod,
    val zone: ZoneId,
    val exchangeName: String? = null,
    val delayMinutes: Int? = null,
)

/** The market status from a response, when it has both the session and a known zone. */
fun History.marketInfo(): MarketInfo? {
    val session = currentSession ?: return null
    return zoneOf(exchangeZone)?.let { MarketInfo(session, it, exchangeName, delayMinutes) }
}

/**
 * Ready with the chart; Empty for a 1D with no bars at all yet (a single bar is drawn as a point); otherwise Failed
 * (the fetch failed, or too few bars to draw a line).
 */
fun History?.toChartState(range: HistoryRange): ChartState = when {
    this == null -> ChartState.Failed
    candles.size >= 2 || (range == HistoryRange.DAY_1 && candles.isNotEmpty()) -> ChartState.Ready(toChartData(range))
    range == HistoryRange.DAY_1 -> ChartState.Empty
    else -> ChartState.Failed
}

private fun History.toChartData(range: HistoryRange): ChartData {
    // All adjusted or none: a bar without an adjusted close among adjusted ones would be a jump in the line.
    // Intraday bars have none, so they are plain closes.
    val adjusted = candles.all { it.adjClose != null }
    val day = range == HistoryRange.DAY_1
    // The 1D axis runs over the session the bars belong to; bars outside it (cannot be placed on it) are left out. If
    // none of the bars is inside the session, the session is not theirs (e.g. a weekend): the axis spans the bars.
    val session = if (day) sessions.lastOrNull()?.takeIf { s -> candles.any { it.epochSeconds in s.startEpoch..s.endEpoch } } else null
    val bars = if (session != null) candles.filter { it.epochSeconds in session.startEpoch..session.endEpoch } else candles
    return ChartData(
        range = range,
        points = bars.map { ChartPoint(it.epochSeconds, it.adjClose?.takeIf { adjusted } ?: it.close) },
        fromPrice = bars.first().close,
        baseline = if (day) previousClose else null,
        session = session,
        zone = marketZone(exchangeZone),
    )
}
