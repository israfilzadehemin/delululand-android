package com.delulutopia.core.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable data class VersionBody(val version: Int)

@Serializable data class ReactionBody(val version: Int, val reaction: String)

@Serializable data class BoardCommentBody(val version: Int, val text: String)

@Serializable data class TextBody(val text: String)

@Serializable data class EmojiBody(val emoji: String)

@Serializable data class MoodBody(val mood: String)

@Serializable data class MoodEditBody(val emoji: String, val label: String)

/** `pinnedLetterId` and `stampSlug` are sent as null to clear them (hence JSON values, not nullable fields). */
@Serializable data class SaveDraftBody(val body: Doc, val pinnedLetterId: JsonElement, val stampSlug: JsonElement, val version: Int) {
    constructor(body: Doc, pinnedLetterId: Long?, stampSlug: String?, version: Int) : this(body, orNull(pinnedLetterId), orNull(stampSlug), version)
}

@Serializable data class DraftVersionBody(val draftVersion: Int)

@Serializable data class SendAgainBody(val body: Doc, val attachmentIds: List<String>, val stampSlug: JsonElement) {
    constructor(body: Doc, attachmentIds: List<String>, stampSlug: String?) : this(body, attachmentIds, orNull(stampSlug))
}

/** A value the API must receive even when it is null (the JSON defaults drop null fields). */
fun orNull(v: Long?): JsonElement = v?.let { JsonPrimitive(it) } ?: JsonNull

fun orNull(v: String?): JsonElement = v?.let { JsonPrimitive(it) } ?: JsonNull

@Serializable data class BodyOnly(val body: Doc)

@Serializable data class SaveDiscussionDraftBody(val title: String, val body: Doc, val version: Int, val color: String)

@Serializable data class EditDiscussionBody(val title: String, val body: Doc, val color: String, val attachmentIds: List<String>)

@Serializable data class SaveEmailDraftBody(val subject: String, val text: String, val version: Int)

@Serializable data class ProfileBody(val username: String, val firstName: String, val lastName: String, val timezone: String)

@Serializable data class NotifyChange(val paused: Boolean? = null, val values: Map<String, Boolean>? = null)

@Serializable data class UniverseUpdate(val name: String, val welcome: String)

@Serializable data class ChangePinBody(val currentPin: String, val newPin: String)
