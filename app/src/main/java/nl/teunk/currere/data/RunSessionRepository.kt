package nl.teunk.currere.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.teunk.currere.data.db.HeartRateSampleEntity
import nl.teunk.currere.data.db.PaceSampleEntity
import nl.teunk.currere.data.db.PaceSplitEntity
import nl.teunk.currere.data.db.RunDetailDao
import nl.teunk.currere.data.db.RunSessionDao
import nl.teunk.currere.data.db.RunSessionEntity
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.domain.model.RunSession
import java.time.Instant

class RunSessionRepository(
    private val dao: RunSessionDao,
    private val runDetailDao: RunDetailDao,
    private val healthConnectSource: HealthConnectSource,
) {

    val sessions: Flow<List<RunSession>> = dao.getAllSessions()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun getAllSessions(): List<RunSession> {
        return dao.getAllSessionsSnapshot().map { it.toDomain() }
    }

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
            cacheDetailsForSessions(newSessions)
        }
    }

    /**
     * Full refresh: re-read all sessions from Health Connect and replace the cache.
     */
    suspend fun refreshFull() {
        val allSessions = healthConnectSource.loadRunSessions()
        dao.replaceAll(allSessions.map { RunSessionEntity.fromDomain(it) })
        cacheDetailsForSessions(allSessions)
    }

    /**
     * Get run detail, reading from Room cache first. Falls back to Health Connect if not cached.
     */
    suspend fun getRunDetail(sessionId: String, startTime: Instant, endTime: Instant): RunDetail {
        if (runDetailDao.isDetailCached(sessionId)) {
            val entity = dao.getById(sessionId)
                ?: throw IllegalStateException("Session $sessionId not found in cache")
            val totalSteps = runDetailDao.getCachedTotalSteps(sessionId) ?: 0L
            val heartRateSamples = runDetailDao.getHeartRateSamples(sessionId).map { it.toDomain() }
            val paceSamples = runDetailDao.getPaceSamples(sessionId).map { it.toDomain() }
            val splits = runDetailDao.getPaceSplits(sessionId).map { it.toDomain() }
            return RunDetail(
                session = entity.toDomain(),
                totalSteps = totalSteps,
                heartRateSamples = heartRateSamples,
                paceSamples = paceSamples,
                splits = splits,
            )
        }

        val detail = healthConnectSource.loadRunDetail(sessionId, startTime, endTime)
        cacheDetail(sessionId, detail)
        return detail
    }

    private suspend fun cacheDetailsForSessions(sessions: List<RunSession>) {
        val stepsUpdates = mutableListOf<Pair<String, Long>>()
        val allHeartRateSamples = mutableListOf<HeartRateSampleEntity>()
        val allPaceSamples = mutableListOf<PaceSampleEntity>()
        val allPaceSplits = mutableListOf<PaceSplitEntity>()

        for (session in sessions) {
            try {
                val detail = healthConnectSource.loadRunDetail(
                    sessionId = session.id,
                    startTime = session.startTime,
                    endTime = session.endTime,
                )
                stepsUpdates.add(session.id to detail.totalSteps)
                allHeartRateSamples.addAll(detail.heartRateSamples.map {
                    HeartRateSampleEntity.fromDomain(session.id, it)
                })
                allPaceSamples.addAll(detail.paceSamples.map {
                    PaceSampleEntity.fromDomain(session.id, it)
                })
                allPaceSplits.addAll(detail.splits.map {
                    PaceSplitEntity.fromDomain(session.id, it)
                })
            } catch (_: Exception) {
                // Continue on individual failure — detail will be fetched on demand
            }
        }

        if (stepsUpdates.isNotEmpty()) {
            runDetailDao.cacheRunDetailsBatch(
                stepsUpdates = stepsUpdates,
                heartRateSamples = allHeartRateSamples,
                paceSamples = allPaceSamples,
                paceSplits = allPaceSplits,
            )
        }
    }

    private suspend fun cacheDetail(sessionId: String, detail: RunDetail) {
        runDetailDao.cacheRunDetail(
            sessionId = sessionId,
            totalSteps = detail.totalSteps,
            heartRateSamples = detail.heartRateSamples.map {
                HeartRateSampleEntity.fromDomain(sessionId, it)
            },
            paceSamples = detail.paceSamples.map {
                PaceSampleEntity.fromDomain(sessionId, it)
            },
            paceSplits = detail.splits.map {
                PaceSplitEntity.fromDomain(sessionId, it)
            },
        )
    }
}
