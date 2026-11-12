package com.delulutopia.core.network

import com.google.common.truth.Truth.assertThat
import java.io.InputStream
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class UploaderTest {
    private val server = MockWebServer()
    private lateinit var uploader: Uploader

    @Before
    fun setUp() {
        server.start()
        val base = server.url("/").toString()
        uploader = Uploader(
            OkHttpClient(),
            object : ServerConfig {
                override fun baseUrl() = base
            },
            Json { ignoreUnknownKeys = true },
            SessionEvents(),
        )
    }

    @After
    fun tearDown() = server.close()

    /** A 3 MB "file" that is read in small pieces and never exists as one array. */
    private class Pieces(val size: Int) : InputStream() {
        var read = 0
        var biggestChunk = 0

        override fun read(): Int = if (read < size) 'a'.code.also { read++ } else -1

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (read >= size) return -1
            val n = minOf(len, size - read)
            b.fill('a'.code.toByte(), off, off + n)
            read += n
            biggestChunk = maxOf(biggestChunk, n)
            return n
        }
    }

    @Test
    fun `a picked file is streamed, with progress, and the whole of it arrives`() = runTest {
        server.enqueue(MockResponse.Builder().code(201).body("""{"id":"a1","kind":"audio","url":"/api/attachments/a1"}""").build())
        val size = 3 * 1024 * 1024
        val input = Pieces(size)
        val progress = mutableListOf<Float>()

        val att = uploader.upload("api/attachments", Uploader.Kind.Audio, size.toLong(), { input }, "voice.m4a", "audio/mp4") { progress += it }

        assertThat(att.id).isEqualTo("a1")
        assertThat(input.read).isEqualTo(size)
        // Read in buffer-sized pieces (okio uses 8 KB segments), never the whole file at once.
        assertThat(input.biggestChunk).isAtMost(64 * 1024)
        val request = server.takeRequest()
        assertThat(request.body!!.size).isGreaterThan(size.toLong())
        assertThat(progress.last()).isEqualTo(1f)
        assertThat(progress.dropLast(1).all { it < 1f }).isTrue()
    }

    @Test
    fun `a file over the kind's limit is refused before anything is sent`() = runTest {
        val e = assertThrows(ApiException::class.java) {
            kotlinx.coroutines.runBlocking {
                uploader.upload("api/attachments", Uploader.Kind.Video, Uploader.VIDEO_MAX + 1, { error("must not open") }, "v.mp4", "video/mp4") {}
            }
        }
        assertThat(e.code).isEqualTo("TOO_LARGE")
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `the server's refusal comes back in its own words`() = runTest {
        server.enqueue(MockResponse.Builder().code(415).body("""{"code":"UNSUPPORTED_TYPE","message":"That file type can't be attached."}""").build())
        val e = assertThrows(ApiException::class.java) {
            kotlinx.coroutines.runBlocking {
                uploader.upload("api/attachments", Uploader.Kind.Photo, { ByteArray(10) }, "p.gif", "image/gif") {}
            }
        }
        assertThat(e.code).isEqualTo("UNSUPPORTED_TYPE")
        assertThat(e.message).isEqualTo("That file type can't be attached.")
    }
}
