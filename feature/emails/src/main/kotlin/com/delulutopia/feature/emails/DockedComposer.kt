package com.delulutopia.feature.emails

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.motion.Motion
import com.delulutopia.core.designsystem.motion.motionOn
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.Uploader
import com.delulutopia.core.network.model.EmailCredits
import com.delulutopia.core.network.model.EmailQuote
import com.delulutopia.core.network.model.EmailSent
import com.delulutopia.core.ui.media.AttachmentStrip
import com.delulutopia.core.ui.media.RetentionScope
import com.delulutopia.core.ui.media.Source
import com.delulutopia.core.ui.media.VIDEO_TOO_LONG
import com.delulutopia.core.ui.media.VideoAdd
import com.delulutopia.core.ui.media.rememberVoiceRecording
import com.delulutopia.core.ui.media.tooLong
import com.delulutopia.core.ui.media.videoMs
import com.delulutopia.core.ui.plan.mediaOf
import com.delulutopia.core.ui.plan.stripLimits
import kotlinx.coroutines.launch

/**
 * The thread's reply bar (doc "Delululand — Email Bubbles" EB8; web ThreadView .dl-dock): one line
 * docked at the bottom — + · the field · Send — with the credits line above it. + opens the tray
 * (attachments and their buttons) by height and turns into ×; while the bar is in use the counter
 * joins the credits line; the field grows to six lines. All motion off = the same bar, static.
 */
