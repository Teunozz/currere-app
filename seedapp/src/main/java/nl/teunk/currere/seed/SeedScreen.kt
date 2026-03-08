package nl.teunk.currere.seed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.viewmodel.compose.viewModel

private val WRITE_PERMISSIONS = setOf(
    HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    HealthPermission.getWritePermission(DistanceRecord::class),
    HealthPermission.getWritePermission(StepsRecord::class),
    HealthPermission.getWritePermission(HeartRateRecord::class),
    HealthPermission.getWritePermission(SpeedRecord::class),
)

private val COUNT_OPTIONS = listOf(10, 25, 50, 100)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeedScreen(viewModel: SeedViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedCount by rememberSaveable { mutableIntStateOf(25) }
    var permissionsGranted by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        permissionsGranted = granted.containsAll(WRITE_PERMISSIONS)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(WRITE_PERMISSIONS)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Currere Seed",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Insert dummy runs into Health Connect",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            if (!permissionsGranted) {
                Text(
                    text = "Write permissions not granted",
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(WRITE_PERMISSIONS) }) {
                    Text("Grant permissions")
                }
            } else {
                Text("Number of runs:", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COUNT_OPTIONS.forEach { count ->
                        FilterChip(
                            selected = selectedCount == count,
                            onClick = { selectedCount = count },
                            label = { Text("$count") },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                when (val s = state) {
                    is SeedState.Idle -> {
                        Button(onClick = { viewModel.seed(context, selectedCount) }) {
                            Text("Seed $selectedCount runs")
                        }
                    }
                    is SeedState.Seeding -> {
                        LinearProgressIndicator(
                            progress = { s.progress.toFloat() / s.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Inserting run ${s.progress}/${s.total}...")
                    }
                    is SeedState.Done -> {
                        Text(
                            text = "Done! Inserted ${s.count} runs.",
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.reset() }) {
                            Text("Seed more")
                        }
                    }
                    is SeedState.Error -> {
                        Text(
                            text = "Error: ${s.message}",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.reset() }) {
                            Text("Try again")
                        }
                    }
                }
            }
        }
    }
}
