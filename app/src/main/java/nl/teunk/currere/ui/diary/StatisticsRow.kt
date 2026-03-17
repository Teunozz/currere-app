package nl.teunk.currere.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.domain.model.RunningStats
import nl.teunk.currere.ui.theme.ChartHeartRate
import nl.teunk.currere.ui.theme.ChartPace
import nl.teunk.currere.ui.theme.CurrereTheme
import nl.teunk.currere.ui.theme.LimeGreen
import nl.teunk.currere.ui.theme.SplitFast

private val GridBreakpoint = 600.dp

@Composable
fun StatisticsSection(stats: RunningStats, modifier: Modifier = Modifier) {
    val cards = buildStatCards(stats)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= GridBreakpoint) {
            // Grid: 2 columns when there's enough room (landscape / tablet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                cards.chunked(2).forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowCards.forEach { card ->
                            StatCard(
                                title = card.title,
                                icon = card.icon,
                                accentColor = card.accentColor,
                                rows = card.rows,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        } else {
            // Vertical list in portrait
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                cards.forEach { card ->
                    StatCard(
                        title = card.title,
                        icon = card.icon,
                        accentColor = card.accentColor,
                        rows = card.rows,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private data class StatCardData(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val rows: List<Pair<String, String>>,
)

private fun buildStatCards(stats: RunningStats): List<StatCardData> = listOf(
    StatCardData(
        title = "Distance",
        icon = Icons.Filled.Straighten,
        accentColor = LimeGreen,
        rows = listOf(
            "Average" to "${stats.avgDistanceKm} km",
            "Longest" to "${stats.longestDistanceKm} km",
            "Total" to "${stats.totalDistanceKm} km",
        ),
    ),
    StatCardData(
        title = "Pace",
        icon = Icons.Filled.Speed,
        accentColor = SplitFast,
        rows = listOf(
            "Average" to "${stats.avgPace} /km",
            "Fastest" to "${stats.fastestPace} /km",
        ),
    ),
    StatCardData(
        title = "Heart Rate",
        icon = Icons.Filled.FavoriteBorder,
        accentColor = ChartHeartRate,
        rows = listOf(
            "Average" to "${stats.avgHeartRate} bpm",
            "Highest" to "${stats.highestHeartRate} bpm",
        ),
    ),
    StatCardData(
        title = "Activity",
        icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
        accentColor = ChartPace,
        rows = listOf(
            "Runs" to stats.totalRuns,
            "Time" to stats.totalTime,
        ),
    ),
)

@Composable
private fun StatCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val cardBackground = accentColor.copy(alpha = 0.08f)
        .compositeOver(MaterialTheme.colorScheme.surface)
    val borderColor = accentColor.copy(alpha = 0.20f)
    val dividerColor = accentColor.copy(alpha = 0.15f)
    val pillBackground = accentColor.copy(alpha = 0.15f)

    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    HorizontalDivider(color = dividerColor)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        color = pillBackground,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun StatisticsSectionPreview() {
    CurrereTheme {
        StatisticsSection(
            stats = RunningStats(
                avgDistanceKm = "7.80",
                longestDistanceKm = "15.01",
                totalDistanceKm = "234.50",
                avgPace = "5:12",
                fastestPace = "4:15",
                avgHeartRate = "152",
                highestHeartRate = "189",
                totalRuns = "30",
                totalTime = "4d 5h 41m",
            ),
        )
    }
}
