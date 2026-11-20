package com.lucrometro.ui.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Height of the chart area; the loading and message states take the same, so nothing moves when the chart loads. */
val ChartHeight = 292.dp

// The plot inside the chart area: the right gutter holds the price labels, the bottom one the time labels.
private val PlotStart = 16.dp
private val PriceGutter = 60.dp
private val PlotTop = 12.dp
private val PlotBottom = 256.dp
private val TimeLabelsY = 280.dp

private fun Density.plotLeft() = PlotStart.toPx()
private fun Density.plotRight(width: Float) = width - PriceGutter.toPx()

/** The owner's average price on the chart (decision 0033): [label] "Your avg", [price] formatted for the gutter. */
class AverageMark(val value: Double, val label: String, val price: String)

/** Labels of the TalkBack actions that step the scrub through the points. */
class StepLabels(val previous: String, val next: String, val latest: String)

/**
 * Doop component PriceChart, ready state (decision 0023), drawn on a Canvas: a line with an area fill
 * in [color], gridlines at round prices with their labels in a right gutter, the [layout]'s labels on the bottom row,
 * and a dot at the last point. The plot sits inside [ChartHeight] with gutters for the labels (see [plotLeft]).
 * [baseline] (1D: the previous close) is a dashed line with its price in medium weight, and always inside the axis.
 * While scrubbing, a 1dp line and a dot mark the point under the finger (and the end dot is hidden). The scrub starts
 * on a horizontal drag only, so a vertical drag still scrolls the page. [formatPrice] formats axis labels; [description]
 * is the TalkBack text (the chart itself has no focus targets).
 */
