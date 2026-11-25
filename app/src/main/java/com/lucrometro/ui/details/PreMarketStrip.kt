package com.lucrometro.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucrometro.R
import com.lucrometro.data.PreMarket
import com.lucrometro.data.Quote
import com.lucrometro.ui.common.ChangeDirection
import com.lucrometro.ui.common.QuoteFormats
import com.lucrometro.ui.common.TimeFormats
import com.lucrometro.ui.common.changeDirection
import com.lucrometro.ui.common.changeLabel
import com.lucrometro.ui.common.color
import java.time.ZoneId

/**
 * Whether the strip shows at [nowEpoch] (decision 0055): the market known to be closed, before the opening the price
 * comes before, and no regular trade ([quote]) newer than the price.
 */
fun PreMarket.isShown(nowEpoch: Long, status: MarketStatus?, quote: Quote): Boolean =
    status != null && status !is MarketStatus.Open && nowEpoch < opensEpoch && timeEpoch > quote.marketTimeEpoch

/**
 * Doop component PreMarketStrip (decision 0055): one tonal line under the quote header with the pre-market price, its
 * change from the last close and its time in the exchange's clock. The time wraps when the row does not fit. Read by
 * TalkBack as one sentence.
 */
@Composable
fun PreMarketStrip(
    preMarket: PreMarket,
    quote: Quote,
    zone: ZoneId,
    formats: QuoteFormats,
    times: TimeFormats,
    modifier: Modifier = Modifier,
) {
    val price = formats.number(quote, preMarket.price)
    val time = times.format(times.timeSkeleton, preMarket.timeEpoch, zone) + " " + times.zoneName(preMarket.timeEpoch, zone)
    val direction = changeDirection(preMarket.changePercent)
    val (icon, sentence) = when (direction) {
        ChangeDirection.Up -> R.drawable.ic_change_up to R.string.pre_market_description_up
        ChangeDirection.Down -> R.drawable.ic_change_down to R.string.pre_market_description_down
        ChangeDirection.Flat -> R.drawable.ic_change_flat to R.string.pre_market_description_flat
    }
    val description = stringResource(sentence, price, formats.changeMagnitude(preMarket.changePercent), time)
    FlowRow(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium)
            .clearAndSetSemantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.pre_market),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(price, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        // The arrow carries the direction too, not the colour alone (decision 0007).
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, Modifier.size(16.dp), tint = direction.color())
            Text(
                changeLabel(preMarket.changePercent, formats),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = direction.color(),
            )
        }
        Text(
            time,
            // At its full width, or a weighted item squeezes in at its longest word's and loses the rest: it must wrap.
            modifier = Modifier.weight(1f).width(IntrinsicSize.Max),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
