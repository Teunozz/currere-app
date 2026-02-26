package nl.teunk.currere.data.sync

import kotlinx.serialization.Serializable

enum class SyncState {
    PENDING,
    SYNCED,
    FAILED,
}

@Serializable
data class SyncRecord(
    val serverId: Long? = null,
    val state: SyncState,
    val lastAttempt: Long = 0L,
    val failureMessage: String? = null,
)
