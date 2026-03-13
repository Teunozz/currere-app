package nl.teunk.currere.data

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.teunk.currere.data.db.HeartRateSampleEntity
import nl.teunk.currere.data.db.PaceSampleEntity
import nl.teunk.currere.data.db.PaceSplitEntity
import nl.teunk.currere.data.db.RunDetailDao
import nl.teunk.currere.data.db.RunSessionDao
import nl.teunk.currere.data.db.RunSessionEntity
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.domain.model.PaceSample
import nl.teunk.currere.domain.model.PaceSplit
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.domain.model.RunSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RunSessionRepositoryTest {

    private val dao = mockk<RunSessionDao>(relaxed = true)
    private val runDetailDao = mockk<RunDetailDao>(relaxed = true)
    private val healthConnectSource = mockk<HealthConnectSource>()

    private val repository = RunSessionRepository(dao, runDetailDao, healthConnectSource)

    private fun makeEntity(id: String, startMillis: Long = 1704067200000L, endMillis: Long = 1704069000000L) =
        RunSessionEntity(
            id = id,
            startTimeEpochMillis = startMillis,
            endTimeEpochMillis = endMillis,
            distanceMeters = 5000.0,
            activeDurationSeconds = 1800L,
            averagePaceSecondsPerKm = 360.0,
            averageHeartRateBpm = 150L,
            title = "Morning run",
        )

    private fun makeDomain(id: String, startMillis: Long = 1704067200000L, endMillis: Long = 1704069000000L) =
        RunSession(
            id = id,
            startTime = Instant.ofEpochMilli(startMillis),
            endTime = Instant.ofEpochMilli(endMillis),
            distanceMeters = 5000.0,
            activeDuration = Duration.ofSeconds(1800L),
            averagePaceSecondsPerKm = 360.0,
            averageHeartRateBpm = 150L,
            title = "Morning run",
        )

    private fun makeDetail(session: RunSession) = RunDetail(
        session = session,
        totalSteps = 5000,
        heartRateSamples = listOf(
            HeartRateSample(time = session.startTime, bpm = 120),
        ),
        paceSamples = listOf(
            PaceSample(time = session.startTime, secondsPerKm = 360.0),
        ),
        splits = listOf(
            PaceSplit(
                kilometerNumber = 1,
                distanceMeters = 1000.0,
                splitDuration = Duration.ofSeconds(360),
                splitPaceSecondsPerKm = 360.0,
                cumulativeDuration = Duration.ofSeconds(360),
                isPartial = false,
            ),
        ),
    )

    // region sessions Flow

    @Test
    fun `sessions Flow maps entities to domain models`() = runTest {
        val entities = listOf(makeEntity("a"), makeEntity("b"))
        coEvery { dao.getAllSessions() } returns flowOf(entities)

        val repo = RunSessionRepository(dao, runDetailDao, healthConnectSource)
        repo.sessions.test {
            val sessions = awaitItem()
            assertEquals(2, sessions.size)
            assertEquals("a", sessions[0].id)
            assertEquals("b", sessions[1].id)
            assertEquals(Instant.ofEpochMilli(1704067200000L), sessions[0].startTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sessions Flow emits empty list when dao is empty`() = runTest {
        coEvery { dao.getAllSessions() } returns flowOf(emptyList())

        val repo = RunSessionRepository(dao, runDetailDao, healthConnectSource)
        repo.sessions.test {
            assertEquals(emptyList<RunSession>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region refreshIncremental

    @Test
    fun `refreshIncremental fetches only new sessions when cache exists`() = runTest {
        val latestEndMillis = 1704069000000L
        coEvery { dao.getLatestEndTimeMillis() } returns latestEndMillis

        val newSessions = listOf(makeDomain("new-1", startMillis = 1704070000000L, endMillis = 1704071000000L))
        coEvery { healthConnectSource.loadRunSessionsAfter(Instant.ofEpochMilli(latestEndMillis)) } returns newSessions
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(newSessions[0])

        repository.refreshIncremental()

        coVerify { dao.insertAll(match { it.size == 1 && it[0].id == "new-1" }) }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    @Test
    fun `refreshIncremental falls back to full refresh when cache empty`() = runTest {
        coEvery { dao.getLatestEndTimeMillis() } returns null

        val allSessions = listOf(makeDomain("s1"), makeDomain("s2"))
        coEvery { healthConnectSource.loadRunSessions() } returns allSessions
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(allSessions[0])

        repository.refreshIncremental()

        coVerify { dao.replaceAll(match { it.size == 2 }) }
        coVerify(exactly = 0) { healthConnectSource.loadRunSessionsAfter(any()) }
    }

    @Test
    fun `refreshIncremental skips insert when no new data`() = runTest {
        coEvery { dao.getLatestEndTimeMillis() } returns 1704069000000L
        coEvery { healthConnectSource.loadRunSessionsAfter(any()) } returns emptyList()

        repository.refreshIncremental()

        coVerify(exactly = 0) { dao.insertAll(any()) }
    }

    // endregion

    // region refreshFull

    @Test
    fun `refreshFull replaces all cached sessions`() = runTest {
        val sessions = listOf(makeDomain("s1"), makeDomain("s2"), makeDomain("s3"))
        coEvery { healthConnectSource.loadRunSessions() } returns sessions
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(sessions[0])

        repository.refreshFull()

        coVerify { dao.replaceAll(match { it.size == 3 }) }
    }

    @Test
    fun `refreshFull handles empty data from Health Connect`() = runTest {
        coEvery { healthConnectSource.loadRunSessions() } returns emptyList()

        repository.refreshFull()

        coVerify { dao.replaceAll(emptyList()) }
    }

    // endregion

    // region getRunDetail

    @Test
    fun `getRunDetail returns cached data when detail is cached`() = runTest {
        val sessionId = "s1"
        val startTime = Instant.ofEpochMilli(1704067200000L)
        val endTime = Instant.ofEpochMilli(1704069000000L)

        coEvery { runDetailDao.isDetailCached(sessionId) } returns true
        coEvery { dao.getById(sessionId) } returns makeEntity(sessionId)
        coEvery { runDetailDao.getCachedTotalSteps(sessionId) } returns 5000L
        coEvery { runDetailDao.getHeartRateSamples(sessionId) } returns listOf(
            HeartRateSampleEntity(id = 1, sessionId = sessionId, timeEpochMillis = 1704067200000L, bpm = 120),
        )
        coEvery { runDetailDao.getPaceSamples(sessionId) } returns emptyList()
        coEvery { runDetailDao.getPaceSplits(sessionId) } returns emptyList()

        val detail = repository.getRunDetail(sessionId, startTime, endTime)

        assertEquals(sessionId, detail.session.id)
        assertEquals(5000L, detail.totalSteps)
        assertEquals(1, detail.heartRateSamples.size)
        coVerify(exactly = 0) { healthConnectSource.loadRunDetail(any(), any(), any()) }
    }

    @Test
    fun `getRunDetail falls back to HC when not cached and caches result`() = runTest {
        val session = makeDomain("s1")
        val detail = makeDetail(session)

        coEvery { runDetailDao.isDetailCached("s1") } returns false
        coEvery { healthConnectSource.loadRunDetail("s1", session.startTime, session.endTime) } returns detail

        val result = repository.getRunDetail("s1", session.startTime, session.endTime)

        assertEquals(detail, result)
        coVerify { runDetailDao.cacheRunDetail(eq("s1"), eq(5000L), any(), any(), any()) }
    }

    @Test
    fun `getRunDetail propagates HC error when not cached`() = runTest {
        val startTime = Instant.ofEpochMilli(1704067200000L)
        val endTime = Instant.ofEpochMilli(1704069000000L)

        coEvery { runDetailDao.isDetailCached("s1") } returns false
        coEvery { healthConnectSource.loadRunDetail("s1", startTime, endTime) } throws RuntimeException("HC error")

        try {
            repository.getRunDetail("s1", startTime, endTime)
            throw AssertionError("Expected exception")
        } catch (e: RuntimeException) {
            assertEquals("HC error", e.message)
        }
    }

    // endregion

    // region detail caching during refresh

    @Test
    fun `refreshFull batch-caches details for all sessions`() = runTest {
        val sessions = listOf(makeDomain("s1"), makeDomain("s2"))
        coEvery { healthConnectSource.loadRunSessions() } returns sessions
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } answers {
            makeDetail(makeDomain(firstArg()))
        }

        repository.refreshFull()

        coVerify(exactly = 1) { runDetailDao.cacheRunDetailsBatch(
            match { it.size == 2 }, any(), any(), any(),
        ) }
    }

    @Test
    fun `refreshFull continues when detail fetch fails for one session`() = runTest {
        val sessions = listOf(makeDomain("s1"), makeDomain("s2"))
        coEvery { healthConnectSource.loadRunSessions() } returns sessions
        coEvery { healthConnectSource.loadRunDetail(eq("s1"), any(), any()) } throws RuntimeException("fail")
        coEvery { healthConnectSource.loadRunDetail(eq("s2"), any(), any()) } returns makeDetail(sessions[1])

        repository.refreshFull()

        coVerify(exactly = 1) { runDetailDao.cacheRunDetailsBatch(
            match { it.size == 1 && it[0].first == "s2" }, any(), any(), any(),
        ) }
    }

    // endregion
}
