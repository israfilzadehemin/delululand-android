package com.delulutopia.feature.emails

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Email

/**
 * Read ticks (doc "Delululand — Read Ticks" TK5): ✓ sent (saved on the server), ✓✓ delivered (the
 * friend's app has it), ✓✓ in the read colour once read. Only my own emails carry them.
 */
enum class TickStatus(val wire: String) {
    Sent("sent"),
    Delivered("delivered"),
    Read("read"),
    ;

    /** "14:16, read" for TalkBack. */
    fun label(time: String): String = t("emails.status.${wire}Label", "time" to time)

    companion object {
        fun of(e: Email): TickStatus = when {
            e.readAt != null -> Read
            e.deliveredAt != null -> Delivered
            else -> Sent
        }

        fun ofWire(s: String?): TickStatus? = entries.firstOrNull { it.wire == s }
    }
}

/** TK4: `#1F6FB2` light, `#7DB8F0` dark; on a media chip a light blue that reads on the dark chip. */
@Composable
fun tickReadColor(onMedia: Boolean = false): Color = when {
    onMedia -> Color(0xFF9FD0FF)
    DlTheme.isDark -> Color(0xFF7DB8F0)
    else -> Color(0xFF1F6FB2)
}

/** The glyph only (16 × 12 dp, ✓ 12 × 12): the time beside it carries the words. */
@Composable
fun ReadTicks(status: TickStatus, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, onMedia: Boolean = false) {
    val color = if (status == TickStatus.Read) tickReadColor(onMedia) else tint
    val single = status == TickStatus.Sent
    Canvas(modifier.size(width = if (single) 12.dp else 16.dp, height = 12.dp)) {
        // The web's viewBoxes: 16 × 12 for ✓, 20 × 12 for ✓✓.
        val sx = size.width / (if (single) 16f else 20f)
        val sy = size.height / 12f
        fun line(vararg pts: Pair<Float, Float>) = Path().apply {
            pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * sx, y * sy) else lineTo(x * sx, y * sy) }
        }
        val stroke = Stroke(width = 1.8f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (single) {
            drawPath(line(2.5f to 6.5f, 5.7f to 9.7f, 13.5f to 2f), color, style = stroke)
        } else {
            drawPath(line(1.5f to 6.5f, 4.7f to 9.7f, 12.5f to 2f), color, style = stroke)
            drawPath(line(9.2f to 9f, 9.9f to 9.7f, 18f to 2f), color, style = stroke)
        }
    }
}

/** TK6, TK7: rows in the delivery card above the quick row; mine list each state reached, the friend's Sent only. */
fun deliveryRows(e: Email, mine: Boolean): List<Pair<TickStatus, String>> = buildList {
    add(TickStatus.Sent to e.sentAt)
    if (mine) {
        e.deliveredAt?.let { add(TickStatus.Delivered to it) }
        e.readAt?.let { add(TickStatus.Read to it) }
    }
}

@Composable
fun DeliveryCard(e: Email, mine: Boolean, nowMs: Long, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val label = t("emails.deliveryInfo")
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .width(220.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(DlTheme.colors.letterEnvelope)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
    ) {
        deliveryRows(e, mine).forEach { (status, at) ->
            Row(Modifier.heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mine) Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) { ReadTicks(status, tint = quiet) }
                Text(
                    t("emails.status.${status.wire}"),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    Time.clock(at, nowMs),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontFeatureSettings = "tnum"),
                    color = quiet,
                )
            }
        }
    }
}
