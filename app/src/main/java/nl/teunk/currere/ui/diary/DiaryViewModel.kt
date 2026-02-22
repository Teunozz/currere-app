package nl.teunk.currere.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.RunSession

sealed interface DiaryUiState {
    data object Loading : DiaryUiState
    data object Empty : DiaryUiState
    data class Success(val sessions: List<RunSession>) : DiaryUiState
}

class DiaryViewModel(
    private val healthConnectSource: HealthConnectSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiaryUiState>(DiaryUiState.Loading)
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadSessions()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            try {
                val sessions = healthConnectSource.loadRunSessions()
                _uiState.value = if (sessions.isEmpty()) {
                    DiaryUiState.Empty
                } else {
                    DiaryUiState.Success(sessions)
                }
            } catch (e: Exception) {
                _uiState.value = DiaryUiState.Empty
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
