package nl.teunk.currere.ui.diary

import android.content.Context
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.teunk.currere.data.RunSessionRepository
import nl.teunk.currere.data.sync.SyncRecord
import nl.teunk.currere.data.sync.SyncState
import nl.teunk.currere.data.sync.SyncStatusStore
import nl.teunk.currere.data.sync.SyncWorker
import nl.teunk.currere.domain.model.RunSession
import nl.teunk.currere.util.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<RunSessionRepository>(relaxed = true)
    private val syncStatusStore = mockk<SyncStatusStore>()
    private val appContext = mockk<Context>()

    private val sessionsFlow = MutableStateFlow<List<RunSession>>(emptyList())

    @Before
    fun setUp() {
        mockkObject(SyncWorker.Companion)
        every { SyncWorker.enqueueOneTime(any()) } returns Unit

        every { repository.sessions } returns sessionsFlow
        every { syncStatusStore.syncMap } returns flowOf(emptyMap())
        coEvery { repository.refreshIncremental() } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(SyncWorker.Companion)
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

    @Test
    fun `emits Empty when sessions list is empty after load`() = runTest {
        val viewModel = DiaryViewModel(repository, syncStatusStore, appContext)

        viewModel.uiState.test {
            // After init completes, _isInitialLoad becomes false -> Empty
            val state = expectMostRecentItem()
            assertEquals(DiaryUiState.Empty, state)
        }
    }

    @Test
    fun `emits Success with items when sessions available`() = runTest {
        sessionsFlow.value = listOf(makeSession("s1"), makeSession("s2"))

        val viewModel = DiaryViewModel(repository, syncStatusStore, appContext)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DiaryUiState.Success)
            assertEquals(2, (state as DiaryUiState.Success).items.size)
            assertEquals("s1", state.items[0].session.id)
        }
    }

    @Test
    fun `items include sync records when available`() = runTest {
        sessionsFlow.value = listOf(makeSession("s1"))
        val syncMap = mapOf("s1" to SyncRecord(serverId = 42, state = SyncState.SYNCED))
        every { syncStatusStore.syncMap } returns flowOf(syncMap)

        val viewModel = DiaryViewModel(repository, syncStatusStore, appContext)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is DiaryUiState.Success)
            val item = (state as DiaryUiState.Success).items[0]
            assertEquals(SyncState.SYNCED, item.syncRecord?.state)
            assertEquals(42L, item.syncRecord?.serverId)
        }
    }

    @Test
    fun `refresh calls refreshFull and enqueues sync`() = runTest {
        coEvery { repository.refreshFull() } returns Unit

        val viewModel = DiaryViewModel(repository, syncStatusStore, appContext)
        viewModel.refresh()

        coVerify { repository.refreshFull() }
        // enqueueOneTime called once in init + once in refresh
        coVerify(atLeast = 2) { SyncWorker.enqueueOneTime(appContext) }
    }

    @Test
    fun `refresh resets isRefreshing to false`() = runTest {
        coEvery { repository.refreshFull() } returns Unit

        val viewModel = DiaryViewModel(repository, syncStatusStore, appContext)
        viewModel.refresh()

        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `clearSyncMessage sets syncMessage to null`() {
        val viewModel = DiaryViewModel(repository, syncStatusStore, appContext)
        viewModel.clearSyncMessage()
        assertEquals(null, viewModel.syncMessage.value)
    }
}
