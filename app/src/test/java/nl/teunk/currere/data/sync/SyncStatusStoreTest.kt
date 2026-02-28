package nl.teunk.currere.data.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var store: SyncStatusStore

    @Before
    fun setUp() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_sync_status.preferences_pb") },
        )
        store = SyncStatusStore(dataStore)
    }

    @Test
    fun `syncMap emits empty map initially`() = runTest {
        store.syncMap.test {
            assertEquals(emptyMap<String, SyncRecord>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lastSyncTime emits null initially`() = runTest {
        store.lastSyncTime.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `markPending adds entries with PENDING state`() = runTest {
        store.markPending(listOf("s1", "s2"))

        store.syncMap.test {
            val map = awaitItem()
            assertEquals(2, map.size)
            assertEquals(SyncState.PENDING, map["s1"]?.state)
            assertEquals(SyncState.PENDING, map["s2"]?.state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `markPending does not overwrite existing entries`() = runTest {
        store.markSynced("s1", serverId = 42)
        store.markPending(listOf("s1", "s2"))

        store.syncMap.test {
            val map = awaitItem()
            assertEquals(SyncState.SYNCED, map["s1"]?.state)
            assertEquals(42L, map["s1"]?.serverId)
            assertEquals(SyncState.PENDING, map["s2"]?.state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `markSynced updates state to SYNCED with serverId`() = runTest {
        store.markPending(listOf("s1"))
        store.markSynced("s1", serverId = 100)

        store.syncMap.test {
            val map = awaitItem()
            assertEquals(SyncState.SYNCED, map["s1"]?.state)
            assertEquals(100L, map["s1"]?.serverId)
            assertTrue(map["s1"]!!.lastAttempt > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `markSynced updates lastSyncTime`() = runTest {
        store.markSynced("s1", serverId = 1)

        store.lastSyncTime.test {
            val time = awaitItem()
            assertNotNull(time)
            assertTrue(time!! > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `markFailed updates state to FAILED with message`() = runTest {
        store.markPending(listOf("s1"))
        store.markFailed("s1", "Network error")

        store.syncMap.test {
            val map = awaitItem()
            assertEquals(SyncState.FAILED, map["s1"]?.state)
            assertEquals("Network error", map["s1"]?.failureMessage)
            assertTrue(map["s1"]!!.lastAttempt > 0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll removes sync map and last sync time`() = runTest {
        store.markSynced("s1", serverId = 1)
        store.clearAll()

        store.syncMap.test {
            assertEquals(emptyMap<String, SyncRecord>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        store.lastSyncTime.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
