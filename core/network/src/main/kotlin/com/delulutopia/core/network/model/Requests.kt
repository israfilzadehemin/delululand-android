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
    constructor(body: Doc, pinnedLetterId: Id?, stampSlug: String?, version: Int) : this(body, orNull(pinnedLetterId), orNull(stampSlug), version)
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

/** `replyToId` (Email Replies RE10): a thread draft's target; null (omitted) clears it. */
@Serializable data class SaveEmailDraftBody(val subject: String, val text: String, val version: Int, val replyToId: String? = null)

@Serializable data class ProfileBody(val username: String, val firstName: String, val lastName: String, val timezone: String)

@Serializable data class NotifyChange(val paused: Boolean? = null, val values: Map<String, Boolean>? = null)

@Serializable data class UniverseUpdate(val name: String, val welcome: String)

@Serializable data class ChangePinBody(val currentPin: String, val newPin: String)

/** Doc "Delululand — Data Deletion" §5.1. `platform` goes to the ledger only. */
@Serializable data class DeleteMeBody(val pin: String, val platform: String)

@Serializable data class DeleteUniverseBody(val pin: String, val name: String, val platform: String)

@Serializable
data class DeletionCounts(
    val letters: Int = 0,
    val emails: Int = 0,
    val discussions: Int = 0,
    val comments: Int = 0,
    val photos: Int = 0,
    val voice: Int = 0,
    val videos: Int = 0,
    val doodles: Int = 0,
)

/** `scope`: USER (second citizen) or UNIVERSE (creator). */
@Serializable data class DeletionPreview(val scope: String, val universe: String, val friend: String? = null, val counts: DeletionCounts = DeletionCounts())

/** `PUT /api/push/devices` (Android Build B3): this install's FCM token. */
@Serializable data class PushDeviceBody(val platform: String, val token: String, val appId: String? = null)

/** `DELETE /api/push/devices`: before log-out. */
@Serializable data class PushDeviceRemoveBody(val platform: String, val token: String)

/** `POST /api/emails/delivered` (Read Ticks TK2): the emails an FCM push announced. */
@Serializable data class EmailsDeliveredBody(val ids: List<String>)

/** Selfies Q3: the friend's selfie shown unlocked. */
@Serializable data class SelfieSeenBody(val id: String)
