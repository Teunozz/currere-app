package nl.teunk.currere.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RunDetailDao {

    @Query("SELECT * FROM heart_rate_samples WHERE sessionId = :sessionId ORDER BY timeEpochMillis")
    suspend fun getHeartRateSamples(sessionId: String): List<HeartRateSampleEntity>

    @Query("SELECT * FROM pace_samples WHERE sessionId = :sessionId ORDER BY timeEpochMillis")
    suspend fun getPaceSamples(sessionId: String): List<PaceSampleEntity>

    @Query("SELECT * FROM pace_splits WHERE sessionId = :sessionId ORDER BY kilometerNumber")
    suspend fun getPaceSplits(sessionId: String): List<PaceSplitEntity>

    @Query("SELECT totalSteps FROM run_sessions WHERE id = :sessionId")
    suspend fun getCachedTotalSteps(sessionId: String): Long?

    @Query("SELECT totalSteps IS NOT NULL FROM run_sessions WHERE id = :sessionId")
    suspend fun isDetailCached(sessionId: String): Boolean

    @Insert
    suspend fun insertHeartRateSamples(samples: List<HeartRateSampleEntity>)

    @Insert
    suspend fun insertPaceSamples(samples: List<PaceSampleEntity>)

    @Insert
    suspend fun insertPaceSplits(splits: List<PaceSplitEntity>)

    @Query("UPDATE run_sessions SET totalSteps = :totalSteps WHERE id = :sessionId")
    suspend fun updateTotalSteps(sessionId: String, totalSteps: Long)

    @Query("DELETE FROM heart_rate_samples WHERE sessionId = :sessionId")
    suspend fun deleteHeartRateSamples(sessionId: String)

    @Query("DELETE FROM pace_samples WHERE sessionId = :sessionId")
    suspend fun deletePaceSamples(sessionId: String)

    @Query("DELETE FROM pace_splits WHERE sessionId = :sessionId")
    suspend fun deletePaceSplits(sessionId: String)

    @Query("DELETE FROM heart_rate_samples WHERE sessionId IN (:sessionIds)")
    suspend fun deleteHeartRateSamplesBySessionIds(sessionIds: List<String>)

    @Query("DELETE FROM pace_samples WHERE sessionId IN (:sessionIds)")
    suspend fun deletePaceSamplesBySessionIds(sessionIds: List<String>)

    @Query("DELETE FROM pace_splits WHERE sessionId IN (:sessionIds)")
    suspend fun deletePaceSplitsBySessionIds(sessionIds: List<String>)

    @Transaction
    suspend fun cacheRunDetail(
        sessionId: String,
        totalSteps: Long,
        heartRateSamples: List<HeartRateSampleEntity>,
        paceSamples: List<PaceSampleEntity>,
        paceSplits: List<PaceSplitEntity>,
    ) {
        deleteHeartRateSamples(sessionId)
        deletePaceSamples(sessionId)
        deletePaceSplits(sessionId)
        updateTotalSteps(sessionId, totalSteps)
        insertHeartRateSamples(heartRateSamples)
        insertPaceSamples(paceSamples)
        insertPaceSplits(paceSplits)
    }

    @Transaction
    suspend fun cacheRunDetailsBatch(
        stepsUpdates: List<Pair<String, Long>>,
        heartRateSamples: List<HeartRateSampleEntity>,
        paceSamples: List<PaceSampleEntity>,
        paceSplits: List<PaceSplitEntity>,
    ) {
        val sessionIds = stepsUpdates.map { it.first }
        deleteHeartRateSamplesBySessionIds(sessionIds)
        deletePaceSamplesBySessionIds(sessionIds)
        deletePaceSplitsBySessionIds(sessionIds)
        for ((sessionId, totalSteps) in stepsUpdates) {
            updateTotalSteps(sessionId, totalSteps)
        }
        insertHeartRateSamples(heartRateSamples)
        insertPaceSamples(paceSamples)
        insertPaceSplits(paceSplits)
    }
}
