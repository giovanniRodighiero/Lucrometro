package com.lucrometro.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.ui.theme.gainLoss
import kotlin.math.abs
import kotlin.math.round

/** A change in price units: [rounded] (signed, to the price's decimals) and its unsigned [text]. */
class AmountChange(val rounded: Double, val text: String)

enum class ChangeDirection { Up, Down, Flat }

/** A percent rounded to the 2 decimals it is shown with. */
fun roundPercent(percent: Double): Double = round(percent * 100) / 100

/**
 * The direction of a change as it is shown, so a rounded 0.00% is flat; with an [absolute] amount, a 0.00% that is
 * not zero in price units follows the amount, and with no [percent] the amount alone decides.
 */
fun changeDirection(percent: Double?, absolute: AmountChange? = null): ChangeDirection {
    val rounded = percent?.let(::roundPercent)
    val shown = if ((rounded == null || rounded == 0.0) && absolute != null) absolute.rounded else rounded ?: 0.0
    return when {
        shown > 0 -> ChangeDirection.Up
        shown < 0 -> ChangeDirection.Down
        else -> ChangeDirection.Flat
    }
}

/** A change as signed text: "+4.03%", with the amount "+0.86 (+2.14%)", or the amount alone "+0.86" (null [percent]). */
@Composable
fun changeLabel(percent: Double?, formats: QuoteFormats, absolute: AmountChange? = null): String {
    if (percent == null) {
        val amount = requireNotNull(absolute) { "a change needs a percent or an amount" }
        return when (changeDirection(null, amount)) {
            ChangeDirection.Up -> stringResource(R.string.change_up_amount, amount.text)
            ChangeDirection.Down -> stringResource(R.string.change_down_amount, amount.text)
            ChangeDirection.Flat -> amount.text
        }
    }
    val magnitude = formats.changeMagnitude(percent)
    val direction = changeDirection(percent, absolute)
    return if (absolute != null) {
        when (direction) {
            ChangeDirection.Up -> stringResource(R.string.change_up_abs, absolute.text, magnitude)
            ChangeDirection.Down -> stringResource(R.string.change_down_abs, absolute.text, magnitude)
            ChangeDirection.Flat -> stringResource(R.string.change_flat_abs, absolute.text, magnitude)
        }
    } else {
        when (direction) {
            ChangeDirection.Up -> stringResource(R.string.change_up, magnitude)
            ChangeDirection.Down -> stringResource(R.string.change_down, magnitude)
            ChangeDirection.Flat -> stringResource(R.string.change_flat, magnitude)
        }
    }
}

/** An amount of money with its sign and all its decimals ("+1,872.10"), for text that is not a badge (no 0048 cut-off). */
@Composable
fun signedMoney(value: Double, formats: QuoteFormats): String =
    changeLabel(null, formats, AmountChange(round(value * 100) / 100, formats.money(abs(value))))

/** The colour of a line or mark that follows a change: gain, loss, or neutral. */
@Composable
fun ChangeDirection.color(): Color = when (this) {
    ChangeDirection.Up -> MaterialTheme.gainLoss.gain
    ChangeDirection.Down -> MaterialTheme.gainLoss.loss
    ChangeDirection.Flat -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * A change in percent as a filled tonal pill (Doop component ChangeBadge, decision 0007): arrow plus
 * signed value on a gain, loss or neutral container. The sign and arrow carry the meaning too, never
 * the colour alone. Direction follows the value as shown (see [changeDirection]).
 */
@Composable
fun ChangeBadge(
    /** Null shows the [absolute] amount alone ("+0.86"). */
    percent: Double?,
    formats: QuoteFormats,
    modifier: Modifier = Modifier,
    /** The change in price units, shown before the percent ("+0.86 (+2.14%)"); null for the percent alone. */
    absolute: AmountChange? = null,
) {
    val colors = MaterialTheme.gainLoss
    val (container, content, icon) = when (changeDirection(percent, absolute)) {
        ChangeDirection.Up -> Triple(colors.gainContainer, colors.onGainContainer, R.drawable.ic_change_up)
        ChangeDirection.Down -> Triple(colors.lossContainer, colors.onLossContainer, R.drawable.ic_change_down)
        ChangeDirection.Flat -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant,
            R.drawable.ic_change_flat,
        )
    }
    Surface(modifier = modifier, shape = CircleShape, color = container, contentColor = content) {
        Row(
            modifier = Modifier.padding(PaddingValues(start = 6.dp, end = 10.dp, top = 2.dp, bottom = 2.dp)),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp))
            // Doop: body-medium size, medium weight, tabular figures.
            Text(changeLabel(percent, formats, absolute), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
