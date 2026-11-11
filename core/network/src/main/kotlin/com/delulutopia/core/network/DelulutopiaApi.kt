package com.delulutopia.core.network

import com.delulutopia.core.network.model.CreateUniverseRequest
import com.delulutopia.core.network.model.CreatedUniverse
import com.delulutopia.core.network.model.EnterRequest
import com.delulutopia.core.network.model.Entered
import com.delulutopia.core.network.model.EntryView
import com.delulutopia.core.network.model.HomeSession
import com.delulutopia.core.network.model.InviteRequest
import com.delulutopia.core.network.model.JoinRequest
import com.delulutopia.core.network.model.LoginRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** The API, one function per endpoint (Technical Design §5.2, doc "Delululand — Universe" §4). */
interface DelulutopiaApi {
    // Entry pass
    @POST("api/entry")
    suspend fun enter(@Body body: EnterRequest): Entered

    @POST("api/entry/forget")
    suspend fun forgetEntry()

    @GET("api/universe/entry")
    suspend fun entry(): EntryView

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest)

    @POST("api/auth/pin")
    suspend fun createPin(@Body body: LoginRequest)

    @POST("api/auth/logout")
    suspend fun logout()

    // Universe
    @POST("api/invites/check")
    suspend fun checkInvite(@Body body: InviteRequest)

    @POST("api/universes")
    suspend fun createUniverse(@Body body: CreateUniverseRequest): CreatedUniverse

    @POST("api/universe/join")
    suspend fun join(@Body body: JoinRequest)

    // Session
    @GET("api/home")
    suspend fun homeSession(): HomeSession
}
