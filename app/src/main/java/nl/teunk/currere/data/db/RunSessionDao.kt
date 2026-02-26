package nl.teunk.currere.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RunSessionDao {

    @Query("SELECT * FROM run_sessions ORDER BY startTimeEpochMillis DESC")
    fun getAllSessions(): Flow<List<RunSessionEntity>>

    @Query("SELECT MAX(endTimeEpochMillis) FROM run_sessions")
    suspend fun getLatestEndTimeMillis(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<RunSessionEntity>)

    @Query("DELETE FROM run_sessions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Transaction
    suspend fun replaceAll(sessions: List<RunSessionEntity>) {
        deleteAll()
        insertAll(sessions)
    }

    @Query("DELETE FROM run_sessions")
    suspend fun deleteAll()
}
