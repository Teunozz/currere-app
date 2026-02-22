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
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.Shape
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.ui.preview.SampleHeartRateSamples
import nl.teunk.currere.ui.preview.SampleRunSession
import nl.teunk.currere.ui.theme.ChartHeartRate
import nl.teunk.currere.ui.theme.CurrereTheme
import nl.teunk.currere.ui.theme.TextSecondary
import java.time.Duration
import java.time.Instant

@Composable
fun HeartRateChart(
    samples: List<HeartRateSample>,
    sessionStartTime: Instant,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) return

    val avgHr = samples.map { it.bpm }.average().toLong()

    // Downsample: 1 point per minute (average BPM per 60s bucket)
    val downsampled = remember(samples, sessionStartTime) {
        samples
            .groupBy { Duration.between(sessionStartTime, it.time).seconds / 60 }
            .entries
            .sortedBy { it.key }
            .map { (minuteBucket, bucketSamples) ->
                minuteBucket * 60L to bucketSamples.map { it.bpm }.average()
            }
    }

    val model = CartesianChartModel(
        LineCartesianLayerModel.build {
            series(
                x = downsampled.map { it.first },
                y = downsampled.map { it.second },
            )
        }
    )

    // Y-axis range: centered on average, padded beyond min/max
    val hrValues = remember(downsampled) { downsampled.map { it.second } }
    val rangeProvider = remember(hrValues) {
        val min = hrValues.min()
        val max = hrValues.max()
        val avg = hrValues.average()
        val padding = 5.0
        val halfRange = maxOf(avg - min, max - avg) + padding
        CartesianLayerRangeProvider.fixed(minY = avg - halfRange, maxY = avg + halfRange)
    }

    val totalDurationMinutes = remember(downsampled) {
        if (downsampled.isEmpty()) 0L else downsampled.last().first / 60
    }
    val labelSpacingMinutes = remember(totalDurationMinutes) {
        when {
            totalDurationMinutes <= 10 -> 2
            totalDurationMinutes <= 30 -> 5
            totalDurationMinutes <= 60 -> 10
            totalDurationMinutes <= 120 -> 15
            else -> 30
        }
    }

    val lineColor = ChartHeartRate

    val gridLine = rememberAxisGuidelineComponent(
        fill = fill(TextSecondary.copy(alpha = 0.15f)),
        thickness = 0.5.dp,
        shape = Shape.Rectangle,
    )

    val axisLabel = rememberAxisLabelComponent(color = TextSecondary)

    val timeFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            formatTimeAxis(value.toLong())
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
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = lineColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Heart Rate",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$avgHr bpm",
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
                            ),
                        )
                    ),
                    rangeProvider = rangeProvider,
                ),
                endAxis = VerticalAxis.rememberEnd(
                    label = axisLabel,
                    valueFormatter = remember {
                        CartesianValueFormatter { _, value, _ -> "${value.toLong()}" }
                    },
                    guideline = gridLine,
                    tick = null,
                    line = null,
                    itemPlacer = remember { VerticalAxis.ItemPlacer.count({ 3 }) },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    label = axisLabel,
                    valueFormatter = timeFormatter,
                    guideline = null,
                    tick = null,
                    line = null,
                    itemPlacer = remember(labelSpacingMinutes) {
                        HorizontalAxis.ItemPlacer.aligned(
                            spacing = { labelSpacingMinutes },
                            addExtremeLabelPadding = true,
                        )
                    },
                ),
            ),
            model = model,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics {
                    contentDescription = "Heart rate chart: average $avgHr bpm"
                },
        )
    }
}

internal fun formatTimeAxis(totalSeconds: Long): String = when {
    totalSeconds <= 0L -> "0 s"
    totalSeconds < 3600L -> "${totalSeconds / 60}m"
    totalSeconds % 3600L == 0L -> "${totalSeconds / 3600}h"
    else -> "${totalSeconds / 3600}h${(totalSeconds % 3600) / 60}m"
}

@Preview
@Composable
private fun HeartRateChartPreview() {
    CurrereTheme {
        HeartRateChart(
            samples = SampleHeartRateSamples,
            sessionStartTime = SampleRunSession.startTime,
            modifier = Modifier.padding(16.dp),
        )
    }
}
