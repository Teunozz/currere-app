package nl.teunk.currere.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.teunk.currere.domain.model.RunSession
import java.time.Duration
import java.time.Instant

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey val id: String,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long,
    val distanceMeters: Double,
    val activeDurationSeconds: Long,
    val averagePaceSecondsPerKm: Double?,
    val averageHeartRateBpm: Long?,
    val title: String,
) {
    fun toDomain(): RunSession = RunSession(
        id = id,
        startTime = Instant.ofEpochMilli(startTimeEpochMillis),
        endTime = Instant.ofEpochMilli(endTimeEpochMillis),
        distanceMeters = distanceMeters,
        activeDuration = Duration.ofSeconds(activeDurationSeconds),
        averagePaceSecondsPerKm = averagePaceSecondsPerKm,
        averageHeartRateBpm = averageHeartRateBpm,
        title = title,
    )

    companion object {
        fun fromDomain(session: RunSession): RunSessionEntity = RunSessionEntity(
            id = session.id,
            startTimeEpochMillis = session.startTime.toEpochMilli(),
            endTimeEpochMillis = session.endTime.toEpochMilli(),
            distanceMeters = session.distanceMeters,
            activeDurationSeconds = session.activeDuration.seconds,
            averagePaceSecondsPerKm = session.averagePaceSecondsPerKm,
            averageHeartRateBpm = session.averageHeartRateBpm,
            title = session.title,
        )
    }
}
