package com.delulutopia.core.ui.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.i18n.t
import com.delulutopia.core.i18n.tn

/*
 * Doc "Delululand — Attachment Retention" (canvas of the same name): what stands in for a file the
 * retention job removed (AR6), the line under a media block that lost files (AR15), and the compose
 * notice under every attachment strip (AR13, AR14).
 */

/** Which retention a strip's notice names (AR13): letters, or discussions and emails. */
enum class RetentionScope { Letter, Other }

/**
 * AR10, AR14: the days from `GET /api/config` `retention`, set once at start (MainViewModel). Null
 * against an older API: then no notice or caption shows.
 */
object Retention {
    var letterDays by mutableStateOf<Int?>(null)
    var otherDays by mutableStateOf<Int?>(null)

    fun days(scope: RetentionScope): Int? = if (scope == RetentionScope.Letter) letterDays else otherDays
}

/** AR6: the words a placeholder says, by kind. */
fun expiredLabel(kind: String): String = when (kind) {
    "video" -> t("media.videoExpired")
    "doodle" -> t("media.doodleExpired")
    else -> t("media.photoExpired")
}

private fun kindIcon(kind: String): ImageVector = when (kind) {
    "video" -> WebIcons.Video
    "audio" -> WebIcons.Mic
    "doodle" -> WebIcons.Doodle
    else -> WebIcons.Photo
}

/** The kind's icon with a slash across it. */
@Composable
fun ExpiredIcon(kind: String, size: Dp = 20.dp, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(Modifier.size(size)) {
        Icon(kindIcon(kind), null, Modifier.matchParentSize(), tint = tint)
        Canvas(Modifier.matchParentSize()) {
            val u = this.size.width / 24f
            drawLine(tint, Offset(3.5f * u, 3.5f * u), Offset(20.5f * u, 20.5f * u), strokeWidth = 1.7f * u, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun expiredFill(): Color = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.45f)

/** The dashed outline every placeholder shares. */
private fun Modifier.expiredFrame(shape: Shape, radius: Dp, line: Color, fill: Color): Modifier = this
    .clip(shape)
    .background(fill)
    .drawBehind {
        drawRoundRect(
            line,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
    }

@Composable
private fun frameLine(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

/** A thumbnail slot (64 px prints, 36 px quote thumbs): the crossed-out icon alone (AR6, AR9). */
@Composable
fun ExpiredThumb(kind: String = "photo", size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val label = expiredLabel(kind)
    val radius = if (size < 48.dp) 6.dp else 8.dp
    Box(
        modifier
            .size(size)
            .expiredFrame(RoundedCornerShape(radius), radius, frameLine(), expiredFill())
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Image
            },
        contentAlignment = Alignment.Center,
    ) {
        ExpiredIcon(kind, if (size < 48.dp) 16.dp else 18.dp)
    }
}

/** A larger slot (the doodle, a video): icon and words (AR6). */
@Composable
fun ExpiredBlock(kind: String, modifier: Modifier = Modifier, minHeight: Dp = 96.dp) {
    val label = expiredLabel(kind)
    Column(
        modifier
            .heightIn(min = minHeight)
            .expiredFrame(RoundedCornerShape(10.dp), 10.dp, frameLine(), expiredFill())
            .padding(12.dp)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Image
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        ExpiredIcon(kind)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** A voice note keeps its row and its length (AR6). */
@Composable
fun ExpiredVoice(durationMs: Long?, modifier: Modifier = Modifier) {
    val text = t("media.voiceExpired", "time" to mmss(durationMs ?: 0))
    Row(
        modifier.heightIn(min = 36.dp).clearAndSetSemantics {
            contentDescription = text
            role = Role.Image
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(32.dp).expiredFrame(CircleShape, 16.dp, frameLine(), expiredFill()), contentAlignment = Alignment.Center) {
            ExpiredIcon("audio", 16.dp)
        }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** An hourglass, as on the canvas (stroke 1.9 on 24). */
val Hourglass: ImageVector by lazy {
    val b = ImageVector.Builder("Hourglass", 24.dp, 24.dp, 24f, 24f)
    listOf("M7 3h10M7 21h10", "M8 3c0 4.5 8 4.5 8 9s-8 4.5-8 9", "M16 3c0 4.5-8 4.5-8 9s8 4.5 8 9").forEach {
        b.addPath(
            addPathNodes(it),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    b.build()
}

/** AR13, AR14: "Attachments are kept for N days" under the strip, above the tier line. Null days: nothing (older API). */
@Composable
fun RetentionNotice(days: Int?, modifier: Modifier = Modifier) {
    if (days == null) return
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Hourglass, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            tn("retention.kept", days),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** AR15: under a media block that holds expired files. */
@Composable
fun RetentionCaption(days: Int?, modifier: Modifier = Modifier, colour: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    if (days == null) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Hourglass, null, Modifier.size(13.dp), tint = colour)
        Text(tn("retention.removed", days), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = colour)
    }
}
