package com.delulutopia.core.ui

import kotlinx.serialization.Serializable

/*
 * Typed routes for every screen behind the PIN, one-to-one with the web's (src/app/routes.ts).
 * Sheets (mood, send, Change PIN, permission asks) are state inside their screens, not routes.
 */

@Serializable data object HomeRoute

@Serializable data object LettersRoute

/** S7; `photo` opens the lightbox on that attachment (web `?photo=n`). */
@Serializable data class LetterRoute(val id: Long, val photo: Int? = null)

/** S4 Write; `editId` edits a letter still in reach (web `/write/edit/:id`), `replyTo` pins the letter answered (web `?reply=`). */
@Serializable data class WriteRoute(val editId: Long? = null, val replyTo: Long? = null)

@Serializable data object DiscussionsRoute

@Serializable data object ArchivedRoute

@Serializable data class DiscussionRoute(val id: Long)

@Serializable data object ComposeDiscussionRoute

@Serializable data class EditDiscussionRoute(val id: Long)

@Serializable data object EmailsRoute

@Serializable data object NewEmailRoute

@Serializable data class ThreadRoute(val id: Long)

@Serializable data object YouRoute

@Serializable data object ProfileRoute

@Serializable data object FriendRoute

@Serializable data object AppearanceRoute

@Serializable data object NotificationsRoute

@Serializable data object UniverseRoute
