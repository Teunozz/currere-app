package nl.teunk.currere.ui.detail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import nl.teunk.currere.R
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import nl.teunk.currere.domain.compute.OutlierFilter
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
    val smoothedYValues: List<Double>,
    val splitYValue: Double,
)

/** Prepare pace data for charting. Negates Y so faster pace appears at top. */
internal fun preparePaceData(
    samples: List<PaceSample>,
    sessionStartTime: Instant,
): PaceChartData {
    val yValues = OutlierFilter.clampOutliers(samples.map { -it.secondsPerKm }, multiplier = 3.0)
    val smoothed = OutlierFilter.movingAverage(yValues, window = 21)
    return PaceChartData(
        xValues = samples.map { Duration.between(sessionStartTime, it.time).seconds },
        yValues = yValues,
        smoothedYValues = smoothed,
        splitYValue = yValues.min() - 50.0,
    )
}

@Composable
fun PaceChart(
    samples: List<PaceSample>,
    sessionStartTime: Instant,
    averagePaceSecondsPerKm: Double?,
    totalDurationSeconds: Long,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) return

    val avgPaceFormatted = averagePaceSecondsPerKm?.let { StatsAggregator.formatPace(it) } ?: "—"
    val chartDescription = stringResource(R.string.pace_chart_description, avgPaceFormatted)

    val chartData = remember(samples, sessionStartTime) {
        preparePaceData(samples, sessionStartTime)
    }

    val avgValue = remember(chartData.yValues) { chartData.yValues.average() }

    val model = CartesianChartModel(
        LineCartesianLayerModel.build {
            series(x = chartData.xValues, y = chartData.smoothedYValues)
            series(x = listOf(0L, totalDurationSeconds), y = listOf(avgValue, avgValue))
        }
    )

    val rangeProvider = remember(chartData.yValues) {
        ChartDefaults.centeredRangeProvider(chartData.yValues, padding = 20.0)
    }

    val labelSpacingMinutes = remember(totalDurationSeconds) {
        ChartDefaults.labelSpacingMinutes(totalDurationSeconds / 60)
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

    val paceMarkerFormatter = remember(lineColor) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.firstOrNull() as? LineCartesianLayerMarkerTarget
                ?: return@ValueFormatter ""
            val point = target.points.firstOrNull() ?: return@ValueFormatter ""
            val paceSeconds = abs(point.entry.y).toLong()
            val min = paceSeconds / 60
            val sec = paceSeconds % 60
            val time = StatsAggregator.formatDuration(Duration.ofSeconds(point.entry.x.toLong()))
            buildAnnotatedString {
                withStyle(SpanStyle(color = lineColor, fontWeight = FontWeight.Bold)) {
                    append("$min:${"%02d".format(sec)} /km")
                }
                append(" at $time")
            }
        }
    }
    val marker = ChartDefaults.rememberMarker(valueFormatter = paceMarkerFormatter)
    val markerVisibilityListener = ChartDefaults.rememberHapticMarkerVisibilityListener()

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
                    text = stringResource(R.string.pace),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.format_pace_per_km, avgPaceFormatted),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.label_avg),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Chart – custom long-press timeout for faster marker activation
        val viewConfig = LocalViewConfiguration.current
        val chartViewConfig = remember(viewConfig) {
            object : ViewConfiguration by viewConfig {
                override val longPressTimeoutMillis get() = ChartDefaults.LONG_PRESS_TIMEOUT_MS
            }
        }
        CompositionLocalProvider(LocalViewConfiguration provides chartViewConfig) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = remember { LineCartesianLayer.LineFill.single(Fill(lineColor)) },
                                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    Fill(
                                        Brush.verticalGradient(
                                            listOf(
                                                lineColor.copy(alpha = 0.4f),
                                                lineColor.copy(alpha = 0f),
                                            )
                                        )
                                    ),
                                    splitY = { chartData.splitYValue },
                                ),
                            ),
                            // Avg line – invisible
                            LineCartesianLayer.rememberLine(
                                fill = remember { LineCartesianLayer.LineFill.single(Fill(Color.Transparent)) },
                                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 0.dp),
                            ),
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
                        size = ChartDefaults.yAxisSize,
                    ),
                    bottomAxis = ChartDefaults.rememberBottomTimeAxis(labelSpacingMinutes),
                    marker = marker,
                    markerVisibilityListener = markerVisibilityListener,
                    markerController = ChartDefaults.rememberShowOnLongPress(),
                    getXStep = { 1.0 },
                ),
                model = model,
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .semantics {
                        contentDescription = chartDescription
                    },
            )
        }
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
            totalDurationSeconds = 42 * 60L,
            modifier = Modifier.padding(16.dp),
        )
    }
}
