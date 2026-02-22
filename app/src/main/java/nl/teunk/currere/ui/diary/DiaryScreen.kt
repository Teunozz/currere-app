package nl.teunk.currere.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    DiaryScreenContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onRunClick = onRunClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreenContent(
    uiState: DiaryUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRunClick: (RunSession) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currere") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
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
                            items = uiState.sessions,
                            key = { it.id },
                        ) { session ->
                            RunCard(
                                session = session,
                                onClick = { onRunClick(session) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DiaryScreenPreview() {
    CurrereTheme {
        DiaryScreenContent(
            uiState = DiaryUiState.Success(SampleRunSessions),
            isRefreshing = false,
            onRefresh = {},
            onRunClick = {},
        )
    }
}
