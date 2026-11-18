package com.lucrometro.ui.details

import android.text.format.DateFormat
import com.lucrometro.data.HistoryRange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class LabelAnchor { Start, Center, End }

/** A label on the bottom row at [fraction] (0..1) of the plot width. */
class AxisLabel(val fraction: Float, val text: String, val anchor: LabelAnchor)

/** Where each point sits across the plot (0..1) and the bottom-row labels. */
class ChartLayout(val xs: List<Float>, val labels: List<AxisLabel>)

/**
 * 1D: time across the trading session (09:00-17:30), so the line stops at the last bar and the rest stays empty.
 * 5D: bars evenly spaced, with the weekday at the start of each day (the overnight and weekend gaps would leave
 * holes on a time axis). Other ranges: time across the range with four dates. Intraday times are the exchange's.
 */
fun chartLayout(data: ChartData, locale: Locale, is24Hour: Boolean): ChartLayout {
    val points = data.points
    val first = points.first().epochSeconds
    val last = points.last().epochSeconds
    fun formatter(skeleton: String, zone: ZoneId) =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).withZone(zone)
    fun format(f: DateTimeFormatter, epoch: Long) = f.format(Instant.ofEpochSecond(epoch))

    return when (data.range) {
        HistoryRange.DAY_1 -> {
            val start = data.session?.startEpoch ?: first
            val end = (data.session?.endEpoch ?: last).coerceAtLeast(start + 1)
            fun at(t: Long) = ((t - start).toFloat() / (end - start)).coerceIn(0f, 1f) // bars are inside the session
            val time = formatter(if (is24Hour) "Hm" else "hm", data.zone ?: ZoneId.systemDefault())
            // Every two hours from the open, then the close: 09:00 11:00 13:00 15:00 17:30.
            val ticks = generateSequence(start) { it + 2 * 3600 }.takeWhile { it < end - 3600 }.toList() + end
            ChartLayout(
                xs = points.map { at(it.epochSeconds) },
                labels = ticks.mapIndexed { i, t ->
                    AxisLabel(at(t), format(time, t), if (i == 0) LabelAnchor.Start else if (t == end) LabelAnchor.End else LabelAnchor.Center)
                },
            )
        }
        HistoryRange.DAYS_5 -> {
            val zone = data.zone ?: ZoneId.systemDefault()
            val weekday = formatter("EEE", zone)
            val n = points.size
            val dayStarts = points.withIndex().filter { (i, p) ->
                i == 0 || Instant.ofEpochSecond(p.epochSeconds).atZone(zone).toLocalDate() !=
                    Instant.ofEpochSecond(points[i - 1].epochSeconds).atZone(zone).toLocalDate()
            }
            ChartLayout(
                xs = List(n) { it.toFloat() / (n - 1) },
                labels = dayStarts.map { (i, p) ->
                    AxisLabel(i.toFloat() / (n - 1), format(weekday, p.epochSeconds), LabelAnchor.Start)
                },
            )
        }
        else -> {
            val span = (last - first).coerceAtLeast(1)
            val date = formatter(data.range.dateSkeleton, ZoneId.systemDefault())
            ChartLayout(
                xs = points.map { (it.epochSeconds - first).toFloat() / span },
                labels = listOf(
                    0f to LabelAnchor.Start, 1f / 3 to LabelAnchor.Center, 2f / 3 to LabelAnchor.Center, 1f to LabelAnchor.End,
                ).map { (f, anchor) -> AxisLabel(f, format(date, first + (span * f).toLong()), anchor) },
            )
        }
    }
}
