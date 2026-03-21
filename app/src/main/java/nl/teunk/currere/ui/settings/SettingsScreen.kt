package nl.teunk.currere.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.R
import nl.teunk.currere.ui.theme.CurrereTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onScanQrCode: () -> Unit,
    onManualSetup: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onDisconnect = {
            viewModel.disconnect()
        },
        onScanQrCode = onScanQrCode,
        onManualSetup = onManualSetup,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    onScanQrCode: () -> Unit,
    onManualSetup: () -> Unit
) {
    var showDisconnectDialog by remember { mutableStateOf(false) }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text(stringResource(R.string.disconnect)) },
            text = { Text(stringResource(R.string.disconnect_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDisconnect()
                    showDisconnectDialog = false
                }) {
                    Text(stringResource(R.string.disconnect), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.server_connection),
                style = MaterialTheme.typography.titleMedium,
            )

            if (uiState.isConnected) {
                Text(
                    text = stringResource(R.string.connected_to),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.serverUrl ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.format_last_sync, uiState.lastSyncTime ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.disconnect))
                }
            } else {
                Text(
                    text = stringResource(R.string.not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onScanQrCode,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.scan_qr_code))
                }

                OutlinedButton(
                    onClick = onManualSetup,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.enter_manually))
                }
            }
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    CurrereTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            onBack = {},
            onDisconnect = {},
            onScanQrCode = {},
            onManualSetup = {}
        )
    }
}
