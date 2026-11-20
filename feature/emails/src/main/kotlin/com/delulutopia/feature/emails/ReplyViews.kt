package com.delulutopia.feature.emails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.EmailQuote
import com.delulutopia.core.ui.Who
import com.delulutopia.core.ui.media.ExpiredThumb
import com.delulutopia.core.ui.whoOf

/**
 * Email replies (doc "Delululand — Email Replies"; web emails/reply.tsx): the quote inside a bubble
 * (RE11) and the strip in the reply bar (RE9). The edge and the name wear the quoted sender's
 * signature, darkened so they read on either bubble tint (doc §1, "Colours").
 */
internal data class QuoteColours(val edge: Color, val name: Color)

@Composable
@ReadOnlyComposable
internal fun quoteColours(senderId: String): QuoteColours {
    val a = whoOf(senderId) == Who.A
    return if (DlTheme.isDark) {
        if (a) QuoteColours(Color(0xFFD99A82), Color(0xFFEBB9A5)) else QuoteColours(Color(0xFF9CBDB3), Color(0xFFAED3C7))
    } else {
        if (a) QuoteColours(Color(0xFFC9785E), Color(0xFF8E4730)) else QuoteColours(Color(0xFF6E9C8E), Color(0xFF35685A))
    }
}

/** RE11: at the top inside the bubble; a tap jumps to the original (RE12). */
@Composable
internal fun BubbleQuote(quote: EmailQuote, name: String, onOpen: () -> Unit) {
    val colours = quoteColours(quote.senderId)
    val fill = if (DlTheme.isDark) Color.Black.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.62f)
    val label = t("emails.goToOriginal", "name" to name)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(fill)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(colours.edge))
        QuoteLines(quote, name, colours, Modifier.weight(1f).padding(start = 8.dp, top = 5.dp, bottom = 5.dp, end = 8.dp), maxLines = 2)
        QuoteThumb(quote, Modifier.padding(end = 6.dp))
    }
}

/** RE9: above the counter line of the reply bar, with × to drop the target. */
@Composable
internal fun ReplyStrip(quote: EmailQuote, name: String, onCancel: () -> Unit) {
    val colours = quoteColours(quote.senderId)
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val heading = t("emails.replyingTo", "name" to name)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(DlTheme.colors.letterEnvelope),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(colours.edge))
        Icon(WebIcons.Reply, null, Modifier.padding(start = 10.dp).size(16.dp), tint = quiet)
        QuoteLines(
            quote,
            heading,
            colours,
            Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            maxLines = 1,
        )
        QuoteThumb(quote, Modifier)
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onCancel)
                .semantics { contentDescription = t("emails.cancelReply") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(WebIcons.Close, null, Modifier.size(16.dp), tint = quiet)
        }
    }
}

@Composable
private fun QuoteLines(quote: EmailQuote, heading: String, colours: QuoteColours, modifier: Modifier, maxLines: Int) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            heading,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.ExtraBold),
            color = colours.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            quoteLine(quote),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuoteThumb(quote: EmailQuote, modifier: Modifier) {
    if (quote.thumbExpired) {
        ExpiredThumb(size = 36.dp, modifier = modifier)
        return
    }
    val thumb = quote.thumbUrl ?: return
    val base = LocalServerBaseUrl.current
    AsyncImage(
        model = resolveMedia(base, thumb),
        contentDescription = null,
        modifier = modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
        contentScale = ContentScale.Crop,
    )
}
