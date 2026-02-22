package nl.teunk.currere.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.teunk.currere.domain.compute.StatsAggregator
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter
    .ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private val timeFormatter = DateTimeFormatter
    .ofPattern("HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? DetailUiState.Success)?.detail?.session?.title ?: "Run Detail"
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is DetailUiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is DetailUiState.Success -> {
                val detail = state.detail
                val session = detail.session

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Header
                    Text(
                        text = dateFormatter.format(session.startTime),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "${timeFormatter.format(session.startTime)} – ${timeFormatter.format(session.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(20.dp))

                    // Stats
                    StatsRow(detail = detail)

                    // Heart Rate Chart (hidden when no data)
                    if (detail.heartRateSamples.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        SectionHeader("Heart Rate")
                        HeartRateChart(
                            samples = detail.heartRateSamples,
                            sessionStartTime = session.startTime,
                        )
                    }

                    // Pace Chart
                    if (detail.paceSamples.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        SectionHeader("Pace")
                        PaceChart(
                            samples = detail.paceSamples,
                            sessionStartTime = session.startTime,
                            averagePaceSecondsPerKm = session.averagePaceSecondsPerKm,
                        )
                    }

                    // Splits Table
                    if (detail.splits.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        SectionHeader("Splits")
                        SplitsTable(splits = detail.splits)
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    )
}
