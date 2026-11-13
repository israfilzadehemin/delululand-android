package com.delulutopia.core.network

/** Where the app talks to (C2): https://test.delulutopia.com/ for staging, https://delulutopia.com/ for prod. Ends with "/". */
fun interface ServerConfig {
    fun baseUrl(): String
}
