package com.delulutopia.app

import com.delulutopia.core.network.ServerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** C2: the flavour picks the server (staging → test.delulutopia.com, prod → delulutopia.com). */
@Module
@InstallIn(SingletonComponent::class)
object ServerModule {
    @Provides
    fun serverConfig(): ServerConfig = ServerConfig { serverUrl() }
}

/**
 * The server: the flavour's, or the parity fixture (tools/parity/server.mjs) when the JVM screenshot
 * tests set the `dl.baseUrl` system property. Nothing sets it on a phone.
 */
fun serverUrl(): String = System.getProperty("dl.baseUrl") ?: BuildConfig.BASE_URL
