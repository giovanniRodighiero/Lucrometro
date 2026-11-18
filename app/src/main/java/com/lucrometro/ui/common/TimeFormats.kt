package com.lucrometro.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lucrometro.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The zone of [id] (IANA, e.g. "Europe/Rome"), or null when it is missing or unknown. */
fun zoneOf(id: String?): ZoneId? = id?.let { runCatching { ZoneId.of(it) }.getOrNull() }

/**
 * The clock market events are shown in (decision 0031): the exchange's zone [id], else [fallback] (an exchange zone
 * known from elsewhere, e.g. the chart), else the phone's.
 */
fun marketZone(id: String?, fallback: ZoneId? = null): ZoneId = zoneOf(id) ?: fallback ?: ZoneId.systemDefault()

/**
 * Times and dates as text, in one locale and the phone's 12/24h setting, created once per screen. Which clock
 * (decision 0031): market events (trades, bars, sessions) in the exchange's zone; phone events (a fetch) in the
 * phone's. Patterns come from `DateFormat.getBestDateTimePattern`, so the order and separators follow the locale.
 * Formatters are kept per skeleton and zone; use from composition only.
 */
class TimeFormats(private val locale: Locale, is24Hour: Boolean) {
    private val formatters = HashMap<Pair<String, ZoneId>, DateTimeFormatter>()

    /** Skeleton of a time of day: "14:32" or "2:32 PM". */
    val timeSkeleton = if (is24Hour) "Hm" else "hm"

    private fun formatter(skeleton: String, zone: ZoneId): DateTimeFormatter = formatters.getOrPut(skeleton to zone) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).withZone(zone)
    }

    /** [epochSeconds] in [zone], with the fields of [skeleton] (e.g. "dMMM", "EEE"). */
    fun format(skeleton: String, epochSeconds: Long, zone: ZoneId): String =
        formatter(skeleton, zone).format(Instant.ofEpochSecond(epochSeconds))

    /** A time of day that is already in its clock (e.g. a session's opening). */
    fun time(time: LocalTime): String = formatter(timeSkeleton, ZoneOffset.UTC).format(time)

    /** The short weekday name ("Mon"). */
    fun weekday(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, locale)

    /** A phone event (when a quote was fetched): the time today, otherwise the date and time, in the phone's zone. */
    fun phoneStamp(epochMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val seconds = epochMillis / 1000
        val today = Instant.ofEpochSecond(seconds).atZone(zone).toLocalDate() == LocalDate.now(zone)
        return format(if (today) timeSkeleton else "dMMM$timeSkeleton", seconds, zone)
    }

    /** A market event (a trade, a bar) in the exchange's [zone]: "Today, 14:32", or the day and time ("Tue, Nov 17, 14:32"). */
    @Composable
    fun marketStamp(epochSeconds: Long, zone: ZoneId): String {
        val time = format(timeSkeleton, epochSeconds, zone)
        val today = Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalDate() == LocalDate.now(zone)
        return if (today) {
            stringResource(R.string.quote_stamp_today, time)
        } else {
            stringResource(R.string.quote_stamp_day, format("EEEdMMM", epochSeconds, zone), time)
        }
    }
}

@Composable
fun rememberTimeFormats(): TimeFormats {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    // Read once per configuration, not on every recomposition (it is a settings lookup).
    val is24Hour = remember(configuration) { DateFormat.is24HourFormat(context) }
    val locale = configuration.locales[0]
    return remember(locale, is24Hour) { TimeFormats(locale, is24Hour) }
}
