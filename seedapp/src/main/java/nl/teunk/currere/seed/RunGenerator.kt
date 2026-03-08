package nl.teunk.currere.seed

import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Velocity
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.random.Random

object RunGenerator {

    private val titles = listOf(
        "Easy Run", "Morning Run", "Tempo Run", "Long Run",
        "Recovery Run", "Interval Session", "Evening Run", "Trail Run",
        "Fartlek", "Progression Run",
    )

    fun generate(now: Instant, dayOffset: Int): List<Record> {
        val random = Random(dayOffset)
        val metadata = Metadata.manualEntry()

        val hourOfDay = 6 + random.nextInt(14)
        val startTime = now
            .minus(Duration.ofDays(dayOffset.toLong()))
            .atZone(ZoneOffset.systemDefault())
            .withHour(hourOfDay)
            .withMinute(random.nextInt(60))
            .withSecond(0)
            .toInstant()

        val durationMinutes = 15 + random.nextInt(61)
        val duration = Duration.ofMinutes(durationMinutes.toLong())
        val endTime = startTime.plus(duration)
        val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)

        val paceSecondsPerKm = 240.0 + random.nextDouble() * 180.0
        val distanceMeters = durationMinutes * 60.0 / paceSecondsPerKm * 1000.0
        val steps = (distanceMeters * (1.2 + random.nextDouble() * 0.2)).toLong()
        val title = titles[random.nextInt(titles.size)]

        val records = mutableListOf<Record>()

        records += ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = zoneOffset,
            endTime = endTime,
            endZoneOffset = zoneOffset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = title,
            metadata = metadata,
        )

        records += DistanceRecord(
            startTime = startTime,
            startZoneOffset = zoneOffset,
            endTime = endTime,
            endZoneOffset = zoneOffset,
            distance = Length.meters(distanceMeters),
            metadata = metadata,
        )

        records += StepsRecord(
            startTime = startTime,
            startZoneOffset = zoneOffset,
            endTime = endTime,
            endZoneOffset = zoneOffset,
            count = steps,
            metadata = metadata,
        )

        val hrSamples = mutableListOf<HeartRateRecord.Sample>()
        val baseHr = 130 + random.nextInt(30)
        var t = startTime.plusSeconds(5)
        while (t.isBefore(endTime)) {
            hrSamples += HeartRateRecord.Sample(t, (baseHr + random.nextInt(21) - 10).toLong())
            t = t.plusSeconds(30)
        }
        if (hrSamples.isNotEmpty()) {
            records += HeartRateRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                samples = hrSamples,
                metadata = metadata,
            )
        }

        val speedSamples = mutableListOf<SpeedRecord.Sample>()
        val baseSpeedMps = 1000.0 / paceSecondsPerKm
        t = startTime.plusSeconds(5)
        while (t.isBefore(endTime)) {
            val speed = baseSpeedMps * (0.9 + random.nextDouble() * 0.2)
            speedSamples += SpeedRecord.Sample(t, Velocity.metersPerSecond(speed))
            t = t.plusSeconds(30)
        }
        if (speedSamples.isNotEmpty()) {
            records += SpeedRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                samples = speedSamples,
                metadata = metadata,
            )
        }

        return records
    }
}
