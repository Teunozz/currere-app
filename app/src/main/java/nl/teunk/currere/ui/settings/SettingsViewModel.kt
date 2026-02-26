package nl.teunk.currere.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.sync.SyncStatusStore
import nl.teunk.currere.data.sync.SyncWorker

data class SettingsUiState(
    val isConnected: Boolean = false,
    val serverUrl: String? = null,
    val lastSyncTime: Long? = null,
)

class SettingsViewModel(
    private val credentialsManager: CredentialsManager,
    private val syncStatusStore: SyncStatusStore,
    private val appContext: Context,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        credentialsManager.credentials,
        syncStatusStore.lastSyncTime,
    ) { credentials, lastSync ->
        SettingsUiState(
            isConnected = credentials != null,
            serverUrl = credentials?.baseUrl,
            lastSyncTime = lastSync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun disconnect() {
        viewModelScope.launch {
            SyncWorker.cancelAll(appContext)
            syncStatusStore.clearAll()
            credentialsManager.clear()
        }
    }
}
