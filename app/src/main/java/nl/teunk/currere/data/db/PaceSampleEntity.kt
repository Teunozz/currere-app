package nl.teunk.currere.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import nl.teunk.currere.domain.model.PaceSample
import java.time.Instant

@Entity(
    tableName = "pace_samples",
    foreignKeys = [ForeignKey(
        entity = RunSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class PaceSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timeEpochMillis: Long,
    val secondsPerKm: Double,
) {
    fun toDomain(): PaceSample = PaceSample(
        time = Instant.ofEpochMilli(timeEpochMillis),
        secondsPerKm = secondsPerKm,
    )

    companion object {
        fun fromDomain(sessionId: String, sample: PaceSample): PaceSampleEntity =
            PaceSampleEntity(
                sessionId = sessionId,
                timeEpochMillis = sample.time.toEpochMilli(),
                secondsPerKm = sample.secondsPerKm,
            )
    }
}
