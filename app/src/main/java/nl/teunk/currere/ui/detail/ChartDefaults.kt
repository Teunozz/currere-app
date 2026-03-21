package nl.teunk.currere.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.common.Fill
import nl.teunk.currere.ui.theme.TextSecondary

internal object ChartDefaults {

    fun formatTimeAxis(totalSeconds: Long): String {
        val totalMinutes = (totalSeconds + 30) / 60
        return when {
            totalMinutes <= 0L -> "0m"
            totalMinutes < 60L -> "${totalMinutes}m"
            totalMinutes % 60L == 0L -> "${totalMinutes / 60}h"
            else -> "${totalMinutes / 60}h${totalMinutes % 60}m"
        }
    }

    private val niceIntervals = intArrayOf(1, 2, 5, 10, 15, 20, 30, 60)

    fun labelSpacingMinutes(totalDurationMinutes: Long): Int {
        val minSpacing = (totalDurationMinutes.toInt() / 4 + 1).coerceAtLeast(1)
        return niceIntervals.firstOrNull { it >= minSpacing } ?: minSpacing
    }

    fun centeredRangeProvider(
        values: List<Double>,
        padding: Double,
    ): CartesianLayerRangeProvider {
        val min = values.min()
        val max = values.max()
        val avg = values.average()
        val halfRange = maxOf(avg - min, max - avg) + padding
        return CartesianLayerRangeProvider.fixed(minY = avg - halfRange, maxY = avg + halfRange)
    }

    val yAxisSize: BaseAxis.Size = BaseAxis.Size.Fixed(32.dp)

    @Composable
    fun rememberGridLine() = rememberAxisGuidelineComponent(
        fill = Fill(TextSecondary.copy(alpha = 0.15f)),
        thickness = 0.5.dp,
        shape = RectangleShape,
    )

    @Composable
    fun rememberLabel() = rememberAxisLabelComponent(style = TextStyle(color = TextSecondary))

    @Composable
    fun rememberTimeFormatter() = remember {
        CartesianValueFormatter { _, value, _ -> formatTimeAxis(value.toLong()) }
    }

    @Composable
    fun rememberBottomTimeAxis(labelSpacingMinutes: Int) = HorizontalAxis.rememberBottom(
        label = rememberLabel(),
        valueFormatter = rememberTimeFormatter(),
        guideline = null,
        tick = null,
        line = null,
        itemPlacer = remember(labelSpacingMinutes) {
            HorizontalAxis.ItemPlacer.aligned(
                spacing = { labelSpacingMinutes * 60 },
                addExtremeLabelPadding = true,
            )
        },
    )
}
