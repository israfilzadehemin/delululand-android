package com.delulutopia.feature.start

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.i18n.t
import com.delulutopia.core.ui.fingerprint.Fingerprint
import com.delulutopia.feature.start.PinViewModel.Mode
import com.delulutopia.feature.start.PinViewModel.Problem

@Composable
fun PinRoute(onBack: () -> Unit, viewModel: PinViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.leave) { if (state.leave) onBack() }
    val activity = LocalActivity.current as? FragmentActivity
    val context = LocalContext.current
    val canFingerprint = activity != null && state.mode == PinViewModel.Mode.Enter && state.user != null &&
        Fingerprint.isOn(context, viewModel.userId) && Fingerprint.available(context)
    val title = t("pin.unlockBrand", "brand" to state.universe.ifBlank { stringResource(R.string.brand) })
    val ask: () -> Unit = {
        if (activity != null) Fingerprint.unlock(activity, viewModel.userId, title, viewModel::onFingerprint) {}
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(canFingerprint) {
        if (canFingerprint && !asked) {
            asked = true
            ask()
        }
    }
    PinScreen(state, onBack, viewModel::onDigits, viewModel::onComplete, if (canFingerprint) ask else null)
}

@Composable
fun PinScreen(
    state: PinViewModel.UiState,
    onBack: () -> Unit,
    onDigits: (String) -> Unit,
    onComplete: (String) -> Unit,
    onFingerprint: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        DlTopAppBar(title = "", onBack = onBack, backDescription = t("common.back"))
        val user = state.user
        PinPanel(
            header = {
                if (user != null) {
                    DlRemoteAvatar(
                        avatarUrl = user.avatarUrl,
                        name = user.nickname,
                        initials = user.nickname.take(1).uppercase(),
                        initialsColor = MaterialTheme.colorScheme.onSurface,
                        signature = signatureTintOf(state.index),
                        ring = signatureOf(state.index),
                        ringWidth = 3.dp,
                        size = 96.dp,
                    )
                    Text(user.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            prompt = t(
                when (state.mode) {
                    Mode.Enter -> "pin.enterYourPin"
                    Mode.Create -> "pin.createYourPin"
                    Mode.Confirm -> "pin.confirmYourPin"
                },
            ),
            dotsLabel = t(
                when (state.mode) {
                    Mode.Enter -> "pin.dots.enter"
                    Mode.Create -> "pin.dots.new"
                    Mode.Confirm -> "pin.dots.confirm"
                },
                "n" to state.digits.length,
                "total" to 6,
            ),
            digits = state.digits,
            error = problemText(state.problem, state.universe),
            hint = if (state.mode == Mode.Create) t("pin.sixDigits") else null,
            busy = state.busy || state.user == null,
            onDigits = onDigits,
            onComplete = onComplete,
            onFingerprint = onFingerprint,
        )
    }
}

@Composable
internal fun problemText(problem: Problem?, universe: String): String? = when (problem) {
    null -> null
    Problem.Wrong -> t("common.wrongPin")
    Problem.Differ -> t("common.pinsDiffer")
    Problem.Offline -> t("common.cantReachTryAgain", "brand" to universe.ifBlank { stringResource(R.string.brand) })
    is Problem.Text -> problem.text
}
