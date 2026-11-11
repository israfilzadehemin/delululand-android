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
    fun serverConfig(): ServerConfig = ServerConfig { BuildConfig.BASE_URL }
}
