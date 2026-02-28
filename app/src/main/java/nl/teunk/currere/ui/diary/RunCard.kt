package nl.teunk.currere.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.data.sync.SyncRecord
import nl.teunk.currere.data.sync.SyncState
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.ui.preview.SampleRunSession
import nl.teunk.currere.ui.theme.CurrereTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter
    .ofPattern("EEE d MMM yyyy, HH:mm", Locale.US)
    .withZone(ZoneId.systemDefault())

@Composable
fun RunCard(
    session: RunSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    syncRecord: SyncRecord? = null,
) {
    val formattedDate = dateFormatter.format(session.startTime)
    val distanceText = "${StatsAggregator.formatDistanceKm(session.distanceMeters)} km"
    val durationText = StatsAggregator.formatDuration(session.activeDuration)
    val paceText = session.averagePaceSecondsPerKm?.let {
        "${StatsAggregator.formatPace(it)}/km"
    } ?: "—"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${session.title}, $formattedDate, $distanceText, $durationText, pace $paceText"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Center: title + date + stats
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (syncRecord != null) {
                        SyncIndicator(syncRecord.state)
                    }
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = paceText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Right: distance + chevron
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncIndicator(state: SyncState) {
    val (icon, tint, description) = when (state) {
        SyncState.SYNCED -> Triple(
            Icons.Filled.CloudDone,
            MaterialTheme.colorScheme.primary,
            "Synced",
        )
        SyncState.PENDING -> Triple(
            Icons.Filled.CloudUpload,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Pending sync",
        )
        SyncState.FAILED -> Triple(
            Icons.Filled.CloudOff,
            MaterialTheme.colorScheme.error,
            "Sync failed",
        )
    }
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
}

@Preview
@Composable
private fun RunCardPreview() {
    CurrereTheme {
        RunCard(
            session = SampleRunSession,
            onClick = {},
        )
    }
}
