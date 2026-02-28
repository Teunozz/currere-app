package nl.teunk.currere.data.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import nl.teunk.currere.data.api.ApiClient
import nl.teunk.currere.data.api.ApiResponse
import nl.teunk.currere.data.api.BatchResultItem
import nl.teunk.currere.data.api.BatchRunResponseData
import nl.teunk.currere.data.api.CurrereApiService
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.credentials.ServerCredentials
import nl.teunk.currere.data.health.HealthConnectSource
import nl.teunk.currere.domain.model.HeartRateSample
import nl.teunk.currere.domain.model.PaceSplit
import nl.teunk.currere.domain.model.RunDetail
import nl.teunk.currere.domain.model.RunSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.Duration
import java.time.Instant

class SyncRepositoryTest {

    private val apiClient = mockk<ApiClient>()
    private val syncStatusStore = mockk<SyncStatusStore>(relaxed = true)
    private val credentialsManager = mockk<CredentialsManager>()
    private val healthConnectSource = mockk<HealthConnectSource>()
    private val apiService = mockk<CurrereApiService>()

    private lateinit var repository: SyncRepository

    private val credentials = ServerCredentials(baseUrl = "https://example.com", token = "test-token")

    @Before
    fun setUp() {
        repository = SyncRepository(apiClient, syncStatusStore, credentialsManager, healthConnectSource)
    }

    private fun makeSession(id: String) = RunSession(
        id = id,
        startTime = Instant.parse("2024-01-01T10:00:00Z"),
        endTime = Instant.parse("2024-01-01T10:30:00Z"),
        distanceMeters = 5000.0,
        activeDuration = Duration.ofSeconds(1800),
        averagePaceSecondsPerKm = 360.0,
        averageHeartRateBpm = 150,
        title = "Morning run",
    )

    private fun makeDetail(session: RunSession) = RunDetail(
        session = session,
        totalSteps = 5000,
        heartRateSamples = listOf(
            HeartRateSample(time = session.startTime, bpm = 120),
            HeartRateSample(time = session.startTime.plusSeconds(600), bpm = 155),
        ),
        paceSamples = emptyList(),
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

    private fun setupConnected() {
        every { credentialsManager.credentials } returns flowOf(credentials)
        coEvery { apiClient.createService() } returns apiService
    }

    private fun setupSyncMap(map: Map<String, SyncRecord> = emptyMap()) {
        every { syncStatusStore.syncMap } returns flowOf(map)
    }

    // region NotConnected

    @Test
    fun `returns NotConnected when no credentials`() = runTest {
        every { credentialsManager.credentials } returns flowOf(null)

        val result = repository.syncSessions(listOf(makeSession("s1")))

        assertEquals(SyncResult.NotConnected, result)
    }

    @Test
    fun `returns NotConnected when createService returns null`() = runTest {
        every { credentialsManager.credentials } returns flowOf(credentials)
        coEvery { apiClient.createService() } returns null

        val result = repository.syncSessions(listOf(makeSession("s1")))

        assertEquals(SyncResult.NotConnected, result)
    }

    // endregion

    // region All synced

    @Test
    fun `returns Success with synced=0 when all sessions already synced`() = runTest {
        setupConnected()
        setupSyncMap(
            mapOf(
                "s1" to SyncRecord(serverId = 1, state = SyncState.SYNCED),
                "s2" to SyncRecord(serverId = 2, state = SyncState.SYNCED),
            ),
        )

        val result = repository.syncSessions(listOf(makeSession("s1"), makeSession("s2")))

        assertEquals(SyncResult.Success(synced = 0, total = 2), result)
    }

    // endregion

    // region Happy path

    @Test
    fun `marks sessions pending before sending batch`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } returns Response.success(
            ApiResponse(BatchRunResponseData(created = 1, skipped = 0, results = listOf(
                BatchResultItem(index = 0, status = "created", id = 100),
            ))),
        )

        repository.syncSessions(listOf(session))

