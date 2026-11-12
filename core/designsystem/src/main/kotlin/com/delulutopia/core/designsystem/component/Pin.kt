package com.delulutopia.core.designsystem.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.motion.dotFill
import com.delulutopia.core.designsystem.motion.pressScale
import com.delulutopia.core.designsystem.text.Ltr
import com.delulutopia.core.designsystem.theme.Manrope
import com.delulutopia.core.i18n.localDigits
import com.delulutopia.core.i18n.t

const val PIN_LENGTH = 6

/** Six 14 dp dots, 16 dp apart: outlined, filled as digits arrive, red outlines on an error (S-PIN). */
@Composable
fun DlPinDots(count: Int, label: String, modifier: Modifier = Modifier, error: Boolean = false) {
    val ink = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(modifier.semantics { contentDescription = label }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PIN_LENGTH) { i ->
            Box(
                Modifier
                    .dotFill(i < count && !error)
                    .size(14.dp)
                    .border(1.5.dp, ink, CircleShape)
                    .background(if (i < count && !error) ink else Color.Transparent, CircleShape),
            )
        }
    }
}

/**
 * The PIN keypad (S-PIN): 3 × 4 rounded keys, 56 dp tall, no letters; bottom left the fingerprint
 * key when [onBiometric] is set (P8), bottom right backspace. Each key ticks (P9). The sixth digit
 * calls [onComplete] with the PIN; [digits] is owned by the caller so nothing outlives an attempt.
 */
@Composable
fun DlPinPad(
    digits: String,
    onDigits: (String) -> Unit,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onBiometric: (() -> Unit)? = null,
    backspaceDescription: String = t("common.backspace"),
    biometricDescription: String = t("pin.unlockWithFingerprint"),
) {
    // L3: a phone keypad stays 1 2 3 from the left in Persian; its digits are Persian (L4).
    Ltr {
        DlPinPadContent(
            digits = digits,
            onDigits = onDigits,
            onComplete = onComplete,
            modifier = modifier,
            enabled = enabled,
            onBiometric = onBiometric,
            backspaceDescription = backspaceDescription,
            biometricDescription = biometricDescription,
        )
    }
}

@Composable
private fun DlPinPadContent(
    digits: String,
    onDigits: (String) -> Unit,
    onComplete: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    onBiometric: (() -> Unit)?,
    backspaceDescription: String,
    biometricDescription: String,
) {
    val view = LocalView.current
    // Two taps inside one frame must both count: each tap builds on the last tap, not on the
    // [digits] of the last composition (which a fast thumb outruns). [digits] still wins when the
    // caller changes it itself: a reset after six digits, or anything that isn't the typing lagging behind.
    val typed = remember { StringBuilder(digits) }
    SideEffect {
        val now = typed.toString()
        if (now != digits && (now.length == PIN_LENGTH || !now.startsWith(digits))) typed.replace(0, typed.length, digits)
    }
    val press: (Char) -> Unit = { d ->
        if (enabled && typed.length < PIN_LENGTH) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            typed.append(d)
            val next = typed.toString()
            onDigits(next)
            if (next.length == PIN_LENGTH) onComplete(next)
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { d -> PinKey(Modifier.weight(1f), enabled, onClick = { press(d) }) { KeyLabel(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBiometric != null) {
                PinKey(Modifier.weight(1f), enabled, plain = true, onClick = onBiometric, description = biometricDescription) {
                    Icon(DlIcons.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
            } else {
                Box(Modifier.weight(1f))
            }
            PinKey(Modifier.weight(1f), enabled, onClick = { press('0') }) { KeyLabel('0') }
            PinKey(
                Modifier.weight(1f),
                enabled,
                onClick = {
                    if (typed.isNotEmpty()) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        typed.setLength(typed.length - 1)
                        onDigits(typed.toString())
                    }
                },
                description = backspaceDescription,
            ) { Icon(DlIcons.Backspace, contentDescription = null, modifier = Modifier.size(26.dp)) }
        }
    }
}

@Composable
private fun KeyLabel(d: Char) {
    Text(localDigits(d.toString()), fontFamily = Manrope, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PinKey(
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
    plain: Boolean = false,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val press = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = press,
        modifier = modifier
            .pressScale(press, 0.9f)
            .height(56.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = if (plain) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (plain) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}

/** Wizard progress (Create: 5, Join: 2): 8 dp dots, 6 dp apart, navy up to the current step. */
@Composable
fun DlStepDots(step: Int, of: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(end = 16.dp)
            .semantics { contentDescription = t("common.stepOf", "step" to step, "total" to of) },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(of) { i ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (i < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
    }
}
