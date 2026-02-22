package nl.teunk.currere.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.ui.preview.SampleRunDetail
import nl.teunk.currere.ui.theme.CurrereTheme

@Composable
fun StatsRow(
    detail: RunDetail,
    modifier: Modifier = Modifier,
) {
    val session = detail.session

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Row 1: Distance, Time, Pace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icon = Icons.Filled.Straighten,
                label = "Distance",
                value = "${StatsAggregator.formatDistanceKm(session.distanceMeters)} km",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Timer,
                label = "Time",
                value = StatsAggregator.formatDuration(session.activeDuration),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Speed,
                label = "Pace",
                value = session.averagePaceSecondsPerKm?.let {
                    "${StatsAggregator.formatPace(it)}/km"
                } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: Avg HR, Steps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icon = Icons.Filled.FavoriteBorder,
                label = "Avg HR",
                value = session.averageHeartRateBpm?.let { "$it bpm" } ?: "—",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                label = "Steps",
                value = "%,d".format(detail.totalSteps),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun StatsRowPreview() {
    CurrereTheme {
        StatsRow(
            detail = SampleRunDetail,
            modifier = Modifier.padding(16.dp),
        )
    }
}
