package nl.teunk.currere.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.teunk.currere.R
import nl.teunk.currere.data.api.ApiClient
import nl.teunk.currere.data.api.AuthenticationException
import nl.teunk.currere.data.api.ConnectionException
import nl.teunk.currere.data.api.ServerErrorException
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.sync.SyncWorker

sealed interface SetupState {
    data object Idle : SetupState
    data object Testing : SetupState
    data object Success : SetupState
    data class Error(val message: String) : SetupState
}

class SetupViewModel(
    private val apiClient: ApiClient,
    private val credentialsManager: CredentialsManager,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<SetupState>(SetupState.Idle)
    val state: StateFlow<SetupState> = _state.asStateFlow()

    fun connectWithCredentials(baseUrl: String, token: String) {
        if (baseUrl.isBlank() || token.isBlank()) {
            _state.value = SetupState.Error(appContext.getString(R.string.error_url_token_required))
            return
        }

        _state.value = SetupState.Testing
        viewModelScope.launch {
            val result = apiClient.testConnection(baseUrl, token)
            result.fold(
                onSuccess = {
                    credentialsManager.save(baseUrl, token)
                    SyncWorker.schedulePeriodicSync(appContext)
                    SyncWorker.enqueueOneTime(appContext)
                    _state.value = SetupState.Success
                },
                onFailure = { error ->
                    val message = when (error) {
                        is AuthenticationException -> appContext.getString(R.string.error_auth_failed)
                        is ServerErrorException -> appContext.getString(R.string.error_server_returned, error.code)
                        is ConnectionException -> appContext.getString(R.string.error_connection_failed)
                        else -> appContext.getString(R.string.error_connection_failed)
                    }
                    _state.value = SetupState.Error(message)
                },
            )
        }
    }

    fun resetState() {
        _state.value = SetupState.Idle
    }
}
