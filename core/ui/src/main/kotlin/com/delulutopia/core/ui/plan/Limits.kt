package com.delulutopia.core.ui.plan

import com.delulutopia.core.data.Time
import com.delulutopia.core.i18n.t
import com.delulutopia.core.i18n.tn
import com.delulutopia.core.network.model.Plan
import com.delulutopia.core.ui.media.Item

/**
 * What a composer may still add and what it must say (doc "Delululand — Subscription Tiers"
 * CM-1–CM-8, ST6, ST8, ST13, ST16; web src/plan/limits.ts).
 *
 * [photos] counts every photo in the message (the per-message limit); [newPhotos] those that spend
 * today's allowance at send (an edit spends only what it adds, ST6); [videos] those that spend at send.
 */
data class MessageMedia(val photos: Int, val newPhotos: Int = photos, val videos: Int = 0)

sealed interface LimitLine {
    data class PerMessage(val n: Int) : LimitLine
    data class PhotosLeft(val n: Int) : LimitLine
    data object NoPhotos : LimitLine
    data object NoMorePhotos : LimitLine
    data class VideosLeft(val n: Int) : LimitLine
    data object NoVideos : LimitLine
}

sealed interface LimitBlock {
    data class OverMessage(val n: Int, val remove: Int) : LimitBlock
    data class PhotosOver(val left: Int, val remove: Int) : LimitBlock
    data object VideosOver : LimitBlock
}

data class StripLimits(
    val photoRoom: Int,
    val videoRoom: Boolean,
    val photosPerMessage: Int,
    val lines: List<LimitLine>,
    val block: LimitBlock?,
    val renewsAt: String,
) {
    private val time: String get() = Time.time(renewsAt)

    /** CM-5: one line joined by " · ", per-message first, "renews at" once for the daily parts. */
    val line: String?
        get() {
            if (lines.isEmpty()) return null
            val parts = lines.map {
                when (it) {
                    is LimitLine.PerMessage -> t("limits.photosPerMessage", "n" to it.n)
                    is LimitLine.PhotosLeft -> tn("limits.photosLeft", it.n)
                    is LimitLine.VideosLeft -> tn("limits.videosLeft", it.n)
                    LimitLine.NoPhotos -> t("limits.noPhotos")
                    LimitLine.NoMorePhotos -> t("limits.noMorePhotos")
                    LimitLine.NoVideos -> t("limits.noVideos")
                }
            }
            val daily = lines.any { it !is LimitLine.PerMessage }
            return (parts + if (daily) listOf(t("limits.renews", "time" to time)) else emptyList()).joinToString(" · ")
        }

    val lineIsZero: Boolean get() = lines.any { it == LimitLine.NoPhotos || it == LimitLine.NoVideos }

    /** CM-7, CM-8: the banner above Send; Send stays off while it shows (ST13). */
    val blockText: String?
        get() = when (val b = block) {
            null -> null
            is LimitBlock.OverMessage -> t("limits.overMessage", "n" to b.n, "remove" to b.remove)
            is LimitBlock.PhotosOver ->
                if (b.left == 0) t("limits.photosNone", "time" to time) else tn("limits.photosOver", b.left, "remove" to b.remove, "time" to time)
            LimitBlock.VideosOver -> t("limits.videosNone", "time" to time)
        }

    /** CM-2: "Added 3 of 5 · 5 photos per message". */
    fun addedNotice(added: Int, picked: Int, inMessage: Int): String {
        val why = if (inMessage + added >= photosPerMessage) t("limits.photosPerMessage", "n" to photosPerMessage) else t("limits.noMorePhotos")
        return t("limits.added", "added" to added, "picked" to picked) + " · " + why
    }
}

const val LOW = 2

fun stripLimits(plan: Plan?, media: MessageMedia): StripLimits? {
    plan ?: return null
    val limits = plan.limits
    val left = plan.left
    val photosAfter = left.photos - media.newPhotos
    val lines = mutableListOf<LimitLine>()
    if (media.photos == limits.photosPerMessage) lines += LimitLine.PerMessage(limits.photosPerMessage)
    when {
        left.photos == 0 && media.newPhotos == 0 -> lines += LimitLine.NoPhotos
        photosAfter == 0 -> lines += LimitLine.NoMorePhotos
        photosAfter in 1..LOW -> lines += LimitLine.PhotosLeft(photosAfter)
    }
    var block: LimitBlock? = when {
        media.photos > limits.photosPerMessage -> LimitBlock.OverMessage(limits.photosPerMessage, media.photos - limits.photosPerMessage)
        media.newPhotos > left.photos -> LimitBlock.PhotosOver(left.photos, media.newPhotos - left.photos)
        else -> null
    }
    when {
        media.videos > left.videos -> if (block == null) block = LimitBlock.VideosOver
        media.videos == 0 && left.videos == 0 -> lines += LimitLine.NoVideos
        media.videos == 0 && left.videos <= LOW -> lines += LimitLine.VideosLeft(left.videos)
    }
    return StripLimits(
        photoRoom = maxOf(0, minOf(limits.photosPerMessage - media.photos, photosAfter)),
        videoRoom = left.videos - media.videos > 0,
        photosPerMessage = limits.photosPerMessage,
        lines = lines,
        block = block,
        renewsAt = plan.renewsAt,
    )
}

/** What the strip's items carry; [original] names attachments the message already had (an edit spends none of them). */
fun mediaOf(items: List<Item>, original: Set<String> = emptySet()): MessageMedia {
    var photos = 0
    var newPhotos = 0
    var videos = 0
    for (it in items) {
        when (it.kind) {
            "photo" -> {
                photos++
                if (it.att?.id?.let { id -> id in original } != true) newPhotos++
            }
            "video" -> videos++
        }
    }
    return MessageMedia(photos, newPhotos, videos)
}
