package com.lucrometro.ui.details

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.HistoryRange

/** Short label of a range in the bar ("1M"). */
@get:StringRes
val HistoryRange.tabLabel: Int
    get() = when (this) {
        HistoryRange.DAY_1 -> R.string.range_1d
        HistoryRange.DAYS_5 -> R.string.range_5d
        HistoryRange.MONTH_1 -> R.string.range_1m
        HistoryRange.MONTHS_3 -> R.string.range_3m
        HistoryRange.MONTHS_6 -> R.string.range_6m
        HistoryRange.YEAR_1 -> R.string.range_1y
        HistoryRange.YEARS_5 -> R.string.range_5y
        HistoryRange.MAX -> R.string.range_max
    }

/** Label of a range above the chart ("Past month"). */
@get:StringRes
val HistoryRange.readoutLabel: Int
    get() = when (this) {
        HistoryRange.DAY_1 -> R.string.readout_today
        HistoryRange.DAYS_5 -> R.string.readout_past_5d
        HistoryRange.MONTH_1 -> R.string.readout_past_1m
        HistoryRange.MONTHS_3 -> R.string.readout_past_3m
        HistoryRange.MONTHS_6 -> R.string.readout_past_6m
        HistoryRange.YEAR_1 -> R.string.readout_past_1y
        HistoryRange.YEARS_5 -> R.string.readout_past_5y
        HistoryRange.MAX -> R.string.readout_all_time
    }

/**
 * Doop component RangeTab in a full-width bar (decision 0023): equal-width cells at least 48dp tall that tile the
 * row, the selected one with a 32dp secondary-container pill. Not a stock Material component: a selectable group,
 * each cell a radio button for TalkBack.
 */
@Composable
fun RangeBar(
    ranges: List<HistoryRange>,
    selected: HistoryRange,
    onSelect: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp).selectableGroup()) {
        ranges.forEach { range ->
            val isSelected = range == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .selectable(selected = isSelected, onClick = { onSelect(range) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.height(32.dp).defaultMinSize(minWidth = 40.dp),
                ) {
                    Box(Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(range.tabLabel),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
