package nl.teunk.currere.data.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_status")

class SyncStatusStore(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.syncDataStore)

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val SYNC_MAP_KEY = stringPreferencesKey("sync_map")
        private val LAST_SYNC_TIME_KEY = longPreferencesKey("last_sync_time")
    }

    val syncMap: Flow<Map<String, SyncRecord>> = store.data.map { prefs ->
        val raw = prefs[SYNC_MAP_KEY] ?: return@map emptyMap()
        try {
            json.decodeFromString<Map<String, SyncRecord>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    val lastSyncTime: Flow<Long?> = store.data.map { prefs ->
        prefs[LAST_SYNC_TIME_KEY]
    }

    suspend fun markSynced(sessionId: String, serverId: Long) {
        updateRecord(sessionId, SyncRecord(serverId = serverId, state = SyncState.SYNCED, lastAttempt = System.currentTimeMillis()))
        store.edit { prefs ->
            prefs[LAST_SYNC_TIME_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun markFailed(sessionId: String, message: String) {
        updateRecord(sessionId, SyncRecord(state = SyncState.FAILED, lastAttempt = System.currentTimeMillis(), failureMessage = message))
    }

    suspend fun markPending(sessionIds: List<String>) {
        store.edit { prefs ->
            val current = readMap(prefs).toMutableMap()
            for (id in sessionIds) {
                if (id !in current) {
                    current[id] = SyncRecord(state = SyncState.PENDING)
                }
            }
            prefs[SYNC_MAP_KEY] = json.encodeToString(current)
        }
    }

    suspend fun clearAll() {
        store.edit { prefs ->
            prefs.remove(SYNC_MAP_KEY)
            prefs.remove(LAST_SYNC_TIME_KEY)
        }
    }

    private suspend fun updateRecord(sessionId: String, record: SyncRecord) {
        store.edit { prefs ->
            val current = readMap(prefs).toMutableMap()
            current[sessionId] = record
            prefs[SYNC_MAP_KEY] = json.encodeToString(current)
        }
    }

    private fun readMap(prefs: Preferences): Map<String, SyncRecord> {
        val raw = prefs[SYNC_MAP_KEY] ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, SyncRecord>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
