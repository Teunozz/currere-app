package nl.teunk.currere.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.common.shape.Shape
import nl.teunk.currere.ui.theme.TextSecondary

internal object ChartDefaults {

    fun formatTimeAxis(totalSeconds: Long): String = when {
        totalSeconds <= 0L -> "0 s"
        totalSeconds < 3600L -> "${totalSeconds / 60}m"
        totalSeconds % 3600L == 0L -> "${totalSeconds / 3600}h"
        else -> "${totalSeconds / 3600}h${(totalSeconds % 3600) / 60}m"
    }

    fun labelSpacingMinutes(totalDurationMinutes: Long): Int = when {
        totalDurationMinutes <= 10 -> 2
        totalDurationMinutes <= 30 -> 5
        totalDurationMinutes <= 60 -> 10
        totalDurationMinutes <= 120 -> 15
        else -> 30
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

    @Composable
    fun rememberGridLine() = rememberAxisGuidelineComponent(
        fill = fill(TextSecondary.copy(alpha = 0.15f)),
        thickness = 0.5.dp,
        shape = Shape.Rectangle,
    )

    @Composable
    fun rememberLabel() = rememberAxisLabelComponent(color = TextSecondary)

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
                spacing = { labelSpacingMinutes },
                addExtremeLabelPadding = true,
            )
        },
    )
}
