package nl.teunk.currere.ui.preview

import nl.teunk.currere.data.sync.SyncRecord
import nl.teunk.currere.data.sync.SyncState
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.domain.model.PaceSample
import nl.teunk.currere.domain.model.PaceSplit
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.ui.diary.DiaryRunItem
import java.time.Duration
import java.time.Instant
import kotlin.math.sin

private val sampleStart: Instant = Instant.parse("2026-02-20T07:30:00Z")
private val sampleEnd: Instant = sampleStart.plus(Duration.ofMinutes(42))

val SampleRunSession = RunSession(
    id = "preview-1",
    startTime = sampleStart,
    endTime = sampleEnd,
    distanceMeters = 8_240.0,
    activeDuration = Duration.ofMinutes(42).plusSeconds(15),
    averagePaceSecondsPerKm = 308.0,
    averageHeartRateBpm = 156,
    title = "Morning Run",
)

val SampleRunSessions = listOf(
    DiaryRunItem(
        session = SampleRunSession,
        syncRecord = SyncRecord(serverId = 1, state = SyncState.SYNCED),
    ),
    DiaryRunItem(
        session = RunSession(
            id = "preview-2",
            startTime = sampleStart.minus(Duration.ofDays(2)),
            endTime = sampleStart.minus(Duration.ofDays(2)).plus(Duration.ofMinutes(28)),
            distanceMeters = 5_120.0,
            activeDuration = Duration.ofMinutes(28).plusSeconds(33),
            averagePaceSecondsPerKm = 335.0,
            averageHeartRateBpm = 148,
            title = "Easy Run",
        ),
        syncRecord = SyncRecord(state = SyncState.PENDING),
    ),
    DiaryRunItem(
        session = RunSession(
            id = "preview-3",
            startTime = sampleStart.minus(Duration.ofDays(4)),
            endTime = sampleStart.minus(Duration.ofDays(4)).plus(Duration.ofMinutes(55)),
            distanceMeters = 10_030.0,
            activeDuration = Duration.ofMinutes(55).plusSeconds(12),
            averagePaceSecondsPerKm = 330.0,
            averageHeartRateBpm = 162,
            title = "Long Run",
        ),
    ),
)

val SampleSplits = listOf(
    PaceSplit(1, 1000.0, Duration.ofMinutes(5).plusSeconds(12), 312.0, Duration.ofMinutes(5).plusSeconds(12), false),
    PaceSplit(2, 1000.0, Duration.ofMinutes(5).plusSeconds(5), 305.0, Duration.ofMinutes(10).plusSeconds(17), false),
    PaceSplit(3, 1000.0, Duration.ofMinutes(5).plusSeconds(18), 318.0, Duration.ofMinutes(15).plusSeconds(35), false),
    PaceSplit(4, 1000.0, Duration.ofMinutes(4).plusSeconds(58), 298.0, Duration.ofMinutes(20).plusSeconds(33), false),
    PaceSplit(5, 1000.0, Duration.ofMinutes(5).plusSeconds(8), 308.0, Duration.ofMinutes(25).plusSeconds(41), false),
    PaceSplit(6, 240.0, Duration.ofMinutes(1).plusSeconds(15), 312.0, Duration.ofMinutes(26).plusSeconds(56), true),
)

val SampleHeartRateSamples = (0..40).map { i ->
    HeartRateSample(
        time = sampleStart.plusSeconds(i * 60L),
        bpm = (140 + (sin(i * 0.3) * 20).toLong()),
    )
}

val SamplePaceSamples = (0..40).map { i ->
    PaceSample(
        time = sampleStart.plusSeconds(i * 60L),
        secondsPerKm = 300.0 + sin(i * 0.25) * 30,
    )
}

val SampleRunDetail = RunDetail(
    session = SampleRunSession,
    totalSteps = 7_832,
    heartRateSamples = SampleHeartRateSamples,
    paceSamples = SamplePaceSamples,
    splits = SampleSplits,
)