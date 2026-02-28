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
import nl.teunk.currere.data.RunSessionRepository
import nl.teunk.currere.data.sync.SyncRecord
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
    private val runSessionRepository: RunSessionRepository,
    private val syncStatusStore: SyncStatusStore,
    private val appContext: Context,
) : ViewModel() {

    private val _isInitialLoad = MutableStateFlow(true)

    val uiState: StateFlow<DiaryUiState> = combine(
        runSessionRepository.sessions,
        _isInitialLoad,
        syncStatusStore.syncMap,
    ) { sessions, isInitialLoad, syncMap ->
        when {
            isInitialLoad && sessions.isEmpty() -> DiaryUiState.Loading
            sessions.isEmpty() -> DiaryUiState.Empty
            else -> {
                _isInitialLoad.value = false
                DiaryUiState.Success(
                    sessions.map { session ->
                        DiaryRunItem(
                            session = session,
                            syncRecord = syncMap[session.id],
                        )
                    }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryUiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                runSessionRepository.refreshIncremental()
                _isInitialLoad.value = false
                SyncWorker.enqueueOneTime(appContext)
            } catch (e: Exception) {
                _isInitialLoad.value = false
            }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                runSessionRepository.refreshFull()
                SyncWorker.enqueueOneTime(appContext)
            } catch (e: Exception) {
                // Cached data remains visible
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
