package nl.teunk.currere

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.teunk.currere.data.api.ApiClient
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.data.sync.SyncRepository
import nl.teunk.currere.data.sync.SyncStatusStore
import nl.teunk.currere.data.sync.SyncWorker

class CurrereApp : Application() {

    val healthConnectSource: HealthConnectSource by lazy {
        HealthConnectSource(HealthConnectClient.getOrCreate(this))
    }

    val credentialsManager: CredentialsManager by lazy {
        CredentialsManager(this)
    }

    val apiClient: ApiClient by lazy {
        ApiClient(credentialsManager)
    }

    val syncStatusStore: SyncStatusStore by lazy {
        SyncStatusStore(this)
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(apiClient, syncStatusStore, credentialsManager, healthConnectSource)
    }

    override fun onCreate() {
        super.onCreate()
        val hasCredentials = runBlocking { credentialsManager.credentials.first() != null }
        if (hasCredentials) {
            SyncWorker.schedulePeriodicSync(this)
        }
    }
}
