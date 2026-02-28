package nl.teunk.currere.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class CurrereApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: CurrereApiService

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CurrereApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `createRunsBatch sends POST to correct path`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":{"created":1,"skipped":0,"results":[{"index":0,"status":"created","id":42}]}}""")
        )

        val batch = BatchRunRequest(
            runs = listOf(
                RunRequest("2024-01-01T10:00:00Z", "2024-01-01T10:30:00Z", 5.0, 1800),
            ),
        )
        service.createRunsBatch(batch)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/runs/batch", recorded.path)
    }

    @Test
    fun `createRunsBatch serializes request body correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":{"created":1,"skipped":0,"results":[]}}""")
        )

        val batch = BatchRunRequest(
            runs = listOf(
                RunRequest(
                    startTime = "2024-01-01T10:00:00Z",
                    endTime = "2024-01-01T10:30:00Z",
                    distanceKm = 5.0,
                    durationSeconds = 1800,
                    steps = 5000,
                ),
            ),
        )
        service.createRunsBatch(batch)

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"start_time\":\"2024-01-01T10:00:00Z\""))
        assertTrue(body.contains("\"distance_km\":5.0"))
        assertTrue(body.contains("\"steps\":5000"))
    }

    @Test
    fun `createRunsBatch deserializes response correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":{"created":2,"skipped":1,"results":[{"index":0,"status":"created","id":10},{"index":1,"status":"created","id":11},{"index":2,"status":"skipped","id":12}]}}""")
        )

        val batch = BatchRunRequest(runs = listOf(
            RunRequest("2024-01-01T10:00:00Z", "2024-01-01T10:30:00Z", 5.0, 1800),
        ))
        val response = service.createRunsBatch(batch)

        assertTrue(response.isSuccessful)
        val data = response.body()!!.data
        assertEquals(2, data.created)
        assertEquals(1, data.skipped)
        assertEquals(3, data.results.size)
        assertEquals("created", data.results[0].status)
        assertEquals(10L, data.results[0].id)
    }

    @Test
    fun `getRuns sends correct query params`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[],"meta":{"current_page":2,"last_page":5,"per_page":10,"total":50}}""")
        )

        service.getRuns(page = 2, perPage = 10)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue(recorded.path!!.contains("page=2"))
        assertTrue(recorded.path!!.contains("per_page=10"))
    }

    @Test
    fun `getRuns deserializes paginated response`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":[{"id":1,"start_time":"2024-01-01T10:00:00Z","distance_km":5.0}],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}""")
        )

        val response = service.getRuns()

        assertTrue(response.isSuccessful)
        val body = response.body()!!
        assertEquals(1, body.data.size)
        assertEquals(1L, body.data[0].id)
        assertNotNull(body.meta)
        assertEquals(1, body.meta!!.total)
    }

    @Test
    fun `HTTP 401 is returned as unsuccessful response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Unauthenticated."}"""))

        val response = service.getRuns()

        assertEquals(401, response.code())
        assertTrue(!response.isSuccessful)
    }

    @Test
    fun `HTTP 422 is returned as unsuccessful response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"message":"Validation failed"}"""))

        val batch = BatchRunRequest(runs = emptyList())
        val response = service.createRunsBatch(batch)

        assertEquals(422, response.code())
        assertTrue(!response.isSuccessful)
    }
}
