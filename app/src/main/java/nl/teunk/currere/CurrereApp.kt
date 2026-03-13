package nl.teunk.currere

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.teunk.currere.data.RunSessionRepository
import nl.teunk.currere.data.api.ApiClient
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.db.CurrereDatabase
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.data.security.PassphraseProvider
import nl.teunk.currere.data.sync.SyncRepository
import nl.teunk.currere.data.sync.SyncStatusStore
import nl.teunk.currere.data.sync.SyncWorker
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class CurrereApp : Application() {

    val healthConnectSource: HealthConnectSource by lazy {
        HealthConnectSource(HealthConnectClient.getOrCreate(this))
    }

    val database: CurrereDatabase by lazy {
        System.loadLibrary("sqlcipher")
        val passphrase = PassphraseProvider.getPassphrase(this)
        val factory = SupportOpenHelperFactory(passphrase)
        CurrereDatabase.getInstance(this, factory)
    }

    val runSessionRepository: RunSessionRepository by lazy {
        RunSessionRepository(database.runSessionDao(), database.runDetailDao(), healthConnectSource)
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
        SyncRepository(apiClient, syncStatusStore, credentialsManager, runSessionRepository)
    }

    override fun onCreate() {
        super.onCreate()
        val hasCredentials = runBlocking { credentialsManager.credentials.first() != null }
        if (hasCredentials) {
            SyncWorker.schedulePeriodicSync(this)
        }
    }
}
