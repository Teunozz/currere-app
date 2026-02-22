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
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.ui.preview.SampleHeartRateSamples
import nl.teunk.currere.ui.preview.SampleRunSession
import nl.teunk.currere.ui.theme.CurrereTheme
import java.time.Duration
import java.time.Instant

@Composable
fun HeartRateChart(
    samples: List<HeartRateSample>,
    sessionStartTime: Instant,
    modifier: Modifier = Modifier,
) {
    if (samples.isEmpty()) return

    val minHr = samples.minOf { it.bpm }
    val maxHr = samples.maxOf { it.bpm }
    val avgHr = samples.map { it.bpm }.average().toLong()

    val model = CartesianChartModel(
        LineCartesianLayerModel.build {
            series(
                x = samples.map { Duration.between(sessionStartTime, it.time).seconds },
                y = samples.map { it.bpm },
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
                    contentDescription = "Heart rate chart: min $minHr bpm, avg $avgHr bpm, max $maxHr bpm"
                },
        )
    }
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
