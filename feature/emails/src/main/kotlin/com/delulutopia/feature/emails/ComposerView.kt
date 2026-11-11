package com.delulutopia.feature.emails

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.Uploader
import com.delulutopia.core.network.model.EmailCredits
import com.delulutopia.core.network.model.EmailSent
import com.delulutopia.core.ui.media.AttachmentStrip
import com.delulutopia.core.ui.media.Source
import com.delulutopia.core.ui.media.VIDEO_TOO_LONG
import com.delulutopia.core.ui.media.VideoAdd
import com.delulutopia.core.ui.media.rememberVoiceRecording
import com.delulutopia.core.ui.media.tooLong
import com.delulutopia.core.ui.media.videoMs
import kotlinx.coroutines.launch

/**
 * The email composer view (FS-E11–E16; canvas E-New, E-Thread): Subject for a new email, the text
 * with "N / 1024", the strip with one video, the credits line and Send.
 */
@Composable
internal fun ComposerView(composer: EmailComposer, credits: EmailCredits?, onSent: (EmailSent) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (composer.failed) {
        Text("Couldn't load your draft. Check your connection and try again.", color = MaterialTheme.colorScheme.error)
        return
    }
    val atts = composer.attachments
    if (!composer.loaded || atts == null) {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val c = DlTheme.colors
    val saveState by composer.saver.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { composer.saver.flushOnHide() }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var recorderOpen by remember { mutableStateOf(false) }
    var videoError by remember { mutableStateOf<String?>(null) }
    val voice = rememberVoiceRecording { bytes, ms -> atts.add(Uploader.Kind.Audio, Source.Made(bytes, "audio/mp4"), "voice.m4a", ms) }
    val addVideo = { uri: Uri, ms: Long? ->
        if (tooLong(ms ?: videoMs(context, uri))) {
            videoError = VIDEO_TOO_LONG
        } else {
            atts.add(Uploader.Kind.Video, Source.Picked(uri), "video.mp4", ms)
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { addVideo(it, null) } }
    val length = count(normalize(composer.text))
    val outOfCredits = credits != null && credits.left <= 0
    val something = composer.text.isNotBlank() || atts.items.isNotEmpty()
    val canSend = (!composer.withSubject || composer.subject.isNotBlank()) && something && !atts.uploading && !voice.recording && !outOfCredits && !sending
    val saved = when {
        saveState.status == DraftSaver.Status.Error -> "Couldn't save your draft"
        saveState.savedAt != null && saveState.status != DraftSaver.Status.Conflict &&
            (composer.text.isNotBlank() || composer.subject.isNotBlank() || atts.items.isNotEmpty()) ->
            "Draft saved ${Time.time(saveState.savedAt)}"
        else -> ""
    }
    val field = MaterialTheme.typography.bodyLarge.copy(color = c.mailInk)
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.mailBackground, RoundedCornerShape(12.dp))
            .border(1.dp, c.mailLine, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (composer.withSubject) {
            Input(composer.subject, composer::changeSubject, "Subject", field.copy(fontWeight = FontWeight.SemiBold), single = true)
        }
        Input(
            composer.text,
            composer::changeText,
            if (composer.withSubject) "Write an email" else "Write a reply",
            field,
            single = false,
            minHeight = if (composer.withSubject) 180.dp else 72.dp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "$length / $EMAIL_MAX",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = if (length >= EMAIL_MAX) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                saved,
                style = MaterialTheme.typography.labelSmall,
                color = if (saveState.status ==
                    DraftSaver.Status.Error
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (composer.trimmed) Text(TRIMMED, style = MaterialTheme.typography.bodySmall)
        AttachmentStrip(
            attachments = atts,
            onRemove = { item, index ->
                atts.remove(item.key)
                item.att?.let { a -> composer.deleteAttachment(a.id) { atts.restore(item, index) } }
            },
            voice = voice,
            video = VideoAdd(full = atts.has("video"), onRecord = { recorderOpen = true }, onPick = { addVideo(it, null) }),
        )
        videoError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                creditsLine(credits).orEmpty(),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = if (outOfCredits) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DlFilledButton(
                if (sending) "Sending…" else "Send",
                {
                    sending = true
                    sendError = null
                    scope.launch {
                        when (val r = composer.send()) {
                            is EmailComposer.SendResult.Sent -> onSent(r.sent)
                            EmailComposer.SendResult.Conflict -> Unit
                            is EmailComposer.SendResult.Failed -> sendError = r.message
                        }
                        sending = false
                    }
                },
                enabled = canSend,
                height = 40.dp,
            )
        }
        sendError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
    if (recorderOpen) {
        VideoRecorderSheet(
            onUse = { uri, ms ->
                recorderOpen = false
                atts.add(Uploader.Kind.Video, Source.Picked(uri), "video.mp4", ms)
            },
            onPick = {
                recorderOpen = false
                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            onClose = { recorderOpen = false },
        )
    }
    if (saveState.status == DraftSaver.Status.Conflict) {
        DlDialog(
            title = "A newer version was saved on another device. Load it?",
            text = null,
            confirmLabel = "Load",
            onConfirm = { scope.launch { composer.loadServer() } },
            dismissLabel = "Keep mine",
            onDismiss = { composer.saver.keepMine() },
        )
    }
}

@Composable
private fun Input(value: String, onChange: (String) -> Unit, label: String, style: TextStyle, single: Boolean, minHeight: Dp = 0.dp) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).semantics { contentDescription = label },
        singleLine = single,
        textStyle = style,
        cursorBrush = SolidColor(DlTheme.colors.mailBlue),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(label, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        },
    )
}
