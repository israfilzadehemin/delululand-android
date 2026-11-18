package com.delulutopia.core.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration

/**
 * Links in what citizens write (doc "Delululand — Links", web text/links.ts): `http://`, `https://`
 * and `www.` runs become links when shown; nothing is stored. Bare domains, emails and phone numbers
 * stay text. Trailing `. , ! ? : ; ' "` and an unbalanced closing bracket stay outside the link.
 */
object Links {
    data class Segment(val text: String, val href: String? = null)

    private val CANDIDATE = Regex("\\b(?:https?://|www\\.)[^\\s<>]+", RegexOption.IGNORE_CASE)
    private val HOST = Regex("^https?://(?:[^/?#@]*@)?([^/?#:]+)", RegexOption.IGNORE_CASE)
    private const val TRAILING = ".,!?:;'\"’”»…"
    private val PAIRS = mapOf(')' to '(', ']' to '[', '}' to '{')

    private fun trim(raw: String): String {
        var s = raw
        while (s.isNotEmpty()) {
            val last = s.last()
            val open = PAIRS[last]
            s = when {
                last in TRAILING -> s.dropLast(1)
                open != null && s.count { it == last } > s.count { it == open } -> s.dropLast(1)
                else -> return s
            }
        }
        return s
    }

    /** The URL a link opens: `www.` gets `https://`; only http(s) with a dotted host is opened (LK5). */
    fun hrefOf(text: String): String? {
        val url = if (text.startsWith("www.", ignoreCase = true)) "https://$text" else text
        val host = HOST.find(url)?.groupValues?.get(1) ?: return null
        return if (host.contains('.') && !host.startsWith('.') && !host.endsWith('.')) url else null
    }

    /** Plain and link segments in order; joining every `text` gives the input back. */
    fun linkify(input: String): List<Segment> {
        val out = ArrayList<Segment>()
        var at = 0
        for (m in CANDIDATE.findAll(input)) {
            val start = m.range.first
            if (start > 0 && (input[start - 1] in "/@.-" || input[start - 1].isLetterOrDigit() || input[start - 1] == '_')) continue
            val text = trim(m.value)
            val href = hrefOf(text) ?: continue
            if (start > at) out += Segment(input.substring(at, start))
            out += Segment(text, href)
            at = start + text.length
        }
        if (at < input.length || out.isEmpty()) out += Segment(input.substring(at))
        return out
    }

    fun hasLink(input: String): Boolean = linkify(input).any { it.href != null }
}

/** LK6: the surrounding ink, underlined. */
private val LINK_STYLES = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))

/** [this] with its links made tappable; marks and the reveal's ink colours stay as they are. */
fun AnnotatedString.withLinks(open: (String) -> Unit): AnnotatedString {
    val segments = Links.linkify(text)
    if (segments.none { it.href != null }) return this
    return AnnotatedString.Builder(this).apply {
        var at = 0
        segments.forEach { s ->
            val href = s.href
            if (href != null) addLink(LinkAnnotation.Url(href, LINK_STYLES) { open(href) }, at, at + s.text.length)
            at += s.text.length
        }
    }.toAnnotatedString()
}

/**
 * LK4: a Custom Tab over the app (the browser's own in-app view, no library needed: the session
 * extra is what makes it one), else whatever browser is there.
 */
fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        putExtras(Bundle().apply { putBinder("android.support.customtabs.extra.SESSION", null) })
        putExtra("android.support.customtabs.extra.SHARE_STATE", 2)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No browser at all: the link stays text.
    }
}

@Composable
fun rememberLinkOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url: String -> openLink(context, url) } }
}

/** Plain text (comments, emails, the board note) with its links tappable. */
@Composable
fun LinkedText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val open = rememberLinkOpener()
    val linked = remember(text, open) { AnnotatedString(text).withLinks(open) }
    Text(linked, modifier, style = style, color = color)
}
