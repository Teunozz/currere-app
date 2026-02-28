package nl.teunk.currere.ui.settings

import android.content.Context
import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import nl.teunk.currere.data.credentials.CredentialsManager
import nl.teunk.currere.data.credentials.ServerCredentials
import nl.teunk.currere.data.sync.SyncStatusStore
import nl.teunk.currere.data.sync.SyncWorker
import nl.teunk.currere.util.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val credentialsManager = mockk<CredentialsManager>(relaxed = true)
    private val syncStatusStore = mockk<SyncStatusStore>(relaxed = true)
    private val appContext = mockk<Context>()

    @Before
    fun setUp() {
        mockkObject(SyncWorker.Companion)
        every { SyncWorker.cancelAll(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(SyncWorker.Companion)
    }

    @Test
    fun `emits connected state when credentials present`() = runTest {
        val credentials = ServerCredentials(baseUrl = "https://api.example.com", token = "tok")
        every { credentialsManager.credentials } returns flowOf(credentials)
        every { syncStatusStore.lastSyncTime } returns flowOf(null)

        val viewModel = SettingsViewModel(credentialsManager, syncStatusStore, appContext)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.isConnected)
            assertEquals("https://api.example.com", state.serverUrl)
            assertEquals("Never", state.lastSyncTime)
        }
    }

    @Test
    fun `emits disconnected state when credentials null`() = runTest {
        every { credentialsManager.credentials } returns flowOf(null)
        every { syncStatusStore.lastSyncTime } returns flowOf(null)

        val viewModel = SettingsViewModel(credentialsManager, syncStatusStore, appContext)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isConnected)
            assertEquals(null, state.serverUrl)
        }
    }

    @Test
    fun `formats lastSyncTime as relative time`() = runTest {
        every { credentialsManager.credentials } returns flowOf(
            ServerCredentials("https://api.example.com", "tok"),
        )
        // Set last sync to ~30 seconds ago
        every { syncStatusStore.lastSyncTime } returns flowOf(System.currentTimeMillis() - 30_000L)

        val viewModel = SettingsViewModel(credentialsManager, syncStatusStore, appContext)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("Just now", state.lastSyncTime)
        }
    }

    @Test
    fun `disconnect cancels sync and clears credentials`() = runTest {
        every { credentialsManager.credentials } returns flowOf(
            ServerCredentials("https://api.example.com", "tok"),
        )
        every { syncStatusStore.lastSyncTime } returns flowOf(null)

        val viewModel = SettingsViewModel(credentialsManager, syncStatusStore, appContext)
        viewModel.disconnect()

        coVerify { SyncWorker.cancelAll(appContext) }
        coVerify { syncStatusStore.clearAll() }
        coVerify { credentialsManager.clear() }
    }
}
