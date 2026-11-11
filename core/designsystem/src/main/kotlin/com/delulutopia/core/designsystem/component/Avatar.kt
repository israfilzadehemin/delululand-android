package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.theme.Manrope

/**
 * A citizen's picture: initials on their signature colour (navy ink, 800 weight), the fallback the
 * canvas draws until a photo loads. Photos are layered on top by callers through [content] once
 * image loading arrives (P1). [ring] draws a border of that colour, e.g. the signature ring on Home.
 */
@Composable
fun DlAvatar(
    initials: String,
    signature: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    ring: Color? = null,
    ringWidth: Dp = 2.dp,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(signature),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) {
            content()
        } else {
            Text(
                text = initials,
                color = InitialsInk,
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.375f).sp,
                letterSpacing = 0.02.em,
                maxLines = 1,
            )
        }
        // Drawn last so a photo never covers the signature ring.
        if (ring != null) Box(Modifier.matchParentSize().border(ringWidth, ring, CircleShape))
    }
}

/** Initials are navy in both themes: signature colours stay light in dark mode. */
private val InitialsInk = Color(0xFF202B45)

/** "Emin Israfilzade" → "EI", "Leyla" → "L". */
fun initialsOf(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    .take(2).joinToString("") { it.first().uppercase() }
