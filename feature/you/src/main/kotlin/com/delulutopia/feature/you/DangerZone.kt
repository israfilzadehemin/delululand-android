package com.delulutopia.feature.you

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.SessionRepository
import com.delulutopia.core.data.SessionState
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlPinDots
import com.delulutopia.core.designsystem.component.DlPinPad
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.PIN_LENGTH
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.ServerConfig
import com.delulutopia.core.network.model.DeletionCounts
import com.delulutopia.core.network.model.DeletionPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Doc "Delululand — Data Deletion" DD10: where deletion and privacy requests go (Pre-publish Checklist A2). */
const val PRIVACY_CONTACT = "emin@delulutopia.com"

/**
 * DL-1–DL-3 (artboards DD-Y*, DD-C*, DD-P*): the creator deletes the universe, the second citizen
 * only their own account. Confirm sheet (counts; the creator types the name), then the PIN — never
 * the fingerprint (DD7). On success the session turns [SessionState.Gone] and the app shows goodbye.
 */
@HiltViewModel
class DeletionViewModel @Inject constructor(
    private val session: SessionRepository,
    config: ServerConfig,
) : ViewModel() {
    val privacyUrl: String = config.baseUrl().trimEnd('/') + "/privacy"

    var preview by mutableStateOf<DeletionPreview?>(null)
        private set
    var digits by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    val signedIn: SessionState.SignedIn? get() = session.state.value as? SessionState.SignedIn

    fun loadPreview() {
        preview = null
        viewModelScope.launch { runCatching { session.deletionPreview() }.onSuccess { preview = it } }
    }

    fun reset() {
        digits = ""
        error = null
        busy = false
    }

    fun submit(pin: String, creator: Boolean, name: String) {
        busy = true
        error = null
        viewModelScope.launch {
            repeat(2) { attempt ->
                try {
                    if (creator) session.deleteUniverse(pin, name.trim()) else session.deleteMe(pin)
                    return@launch
                } catch (e: ApiException) {
                    // A PIN typed right after another check can meet the 1 s throttle: wait once, try again.
                    if (e.code == "PIN_THROTTLED" && attempt == 0) {
                        delay(1_050)
                    } else {
                        busy = false
                        digits = ""
                        error = when {
                            e.code == "WRONG_PIN" || e.code == "PIN_THROTTLED" -> t("common.wrongPin")
                            e.status in 400..499 && e.message.isNotBlank() -> e.message
                            else -> t("deletion.failed")
                        }
                        return@launch
                    }
                }
            }
        }
    }
}

private val COUNT_KEYS = listOf("letters", "emails", "discussions", "comments", "photos", "voice", "videos", "doodles")

private fun DeletionCounts.of(key: String): Int = when (key) {
    "letters" -> letters
    "emails" -> emails
    "discussions" -> discussions
    "comments" -> comments
    "photos" -> photos
    "voice" -> voice
    "videos" -> videos
    else -> doodles
}

