package nl.teunk.currere.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.data.sync.SyncRecord
import nl.teunk.currere.data.sync.SyncState
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.ui.DateFormatters
import nl.teunk.currere.ui.preview.SampleRunSession
import nl.teunk.currere.ui.preview.SampleRunSessions
import nl.teunk.currere.ui.theme.CurrereTheme

@Composable
fun RunCard(
    session: RunSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    syncRecord: SyncRecord? = null,
) {
    val formattedDate = DateFormatters.dateTimeCompact.format(session.startTime)
    val distanceText = "${StatsAggregator.formatDistanceKm(session.distanceMeters)} km"
    val durationText = StatsAggregator.formatDuration(session.activeDuration)
    val paceText = session.averagePaceSecondsPerKm?.let {
        StatsAggregator.formatPace(it)
    } ?: "—"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription =
                    "${session.title}, $formattedDate, $distanceText, $durationText, pace $paceText"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Header: title + sync indicator + chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Date
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(
                    icon = Icons.Filled.Straighten,
                    value = distanceText,
                    label = "Distance",
                )
                StatItem(
                    icon = Icons.Filled.Timer,
                    value = durationText,
                    label = "Duration",
                )
                StatItem(
                    icon = Icons.Filled.Speed,
                    value = paceText,
                    label = "Pace",
                )
                StatItem(
                    icon = Icons.Filled.FavoriteBorder,
                    value = session.averageHeartRateBpm?.toString() ?: "—",
                    label = "Avg BPM",
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
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
            syncRecord = SyncRecord(serverId = 1, state = SyncState.SYNCED),
        )
    }
}

@Preview
@Composable
private fun RunCardNoHeartRatePreview() {
    CurrereTheme {
        RunCard(
            session = SampleRunSession.copy(averageHeartRateBpm = null),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun RunCardListPreview() {
    CurrereTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            SampleRunSessions.forEach { item ->
                RunCard(
                    session = item.session,
                    syncRecord = item.syncRecord,
                    onClick = {},
                )
            }
        }
    }
}
