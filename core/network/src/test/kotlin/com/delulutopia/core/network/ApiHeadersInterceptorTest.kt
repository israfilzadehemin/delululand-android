package com.delulutopia.core.network

import com.google.common.truth.Truth.assertThat
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class ApiHeadersInterceptorTest {
    private var seen: Request? = null

    /** The interceptor in front of a fake server that records the request it would have received. */
    private val client = OkHttpClient.Builder()
        .addInterceptor(ApiHeadersInterceptor(ServerClock(), "Delulutopia-Android/test"))
        .addInterceptor(
            Interceptor { chain ->
                seen = chain.request()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .header("X-Server-Time", "1700000000000")
                    .body("".toResponseBody()).build()
            },
        )
        .build()

    private fun get(builder: Request.Builder.() -> Unit = {}) {
        client.newCall(Request.Builder().url("https://test.delulutopia.com/api/users/2/avatar/1").apply(builder).build()).execute().close()
    }

    @Test
    fun `an image request keeps its own Accept (a forced JSON one got 406 for every photo)`() {
        get { header("Accept", "image/webp,image/*") }
        assertThat(seen!!.header("Accept")).isEqualTo("image/webp,image/*")
    }

    @Test
    fun `media without an Accept gets none added`() {
        get()
        assertThat(seen!!.header("Accept")).isNull()
    }

    @Test
    fun `every call carries the CSRF header and the app's user agent`() {
        get()
        assertThat(seen!!.header("X-Requested-With")).isEqualTo("delululand")
        assertThat(seen!!.header("User-Agent")).isEqualTo("Delulutopia-Android/test")
    }
}