@Composable
fun PriceChart(
    points: List<ChartPoint>,
    layout: ChartLayout,
    baseline: Double?,
    color: Color,
    formatPrice: (Double) -> String,
    /** Smallest price step worth a gridline: the price's last decimal, so labels never repeat. */
    minTickStep: Double,
    description: String,
    modifier: Modifier = Modifier,
    /** The point under the finger while scrubbing (a horizontal drag on the chart), or null. */
    scrubIndex: Int? = null,
    onScrub: (Int?) -> Unit = {},
    /** TalkBack: what the scrubbed point says (date, price, change), and the labels of the actions that step through points. */
    scrubDescription: String? = null,
    stepLabels: StepLabels? = null,
    /** The owner's average price: a dotted line, or a stub at the plot edge when it is outside the axis (decision 0033). */
    average: AverageMark? = null,
    /** The point of the last buy, drawn as a diamond (decision 0034); null when it is outside the range. */
    lastBuyIndex: Int? = null,
) {
    val onScrubState by rememberUpdatedState(onScrub)
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val baselineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val scrubColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val surface = MaterialTheme.colorScheme.surface
    val tertiary = MaterialTheme.colorScheme.tertiary
    val averageLabelStyle = MaterialTheme.typography.labelSmall.copy(color = tertiary)
    val averagePriceStyle = labelStyle.copy(color = tertiary, fontWeight = FontWeight.Medium)
    val axis = remember(points, baseline) {
        val low = listOfNotNull(points.minOf { it.price }, baseline).min()
        val high = listOfNotNull(points.maxOf { it.price }, baseline).max()
        PriceAxis.of(low, high, minTickStep)
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(ChartHeight)
            .semantics {
                contentDescription = description
                scrubDescription?.let { stateDescription = it }
                // A drag cannot be done with TalkBack: the same scrub as three actions.
                if (stepLabels != null && points.size > 1) {
                    customActions = listOf(
                        CustomAccessibilityAction(stepLabels.previous) {
                            onScrubState(((scrubIndex ?: points.lastIndex) - 1).coerceAtLeast(0)); true
                        },
                        CustomAccessibilityAction(stepLabels.next) {
                            onScrubState(((scrubIndex ?: -1) + 1).coerceAtMost(points.lastIndex)); true
                        },
                        CustomAccessibilityAction(stepLabels.latest) { onScrubState(null); true },
                    )
                }
            }
            .pointerInput(layout) {
                // The plot bounds are read per event: the width changes on rotation while the layout does not.
                fun indexAt(px: Float): Int {
                    val left = plotLeft()
                    val right = plotRight(size.width.toFloat())
                    val fraction = ((px - left) / (right - left)).coerceIn(0f, 1f)
                    return layout.xs.indices.minByOrNull { abs(layout.xs[it] - fraction) } ?: 0
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Only a horizontal drag scrubs: if the page scroll takes the gesture first, this returns null.
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (drag != null) {
                        try {
                            onScrubState(indexAt(drag.position.x))
                            horizontalDrag(drag.id) { change ->
                                onScrubState(indexAt(change.position.x))
                                change.consume()
                            }
                        } finally {
                            onScrubState(null) // also when the gesture is cancelled (the chart data changed mid-drag)
                        }
                    }
                }
            },
    ) {
        val left = plotLeft()
        val right = plotRight(size.width)
        val top = PlotTop.toPx()
        val bottom = PlotBottom.toPx()
        fun x(fraction: Float) = left + (right - left) * fraction
        fun y(price: Double) = bottom - (bottom - top) * ((price - axis.low) / (axis.high - axis.low)).toFloat()
        val baselineY = baseline?.let { y(it) }
        // The axis is never stretched to the average (it would flatten the line): outside it, a stub at the nearer edge.
        val averageInside = average != null && average.value in axis.low..axis.high
        val averageY = average?.let {
            when {
                averageInside -> y(it.value)
                it.value < axis.low -> bottom
                else -> top
            }
        }

        for (tick in axis.ticks) {
            val ty = y(tick)
            drawLine(gridColor, Offset(left, ty), Offset(right, ty), strokeWidth = 1.dp.toPx())
            // The baseline's and the average's own labels sit at their lines: skip a gridline label that would overlap.
            val clear = listOfNotNull(baselineY, averageY).all { abs(ty - it) > 16.dp.toPx() }
            if (clear) {
                drawLabel(measurer, formatPrice(tick), labelStyle, right + 8.dp.toPx(), ty, LabelAnchor.Start)
            }
        }
        if (baseline != null && baselineY != null) {
            drawLine(
                baselineColor, Offset(left, baselineY), Offset(right, baselineY), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            drawLabel(
                measurer, formatPrice(baseline), labelStyle.copy(fontWeight = FontWeight.Medium),
                right + 8.dp.toPx(), baselineY, LabelAnchor.Start,
            )
        }

        val xs = layout.xs
        val line = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(x(xs[i]), y(p.price)) else lineTo(x(xs[i]), y(p.price))
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(x(xs.last()), bottom)
            lineTo(x(xs.first()), bottom)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f)), top, bottom))
        drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        if (average != null && averageY != null) {
            // Dotted (round dots 5dp apart), never dashed: dashed is the 1D previous close.
            val dots = PathEffect.dashPathEffect(floatArrayOf(0.1f, 5.dp.toPx()))
            if (averageInside) {
                drawLine(tertiary, Offset(left, averageY), Offset(right, averageY), 1.5.dp.toPx(), StrokeCap.Round, dots)
                val label = measurer.measure(average.label, averageLabelStyle)
                drawText(label, topLeft = Offset(left + 2.dp.toPx(), averageY - 2.dp.toPx() - label.size.height))
                drawLabel(measurer, average.price, averagePriceStyle, right + 8.dp.toPx(), averageY, LabelAnchor.Start)
            } else {
                val below = average.value < axis.low
                val inset = if (below) -3.5.dp.toPx() else 3.5.dp.toPx()
                drawLine(
                    tertiary, Offset(right - 56.dp.toPx(), averageY + inset), Offset(right, averageY + inset),
                    1.5.dp.toPx(), StrokeCap.Round, dots,
                )
                // A chevron pointing off the plot, then the price.
                val tip = if (below) 4.dp.toPx() else -4.dp.toPx()
                val chevron = Path().apply {
                    moveTo(right + 7.dp.toPx(), averageY)
                    lineTo(right + 11.5.dp.toPx(), averageY + tip)
                    lineTo(right + 16.dp.toPx(), averageY)
                }
                drawPath(chevron, tertiary, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLabel(measurer, average.price, averagePriceStyle, right + 19.dp.toPx(), averageY, LabelAnchor.Start)
            }
        }

        // A guard: the index belongs to the parent's state, the points to this call.
        val scrubbed = scrubIndex?.takeIf { it in points.indices && it in xs.indices }
        if (scrubbed == null) {
            val end = Offset(x(xs.last()), y(points.last().price))
            drawCircle(color, 5.dp.toPx(), end)
            drawCircle(surface, 5.dp.toPx(), end, style = Stroke(2.dp.toPx()))
        }
        // Above the end dot, which would hide a buy made on the last point; below the scrub marker.
        lastBuyIndex?.takeIf { it in points.indices && it in xs.indices }?.let { i ->
            val cx = x(xs[i])
            val cy = y(points[i].price)
            val r = 7.5.dp.toPx()
            val diamond = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(diamond, tertiary)
            drawPath(diamond, surface, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        }
        if (scrubbed != null) {
            val sx = x(xs[scrubbed])
            drawLine(scrubColor, Offset(sx, top), Offset(sx, bottom), strokeWidth = 1.dp.toPx())
            val dot = Offset(sx, y(points[scrubbed].price))
            drawCircle(color, 6.dp.toPx(), dot)
            drawCircle(surface, 6.dp.toPx(), dot, style = Stroke(2.dp.toPx()))
        }

        val labelY = TimeLabelsY.toPx()
        layout.labels.forEach { drawLabel(measurer, it.text, labelStyle, x(it.fraction), labelY, it.anchor) }
    }
}

/** Draws [text] vertically centred on [y] (for axis labels) and anchored at [x]. */
private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    y: Float,
    anchor: LabelAnchor,
) {
    val layout = measurer.measure(text, style)
    val dx = when (anchor) {
        LabelAnchor.Start -> 0f
        LabelAnchor.Center -> -layout.size.width / 2f
        LabelAnchor.End -> -layout.size.width.toFloat()
    }
    drawText(layout, topLeft = Offset(x + dx, y - layout.size.height / 2f))
}

/** Gridlines at round prices that bracket the data: [low] and [high] are the first and last tick. */
internal class PriceAxis(val low: Double, val high: Double, val ticks: List<Double>) {
    companion object {
        fun of(min: Double, max: Double, minStep: Double): PriceAxis {
            val span = (max - min).takeIf { it > 0 } ?: (abs(max) * 0.01).takeIf { it > 0 } ?: 1.0
            val step = maxOf(niceStep(span / 3), minStep)
            val low = floor(min / step) * step
            var high = ceil(max / step) * step
            if (high - low < step) high = low + step
            val n = Math.round((high - low) / step).toInt()
            return PriceAxis(low, high, (0..n).map { low + it * step })
        }

        /** 1, 2 or 5 times a power of ten, the smallest one that is at least [raw]. */
        private fun niceStep(raw: Double): Double {
            val pow = 10.0.pow(floor(log10(raw)))
            return listOf(1.0, 2.0, 5.0, 10.0).map { it * pow }.first { it >= raw }
        }
    }
}
