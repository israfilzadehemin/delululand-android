package com.delulutopia.core.network.model

import kotlinx.serialization.Serializable

@Serializable data class VersionBody(val version: Int)

@Serializable data class ReactionBody(val version: Int, val reaction: String)

@Serializable data class BoardCommentBody(val version: Int, val text: String)

@Serializable data class TextBody(val text: String)

@Serializable data class EmojiBody(val emoji: String)

@Serializable data class MoodBody(val mood: String)

@Serializable data class MoodEditBody(val emoji: String, val label: String)

@Serializable data class SaveDraftBody(val body: Doc, val pinnedLetterId: Long?, val stampSlug: String?, val version: Int)

@Serializable data class DraftVersionBody(val draftVersion: Int)

@Serializable data class SendAgainBody(val body: Doc, val attachmentIds: List<String>, val stampSlug: String?)

@Serializable data class BodyOnly(val body: Doc)

@Serializable data class SaveDiscussionDraftBody(val title: String, val body: Doc, val version: Int, val color: String)

@Serializable data class EditDiscussionBody(val title: String, val body: Doc, val color: String, val attachmentIds: List<String>)

@Serializable data class SaveEmailDraftBody(val subject: String, val text: String, val version: Int)

@Serializable data class ProfileBody(val username: String, val firstName: String, val lastName: String, val timezone: String)

@Serializable data class NotifyChange(val paused: Boolean? = null, val values: Map<String, Boolean>? = null)

@Serializable data class UniverseUpdate(val name: String, val welcome: String)

@Serializable data class ChangePinBody(val currentPin: String, val newPin: String)
