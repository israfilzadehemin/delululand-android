package com.delulutopia.feature.you

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlGhostButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.theme.Literata
import com.delulutopia.core.network.model.UniverseView
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.signatureOf
import com.delulutopia.core.ui.tintOf
import com.delulutopia.feature.start.KeyReveal
import com.delulutopia.feature.start.Rules
import com.delulutopia.feature.start.WelcomeDialog
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

/**
 * Settings › Universe (doc "Delululand — Universe" §2.6): the creator renames, edits the welcome
 * message and makes a new key (shown once); the other citizen reads the same screen.
 */
@Composable
fun UniverseScreen(vm: UniverseViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val v = vm.view
    val code = vm.revealed
    Column(Modifier.fillMaxSize().imePadding()) {
        if (code != null && v != null) {
            DlTopAppBar(title = "New key", onBack = { vm.revealed = null })
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                KeyReveal(v.name, code, continueLabel = "Done") { vm.revealed = null }
            }
            return@Column
        }
        DlTopAppBar(title = "Universe", onBack = { nav.back() })
        // web .dl-sub-main.dl-settings: 4/20/24 padding, 12 dp between.
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                v == null -> LoadOrFail(vm.failed)
                v.isCreator -> CreatorForm(vm, v)
                else -> ReadOnly(vm, v)
            }
        }
    }
}

@Composable
private fun CreatorForm(vm: UniverseViewModel, view: UniverseView) {
    val notice = LocalNotice.current
    var preview by remember { mutableStateOf(false) }
    var confirmKey by remember { mutableStateOf(false) }
    val nameProblem = vm.serverError ?: universeNameError(vm.name)
    val welcomeProblem = welcomeError(vm.welcome)
    val dirty = vm.name.trim() != vm.savedName || vm.welcome != vm.savedWelcome
    val canSave = dirty && nameProblem == null && welcomeProblem == null && !vm.busy
    val trimmed = vm.welcome.trim()

    DlTextField(
        value = vm.name,
        onValueChange = {
            vm.name = it.filterNot(Char::isWhitespace)
            vm.serverError = null
        },
        label = "Name",
        errorText = nameProblem,
        maxLength = 24,
        counter = "${vm.name.trim().codePointCount(0, vm.name.trim().length)}/12",
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
    )
    DlTextField(
        value = vm.welcome,
        onValueChange = { vm.welcome = it },
        label = "Welcome message",
        errorText = welcomeProblem,
        singleLine = false,
        minLines = 6,
        counter = "${trimmed.codePointCount(0, trimmed.length)}/$WELCOME_MAX",
    )
    DlTextButton("See it as your friend will", { preview = true }, enabled = welcomeProblem == null)
    DlFilledButton(if (vm.busy) "Saving…" else "Save", { vm.save(notice) }, Modifier.fillMaxWidth(), enabled = canSave)

    // web .dl-universe-section: a 1 dp outlined box, 12 dp corners, 16 padding, 10 between.
    Column(
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ENTRY KEY", style = FieldLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val made = view.codeRotatedAt?.let { runCatching { SimpleDateFormat("d MMM", Locale.UK).format(Date.from(Instant.parse(it))) }.getOrNull() }
        Text(
            (if (made != null) "Made $made. " else "") + "Keys can't be shown again; make a new one if it's lost.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        )
        DlGhostButton("Make a new key", { confirmKey = true })
    }

    if (preview) WelcomeDialog(vm.name.trim().ifEmpty { view.name }, vm.welcome, { preview = false }, preview = true)
    if (confirmKey) {
        DlDialog(
            title = "Make a new key?",
            text = "The old key stops working. Devices that are logged in stay in; devices that only entered the key will need the new one.",
            confirmLabel = "Make a new key",
            onConfirm = {
                confirmKey = false
                vm.rotate(notice)
            },
            dismissLabel = "Cancel",
            onDismiss = { confirmKey = false },
        )
    }
}

/** SU-4: what the other citizen sees. */
@Composable
private fun ReadOnly(vm: UniverseViewModel, view: UniverseView) {
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val friend = homeState.home?.friend
    val creator = view.creator
    if (creator != null) {
        // web .dl-key-warning
        Text(
            "Only ${creator.username} can change this.",
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        )
    }
    // web .dl-field: the label above, 6 dp, the value as a settings label (16/600).
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("NAME", style = FieldLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(view.name, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("WELCOME MESSAGE", style = FieldLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // web .dl-universe-paper: the letter surface, 4 dp corners, a card shadow, Literata 14/1.55.
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Rules.paragraphs(view.welcome).forEach {
                Text(it, fontFamily = Literata, fontSize = 14.sp, lineHeight = 21.7.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    if (creator != null) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CREATED BY", style = FieldLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val photo = if (friend != null && friend.id == creator.id) friend.avatarUrl else null
                DlRemoteAvatar(
                    photo,
                    creator.username,
                    tintOf(creator.id),
                    size = 32.dp,
                    initials = creator.username.take(1).uppercase(),
                    ring = signatureOf(creator.id),
                )
                Text(creator.username, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** web .dl-field-label: 12/700, 0.06 em, caps. */
private val FieldLabel = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em)
