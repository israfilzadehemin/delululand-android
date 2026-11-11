package com.delulutopia.feature.start

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlPinDots
import com.delulutopia.core.designsystem.component.DlPinPad
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.motion.pressScale
import com.delulutopia.core.designsystem.motion.shake
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.Literata
import com.delulutopia.core.network.model.UserCard

/** First citizen of a universe wears terracotta, the second sage (signature colours, canvas Z-Tokens). */
@Composable
internal fun signatureOf(index: Int): Color = if (index == 0) DlTheme.colors.userA else DlTheme.colors.userB

@Composable
internal fun signatureTintOf(index: Int): Color = if (index == 0) DlTheme.colors.userATint else DlTheme.colors.userBTint

/** UV-3: "Welcome to <name>" with the universe's paragraphs; Enter only (Close for the creator's preview). */
@Composable
fun WelcomeDialog(name: String, welcome: String, onEnter: () -> Unit, preview: Boolean = false) {
    AlertDialog(
        onDismissRequest = { if (preview) onEnter() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.welcome_to, name), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Rules.paragraphs(welcome).forEach { p ->
                    Text(p, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal))
                }
            }
        },
        confirmButton = {
            DlFilledButton(
                stringResource(if (preview) R.string.close else R.string.enter),
                onEnter,
                Modifier.fillMaxWidth(),
            )
        },
    )
}

/** A user card on the cards screen (S-Cards): 88 dp picture on the signature tint, name, "Tap to …". */
@Composable
internal fun UserCardRow(user: UserCard, index: Int, onClick: () -> Unit) {
    CardFrame(onClick) {
        DlRemoteAvatar(
            avatarUrl = user.avatarUrl,
            name = user.nickname,
            initials = user.nickname.take(1).uppercase(),
            signature = signatureTintOf(index),
            initialsColor = MaterialTheme.colorScheme.onSurface,
            ring = signatureOf(index),
            size = 88.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(user.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(if (user.hasPin) R.string.tap_to_enter else R.string.tap_to_set_pin),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Join <name> · Create your citizen" with the dashed + mark (S-Cards-join). */
@Composable
internal fun JoinCardRow(name: String, onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    CardFrame(onClick) {
        Box(
            Modifier
                .size(64.dp)
                .background(Color.Transparent, CircleShape)
                .drawDashedCircle(outline),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DlIcons.Plus, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.join_universe, name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.create_your_citizen), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.drawDashedCircle(color: Color) = drawBehind {
    drawCircle(
        color,
        radius = size.minDimension / 2 - 1.dp.toPx(),
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}

@Composable
private fun CardFrame(onClick: () -> Unit, content: @Composable () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(press, 0.98f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(interactionSource = press, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** The universe's name on the cards screen: Literata 30/600, centred. */
@Composable
internal fun UniverseTitle(name: String, modifier: Modifier = Modifier) {
    Text(
        name,
        modifier = modifier.fillMaxWidth(),
        fontFamily = Literata,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

/**
 * The PIN layout shared by S1 and the wizards' PIN step: [header] (picture and name, or "who"),
 * prompt, dots, the error or hint line, space, then the keypad at the bottom.
 */
@Composable
internal fun ColumnScope.PinPanel(
    header: @Composable () -> Unit,
    prompt: String,
    dotsLabel: String,
    digits: String,
    error: String?,
    hint: String?,
    busy: Boolean,
    onDigits: (String) -> Unit,
    onComplete: (String) -> Unit,
    onFingerprint: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.enter(Enter.Rise, 60, 340)) { header() }
        Text(
            prompt,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        // A wrong PIN shakes the dots (dl-shake); the line under them fades in.
        DlPinDots(count = digits.length, label = dotsLabel, error = error != null, modifier = Modifier.shake(error))
        Text(
            error ?: hint.orEmpty(),
            modifier = Modifier.heightIn(min = 20.dp).padding(top = 4.dp).enter(Enter.Fade, durationMs = 200, key = error ?: hint),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.weight(1f))
    DlPinPad(
        digits = digits,
        onDigits = onDigits,
        onComplete = onComplete,
        enabled = !busy,
        onBiometric = onFingerprint,
        backspaceDescription = stringResource(R.string.backspace),
        modifier = Modifier.padding(bottom = 20.dp),
    )
}
