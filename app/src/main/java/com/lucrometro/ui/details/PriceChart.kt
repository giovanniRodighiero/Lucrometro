package com.lucrometro.ui.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Doop component PriceChart, ready state (decision 0023), drawn on a Canvas: a line with an area fill
 * in [color], gridlines at round prices with their labels in a right gutter, the [layout]'s labels on the bottom row,
 * and a dot at the last point. The plot spans x 16dp to (width - 60dp) and y 12dp to 288dp of a 324dp area.
 * [baseline] (1D: the previous close) is a dashed line with its price in medium weight, and always inside the axis.
 * [formatPrice] formats axis labels; [description] is the TalkBack text (the chart itself has no focus targets).
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
) {
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val baselineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val surface = MaterialTheme.colorScheme.surface
    val axis = remember(points, baseline) {
        val low = listOfNotNull(points.minOf { it.price }, baseline).min()
        val high = listOfNotNull(points.maxOf { it.price }, baseline).max()
        PriceAxis.of(low, high, minTickStep)
    }

    Canvas(modifier.fillMaxWidth().height(324.dp).semantics { contentDescription = description }) {
        val left = 16.dp.toPx()
        val right = size.width - 60.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = 288.dp.toPx()
        fun x(fraction: Float) = left + (right - left) * fraction
        fun y(price: Double) = bottom - (bottom - top) * ((price - axis.low) / (axis.high - axis.low)).toFloat()
        val baselineY = baseline?.let { y(it) }

        for (tick in axis.ticks) {
            val ty = y(tick)
            drawLine(gridColor, Offset(left, ty), Offset(right, ty), strokeWidth = 1.dp.toPx())
            // The baseline's own label sits at its line: skip a gridline label that would overlap it.
            if (baselineY == null || abs(ty - baselineY) > 14.dp.toPx()) {
                drawLabel(measurer, formatPrice(tick), labelStyle, right + 8.dp.toPx(), ty, Anchor.Start)
            }
        }
        if (baseline != null && baselineY != null) {
            drawLine(
                baselineColor, Offset(left, baselineY), Offset(right, baselineY), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            drawLabel(
                measurer, formatPrice(baseline), labelStyle.copy(fontWeight = FontWeight.Medium),
                right + 8.dp.toPx(), baselineY, Anchor.Start,
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

        val end = Offset(x(xs.last()), y(points.last().price))
        drawCircle(color, 5.dp.toPx(), end)
        drawCircle(surface, 5.dp.toPx(), end, style = Stroke(2.dp.toPx()))

        val labelY = 312.dp.toPx()
        layout.labels.forEach {
            val anchor = when (it.anchor) {
                LabelAnchor.Start -> Anchor.Start
                LabelAnchor.Center -> Anchor.Center
                LabelAnchor.End -> Anchor.End
            }
            drawLabel(measurer, it.text, labelStyle, x(it.fraction), labelY, anchor)
        }
    }
}

private enum class Anchor { Start, Center, End }

/** Draws [text] vertically centred on [y] (for axis labels) and anchored at [x]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    x: Float,
    y: Float,
    anchor: Anchor,
) {
    val layout = measurer.measure(text, style)
    val dx = when (anchor) {
        Anchor.Start -> 0f
        Anchor.Center -> -layout.size.width / 2f
        Anchor.End -> -layout.size.width.toFloat()
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