        coVerify { syncStatusStore.markPending(listOf("s1")) }
    }

    @Test
    fun `happy path marks synced on created status`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } returns Response.success(
            ApiResponse(BatchRunResponseData(created = 1, skipped = 0, results = listOf(
                BatchResultItem(index = 0, status = "created", id = 100),
            ))),
        )

        val result = repository.syncSessions(listOf(session))

        assertTrue(result is SyncResult.Success)
        assertEquals(1, (result as SyncResult.Success).synced)
        coVerify { syncStatusStore.markSynced("s1", 100) }
    }

    @Test
    fun `happy path marks synced on skipped status`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } returns Response.success(
            ApiResponse(BatchRunResponseData(created = 0, skipped = 1, results = listOf(
                BatchResultItem(index = 0, status = "skipped", id = 200),
            ))),
        )

        val result = repository.syncSessions(listOf(session))

        assertEquals(SyncResult.Success(synced = 1, total = 1), result)
        coVerify { syncStatusStore.markSynced("s1", 200) }
    }

    @Test
    fun `marks failed for unknown result status`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } returns Response.success(
            ApiResponse(BatchRunResponseData(created = 0, skipped = 0, results = listOf(
                BatchResultItem(index = 0, status = "error", id = 99),
            ))),
        )

        repository.syncSessions(listOf(session))

        coVerify { syncStatusStore.markFailed("s1", "Status: error") }
    }

    // endregion

    // region Fallback to session-only request

    @Test
    fun `falls back to session-only RunRequest when loadRunDetail throws`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } throws RuntimeException("HC unavailable")
        coEvery { apiService.createRunsBatch(any()) } returns Response.success(
            ApiResponse(BatchRunResponseData(created = 1, skipped = 0, results = listOf(
                BatchResultItem(index = 0, status = "created", id = 300),
            ))),
        )

        val result = repository.syncSessions(listOf(session))

        assertTrue(result is SyncResult.Success)
        coVerify { apiService.createRunsBatch(match { batch ->
            val run = batch.runs[0]
            // Session-only request should not have heart rate samples or pace splits
            run.heartRateSamples == null && run.paceSplits == null && run.steps == null
        }) }
    }

    // endregion

    // region Error paths

    @Test
    fun `returns Unauthorized on 401`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } returns Response.error(
            401,
            """{"message":"Unauthenticated."}""".toResponseBody("application/json".toMediaType()),
        )

        val result = repository.syncSessions(listOf(session))

        assertEquals(SyncResult.Unauthorized, result)
    }

    @Test
    fun `returns Error on 422`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } returns Response.error(
            422,
            """{"message":"Validation failed"}""".toResponseBody("application/json".toMediaType()),
        )

        val result = repository.syncSessions(listOf(session))

        assertTrue(result is SyncResult.Error)
        assertEquals("Validation error from server", (result as SyncResult.Error).message)
    }

    @Test
    fun `returns Error on 500`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } returns Response.error(
            500,
            """{"message":"Internal server error"}""".toResponseBody("application/json".toMediaType()),
        )

        val result = repository.syncSessions(listOf(session))

        assertTrue(result is SyncResult.Error)
        assertEquals("Server returned 500", (result as SyncResult.Error).message)
    }

    @Test
    fun `returns Error and marks failed on network exception`() = runTest {
        setupConnected()
        setupSyncMap()

        val session = makeSession("s1")
        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } returns makeDetail(session)
        coEvery { apiService.createRunsBatch(any()) } throws java.io.IOException("Network unreachable")

        val result = repository.syncSessions(listOf(session))

        assertTrue(result is SyncResult.Error)
        assertEquals("Network unreachable", (result as SyncResult.Error).message)
        coVerify { syncStatusStore.markFailed("s1", "Network unreachable") }
    }

    // endregion

    // region Multi-session batch

    @Test
    fun `syncs only unsynced sessions in a mixed batch`() = runTest {
        setupConnected()
        setupSyncMap(
            mapOf("s1" to SyncRecord(serverId = 1, state = SyncState.SYNCED)),
        )

        val s1 = makeSession("s1")
        val s2 = makeSession("s2")
        val s3 = makeSession("s3")

        coEvery { healthConnectSource.loadRunDetail(any(), any(), any()) } answers {
            makeDetail(makeSession("any"))
        }
        coEvery { apiService.createRunsBatch(any()) } returns Response.success(
            ApiResponse(BatchRunResponseData(created = 2, skipped = 0, results = listOf(
                BatchResultItem(index = 0, status = "created", id = 400),
                BatchResultItem(index = 1, status = "created", id = 401),
            ))),
        )

        val result = repository.syncSessions(listOf(s1, s2, s3))

        assertEquals(SyncResult.Success(synced = 2, total = 3), result)
        // Only s2 and s3 should be in the batch (s1 already synced)
        coVerify { apiService.createRunsBatch(match { it.runs.size == 2 }) }
        coVerify { syncStatusStore.markPending(listOf("s2", "s3")) }
    }

    // endregion
}
