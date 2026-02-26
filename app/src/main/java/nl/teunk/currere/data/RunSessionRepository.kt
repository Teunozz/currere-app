package nl.teunk.currere.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.teunk.currere.data.db.RunSessionDao
import nl.teunk.currere.data.db.RunSessionEntity
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.RunSession
import java.time.Instant

class RunSessionRepository(
    private val dao: RunSessionDao,
    private val healthConnectSource: HealthConnectSource,
) {

    val sessions: Flow<List<RunSession>> = dao.getAllSessions()
        .map { entities -> entities.map { it.toDomain() } }

    /**
     * Incremental refresh: fetch only sessions newer than the latest cached end time.
     * If the cache is empty, falls back to a full refresh.
     */
    suspend fun refreshIncremental() {
        val latestMillis = dao.getLatestEndTimeMillis()
        if (latestMillis == null) {
            refreshFull()
            return
        }
        val after = Instant.ofEpochMilli(latestMillis)
        val newSessions = healthConnectSource.loadRunSessionsAfter(after)
        if (newSessions.isNotEmpty()) {
            dao.insertAll(newSessions.map { RunSessionEntity.fromDomain(it) })
        }
    }

    /**
     * Full refresh: re-read all sessions from Health Connect and replace the cache.
     */
    suspend fun refreshFull() {
        val allSessions = healthConnectSource.loadRunSessions()
        dao.replaceAll(allSessions.map { RunSessionEntity.fromDomain(it) })
    }
}
