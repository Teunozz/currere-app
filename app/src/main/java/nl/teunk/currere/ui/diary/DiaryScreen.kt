package nl.teunk.currere.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.domain.model.RunningStats
import nl.teunk.currere.ui.DateFormatters
import nl.teunk.currere.ui.components.EmptyState
import nl.teunk.currere.ui.preview.SampleRunSessions
import nl.teunk.currere.ui.theme.CurrereTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onRunClick: (RunSession) -> Unit,
    onSettingsClick: () -> Unit,
    onShowAllRuns: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    DiaryScreenContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onRunClick = onRunClick,
        onSettingsClick = onSettingsClick,
        onShowAllRuns = onShowAllRuns,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreenContent(
    uiState: DiaryUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRunClick: (RunSession) -> Unit,
    onSettingsClick: () -> Unit = {},
    onShowAllRuns: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currere") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                is DiaryUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is DiaryUiState.Empty -> {
                    EmptyState()
                }

                is DiaryUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "recent_runs") {
                            RecentRunsCard(
                                items = uiState.items.take(3),
                                totalCount = uiState.items.size,
                                onRunClick = onRunClick,
                                onShowAllRuns = onShowAllRuns,
                            )
                        }
                        item(key = "pace_chart") {
                            PaceBarChart(
                                runs = uiState.items.take(5).map { it.session },
                            )
                        }
                        uiState.stats?.let { stats ->
                            item(key = "statistics") {
                                StatisticsSection(stats = stats)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRunsCard(
    items: List<DiaryRunItem>,
    totalCount: Int,
    onRunClick: (RunSession) -> Unit,
    onShowAllRuns: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Runs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (totalCount > items.size) {
                    Text(
                        text = "$totalCount total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Latest run — hero treatment
            LatestRunRow(
                item = items.first(),
                accent = accent,
                onClick = { onRunClick(items.first().session) },
            )

            // Older runs
            if (items.size > 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                items.drop(1).forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                    OlderRunRow(
                        session = item.session,
                        accent = accent,
                        onClick = { onRunClick(item.session) },
                    )
                }
            }

            // View all
            if (totalCount > items.size) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShowAllRuns)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "View all $totalCount runs",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestRunRow(
    item: DiaryRunItem,
    accent: Color,
    onClick: () -> Unit,
) {
    val session = item.session
    val distanceNum = StatsAggregator.formatDistanceKm(session.distanceMeters)
    val duration = StatsAggregator.formatDuration(session.activeDuration)
    val pace = session.averagePaceSecondsPerKm?.let {
        "${StatsAggregator.formatPace(it)} /km"
    }
    val hr = session.averageHeartRateBpm?.let { "$it bpm" }
    val date = DateFormatters.dateShort.format(session.startTime)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Title + chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Hero distance — left-aligned, number and unit on same baseline
        Row {
            Text(
                text = distanceNum,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "km",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Spacer(Modifier.height(10.dp))

        // Supporting details
        Text(
            text = listOfNotNull(date, duration, pace, hr).joinToString(" \u00B7 "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OlderRunRow(
    session: RunSession,
    accent: Color,
    onClick: () -> Unit,
) {
    val distance = "${StatsAggregator.formatDistanceKm(session.distanceMeters)} km"
    val duration = StatsAggregator.formatDuration(session.activeDuration)
    val pace = session.averagePaceSecondsPerKm?.let {
        "${StatsAggregator.formatPace(it)} /km"
    }
    val date = DateFormatters.dateShort.format(session.startTime)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = distance,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOfNotNull(date, duration, pace).joinToString(" \u00B7 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview
@Composable
private fun DiaryScreenSuccessPreview() {
    CurrereTheme {
        DiaryScreenContent(
            uiState = DiaryUiState.Success(
                items = SampleRunSessions,
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
            ),
            isRefreshing = false,
            onRefresh = {},
            onRunClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview
@Composable
private fun DiaryScreenEmptyPreview() {
    CurrereTheme {
        DiaryScreenContent(
            uiState = DiaryUiState.Empty,
            isRefreshing = false,
            onRefresh = {},
            onRunClick = {},
            onSettingsClick = {},
        )
    }
}
