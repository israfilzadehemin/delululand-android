package com.delulutopia.core.network.model

import kotlinx.serialization.Serializable

/*
 * Wire shapes of the entry, auth and universe endpoints (delululand-api: EntryController,
 * AuthController, UniverseController; web: src/api/account.ts, universe.ts). Field names are the API.
 */

@Serializable
data class EnterRequest(val code: String)

@Serializable
data class Entered(val name: String, val welcome: String)

@Serializable
data class UserCard(val id: Int, val nickname: String, val avatarUrl: String? = null, val hasPin: Boolean)

/** UV-4: the universe behind the entry pass. */
@Serializable
data class EntryView(val name: String, val welcome: String, val canJoin: Boolean, val users: List<UserCard>)

@Serializable
data class LoginRequest(val userId: Int, val pin: String)

@Serializable
data class InviteRequest(val invite: String)

@Serializable
data class CreateUniverseRequest(
    val invite: String,
    val name: String,
    val welcome: String,
    val username: String,
    val timezone: String,
    val pin: String,
    /** L6: the creator's language; also the language of the welcome template (C52). */
    val language: String? = null,
)

@Serializable
data class CreatedUniverse(val code: String, val universe: UniverseRef)

@Serializable
data class UniverseRef(val id: Int, val name: String)

@Serializable
data class JoinRequest(val username: String, val timezone: String, val pin: String, val language: String? = null)

/** `PUT /api/me/language` (L8). */
@Serializable
data class LanguageBody(val language: String)

/** The parts of `GET /api/home` the session needs; the rest of the payload arrives with Home (P2). */
@Serializable
data class HomeSession(val me: Person, val friend: Person? = null, val universe: UniverseInfo)

@Serializable
data class Person(val id: Int, val nickname: String, val avatarUrl: String? = null, val timezone: String? = null)

@Serializable
data class UniverseInfo(val id: Int, val name: String, val isCreator: Boolean = false, val waiting: Boolean = false)
