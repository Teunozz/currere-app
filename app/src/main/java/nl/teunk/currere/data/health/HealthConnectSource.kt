package nl.teunk.currere.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import nl.teunk.currere.domain.compute.DistanceSegment
import nl.teunk.currere.domain.compute.PaceCalculator
import nl.teunk.currere.domain.compute.SplitCalculator
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.domain.model.RunSession
import java.time.Instant

class HealthConnectSource(
    private val client: HealthConnectClient,
) {

    /**
     * Read all running sessions, paginated. Returns sessions sorted by startTime descending.
     */
    suspend fun readRunningSessions(): List<ExerciseSessionRecord> = withContext(Dispatchers.IO) {
        val allRecords = mutableListOf<ExerciseSessionRecord>()
        var pageToken: String? = null

        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                    pageToken = pageToken,
                )
            )
            allRecords.addAll(response.records)
            pageToken = response.pageToken
        } while (pageToken != null)

        allRecords
            .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING }
            .sortedByDescending { it.startTime }
    }

    /**
     * Read running sessions after the given instant, paginated.
     */
    suspend fun readRunningSessionsAfter(after: Instant): List<ExerciseSessionRecord> =
        withContext(Dispatchers.IO) {
            val allRecords = mutableListOf<ExerciseSessionRecord>()
            var pageToken: String? = null

            do {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.after(after),
                        pageToken = pageToken,
                    )
                )
                allRecords.addAll(response.records)
                pageToken = response.pageToken
            } while (pageToken != null)

            allRecords
                .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING }
                .sortedByDescending { it.startTime }
        }

    /**
     * Load sessions after the given instant with their summary stats.
     */
    suspend fun loadRunSessionsAfter(after: Instant): List<RunSession> =
        withContext(Dispatchers.IO) {
            val sessions = readRunningSessionsAfter(after)
            sessions.map { session ->
                val agg = aggregateSessionStats(session.startTime, session.endTime)
                Mappers.toRunSession(session, agg)
            }
        }

    /**
     * Aggregate summary stats for a single session.
     */
    suspend fun aggregateSessionStats(
        startTime: Instant,
        endTime: Instant,
    ) = withContext(Dispatchers.IO) {
        client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    DistanceRecord.DISTANCE_TOTAL,
                    StepsRecord.COUNT_TOTAL,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                    HeartRateRecord.BPM_AVG,
                ),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
            )
        )
    }

    /**
     * Load all sessions with their summary stats as RunSession domain models.
     */
    suspend fun loadRunSessions(): List<RunSession> = withContext(Dispatchers.IO) {
        val sessions = readRunningSessions()
        sessions.map { session ->
            val agg = aggregateSessionStats(session.startTime, session.endTime)
            Mappers.toRunSession(session, agg)
        }
    }

    /**
     * Load full detail for a single run session.
     */
    suspend fun loadRunDetail(
        sessionId: String,
        startTime: Instant,
        endTime: Instant,
    ): RunDetail = coroutineScope {
        val timeRange = TimeRangeFilter.between(startTime, endTime)

        val hrDeferred = async(Dispatchers.IO) {
            client.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = timeRange)
            ).records
        }
        val speedDeferred = async(Dispatchers.IO) {
            client.readRecords(
                ReadRecordsRequest(SpeedRecord::class, timeRangeFilter = timeRange)
            ).records
        }
        val distanceDeferred = async(Dispatchers.IO) {
            client.readRecords(
                ReadRecordsRequest(DistanceRecord::class, timeRangeFilter = timeRange)
            ).records
        }
        val aggDeferred = async(Dispatchers.IO) {
            aggregateSessionStats(startTime, endTime)
        }
        val sessionsDeferred = async(Dispatchers.IO) {
            client.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    timeRangeFilter = timeRange,
                )
            ).records
        }

        val hrRecords = hrDeferred.await()
        val speedRecords = speedDeferred.await()
        val distanceRecords = distanceDeferred.await()
        val agg = aggDeferred.await()
        val sessionRecords = sessionsDeferred.await()

        val exerciseSession = sessionRecords.firstOrNull { it.metadata.id == sessionId }
            ?: sessionRecords.first()

        val session = Mappers.toRunSession(exerciseSession, agg)
        val heartRateSamples = Mappers.toHeartRateSamples(hrRecords)
        val speedPairs = Mappers.toSpeedPairs(speedRecords)
        val paceSamples = PaceCalculator.toPaceSamples(speedPairs)
        val distanceSegments = Mappers.toDistanceSegments(distanceRecords)
        val splits = SplitCalculator.computeSplits(distanceSegments)

        RunDetail(
            session = session,
            totalSteps = Mappers.totalSteps(agg),
            heartRateSamples = heartRateSamples,
            paceSamples = paceSamples,
            splits = splits,
        )
    }
}
