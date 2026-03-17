package nl.teunk.currere.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.domain.model.RunningStats
import nl.teunk.currere.ui.theme.ChartHeartRate
import nl.teunk.currere.ui.theme.ChartPace
import nl.teunk.currere.ui.theme.CurrereTheme
import nl.teunk.currere.ui.theme.LimeGreen
import nl.teunk.currere.ui.theme.SplitFast

@Composable
fun StatisticsSection(stats: RunningStats, modifier: Modifier = Modifier) {
    val cells = buildStatCells(stats)

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(cells[0], Modifier.weight(1f))
            StatTile(cells[1], Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(cells[2], Modifier.weight(1f))
            StatTile(cells[3], Modifier.weight(1f))
        }
    }
}

private data class StatCellData(
    val heroValue: String,
    val heroUnit: String,
    val label: String,
    val detail: String,
    val accentColor: Color,
)

private fun buildStatCells(stats: RunningStats): List<StatCellData> = listOf(
    StatCellData(
        heroValue = stats.totalDistanceKm,
        heroUnit = "km",
        label = "Total distance",
        detail = "Longest ${stats.longestDistanceKm} km",
        accentColor = LimeGreen,
    ),
    StatCellData(
        heroValue = stats.avgPace,
        heroUnit = "/km",
        label = "Average pace",
        detail = "Fastest ${stats.fastestPace} /km",
        accentColor = SplitFast,
    ),
    StatCellData(
        heroValue = stats.avgHeartRate,
        heroUnit = "bpm",
        label = "Average heart rate",
        detail = "Highest ${stats.highestHeartRate} bpm",
        accentColor = ChartHeartRate,
    ),
    StatCellData(
        heroValue = stats.totalRuns,
        heroUnit = "runs",
        label = "Total activity",
        detail = "${stats.totalTime} total",
        accentColor = ChartPace,
    ),
)

@Composable
private fun StatTile(
    data: StatCellData,
    modifier: Modifier = Modifier,
) {
    val tileBackground = data.accentColor.copy(alpha = 0.15f)
        .compositeOver(MaterialTheme.colorScheme.surface)

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = tileBackground),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text(
                    text = data.heroValue,
                    style = MaterialTheme.typography.headlineSmall,
                    color = data.accentColor,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = data.heroUnit,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = data.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = data.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
