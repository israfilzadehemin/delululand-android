package com.delulutopia.core.network

import com.delulutopia.core.network.model.EnterRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiCallerTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private val events = SessionEvents()
    private val clock = ServerClock()
    private lateinit var api: DelulutopiaApi
    private val caller = ApiCaller(json, events)

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(ApiHeadersInterceptor(clock, "test")).build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DelulutopiaApi::class.java)
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `headers and server time`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("X-Server-Time", "${System.currentTimeMillis() + 60_000}")
                .body("""{"name":"Moonbase","welcome":"Hi"}""")
                .build(),
        )
        val r = caller.call { api.enter(EnterRequest("K7QM-WX4P-2HRN")) }
        assertThat(r.name).isEqualTo("Moonbase")
        val req = server.takeRequest()
        assertThat(req.headers["X-Requested-With"]).isEqualTo("delululand")
        assertThat(clock.offsetMs).isAtLeast(59_000)
    }

    @Test
    fun `refusals carry the API code, message and extra`() = runTest {
        server.enqueue(MockResponse.Builder().code(429).body("""{"code":"CODE_THROTTLED","message":"Wait.","retryAfterMs":700}""").build())
        val e = runCatching { caller.call { api.enter(EnterRequest("x")) } }.exceptionOrNull() as ApiException
        assertThat(e.status).isEqualTo(429)
        assertThat(e.code).isEqualTo("CODE_THROTTLED")
        assertThat(e.message).isEqualTo("Wait.")
        assertThat(e.retryAfterMs).isEqualTo(700)
    }

    @Test
    fun `401 NO_SESSION ends the session`() = runTest(UnconfinedTestDispatcher()) {
        var ended = false
        val job = launch {
            events.sessionEnded.first()
            ended = true
        }
        server.enqueue(MockResponse.Builder().code(401).body("""{"code":"NO_SESSION","message":"Log in."}""").build())
        runCatching { caller.call { api.homeSession() } }
        assertThat(ended).isTrue()
        job.cancel()
    }

    @Test
    fun `no server is offline`() = runTest {
        server.close()
        val e = runCatching { caller.call { api.entry() } }.exceptionOrNull() as ApiException
        assertThat(e.isOffline).isTrue()
        assertThat(e.code).isEqualTo(ApiException.OFFLINE)
    }
}
