package com.delulutopia.core.ui

import android.content.Context
import okhttp3.OkHttpClient

/**
 * The app's OkHttp client (cookies, headers, the server clock), for media the screens stream
 * themselves: voice notes and videos must carry the session like every other request.
 */
interface HttpOwner {
    val httpClient: OkHttpClient
}

fun Context.httpClient(): OkHttpClient = (applicationContext as HttpOwner).httpClient
