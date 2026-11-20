package com.delulutopia.core.network.model

import com.delulutopia.core.i18n.t
import kotlinx.serialization.Serializable

/*
 * Response shapes of Technical Design §5.2, one-to-one with delululand-web src/api/types.ts.
 * Instants are ISO-8601 UTC strings; Instants.kt parses them.
 */

/**
 * A public id (doc "Delululand — Public IDs"): a random UUID the server gives every letter,
 * discussion, comment, email thread, email, user and universe. Opaque: compared, never parsed or ordered.
 */
typealias Id = String

@Serializable
data class Att(
    val id: String,
    /** photo | audio | video | doodle; `video` only on emails, its `thumbUrl` is the poster. */
    val kind: String,
    val url: String,
    val thumbUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val position: Int = 0,
)

@Serializable
data class StampMark(val slug: String, val name: String, val imageUrl: String)

@Serializable
data class StampView(val slug: String, val name: String, val imageUrl: String, val keywords: List<String> = emptyList(), val theme: String, val sort: Int = 0)

@Serializable
data class StampTheme(val slug: String, val name: String, val sort: Int = 0)

@Serializable
data class StampCatalogue(val themes: List<StampTheme>, val stamps: List<StampView>, val recent: List<String> = emptyList())

@Serializable
data class LetterMedia(val photos: Int = 0, val audios: Int = 0, val doodles: Int = 0)

/** A letter list row; `state` is IN_REACH | IN_TRANSIT | DELIVERED | READ. */
@Serializable
data class Row(
    val id: Id,
    /** to | from */
    val direction: String,
    val state: String,
    val sentAt: String,
    val windowEndsAt: String,
    val arrivesAt: String,
    val readAt: String? = null,
    val firstLine: String? = null,
    val media: LetterMedia? = null,
    val moodAtSend: String? = null,
    val moodEmoji: String? = null,
    val moodLabel: String? = null,
    val stamp: StampMark? = null,
    val replyToId: Id? = null,
    /** Doc "Delululand — Data Deletion" DL-5: my letter to a citizen who has since left. */
    val toFormer: Boolean = false,
)

@Serializable
data class ReplyRef(val id: Id, val direction: String, val sentAt: String, val firstLine: String? = null, val media: LetterMedia? = null)

@Serializable
data class LetterFull(
    val id: Id,
    val direction: String,
    val state: String,
    val sentAt: String,
    val windowEndsAt: String,
    val arrivesAt: String,
    val readAt: String? = null,
    val firstLine: String? = null,
    val media: LetterMedia? = null,
    val moodAtSend: String? = null,
    val moodEmoji: String? = null,
    val moodLabel: String? = null,
    val stamp: StampMark? = null,
    val replyToId: Id? = null,
    val body: Doc = Doc.EMPTY,
    val attachments: List<Att> = emptyList(),
    val canEdit: Boolean = false,
    val canCancel: Boolean = false,
    val editAttachments: List<Att>? = null,
    val replyTo: ReplyRef? = null,
) {
    fun asRow() = Row(id, direction, state, sentAt, windowEndsAt, arrivesAt, readAt, firstLine, media, moodAtSend, moodEmoji, moodLabel, stamp, replyToId)
}

@Serializable
data class LetterPage(val items: List<Row>, val nextCursor: String? = null)

/** A citizen as Home sees them. */
@Serializable
data class Citizen(
    val id: Id,
    val nickname: String,
    val avatarUrl: String? = null,
    val mood: String? = null,
    val moodSetAt: String? = null,
    val moodEmoji: String? = null,
    val moodLabel: String? = null,
    val timezone: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val draftEmpty: Boolean = true,
    val discussionDraftEmpty: Boolean = true,
    /** Home `me` only: the account's language (L8), which every device follows. */
    val language: String? = null,
)

@Serializable
data class Mood(val key: String, val emoji: String, val label: String, val custom: Boolean = false)

@Serializable
data class MoodCatalogue(val limit: Int = 48, val moods: List<Mood>)

@Serializable
data class NotifyPrefs(val paused: Boolean = false, val enabled: List<String> = emptyList())

@Serializable
data class Home(
    val serverNow: String,
    val me: Citizen,
    val friend: Citizen? = null,
    val from: List<Row> = emptyList(),
    val to: List<Row> = emptyList(),
    val unreadCount: Int = 0,
    val discussionsUnseen: Int = 0,
    val newDiscussions: List<DiscussionArrival> = emptyList(),
    val emailsUnread: Int = 0,
    val emailsReacted: Int = 0,
    val newEmails: List<EmailArrival> = emptyList(),
    val emailCredits: EmailCredits? = null,
    val board: Board? = null,
    val boardNew: Boolean = false,
    val notifyPrefs: NotifyPrefs? = null,
    val universe: UniverseInfo,
    /** Subscription Tiers: null from an API before V24; nothing is limited on screen then. */
    val plan: Plan? = null,
) {
    /** The other citizen's name for sentences, or "your friend" while the creator is alone (Universe U8). */
    val friendName: String get() = friend?.nickname ?: t("settings.yourFriend")
}

