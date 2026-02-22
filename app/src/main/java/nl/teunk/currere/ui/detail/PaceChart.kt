package nl.teunk.currere.ui.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.PaceSample
import java.time.Instant

@Composable
fun PaceChart(
    samples: List<PaceSample>,
    sessionStartTime: Instant,
    averagePaceSecondsPerKm: Double?,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) return

    val minPace = samples.minOf { it.secondsPerKm }
    val maxPace = samples.maxOf { it.secondsPerKm }
    val avgPaceFormatted = averagePaceSecondsPerKm?.let { StatsAggregator.formatPace(it) } ?: "—"

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(samples) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    samples.map { sample ->
                        java.time.Duration.between(sessionStartTime, sample.time).seconds.toFloat()
                    },
                    samples.map { it.secondsPerKm.toFloat() },
                )
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics {
                contentDescription = "Pace chart: fastest ${StatsAggregator.formatPace(minPace)}/km, " +
                    "slowest ${StatsAggregator.formatPace(maxPace)}/km, " +
                    "average $avgPaceFormatted/km"
            },
    )
}
