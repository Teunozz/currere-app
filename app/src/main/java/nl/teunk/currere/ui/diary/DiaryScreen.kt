package nl.teunk.currere.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.ui.components.EmptyState
import nl.teunk.currere.ui.preview.SampleRunSessions
import nl.teunk.currere.ui.theme.CurrereTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onRunClick: (RunSession) -> Unit,
    onSettingsClick: () -> Unit,
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.session.id },
                        ) { item ->
                            RunCard(
                                session = item.session,
                                syncRecord = item.syncRecord,
                                onClick = { onRunClick(item.session) },
                            )
                        }
                    }
                }
            }
        }
    }
}
