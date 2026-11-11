package com.delulutopia.feature.you

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlSectionLabel
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.Literata
import com.delulutopia.core.network.model.UniverseView
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.signatureOf
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
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Entry key", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        val made = view.codeRotatedAt?.let { runCatching { SimpleDateFormat("d MMM", Locale.UK).format(Date.from(Instant.parse(it))) }.getOrNull() }
        Text(
            (if (made != null) "Made $made. " else "") + "Keys can't be shown again; make a new one if it's lost.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DlOutlinedButton("Make a new key", { confirmKey = true })
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
        Text(
            "Only ${creator.username} can change this.",
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp)).padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DlSectionLabel("Name")
        Text(view.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DlSectionLabel("Welcome message")
        Column(
            Modifier.fillMaxWidth().background(DlTheme.colors.letterSheet, RoundedCornerShape(8.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Rules.paragraphs(view.welcome).forEach {
                Text(it, fontFamily = Literata, color = DlTheme.colors.letterInk, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (creator != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DlSectionLabel("Created by")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val photo = if (friend != null && friend.id == creator.id) friend.avatarUrl else null
                DlRemoteAvatar(photo, creator.username, signatureOf(creator.id), size = 32.dp, initials = creator.username.take(1).uppercase())
                Text(creator.username, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
