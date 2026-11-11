package com.delulutopia.core.network

import android.content.Context
import com.delulutopia.core.network.cookie.KeystoreCookieVault
import com.delulutopia.core.network.cookie.PersistentCookieJar
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun cookieJar(@ApplicationContext context: Context): PersistentCookieJar =
        PersistentCookieJar(File(context.noBackupFilesDir, "cookies.bin"), KeystoreCookieVault())

    @Provides
    @Singleton
    fun sessionEvents(): SessionEvents = SessionEvents()

    @Provides
    @Singleton
    fun okHttp(cookies: PersistentCookieJar, clock: ServerClock): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .addInterceptor(ApiHeadersInterceptor(clock, "Delulutopia-Android/${android.os.Build.VERSION.RELEASE} (${android.os.Build.MODEL})"))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun retrofit(config: ServerConfig, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl())
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun api(retrofit: Retrofit): DelulutopiaApi = retrofit.create(DelulutopiaApi::class.java)

    @Provides
    @Singleton
    fun apiCaller(json: Json, events: SessionEvents): ApiCaller = ApiCaller(json, events)
}
