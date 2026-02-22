package nl.teunk.currere.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.PaceSplit
import nl.teunk.currere.ui.theme.SplitFast
import nl.teunk.currere.ui.theme.SplitSlow

@Composable
fun SplitsTable(
    splits: List<PaceSplit>,
    modifier: Modifier = Modifier,
) {
    if (splits.isEmpty()) return

    val minPace = splits.minOf { it.splitPaceSecondsPerKm }
    val maxPace = splits.maxOf { it.splitPaceSecondsPerKm }
    val paceRange = (maxPace - minPace).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Km",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(40.dp),
            )
            Text(
                text = "Pace",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(56.dp),
            )
            Box(Modifier.weight(1f))
            Text(
                text = "Time",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(64.dp),
            )
        }

        HorizontalDivider()

        splits.forEach { split ->
            SplitRow(
                split = split,
                minPace = minPace,
                paceRange = paceRange,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun SplitRow(
    split: PaceSplit,
    minPace: Double,
    paceRange: Double,
) {
    // 0.0 = fastest, 1.0 = slowest
    val normalizedPace = ((split.splitPaceSecondsPerKm - minPace) / paceRange).toFloat().coerceIn(0f, 1f)
    val barColor = lerp(SplitFast, SplitSlow, normalizedPace)
    // Bar width: faster = shorter, slower = longer (0.3 to 1.0 range)
    val barFraction = 0.3f + 0.7f * normalizedPace

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (split.isPartial) "${split.kilometerNumber}*" else "${split.kilometerNumber}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(40.dp),
        )

        Text(
            text = StatsAggregator.formatPace(split.splitPaceSecondsPerKm),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(56.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor),
            )
        }

        Text(
            text = StatsAggregator.formatDuration(split.cumulativeDuration),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp),
        )
    }
}