@Composable
internal fun DockedComposer(
    composer: EmailComposer,
    credits: EmailCredits?,
    quoteName: (String) -> String = { "" },
    onSent: (EmailSent) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = DlTheme.colors
    val line = MaterialTheme.colorScheme.outlineVariant
    val bar = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .drawBehind { drawLine(line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
    if (composer.failed) {
        Box(bar) { Text(t("emails.couldntLoadYourDraftCheck"), color = MaterialTheme.colorScheme.error) }
        return
    }
    val atts = composer.attachments
    if (!composer.loaded || atts == null) {
        Box(bar.heightIn(min = 52.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
        return
    }
    val saveState by composer.saver.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { composer.saver.flushOnHide() }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var trayAsked by remember { mutableStateOf(false) }
    val focus = remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focused by focus.collectIsFocusedAsState()
    // Email Replies RE8: a picked reply puts the cursor in the field (unlike +, EB15).
    val field = remember { FocusRequester() }
    LaunchedEffect(composer.focusAsk) {
        if (composer.focusAsk > 0) {
            runCatching { field.requestFocus() }
            keyboard?.show()
        }
    }
    val voice = rememberVoiceRecording { bytes, ms -> atts.add(Uploader.Kind.Audio, Source.Made(bytes, "audio/mp4"), "voice.m4a", ms) }
    val addVideo = { uri: Uri, ms: Long? ->
        if (tooLong(ms ?: videoMs(context, uri))) {
            videoError = VIDEO_TOO_LONG
        } else {
            atts.add(Uploader.Kind.Video, Source.Picked(uri), "video.mp4", ms)
        }
    }
    val length = count(normalize(composer.text))
    // OS11: the credits line drops at once while an email is in the air.
    val inAir = composer.inAir
    val shown = if (credits != null && inAir > 0) credits.copy(left = maxOf(0, credits.left - inAir), used = credits.used + inAir) else credits
    val held = composer.held
    val outOfCredits = shown != null && shown.left <= 0
    val something = composer.text.isNotBlank() || atts.items.isNotEmpty()
    val homeState by composer.homeState.collectAsStateWithLifecycle()
    val limits = stripLimits(homeState.home?.plan, mediaOf(atts.items))
    val canSend = something && !atts.uploading && !voice.recording && !outOfCredits && !sending && limits?.block == null && !held
    val saved = when {
        saveState.status == DraftSaver.Status.Error -> t("common.draftNotSaved")
        saveState.savedAt != null && saveState.status != DraftSaver.Status.Conflict && something ->
            t("common.draftSaved", "time" to Time.time(saveState.savedAt))
        else -> ""
    }
    // The tray stays open while it holds something; the bar is open while it is in use.
    val tray = trayAsked || atts.items.isNotEmpty() || voice.recording
    val open = tray || focused || composer.text.isNotEmpty() || composer.replyTo != null
    val on = motionOn()
    val plusTurn by animateFloatAsState(if (tray) 45f else 0f, if (on) tween(220, easing = Motion.EaseOut) else tween(0), label = "plus")
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant

    Column(bar, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AnimatedVisibility(
            visible = tray,
            enter = if (on) {
                expandVertically(tween(260, easing = Motion.EmphasizedDecelerate), expandFrom = Alignment.Bottom) + fadeIn(tween(200, 60))
            } else {
                fadeIn(tween(0))
            },
            exit = if (on) {
                shrinkVertically(tween(180, easing = Motion.EmphasizedAccelerate), shrinkTowards = Alignment.Bottom) + fadeOut(tween(140))
            } else {
                fadeOut(tween(0))
            },
        ) {
            AttachmentStrip(
                attachments = atts,
                onRemove = { item, index ->
                    atts.remove(item.key)
                    item.att?.let { a -> composer.deleteAttachment(a.id) { atts.restore(item, index) } }
                },
                voice = voice,
                video = VideoAdd(full = atts.has("video"), onPick = { addVideo(it, null) }),
                background = Color.Transparent,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                limits = limits,
                retention = RetentionScope.Other,
                stacked = true,
            )
        }
        if (composer.trimmed) Text(TRIMMED, style = MaterialTheme.typography.bodySmall)
        videoError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        sendError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        // OS7: why Send waits.
        val unsent = composer.unsent
        if (held && unsent != null && open) {
            Text(
                if (unsent.status == EmailOutbox.Status.Sending) t("emails.waitPrevious") else t("emails.resolveUnsent"),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall,
                color = quiet,
            )
        }
        // RE9: the strip, opening like the tray.
        AnimatedVisibility(
            visible = composer.replyTo != null,
            enter = if (on) {
                expandVertically(
                    tween(260, easing = Motion.EmphasizedDecelerate),
                    expandFrom = Alignment.Bottom,
                ) + fadeIn(tween(200, 60))
            } else {
                fadeIn(tween(0))
            },
            exit = if (on) {
                shrinkVertically(
                    tween(180, easing = Motion.EmphasizedAccelerate),
                    shrinkTowards = Alignment.Bottom,
                ) + fadeOut(tween(140))
            } else {
                fadeOut(tween(0))
            },
        ) {
            // The last target, kept (not state) so the strip still shows while it closes.
            val last = remember { arrayOfNulls<EmailQuote>(1) }
            composer.replyTo?.let { last[0] = it }
            last[0]?.let { q -> ReplyStrip(q, quoteName(q.senderId), onCancel = composer::cancelReply) }
        }
        // The credits, centred on their own line; open, the counter and the saved note follow, start-aligned.
        val small = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum")
        Text(
            creditsLine(shown).orEmpty(),
            Modifier.fillMaxWidth(),
            style = small,
            color = if (outOfCredits) MaterialTheme.colorScheme.error else quiet,
            textAlign = TextAlign.Center,
        )
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(if (on) 160 else 0)) + expandVertically(tween(if (on) 160 else 0)),
            exit = fadeOut(tween(if (on) 120 else 0)) + shrinkVertically(tween(if (on) 120 else 0)),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    t("media.progress", "time" to length, "total" to EMAIL_MAX),
                    style = small.copy(fontWeight = if (length >= EMAIL_MAX) FontWeight.Bold else FontWeight.Normal),
                    color = if (length >= EMAIL_MAX) MaterialTheme.colorScheme.error else quiet,
                )
                if (saved.isNotEmpty()) {
                    Text(saved, style = small, color = if (saveState.status == DraftSaver.Status.Error) MaterialTheme.colorScheme.error else quiet)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val plusLabel = if (tray && trayAsked) t("emails.closeAttachments") else t("emails.attach")
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.letterEnvelope)
                    .border(1.dp, line, CircleShape)
                    .alpha(if (held) 0.4f else 1f)
                    .clickable(enabled = !held, role = Role.Button) {
                        // Attaching is not typing (EB15): opening the tray puts the keyboard away.
                        if (!tray) {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }
                        trayAsked = !trayAsked
                    }
                    .semantics { contentDescription = plusLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(DlIcons.Plus, null, Modifier.size(22.dp).rotate(plusTurn), tint = MaterialTheme.colorScheme.onSurface)
            }
            val style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            val shape = RoundedCornerShape(22.dp)
            BasicTextField(
                value = composer.text,
                onValueChange = composer::changeText,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .focusRequester(field)
                    .animateContentSize(if (on) tween(160, easing = Motion.EaseOut) else tween(0))
                    .background(c.letterEnvelope, shape)
                    .border(1.dp, line, shape)
                    .padding(horizontal = 16.dp, vertical = 11.dp)
                    .semantics { contentDescription = t("emails.writeAReply") },
                textStyle = style,
                maxLines = 6,
                interactionSource = focus,
                cursorBrush = SolidColor(c.mailBlue),
                decorationBox = { inner ->
                    Box {
                        if (composer.text.isEmpty()) Text(t("emails.writeAReply"), style = style, color = c.inkDisabled)
                        inner()
                    }
                },
            )
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val sendBg = if (canSend) MaterialTheme.colorScheme.primary else line
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(sendBg)
                    .clickable(enabled = canSend, role = Role.Button) {
                        if (composer.optimistic) {
                            // OS3: the bubble appears now; the bar is free at once.
                            val me = homeState.home?.me?.id ?: return@clickable
                            sendError = null
                            trayAsked = false
                            composer.sendOptimistic(me)
                            return@clickable
                        }
                        sending = true
                        sendError = null
                        scope.launch {
                            when (val r = composer.send()) {
                                is EmailComposer.SendResult.Sent -> {
                                    trayAsked = false
                                    onSent(r.sent)
                                }
                                EmailComposer.SendResult.Conflict -> Unit
                                is EmailComposer.SendResult.Failed -> sendError = r.message
                            }
                            sending = false
                        }
                    }
                    .semantics { contentDescription = if (sending) t("common.sending") else t("common.send") },
                contentAlignment = Alignment.Center,
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(
                        DlIcons.Send,
                        null,
                        Modifier.size(20.dp).scale(if (rtl) -1f else 1f, 1f),
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary else c.inkDisabled,
                    )
                }
            }
        }
    }
    // OS8: a conflict while held is reconciled by the send itself.
    if (saveState.status == DraftSaver.Status.Conflict && !held) {
        DlDialog(
            title = t("emails.aNewerVersionWasSaved"),
            text = null,
            confirmLabel = t("emails.load"),
            onConfirm = { scope.launch { composer.loadServer() } },
            dismissLabel = t("emails.keepMine"),
            onDismiss = { composer.saver.keepMine() },
        )
    }
}
