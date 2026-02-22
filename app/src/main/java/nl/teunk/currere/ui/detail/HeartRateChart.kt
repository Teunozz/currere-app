package nl.teunk.currere.ui.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import nl.teunk.currere.domain.model.HeartRateSample
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

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(samples) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    samples.map { sample ->
                        java.time.Duration.between(sessionStartTime, sample.time).seconds.toFloat()
                    },
                    samples.map { it.bpm.toFloat() },
                )
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.error

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics {
                contentDescription = "Heart rate chart: min $minHr bpm, avg $avgHr bpm, max $maxHr bpm"
            },
    )
}
