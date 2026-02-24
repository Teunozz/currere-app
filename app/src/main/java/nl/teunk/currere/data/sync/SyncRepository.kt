package nl.teunk.currere.data.sync

import kotlinx.coroutines.flow.first
import nl.teunk.currere.data.api.ApiClient
import nl.teunk.currere.data.api.BatchRunRequest
import nl.teunk.currere.data.api.HeartRateSampleRequest
import nl.teunk.currere.data.api.PaceSplitRequest
import nl.teunk.currere.data.api.RunRequest
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.domain.model.RunSession
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

sealed interface SyncResult {
    data class Success(val synced: Int, val total: Int) : SyncResult
    data object NotConnected : SyncResult
    data object Unauthorized : SyncResult
    data class Error(val message: String) : SyncResult
}

class SyncRepository(
    private val apiClient: ApiClient,
    private val syncStatusStore: SyncStatusStore,
    private val credentialsManager: CredentialsManager,
    private val healthConnectSource: HealthConnectSource,
) {

    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    suspend fun syncSessions(sessions: List<RunSession>): SyncResult {
        if (credentialsManager.credentials.first() == null) {
            return SyncResult.NotConnected
        }

        val service = apiClient.createService() ?: return SyncResult.NotConnected

        val syncMap = syncStatusStore.syncMap.first()
        val unsynced = sessions.filter { session ->
            val record = syncMap[session.id]
            record == null || record.state != SyncState.SYNCED
        }

        if (unsynced.isEmpty()) {
            return SyncResult.Success(synced = 0, total = sessions.size)
        }

        syncStatusStore.markPending(unsynced.map { it.id })

        // Load full detail for each unsynced session
        val runRequests = unsynced.map { session ->
            try {
                val detail = healthConnectSource.loadRunDetail(
                    sessionId = session.id,
                    startTime = session.startTime,
                    endTime = session.endTime,
                )
                detail.toRunRequest()
            } catch (_: Exception) {
                session.toRunRequest()
            }
        }
        val batchRequest = BatchRunRequest(runs = runRequests)

        return try {
            val response = service.createRunsBatch(batchRequest)

            when {
                response.isSuccessful -> {
                    val batchData = response.body()?.data

                    batchData?.results?.forEach { result ->
                        val sessionId = unsynced.getOrNull(result.index)?.id ?: return@forEach
                        val serverId = result.id ?: return@forEach

                        when (result.status) {
                            "created", "skipped" -> syncStatusStore.markSynced(sessionId, serverId)
                            else -> syncStatusStore.markFailed(sessionId, "Status: ${result.status}")
                        }
                    }

                    val syncedCount = (batchData?.created ?: 0) + (batchData?.skipped ?: 0)
                    SyncResult.Success(synced = syncedCount, total = sessions.size)
                }
                response.code() == 401 -> SyncResult.Unauthorized
                response.code() == 422 -> SyncResult.Error("Validation error from server")
                else -> SyncResult.Error("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            unsynced.forEach { session ->
                syncStatusStore.markFailed(session.id, e.message ?: "Unknown error")
            }
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun RunDetail.toRunRequest(): RunRequest {
        return RunRequest(
            startTime = isoFormatter.format(session.startTime.atOffset(ZoneOffset.UTC)),
            endTime = isoFormatter.format(session.endTime.atOffset(ZoneOffset.UTC)),
            distanceKm = session.distanceMeters / 1000.0,
            durationSeconds = session.activeDuration.seconds,
            avgPaceSecondsPerKm = session.averagePaceSecondsPerKm?.toLong(),
            avgHeartRate = session.averageHeartRateBpm,
            steps = totalSteps,
            heartRateSamples = heartRateSamples.map { sample ->
                HeartRateSampleRequest(
                    timestamp = isoFormatter.format(sample.time.atOffset(ZoneOffset.UTC)),
                    bpm = sample.bpm,
                )
            }.ifEmpty { null },
            paceSplits = splits.map { split ->
                PaceSplitRequest(
                    kilometerNumber = split.kilometerNumber,
                    splitTimeSeconds = split.splitDuration.seconds,
                    paceSecondsPerKm = split.splitPaceSecondsPerKm.toLong(),
                    isPartial = split.isPartial,
                    partialDistanceKm = if (split.isPartial) split.distanceMeters / 1000.0 else null,
                )
            }.ifEmpty { null },
        )
    }

    private fun RunSession.toRunRequest(): RunRequest {
        return RunRequest(
            startTime = isoFormatter.format(startTime.atOffset(ZoneOffset.UTC)),
            endTime = isoFormatter.format(endTime.atOffset(ZoneOffset.UTC)),
            distanceKm = distanceMeters / 1000.0,
            durationSeconds = activeDuration.seconds,
            avgPaceSecondsPerKm = averagePaceSecondsPerKm?.toLong(),
            avgHeartRate = averageHeartRateBpm,
        )
    }
}
