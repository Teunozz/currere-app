package nl.teunk.currere.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.RunDetail

@Composable
fun StatsRow(
    detail: RunDetail,
    modifier: Modifier = Modifier,
) {
    val session = detail.session

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(
            icon = android.R.drawable.ic_menu_mapmode,
            label = "Distance",
            value = "${StatsAggregator.formatDistanceKm(session.distanceMeters)} km",
        )
        StatItem(
            icon = android.R.drawable.ic_menu_recent_history,
            label = "Time",
            value = StatsAggregator.formatDuration(session.activeDuration),
        )
        StatItem(
            icon = android.R.drawable.ic_menu_myplaces,
            label = "Avg HR",
            value = session.averageHeartRateBpm?.let { "$it bpm" } ?: "—",
        )
        StatItem(
            icon = android.R.drawable.ic_menu_send,
            label = "Pace",
            value = session.averagePaceSecondsPerKm?.let {
                "${StatsAggregator.formatPace(it)}/km"
            } ?: "—",
        )
        StatItem(
            icon = android.R.drawable.ic_menu_directions,
            label = "Steps",
            value = "%,d".format(detail.totalSteps),
        )
    }
}

@Composable
private fun StatItem(
    icon: Int,
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
