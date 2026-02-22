package nl.teunk.currere.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.RunDetail
import java.time.Instant

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val detail: RunDetail) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class DetailViewModel(
    private val healthConnectSource: HealthConnectSource,
    private val sessionId: String,
    private val startTime: Instant,
    private val endTime: Instant,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            try {
                val detail = healthConnectSource.loadRunDetail(sessionId, startTime, endTime)
                _uiState.value = DetailUiState.Success(detail)
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Failed to load run detail")
            }
        }
    }
}
