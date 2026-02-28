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
    val lastSyncTime: String? = null,
)

class SettingsViewModel(
    private val credentialsManager: CredentialsManager,
    private val syncStatusStore: SyncStatusStore,
    private val appContext: Context,
) : ViewModel() {

    private fun formatRelativeTime(epochMillis: Long?): String {
        if (epochMillis == null) return "Never"
        val diff = System.currentTimeMillis() - epochMillis
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            else -> "${diff / 86_400_000} days ago"
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        credentialsManager.credentials,
        syncStatusStore.lastSyncTime,
    ) { credentials, lastSync ->
        SettingsUiState(
            isConnected = credentials != null,
            serverUrl = credentials?.baseUrl,
            lastSyncTime = formatRelativeTime(lastSync),
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
