package nl.teunk.currere.data.health

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import nl.teunk.currere.domain.compute.PaceCalculator
import nl.teunk.currere.domain.compute.StatsAggregator
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.domain.model.RunSession
import java.time.Duration

object Mappers {

    fun toRunSession(
        session: ExerciseSessionRecord,
        aggregation: AggregationResult,
    ): RunSession {
        val distanceMeters = aggregation[DistanceRecord.DISTANCE_TOTAL]
            ?.inMeters?.coerceAtLeast(0.0) ?: 0.0
        val activeDuration = aggregation[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
            ?: Duration.between(session.startTime, session.endTime)
        val avgHr = aggregation[HeartRateRecord.BPM_AVG]

        val avgPace = PaceCalculator.averagePace(activeDuration, distanceMeters)

        return RunSession(
            id = session.metadata.id,
            startTime = session.startTime,
            endTime = session.endTime,
            distanceMeters = distanceMeters,
            activeDuration = activeDuration,
            averagePaceSecondsPerKm = avgPace,
            averageHeartRateBpm = avgHr,
            title = StatsAggregator.activityTitle(session.startTime),
        )
    }

    fun toHeartRateSamples(records: List<HeartRateRecord>): List<HeartRateSample> {
        return records.flatMap { record ->
            record.samples.map { sample ->
                HeartRateSample(
                    time = sample.time,
                    bpm = sample.beatsPerMinute,
                )
            }
        }.sortedBy { it.time }
    }

    fun toSpeedPairs(records: List<SpeedRecord>): List<Pair<java.time.Instant, Double>> {
        return records.flatMap { record ->
            record.samples.map { sample ->
                sample.time to sample.speed.inMetersPerSecond
            }
        }.sortedBy { it.first }
    }
}
