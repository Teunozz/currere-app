package nl.teunk.currere.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import nl.teunk.currere.domain.model.HeartRateSample
import java.time.Instant

@Entity(
    tableName = "heart_rate_samples",
    foreignKeys = [ForeignKey(
        entity = RunSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timeEpochMillis: Long,
    val bpm: Long,
) {
    fun toDomain(): HeartRateSample = HeartRateSample(
        time = Instant.ofEpochMilli(timeEpochMillis),
        bpm = bpm,
    )

    companion object {
        fun fromDomain(sessionId: String, sample: HeartRateSample): HeartRateSampleEntity =
            HeartRateSampleEntity(
                sessionId = sessionId,
                timeEpochMillis = sample.time.toEpochMilli(),
                bpm = sample.bpm,
            )
    }
}
