package nl.teunk.currere.data

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.teunk.currere.data.db.RunSessionDao
import nl.teunk.currere.data.db.RunSessionEntity
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.RunSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RunSessionRepositoryTest {

    private val dao = mockk<RunSessionDao>(relaxed = true)
    private val healthConnectSource = mockk<HealthConnectSource>()

    private val repository = RunSessionRepository(dao, healthConnectSource)

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

    // region sessions Flow

    @Test
    fun `sessions Flow maps entities to domain models`() = runTest {
        val entities = listOf(makeEntity("a"), makeEntity("b"))
        coEvery { dao.getAllSessions() } returns flowOf(entities)

        val repo = RunSessionRepository(dao, healthConnectSource)
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

        val repo = RunSessionRepository(dao, healthConnectSource)
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

        repository.refreshIncremental()

        coVerify { dao.insertAll(match { it.size == 1 && it[0].id == "new-1" }) }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    @Test
    fun `refreshIncremental falls back to full refresh when cache empty`() = runTest {
        coEvery { dao.getLatestEndTimeMillis() } returns null

        val allSessions = listOf(makeDomain("s1"), makeDomain("s2"))
        coEvery { healthConnectSource.loadRunSessions() } returns allSessions

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
}
