package nl.teunk.currere.seed

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface SeedState {
    data object Idle : SeedState
    data class Seeding(val progress: Int, val total: Int) : SeedState
    data class Done(val count: Int) : SeedState
    data class Error(val message: String) : SeedState
}

class SeedViewModel : ViewModel() {

    private val _state = MutableStateFlow<SeedState>(SeedState.Idle)
    val state: StateFlow<SeedState> = _state

    fun seed(context: Context, count: Int) {
        viewModelScope.launch {
            _state.value = SeedState.Seeding(0, count)
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val now = Instant.now()
                for (i in 0 until count) {
                    val records = RunGenerator.generate(now, i)
                    client.insertRecords(records)
                    _state.value = SeedState.Seeding(i + 1, count)
                }
                _state.value = SeedState.Done(count)
            } catch (e: Exception) {
                _state.value = SeedState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _state.value = SeedState.Idle
    }
}
