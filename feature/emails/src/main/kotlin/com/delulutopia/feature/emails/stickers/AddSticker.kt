package com.delulutopia.feature.emails.stickers

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlGhostButton
import com.delulutopia.core.designsystem.component.DlListItem
import com.delulutopia.core.designsystem.component.DlSwitchItem
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Sticker
import com.delulutopia.core.ui.doodle.DoodlePad
import com.delulutopia.core.ui.doodle.Surfaces
import com.delulutopia.core.ui.doodle.drawChecker
import com.delulutopia.core.ui.photo.decodePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Source { Draw, Photo, Import }

private sealed interface Step {
    data object Menu : Step
    data object Draw : Step
    data object Working : Step
    data class Preview(val bytes: ByteArray, val mime: String, val source: Source) : Step
}

/**
 * Adding one of Ours (doc "Delululand — Stickers" SK8, SK13, FS-SK18–SK24; web stickers/AddSticker.tsx):
 * Draw on a clear square, cut the subject out of a photo on the phone, or import an image; then a
 * preview with keywords, the white outline and the count after adding.
 */
@Composable
internal fun AddSticker(store: StickerStore, onClose: () -> Unit, start: Pair<ByteArray, String>? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<Step>(start?.let { Step.Preview(it.first, it.second, Source.Import) } ?: Step.Menu) }
    var error by remember { mutableStateOf<String?>(null) }

    fun take(uri: Uri) {
        error = null
        val mime = context.contentResolver.getType(uri) ?: ""
        val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        error = checkSticker(bytes?.size ?: 0, mime)
        if (error == null && bytes != null) step = Step.Preview(bytes, mime, Source.Import)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(::take) }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        step = Step.Working
        scope.launch {
            val png = withContext(Dispatchers.Default) { decodePhoto(context, uri, maxEdge = 1024)?.let { cutOut(it) } }
            if (png == null) {
                Toast.makeText(context, t("common.somethingWentWrong"), Toast.LENGTH_SHORT).show()
                step = Step.Menu
            } else {
                step = Step.Preview(png, "image/png", Source.Photo)
            }
        }
    }

    when (val s = step) {
        Step.Menu -> DlBottomSheet(onDismissRequest = onClose) {
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    t("stickers.newSticker"),
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                val images = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                DlListItem(t("stickers.draw"), supporting = t("stickers.drawHint"), icon = WebIcons.Doodle, showChevron = false, onClick = { step = Step.Draw })
                DlListItem(t("stickers.fromPhoto"), icon = WebIcons.Photo, showChevron = false, onClick = { photos.launch(images) })
                DlListItem(
                    t("stickers.import"),
                    supporting = t("stickers.importHint"),
                    icon = WebIcons.Import,
                    showChevron = false,
                    onClick = { importer.launch(images) },
                )
                error?.let {
                    Text(
                        it,
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    t("stickers.capHint"),
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Step.Draw -> DoodlePad(
            onDone = { png -> step = Step.Preview(png, "image/png", Source.Draw) },
            onClose = { step = Step.Menu },
            surface = Surfaces.STICKER,
            title = t("stickers.draw"),
            doneLabel = t("stickers.next"),
        )
        Step.Working -> Dialog(onDismissRequest = {}) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        is Step.Preview -> Preview(store, s.bytes, s.mime, s.source, onBack = { if (start != null) onClose() else step = Step.Menu }, onDone = onClose)
    }
}

/** SK15 said before uploading: over 1 MB, or a type the server can't read. */
internal fun checkSticker(size: Int, mime: String): String? = when {
    size > StickerStore.MAX_BYTES -> t("stickers.tooLarge")
    mime !in StickerStore.TYPES -> t("stickers.unsupported")
    else -> null
}

@Composable
private fun Preview(store: StickerStore, bytes: ByteArray, mime: String, source: Source, onBack: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val cat = store.catalogue
    var outline by remember { mutableStateOf(source != Source.Import) }
    var outlined by remember { mutableStateOf<ByteArray?>(null) }
    var keywords by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bytes, outline) {
        if (outline && outlined == null) outlined = withContext(Dispatchers.Default) { withOutline(bytes, outlinePx(bytes)) }
    }
    val shown = if (outline) outlined ?: bytes else bytes
    val shownMime = if (outline && outlined != null) "image/png" else mime
    val full = cat != null && cat.custom.used >= cat.custom.cap
    val c = DlTheme.colors
    val paper = MaterialTheme.colorScheme.surface

    Dialog(onDismissRequest = { if (!busy) onBack() }) {
        Surface(Modifier.widthIn(max = 420.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(t("stickers.newSticker"), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Box(
                    Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)).drawBehind { drawChecker(c.letterEnvelope, paper) },
                    contentAlignment = Alignment.Center,
                ) { AsyncImage(shown, t("stickers.previewAlt"), Modifier.size(200.dp)) }
                if (source != Source.Import) DlSwitchItem(t("stickers.whiteOutline"), outline, { outline = it })
                DlTextField(
                    keywords,
                    { keywords = it.take(260) },
                    t("stickers.keywords"),
                    Modifier.fillMaxWidth(),
                    supportingText = t("stickers.keywordsHint"),
                    enabled = !busy,
                )
                Text(
                    t("stickers.bothCanUse"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (cat != null) {
                    Text(
                        t("stickers.afterAdding", "used" to minOf(cat.custom.used + 1, cat.custom.cap), "cap" to cat.custom.cap),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DlGhostButton(t("stickers.cancel"), onBack, Modifier.weight(1f), enabled = !busy)
                    DlFilledButton(
                        if (busy) t("stickers.adding") else t("stickers.add"),
                        {
                            busy = true
                            error = null
                            scope.launch {
                                val err = store.add(shown, shownMime, keywords)
                                busy = false
                                if (err == null) onDone() else error = err
                            }
                        },
                        Modifier.weight(1f),
                        enabled = !busy && !full,
                    )
                }
            }
        }
    }
}

/** FS-SK25: "Delete this sticker for both of you? Sent ones stay." */
@Composable
internal fun DeleteSticker(store: StickerStore, sticker: Sticker, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    DlDialog(
        title = t("stickers.deleteTitle"),
        text = t("stickers.deleteBody"),
        confirmLabel = t("stickers.delete"),
        onConfirm = {
            busy = true
            scope.launch {
                val ok = store.delete(sticker.id)
                busy = false
                if (!ok) Toast.makeText(context, t("common.somethingWentWrong"), Toast.LENGTH_SHORT).show()
                onClose()
            }
        },
        dismissLabel = t("stickers.cancel"),
        onDismiss = onClose,
        destructive = true,
        busy = busy,
    )
}
