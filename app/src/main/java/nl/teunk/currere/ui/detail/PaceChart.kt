package nl.teunk.currere.ui.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.PaceSample
import nl.teunk.currere.ui.preview.SamplePaceSamples
import nl.teunk.currere.ui.preview.SampleRunSession
import nl.teunk.currere.ui.theme.CurrereTheme
import java.time.Duration
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

    val model = CartesianChartModel(
        LineCartesianLayerModel.build {
            series(
                x = samples.map { Duration.between(sessionStartTime, it.time).seconds },
                y = samples.map { it.secondsPerKm },
            )
        }
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(rememberLineCartesianLayer()),
            model = model,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
                .semantics {
                    contentDescription = "Pace chart: fastest ${StatsAggregator.formatPace(minPace)}/km, " +
                        "slowest ${StatsAggregator.formatPace(maxPace)}/km, " +
                        "average $avgPaceFormatted/km"
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
