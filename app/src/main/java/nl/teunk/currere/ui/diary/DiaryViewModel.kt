package nl.teunk.currere.ui.diary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.data.sync.SyncRecord
import nl.teunk.currere.data.sync.SyncRepository
import nl.teunk.currere.data.sync.SyncResult
import nl.teunk.currere.data.sync.SyncStatusStore
import nl.teunk.currere.data.sync.SyncWorker
import nl.teunk.currere.domain.model.RunSession

data class DiaryRunItem(
    val session: RunSession,
    val syncRecord: SyncRecord? = null,
)

sealed interface DiaryUiState {
    data object Loading : DiaryUiState
    data object Empty : DiaryUiState
    data class Success(val items: List<DiaryRunItem>) : DiaryUiState
}

class DiaryViewModel(
    private val healthConnectSource: HealthConnectSource,
    private val syncStatusStore: SyncStatusStore,
    private val syncRepository: SyncRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<RunSession>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<DiaryUiState> = combine(
        _sessions,
        _isLoading,
        syncStatusStore.syncMap,
    ) { sessions, isLoading, syncMap ->
        when {
            isLoading -> DiaryUiState.Loading
            sessions.isEmpty() -> DiaryUiState.Empty
            else -> DiaryUiState.Success(
                sessions.map { session ->
                    DiaryRunItem(
                        session = session,
                        syncRecord = syncMap[session.id],
                    )
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        loadSessions()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadSessions()
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val sessions = healthConnectSource.loadRunSessions()
                _sessions.value = sessions
                _isLoading.value = false

                // Trigger background sync
                if (sessions.isNotEmpty()) {
                    SyncWorker.enqueueOneTime(appContext)
                }
            } catch (e: Exception) {
                _sessions.value = emptyList()
                _isLoading.value = false
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
