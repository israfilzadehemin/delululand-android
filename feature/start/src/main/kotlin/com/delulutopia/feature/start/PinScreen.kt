package com.delulutopia.feature.start

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.feature.start.PinViewModel.Mode
import com.delulutopia.feature.start.PinViewModel.Problem

@Composable
fun PinRoute(onBack: () -> Unit, viewModel: PinViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.leave) { if (state.leave) onBack() }
    PinScreen(state, onBack, viewModel::onDigits, viewModel::onComplete)
}

@Composable
fun PinScreen(state: PinViewModel.UiState, onBack: () -> Unit, onDigits: (String) -> Unit, onComplete: (String) -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        DlTopAppBar(title = "", onBack = onBack, backDescription = stringResource(R.string.back))
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
            prompt = stringResource(
                when (state.mode) {
                    Mode.Enter -> R.string.pin_enter
                    Mode.Create -> R.string.pin_create
                    Mode.Confirm -> R.string.pin_confirm
                },
            ),
            dotsLabel = stringResource(
                R.string.pin_dots,
                stringResource(
                    when (state.mode) {
                        Mode.Enter -> R.string.pin_dots_pin
                        Mode.Create -> R.string.pin_dots_new
                        Mode.Confirm -> R.string.pin_dots_confirm
                    },
                ),
                state.digits.length,
            ),
            digits = state.digits,
            error = problemText(state.problem, state.universe),
            hint = if (state.mode == Mode.Create) stringResource(R.string.pin_hint) else null,
            busy = state.busy || state.user == null,
            onDigits = onDigits,
            onComplete = onComplete,
        )
    }
}

@Composable
internal fun problemText(problem: Problem?, universe: String): String? = when (problem) {
    null -> null
    Problem.Wrong -> stringResource(R.string.pin_wrong)
    Problem.Differ -> stringResource(R.string.pin_differ)
    Problem.Offline -> stringResource(R.string.cant_reach, universe.ifBlank { stringResource(R.string.brand) })
    is Problem.Text -> problem.text
}
