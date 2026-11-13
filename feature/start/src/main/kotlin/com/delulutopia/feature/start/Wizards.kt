package com.delulutopia.feature.start

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlStepDots
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.theme.Literata
import com.delulutopia.core.i18n.counter
import com.delulutopia.core.i18n.t
import com.delulutopia.feature.start.CreateUniverseViewModel.InviteProblem

@Composable
fun CreateUniverseRoute(onLeave: () -> Unit, viewModel: CreateUniverseViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val back = { if (s.step != 5 && !viewModel.back()) onLeave() }
    BackHandler(enabled = s.step != 5) { back() }
    val brand = stringResource(R.string.brand)

    WizardFrame(
        title = if (s.step == 5) "" else t("universe.createAUniverse"),
        step = s.step,
        of = 5,
        onBack = if (s.step == 5) null else back,
    ) {
        when (s.step) {
            1 -> FormColumn {
                WizardTitle(t("universe.yourInvite"))
                Text(t("universe.aUniverseStartsWithAn"), style = MaterialTheme.typography.bodyMedium)
                DlTextField(
                    value = s.invite,
                    onValueChange = viewModel::onInvite,
                    label = t("universe.invite"),
                    errorText = when (s.inviteProblem) {
                        InviteProblem.Wrong -> t("universe.inviteInvalid")
                        InviteProblem.Offline -> t("common.cantReachTryAgain", "brand" to brand)
                        null -> null
                    },
                    centeredMono = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { viewModel.checkInvite() }),
                )
                DlFilledButton(
                    t("universe.continue"),
                    viewModel::checkInvite,
                    Modifier.fillMaxWidth(),
                    enabled = !s.busy && s.invite.isNotBlank(),
                    height = 48.dp,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(t("universe.haveAKeyQ"), fontSize = 14.sp)
                    TextButton(onClick = onLeave) { StrongLink(t("universe.enterIt")) }
                }
            }
            2 -> FormColumn {
                val typed = s.name.trim()
                val nameError = s.nameServerError ?: if (s.name.isNotEmpty() && !Rules.nameOk(s.name)) t("universe.nameRule") else null
                val welcomeLength = Rules.welcomeLength(s.welcome)
                val welcomeError = when {
                    s.welcome.isBlank() -> t("universe.writeAFewWords")
                    welcomeLength > Rules.WELCOME_MAX -> t("universe.welcomeMax")
                    else -> null
                }
                WizardTitle(t("universe.yourUniverse"))
                DlTextField(
                    value = s.name,
                    onValueChange = viewModel::onName,
                    label = t("universe.name"),
                    supportingText = t("universe.212LettersOrDigits"),
                    errorText = nameError,
                    maxLength = 12,
                    counter = counter(s.name.trim().codePointCount(0, s.name.trim().length), 12),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                )
                val shownName = typed.ifEmpty { t("universe.yourUniverseLower") }
                DlTextField(
                    value = s.welcome,
                    onValueChange = viewModel::onWelcome,
                    label = t("universe.welcomeMessage"),
                    supportingText = t(if (s.welcomeEdited) "universe.welcomeHint" else "universe.welcomeHintFollows", "name" to shownName),
                    errorText = welcomeError.takeIf { s.welcomeEdited },
                    singleLine = false,
                    maxLength = null,
                )
                Text(
                    "$welcomeLength/${Rules.WELCOME_MAX}",
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.setPreview(true) }, enabled = welcomeError == null) {
                        StrongLink(t("universe.seeItAsYourFriend"))
                    }
                    if (s.welcomeEdited) TextButton(onClick = viewModel::resetWelcome) { Text(t("universe.resetToTheOriginal")) }
                }
                DlFilledButton(
                    t("universe.continue"),
                    viewModel::next,
                    Modifier.fillMaxWidth(),
                    enabled = Rules.nameOk(s.name) && welcomeError == null,
                    height = 48.dp,
                )
                if (s.preview) WelcomeDialog(shownName, s.welcome, { viewModel.setPreview(false) }, preview = true)
            }
            3 -> FormColumn {
                WizardTitle(t("universe.yourCitizen"))
                CitizenStep(s.citizen, viewModel::onCitizen, taken = emptyList(), serverError = s.citizenError)
                DlFilledButton(
                    t("universe.continue"),
                    viewModel::next,
                    Modifier.fillMaxWidth(),
                    enabled = Rules.usernameOk(s.citizen.username),
                    height = 48.dp,
                )
            }
            4 -> WizardPinStep(
                who = t("profile.moodSince", "mood" to s.citizen.username, "age" to s.name.trim()),
                pin = s.pin,
                universe = s.name.trim(),
                onDigits = viewModel::onPinDigits,
                onComplete = viewModel::onPinComplete,
            )
            5 -> s.created?.let { c -> FormColumn { KeyReveal(c.name, c.code, onContinue = viewModel::finish) } }
        }
    }
}

@Composable
fun JoinRoute(onLeave: () -> Unit, viewModel: JoinViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.leave) { if (s.leave) onLeave() }
    val back = { if (!viewModel.back()) onLeave() }
    BackHandler { back() }
    val name = s.name.ifBlank { stringResource(R.string.brand) }

    WizardFrame(title = t("entry.join", "name" to name), step = s.step, of = 2, onBack = back) {
        if (s.step == 1) {
            FormColumn {
                WizardTitle(t("universe.yourCitizen"))
                CitizenStep(s.citizen, viewModel::onCitizen, taken = s.taken, serverError = s.serverError, signature = 1)
                DlFilledButton(
                    t("universe.continue"),
                    viewModel::next,
                    Modifier.fillMaxWidth(),
                    enabled = s.loaded && Rules.usernameOk(s.citizen.username) && !Rules.usernameTaken(s.citizen.username, s.taken),
                    height = 48.dp,
                )
            }
        } else {
            WizardPinStep(
                who = t("profile.moodSince", "mood" to s.citizen.username, "age" to name),
                pin = s.pin,
                universe = name,
                onDigits = viewModel::onPinDigits,
                onComplete = viewModel::onPinComplete,
            )
        }
    }
}

@Composable
private fun WizardFrame(title: String, step: Int, of: Int, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {
        DlTopAppBar(
            title = title,
            onBack = onBack,
            backDescription = t("common.back"),
            actions = { DlStepDots(step, of) },
        )
        content()
    }
}

@Composable
private fun ColumnScope.FormColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun WizardTitle(text: String) {
    Text(text, fontFamily = Literata, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ColumnScope.WizardPinStep(
    who: String,
    pin: WizardPin,
    universe: String,
    onDigits: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    PinPanel(
        header = { Text(who, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.heightIn(min = 24.dp)) },
        prompt = t(if (pin.confirming) "pin.confirmYourPin" else "pin.createYourPin"),
        dotsLabel = t(if (pin.confirming) "pin.dots.confirm" else "pin.dots.new", "n" to pin.digits.length, "total" to 6),
        digits = pin.digits,
        error = problemText(pin.problem, universe),
        hint = if (!pin.confirming) t("pin.sixDigits") else null,
        busy = pin.busy,
        onDigits = onDigits,
        onComplete = onComplete,
    )
}
