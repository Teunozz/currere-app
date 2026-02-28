package nl.teunk.currere.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.PaceSample
import nl.teunk.currere.ui.preview.SamplePaceSamples
import nl.teunk.currere.ui.preview.SampleRunSession
import nl.teunk.currere.ui.theme.ChartPace
import nl.teunk.currere.ui.theme.CurrereTheme
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

internal data class PaceChartData(
    val xValues: List<Long>,
    val yValues: List<Double>,
    val splitYValue: Double,
)

/** Prepare pace data for charting. Negates Y so faster pace appears at top. */
internal fun preparePaceData(
    samples: List<PaceSample>,
    sessionStartTime: Instant,
): PaceChartData {
    val yValues = samples.map { -it.secondsPerKm }
    return PaceChartData(
        xValues = samples.map { Duration.between(sessionStartTime, it.time).seconds },
        yValues = yValues,
        splitYValue = yValues.min() - 50.0,
    )
}

@Composable
fun PaceChart(
    samples: List<PaceSample>,
    sessionStartTime: Instant,
    averagePaceSecondsPerKm: Double?,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) return

    val avgPaceFormatted = averagePaceSecondsPerKm?.let { StatsAggregator.formatPace(it) } ?: "—"

    val (xValues, yValues, splitYValue) = remember(samples, sessionStartTime) {
        preparePaceData(samples, sessionStartTime)
    }

    val model = CartesianChartModel(
        LineCartesianLayerModel.build {
            series(
                x = xValues,
                y = yValues,
            )
        }
    )

    val rangeProvider = remember(yValues) {
        ChartDefaults.centeredRangeProvider(yValues, padding = 20.0)
    }

    val totalDurationMinutes = remember(xValues) {
        if (xValues.isEmpty()) 0L else xValues.last() / 60
    }
    val labelSpacingMinutes = remember(totalDurationMinutes) {
        ChartDefaults.labelSpacingMinutes(totalDurationMinutes)
    }

    val lineColor = ChartPace
    val gridLine = ChartDefaults.rememberGridLine()
    val axisLabel = ChartDefaults.rememberLabel()

    val paceFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            val seconds = abs(value).toLong()
            val min = seconds / 60
            val sec = seconds % 60
            "$min:${"%02d".format(sec)}"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = lineColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Pace",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$avgPaceFormatted/km",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "(avg)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Chart
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = remember { LineCartesianLayer.LineFill.single(fill(lineColor)) },
                            stroke = LineCartesianLayer.LineStroke.continuous(thickness = 2.dp),
                            areaFill = LineCartesianLayer.AreaFill.single(
                                fill(
                                    ShaderProvider.verticalGradient(
                                        lineColor.copy(alpha = 0.4f).toArgb(),
                                        lineColor.copy(alpha = 0f).toArgb(),
                                    )
                                ),
                                splitY = { splitYValue },
                            ),
                        )
                    ),
                    rangeProvider = rangeProvider,
                ),
                endAxis = VerticalAxis.rememberEnd(
                    label = axisLabel,
                    valueFormatter = paceFormatter,
                    guideline = gridLine,
                    tick = null,
                    line = null,
                    itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 3 }) },
                ),
                bottomAxis = ChartDefaults.rememberBottomTimeAxis(labelSpacingMinutes),
            ),
            model = model,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics {
                    contentDescription = "Pace chart: average $avgPaceFormatted per km"
                },
        )
    }
}

@Preview
@Composable
private fun PaceChartPreview() {
    CurrereTheme {
        PaceChart(
            samples = SamplePaceSamples,
            sessionStartTime = SampleRunSession.startTime,
            averagePaceSecondsPerKm = SampleRunSession.averagePaceSecondsPerKm,
            modifier = Modifier.padding(16.dp),
        )
    }
}