@Serializable
data class HomeCounts(
    val unreadCount: Int = 0,
    val discussionsUnseen: Int = 0,
    val emailsUnread: Int = 0,
    val emailsReacted: Int = 0,
    val boardNew: Boolean = false,
)

// Board (doc "Delululand — Board" §3)

@Serializable
data class Board(
    val version: Int,
    val authorId: Id,
    val createdAt: String,
    val url: String,
    val reaction: String? = null,
    val editedFrom: Id? = null,
    val comment: BoardComment? = null,
)

@Serializable
data class BoardComment(val text: String, val createdAt: String)

// Emails (doc "Delululand — Emails" §4)

@Serializable
data class EmailArrival(val id: Id, val threadId: Id, val subject: String, val sentAt: String)

@Serializable
data class EmailCredits(val limit: Int, val used: Int, val left: Int, val renewsAt: String)

@Serializable
data class ThreadMedia(val photos: Int = 0, val audios: Int = 0, val videos: Int = 0)

@Serializable
data class ThreadRow(
    val id: Id,
    val subject: String,
    val startedBy: Id,
    val createdAt: String,
    val lastEmailAt: String,
    val lastSenderId: Id? = null,
    val lastText: String? = null,
    val lastMedia: ThreadMedia? = null,
    val emailCount: Int = 0,
    val unread: Int = 0,
    val unseenReactions: Int = 0,
    val lastReaction: ReactionNotice? = null,
    /** The newest email answers another (doc "Delululand — Email Replies" RE13: the ↩ prefix). */
    val lastIsReply: Boolean = false,
)

/**
 * The quote of a reply (Email Replies RE9, RE11): the answered email's first 120 characters on one
 * line (null without text), its media counts and the first photo's thumbnail or the video's poster.
 */
@Serializable
data class EmailQuote(
    val id: Id,
    val senderId: Id,
    val text: String? = null,
    val media: ThreadMedia? = null,
    val thumbUrl: String? = null,
    val sentAt: String,
)

@Serializable
data class Email(
    val id: Id,
    val threadId: Id,
    val senderId: Id,
    val text: String = "",
    val sentAt: String,
    val readAt: String? = null,
    val attachments: List<Att> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    /** The email this one answers (RE11); null for a plain email. */
    val replyTo: EmailQuote? = null,
)

@Serializable
data class EmailList(val items: List<Email>, val olderCursor: String? = null)

@Serializable
data class ThreadPage(val items: List<ThreadRow>, val nextCursor: String? = null)

@Serializable
data class ThreadFull(val thread: ThreadRow, val emails: EmailList)

@Serializable
data class EmailSent(val thread: ThreadRow, val email: Email, val credits: EmailCredits)

@Serializable
data class EmailDraft(
    val subject: String = "",
    val text: String = "",
    val attachments: List<Att> = emptyList(),
    val version: Int = 0,
    val savedAt: String? = null,
    /** A thread draft's reply target (RE10). */
    val replyTo: EmailQuote? = null,
)

// Reactions (doc "Delululand — Reactions")

@Serializable
data class Reaction(val emoji: String, val userIds: List<Id>)

@Serializable
data class Reactions(val reactions: List<Reaction>)

@Serializable
data class ReactionNotice(
    val userId: Id,
    val emoji: String,
    /** discussion | comment | email */
    val target: String,
    val onMine: Boolean = false,
    val text: String? = null,
    /** video | photo | audio */
    val media: String? = null,
    val at: String,
)

// Discussions (doc "Delululand — Discussions" §5)

@Serializable
data class DiscussionArrival(val id: Id, val title: String, val arrivesAt: String)

@Serializable
data class DMedia(val photos: Int = 0, val audios: Int = 0)

@Serializable
data class DRow(
    val id: Id,
    val authorId: Id,
    val mine: Boolean,
    val color: String? = null,
    /** IN_REACH | IN_TRANSIT | ARRIVED */
    val state: String,
    val postedAt: String,
    val windowEndsAt: String,
    val arrivesAt: String,
    val title: String? = null,
    val firstLine: String? = null,
    val media: DMedia? = null,
    val commentCount: Int? = null,
    val lastCommentAt: String? = null,
    val isNew: Boolean = false,
    val unseenComments: Int = 0,
    val unseenReactions: Int = 0,
    val lastReaction: ReactionNotice? = null,
    val editedAt: String? = null,
    val activityAt: String? = null,
    val archivedAt: String? = null,
    val archivedBy: Id? = null,
)

