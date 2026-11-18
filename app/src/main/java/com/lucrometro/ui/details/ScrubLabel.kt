package com.lucrometro.ui.details

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lucrometro.R
import com.lucrometro.data.isIntraday
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The date (and time) of the scrubbed [point] for the readout, in the exchange's clock (a daily bar is stamped at the
 * exchange's open, which is the day before on a phone far to the west). Intraday ranges: "Today, 11:45", or the day and
 * time ("Tue, Nov 17, 11:45 AM"). Daily and longer ranges: the date ("Nov 14, 2026").
 */
@Composable
fun scrubLabel(data: ChartData, point: ChartPoint): String {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val intraday = data.range.isIntraday
    val zone = data.zone ?: ZoneId.systemDefault()
    // Built once per locale and zone, not per drag event.
    val formats = remember(locale, is24Hour, zone) {
        fun formatter(skeleton: String) =
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).withZone(zone)
        listOf(formatter("dMMMyyyy"), formatter(if (is24Hour) "Hm" else "hm"), formatter("EEEdMMM"))
    }
    val instant = Instant.ofEpochSecond(point.epochSeconds)
    if (!intraday) return formats[0].format(instant)
    val time = formats[1].format(instant)
    return if (instant.atZone(zone).toLocalDate() == LocalDate.now(zone)) {
        stringResource(R.string.quote_stamp_today, time)
    } else {
        stringResource(R.string.quote_stamp_day, formats[2].format(instant), time)
    }
}
