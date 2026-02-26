package nl.teunk.currere.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSetupScreen(
    viewModel: SetupViewModel,
    onBack: () -> Unit,
    onConnected: () -> Unit,
    initialBaseUrl: String = "",
    initialToken: String = "",
) {
    val state by viewModel.state.collectAsState()
    var serverUrl by rememberSaveable { mutableStateOf(initialBaseUrl) }
    var token by rememberSaveable { mutableStateOf(initialToken) }

    LaunchedEffect(state) {
        if (state is SetupState.Success) {
            onConnected()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect to Server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    if (state is SetupState.Error) viewModel.resetState()
                },
                label = { Text("Server URL") },
                placeholder = { Text("https://your-server.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is SetupState.Testing,
            )

            OutlinedTextField(
                value = token,
                onValueChange = {
                    token = it
                    if (state is SetupState.Error) viewModel.resetState()
                },
                label = { Text("API Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is SetupState.Testing,
            )

            if (state is SetupState.Error) {
                Text(
                    text = (state as SetupState.Error).message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.connectWithCredentials(serverUrl, token) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is SetupState.Testing && serverUrl.isNotBlank() && token.isNotBlank(),
            ) {
                if (state is SetupState.Testing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Connect")
                }
            }
        }
    }
}
