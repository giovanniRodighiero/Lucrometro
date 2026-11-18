package com.lucrometro.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lucrometro.data.isIntraday
import com.lucrometro.ui.common.TimeFormats

/**
 * Doop component ChartReadout, summary mode (decision 0023): the line above the chart, a label and a
 * value on the left and an optional change pill on the right. It keeps a 56dp height in every state, so
 * nothing moves when the chart loads or a scrub starts. While [scrubbing] (scrub mode) the label turns primary and the
 * value is medium weight.
 */
@Composable
fun ChartReadout(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    scrubbing: Boolean = false,
    change: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (scrubbing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (scrubbing) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        change()
    }
}

/**
 * The date (and time) of the scrubbed [point] for the readout, in the exchange's clock (a daily bar is stamped at the
 * exchange's open, which is the day before on a phone far to the west). Intraday ranges: "Today, 11:45", or the day and
 * time ("Tue, Nov 17, 11:45 AM"). Daily and longer ranges: the date ("Nov 14, 2026").
 */
@Composable
fun scrubLabel(data: ChartData, point: ChartPoint, times: TimeFormats): String =
    if (data.range.isIntraday) {
        times.marketStamp(point.epochSeconds, data.zone)
    } else {
        times.format("dMMMyyyy", point.epochSeconds, data.zone)
    }