@Serializable
data class DComment(
    val id: Id,
    val authorId: Id,
    val text: String,
    val createdAt: String,
    val editedAt: String? = null,
    val reactions: List<Reaction> = emptyList(),
)

@Serializable
data class DComments(val items: List<DComment>, val olderCursor: String? = null)

@Serializable
data class DFull(
    val id: Id,
    val authorId: Id,
    val mine: Boolean,
    val color: String? = null,
    val state: String,
    val postedAt: String,
    val windowEndsAt: String,
    val arrivesAt: String,
    val title: String = "",
    val firstLine: String? = null,
    val media: DMedia? = null,
    val commentCount: Int? = null,
    val lastCommentAt: String? = null,
    val isNew: Boolean = false,
    val unseenComments: Int = 0,
    val editedAt: String? = null,
    val activityAt: String? = null,
    val archivedAt: String? = null,
    val archivedBy: Id? = null,
    val body: Doc = Doc.EMPTY,
    val attachments: List<Att> = emptyList(),
    val canCancel: Boolean = false,
    val comments: DComments = DComments(emptyList()),
    val reactions: List<Reaction> = emptyList(),
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canArchive: Boolean = false,
    val canUnarchive: Boolean = false,
)

@Serializable
data class DPage(val items: List<DRow>, val nextCursor: String? = null, val archivedCount: Int = 0)

@Serializable
data class DDraft(
    val title: String = "",
    val color: String? = null,
    val body: Doc = Doc.EMPTY,
    val attachments: List<Att> = emptyList(),
    val version: Int = 0,
    val savedAt: String? = null,
)

@Serializable
data class DPosted(val id: Id, val postedAt: String, val windowEndsAt: String, val arrivesAt: String)

@Serializable
data class Draft(
    val body: Doc = Doc.EMPTY,
    val attachments: List<Att> = emptyList(),
    val pinnedLetterId: Id? = null,
    val stamp: StampMark? = null,
    val version: Int = 0,
    val savedAt: String? = null,
)

@Serializable
data class Saved(val version: Int, val savedAt: String)

@Serializable
data class Sent(val id: Id, val sentAt: String, val windowEndsAt: String, val arrivesAt: String)

@Serializable
data class DraftVersion(val draftVersion: Int)

// Profile and settings (doc "Delululand — Menu, Profile & Settings" §6)

@Serializable
data class Profile(
    val id: Id,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val timezone: String,
    val avatarUrl: String? = null,
)

@Serializable
data class NotifyEvent(val key: String, val enabled: Boolean, val defaultOn: Boolean = true)

@Serializable
data class NotifySection(val key: String, val enabled: Boolean, val events: List<NotifyEvent>)

@Serializable
data class NotifySettings(val paused: Boolean, val sections: List<NotifySection>)

@Serializable
data class UniverseCreator(val id: Id, val username: String)

@Serializable
data class UniverseView(
    val id: Id,
    val name: String,
    val welcome: String,
    val creator: UniverseCreator? = null,
    val isCreator: Boolean = false,
    val codeRotatedAt: String? = null,
    val tier: String? = null,
    val limits: PlanLimits? = null,
)

// Subscription Tiers (doc "Delululand — Subscription Tiers" §5)

@Serializable
data class PlanLimits(
    val emailsDaily: Int,
    val photosPerMessage: Int,
    val photosDaily: Int,
    val videoSeconds: Int,
    val videoMb: Int,
    val videosDaily: Int,
    /** Null is unlimited (Premium, ST1). */
    val boardDoodlesDaily: Int? = null,
)

@Serializable
data class PlanLeft(val emails: Int, val photos: Int, val videos: Int, val boardDoodles: Int? = null)

@Serializable
data class Plan(val tier: String, val limits: PlanLimits, val left: PlanLeft, val renewsAt: String) {
    val premium: Boolean get() = tier == "premium"
}

@Serializable
data class KeyCode(val code: String)

@Serializable
data class AppConfig(
    val delay: String,
    val window: String,
    val discussionDelay: String? = null,
    val emailDailyCredits: Int = 0,
    val env: String,
    val testTiming: Boolean = false,
    val serverNow: String,
    val vapidPublicKey: String? = null,
)

@Serializable
data class MoodChange(val mood: String, val moodSetAt: String)
