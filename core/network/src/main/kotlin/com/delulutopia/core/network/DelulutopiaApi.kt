package com.delulutopia.core.network

import com.delulutopia.core.network.model.AppConfig
import com.delulutopia.core.network.model.Board
import com.delulutopia.core.network.model.BoardCommentBody
import com.delulutopia.core.network.model.BodyOnly
import com.delulutopia.core.network.model.ChangePinBody
import com.delulutopia.core.network.model.CreateUniverseRequest
import com.delulutopia.core.network.model.CreatedUniverse
import com.delulutopia.core.network.model.DComment
import com.delulutopia.core.network.model.DComments
import com.delulutopia.core.network.model.DDraft
import com.delulutopia.core.network.model.DFull
import com.delulutopia.core.network.model.DPage
import com.delulutopia.core.network.model.DPosted
import com.delulutopia.core.network.model.Draft
import com.delulutopia.core.network.model.DraftVersion
import com.delulutopia.core.network.model.DraftVersionBody
import com.delulutopia.core.network.model.EditDiscussionBody
import com.delulutopia.core.network.model.EmailCredits
import com.delulutopia.core.network.model.EmailDraft
import com.delulutopia.core.network.model.EmailList
import com.delulutopia.core.network.model.EmailSent
import com.delulutopia.core.network.model.EmojiBody
import com.delulutopia.core.network.model.EnterRequest
import com.delulutopia.core.network.model.Entered
import com.delulutopia.core.network.model.EntryView
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.HomeCounts
import com.delulutopia.core.network.model.HomeSession
import com.delulutopia.core.network.model.InviteRequest
import com.delulutopia.core.network.model.JoinRequest
import com.delulutopia.core.network.model.KeyCode
import com.delulutopia.core.network.model.LetterFull
import com.delulutopia.core.network.model.LetterPage
import com.delulutopia.core.network.model.LoginRequest
import com.delulutopia.core.network.model.Mood
import com.delulutopia.core.network.model.MoodBody
import com.delulutopia.core.network.model.MoodCatalogue
import com.delulutopia.core.network.model.MoodChange
import com.delulutopia.core.network.model.MoodEditBody
import com.delulutopia.core.network.model.NotifyChange
import com.delulutopia.core.network.model.NotifySettings
import com.delulutopia.core.network.model.Profile
import com.delulutopia.core.network.model.ProfileBody
import com.delulutopia.core.network.model.ReactionBody
import com.delulutopia.core.network.model.Reactions
import com.delulutopia.core.network.model.SaveDiscussionDraftBody
import com.delulutopia.core.network.model.SaveDraftBody
import com.delulutopia.core.network.model.SaveEmailDraftBody
import com.delulutopia.core.network.model.Saved
import com.delulutopia.core.network.model.SendAgainBody
import com.delulutopia.core.network.model.Sent
import com.delulutopia.core.network.model.StampCatalogue
import com.delulutopia.core.network.model.TextBody
import com.delulutopia.core.network.model.ThreadFull
import com.delulutopia.core.network.model.ThreadPage
import com.delulutopia.core.network.model.UniverseUpdate
import com.delulutopia.core.network.model.UniverseView
import com.delulutopia.core.network.model.UserCard
import com.delulutopia.core.network.model.VersionBody
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The API, one function per endpoint (Technical Design §5.2 and the story docs; web src/api/*.ts).
 * Attachment uploads with progress go through [Uploader] instead.
 */
interface DelulutopiaApi {
    @GET("api/config")
    suspend fun config(): AppConfig

    // Entry pass
    @POST("api/entry")
    suspend fun enter(@Body body: EnterRequest): Entered

    @POST("api/entry/forget")
    suspend fun forgetEntry()

    @GET("api/universe/entry")
    suspend fun entry(): EntryView

    @GET("api/users")
    suspend fun users(): List<UserCard>

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest)

    @POST("api/auth/pin")
    suspend fun createPin(@Body body: LoginRequest)

    @POST("api/auth/logout")
    suspend fun logout()

    @POST("api/me/pin")
    suspend fun changePin(@Body body: ChangePinBody)

    // Universe
    @POST("api/invites/check")
    suspend fun checkInvite(@Body body: InviteRequest)

    @POST("api/universes")
    suspend fun createUniverse(@Body body: CreateUniverseRequest): CreatedUniverse

    @POST("api/universe/join")
    suspend fun join(@Body body: JoinRequest)

    @GET("api/universe")
    suspend fun universe(): UniverseView

    @PUT("api/universe")
    suspend fun updateUniverse(@Body body: UniverseUpdate): UniverseView

    @POST("api/universe/code")
    suspend fun rotateKey(): KeyCode

    // Home
    @GET("api/home")
    suspend fun homeSession(): HomeSession

    @GET("api/home")
    suspend fun home(): Home

    @GET("api/home/counts")
    suspend fun homeCounts(): HomeCounts

    @PUT("api/me/mood")
    suspend fun setMood(@Body body: MoodBody): MoodChange

    // Moods (Moods CM1–CM13)
    @GET("api/moods")
    suspend fun moods(): MoodCatalogue

    @POST("api/moods")
    suspend fun addMood(@Body body: MoodEditBody): Mood

    @PUT("api/moods/{key}")
    suspend fun editMood(@Path("key") key: String, @Body body: MoodEditBody): Mood

    @DELETE("api/moods/{key}")
    suspend fun deleteMood(@Path("key") key: String)

    // Board (Board §3)
    @Multipart
    @POST("api/board")
    suspend fun postBoard(@Part("baseVersion") baseVersion: RequestBody, @Part("edit") edit: RequestBody?, @Part file: MultipartBody.Part): Board

    @PUT("api/board/reaction")
    suspend fun reactBoard(@Body body: ReactionBody): Board

    @DELETE("api/board/reaction")
    suspend fun unreactBoard(@Query("version") version: Int): Board

    @PUT("api/board/comment")
    suspend fun commentBoard(@Body body: BoardCommentBody): Board

    @DELETE("api/board/comment")
    suspend fun uncommentBoard(@Query("version") version: Int): Board

    @POST("api/board/seen")
    suspend fun boardSeen(@Body body: VersionBody)

    // Letters and the draft
    @GET("api/draft")
    suspend fun draft(): Draft

    @PUT("api/draft")
    suspend fun saveDraft(@Body body: SaveDraftBody): Saved

    @DELETE("api/draft/attachments/{id}")
    suspend fun deleteDraftAttachment(@Path("id") id: String)

    @POST("api/letters")
    suspend fun send(@Body body: DraftVersionBody): Sent

    @GET("api/letters/{id}")
    suspend fun letter(@Path("id") id: Long): LetterFull

    @GET("api/letters")
    suspend fun letters(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null, @Query("direction") direction: String? = null): LetterPage

    @DELETE("api/letters/{id}")
    suspend fun cancelLetter(@Path("id") id: Long): DraftVersion

    @PUT("api/letters/{id}")
    suspend fun sendAgain(@Path("id") id: Long, @Body body: SendAgainBody): LetterFull

    @DELETE("api/letters/{id}/edit")
    suspend fun discardEdit(@Path("id") id: Long)

    @POST("api/letters/{id}/edit/keep-in-draft")
    suspend fun keepInDraft(@Path("id") id: Long, @Body body: BodyOnly): DraftVersion

    @GET("api/stamps")
    suspend fun stamps(): StampCatalogue

    // Discussions
    @GET("api/discussions")
    suspend fun discussions(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null, @Query("archived") archived: Boolean? = null): DPage

    @GET("api/discussions/{id}")
    suspend fun discussion(@Path("id") id: Long): DFull

    @GET("api/discussions/{id}/comments")
    suspend fun comments(@Path("id") id: Long, @Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null, @Query("from") from: String? = null): DComments

    @POST("api/discussions/{id}/comments")
    suspend fun comment(@Path("id") id: Long, @Body body: TextBody): DComment

    @PATCH("api/discussions/{id}/comments/{commentId}")
    suspend fun editComment(@Path("id") id: Long, @Path("commentId") commentId: Long, @Body body: TextBody): DComment

    @DELETE("api/discussions/{id}/comments/{commentId}")
    suspend fun deleteComment(@Path("id") id: Long, @Path("commentId") commentId: Long)

    @POST("api/discussions/{id}/seen")
    suspend fun discussionSeen(@Path("id") id: Long)

    /** Cancel inside the window and Delete at any time: the same call. */
    @DELETE("api/discussions/{id}")
    suspend fun deleteDiscussion(@Path("id") id: Long)

    @PUT("api/discussions/{id}")
    suspend fun editDiscussion(@Path("id") id: Long, @Body body: EditDiscussionBody): DFull

    @DELETE("api/discussions/{id}/edit")
    suspend fun discardDiscussionEdit(@Path("id") id: Long)

    @POST("api/discussions/{id}/archive")
    suspend fun archive(@Path("id") id: Long): DFull

    @DELETE("api/discussions/{id}/archive")
    suspend fun unarchive(@Path("id") id: Long): DFull

    @GET("api/discussions/draft")
    suspend fun discussionDraft(): DDraft

    @PUT("api/discussions/draft")
    suspend fun saveDiscussionDraft(@Body body: SaveDiscussionDraftBody): Saved

    @DELETE("api/discussions/draft/attachments/{id}")
    suspend fun deleteDiscussionDraftAttachment(@Path("id") id: String)

    @POST("api/discussions")
    suspend fun post(@Body body: DraftVersionBody): DPosted

    // Emails (key = "new" or a thread id)
    @GET("api/emails/threads")
    suspend fun threads(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int? = null): ThreadPage

    @GET("api/emails/threads/{id}")
    suspend fun thread(@Path("id") id: Long): ThreadFull

    @GET("api/emails/threads/{id}/emails")
    suspend fun olderEmails(@Path("id") id: Long, @Query("cursor") cursor: String, @Query("limit") limit: Int = 20): EmailList

    @POST("api/emails/threads/{id}/seen")
    suspend fun threadSeen(@Path("id") id: Long)

    @GET("api/emails/credits")
    suspend fun credits(): EmailCredits

    @GET("api/emails/drafts/{key}")
    suspend fun emailDraft(@Path("key") key: String): EmailDraft

    @PUT("api/emails/drafts/{key}")
    suspend fun saveEmailDraft(@Path("key") key: String, @Body body: SaveEmailDraftBody): Saved

    @DELETE("api/emails/drafts/{key}/attachments/{id}")
    suspend fun deleteEmailDraftAttachment(@Path("key") key: String, @Path("id") id: String)

    @POST("api/emails/threads")
    suspend fun sendNewEmail(@Body body: DraftVersionBody): EmailSent

    @POST("api/emails/threads/{id}/emails")
    suspend fun reply(@Path("id") id: Long, @Body body: DraftVersionBody): EmailSent

    // Reactions: target = discussions | comments | emails
    @PUT("api/reactions/{target}/{id}")
    suspend fun react(@Path("target") target: String, @Path("id") id: Long, @Body body: EmojiBody): Reactions

    @HTTP(method = "DELETE", path = "api/reactions/{target}/{id}", hasBody = true)
    suspend fun unreact(@Path("target") target: String, @Path("id") id: Long, @Body body: EmojiBody): Reactions

    // Profile and settings
    @GET("api/me/profile")
    suspend fun profile(): Profile

    @PUT("api/me/profile")
    suspend fun saveProfile(@Body body: ProfileBody): Profile

    @Multipart
    @POST("api/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Profile

    @DELETE("api/me/avatar")
    suspend fun removeAvatar(): Profile

    @GET("api/me/notifications")
    suspend fun notifications(): NotifySettings

    @PUT("api/me/notifications")
    suspend fun saveNotifications(@Body body: NotifyChange): NotifySettings
}
