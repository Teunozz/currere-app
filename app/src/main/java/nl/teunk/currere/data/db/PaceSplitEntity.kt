package nl.teunk.currere.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import nl.teunk.currere.domain.model.PaceSplit
import java.time.Duration

@Entity(
    tableName = "pace_splits",
    foreignKeys = [ForeignKey(
        entity = RunSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class PaceSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val kilometerNumber: Int,
    val distanceMeters: Double,
    val splitDurationMs: Long,
    val splitPaceSecondsPerKm: Double,
    val cumulativeDurationMs: Long,
    val isPartial: Boolean,
) {
    fun toDomain(): PaceSplit = PaceSplit(
        kilometerNumber = kilometerNumber,
        distanceMeters = distanceMeters,
        splitDuration = Duration.ofMillis(splitDurationMs),
        splitPaceSecondsPerKm = splitPaceSecondsPerKm,
        cumulativeDuration = Duration.ofMillis(cumulativeDurationMs),
        isPartial = isPartial,
    )

    companion object {
        fun fromDomain(sessionId: String, split: PaceSplit): PaceSplitEntity =
            PaceSplitEntity(
                sessionId = sessionId,
                kilometerNumber = split.kilometerNumber,
                distanceMeters = split.distanceMeters,
                splitDurationMs = split.splitDuration.toMillis(),
                splitPaceSecondsPerKm = split.splitPaceSecondsPerKm,
                cumulativeDurationMs = split.cumulativeDuration.toMillis(),
                isPartial = split.isPartial,
            )
    }
}