/** The last group of You (DD15): what deleting means for me, one error-outlined button, privacy links. */
@Composable
internal fun DangerZone(brand: String, vm: DeletionViewModel = hiltViewModel()) {
    val signed = vm.signedIn ?: return
    val creator = signed.session.universe.isCreator
    val friend = signed.session.friend?.nickname
    val uri = LocalUriHandler.current
    var step by remember { mutableStateOf(0) } // 0 closed, 1 confirm, 2 PIN
    var name by remember { mutableStateOf("") }
    val close = {
        step = 0
        name = ""
        vm.reset()
    }

    Column {
        Text(
            t("deletion.dangerZone").uppercase(),
            Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
            color = MaterialTheme.colorScheme.error,
        )
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    when {
                        creator && friend != null -> t("deletion.ownerText", "brand" to brand, "friend" to friend)
                        creator -> t("deletion.ownerAloneText", "brand" to brand)
                        else -> t("deletion.memberText", "brand" to brand, "friend" to (friend ?: t("common.friend")))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DlOutlinedButton(
                    if (creator) t("deletion.deleteUniverse", "brand" to brand) else t("deletion.deleteMyAccount"),
                    onClick = {
                        vm.loadPreview()
                        step = 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                    destructive = true,
                )
            }
        }
        Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DlTextButton(t("deletion.privacy"), onClick = { runCatching { uri.openUri(vm.privacyUrl) } })
        }
        Text(
            t("deletion.questions", "email" to PRIVACY_CONTACT),
            Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (step == 1) {
        ConfirmSheet(creator, brand, vm.preview, name, { name = it }, onCancel = close, onContinue = { step = 2 })
    }
    if (step == 2) {
        DeletePinSheet(vm, onClose = { if (!vm.busy) close() }, onComplete = { vm.submit(it, creator, name) })
    }
}

@Composable
private fun ConfirmSheet(
    creator: Boolean,
    brand: String,
    preview: DeletionPreview?,
    name: String,
    onName: (String) -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    val friend = preview?.friend
    val nameOk = !creator || name.trim() == brand
    DlBottomSheet(onDismissRequest = onCancel) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (creator) t("deletion.confirmUniverseTitle", "brand" to brand) else t("deletion.confirmMeTitle"),
                style = MaterialTheme.typography.titleLarge,
            )
            if (creator && friend != null) {
                Text(t("deletion.friendLoses", "friend" to friend), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            }
            Text(t("deletion.thisDeletes"), style = MaterialTheme.typography.bodyMedium)
            Bullet(t(if (creator) (if (friend != null) "deletion.list.profiles" else "deletion.list.profileAlone") else "deletion.list.profile"))
            val counts = preview?.counts?.let { c -> COUNT_KEYS.filter { c.of(it) > 0 }.map { it to c.of(it) } }.orEmpty()
            counts.forEach { (key, n) ->
                Row(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                    Text(t("deletion.count.$key"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(n.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Bullet(t(if (creator) "deletion.list.universeRest" else "deletion.list.memberRest"))
            Text(
                if (creator) {
                    listOfNotNull(friend?.let { t("deletion.friendNotified", "friend" to it) }, t("deletion.keptOwner")).joinToString(" ")
                } else {
                    t("deletion.keptMember", "brand" to brand, "friend" to (friend ?: t("common.friend")))
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (creator) {
                DlTextField(name, onName, label = t("deletion.typeName", "brand" to brand), modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                DlTextButton(if (creator) t("app.cancel") else t("deletion.keep"), onCancel)
                DlTextButton(t("deletion.continue"), onContinue, destructive = true, enabled = nameOk)
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** DD-P*: six digits, deleted on the sixth. No fingerprint button here (DD7). */
@Composable
private fun DeletePinSheet(vm: DeletionViewModel, onClose: () -> Unit, onComplete: (String) -> Unit) {
    DlBottomSheet(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(t("deletion.pinTitle"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            DlPinDots(
                vm.digits.length,
                t("home.ofDigits", "label" to t("deletion.pinTitle"), "count" to vm.digits.length, "pinlength" to PIN_LENGTH),
                error = vm.error != null,
            )
            Text(
                vm.error ?: t("deletion.pinLine"),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.error,
            )
            DlPinPad(
                digits = vm.digits,
                onDigits = { vm.digits = it },
                onComplete = onComplete,
                enabled = !vm.busy,
            )
        }
    }
}

/**
 * G1–G4 (artboards DD-G*): shown in place of the app once the device is wiped. "Done" leads to the
 * start, which has no session and no entry pass left.
 */
@Composable
fun GoodbyeScreen(reason: String, brand: String?, onDone: () -> Unit) {
    val b = brand ?: "Delulutopia"
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(t("deletion.goodbye.${reason}Title", "brand" to b), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            t("deletion.goodbye.${reason}Text", "brand" to b),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        DlFilledButton(t("deletion.done"), onDone)
    }
}
