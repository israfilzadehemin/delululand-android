package com.delulutopia.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.enter

@Composable
fun EntryRoute(
    onOpenPin: (userId: Int) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EntryScreen(
        state = state,
        onKeyChange = viewModel::onKeyChange,
        onSubmitKey = viewModel::submitKey,
        onRetry = viewModel::load,
        onForget = viewModel::forget,
        onDismissWelcome = viewModel::dismissWelcome,
        onOpenPin = onOpenPin,
        onCreate = onCreate,
        onJoin = onJoin,
    )
}

@Composable
fun EntryScreen(
    state: EntryViewModel.UiState,
    onKeyChange: (String) -> Unit,
    onSubmitKey: () -> Unit,
    onRetry: () -> Unit,
    onForget: () -> Unit,
    onDismissWelcome: () -> Unit,
    onOpenPin: (Int) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {
        when (val c = state.content) {
            EntryViewModel.Content.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(32.dp), strokeWidth = 3.dp)
            EntryViewModel.Content.Unreachable -> Unreachable(onRetry)
            is EntryViewModel.Content.Landing -> Landing(c, onKeyChange, onSubmitKey, onCreate)
            is EntryViewModel.Content.Cards -> Cards(c, onOpenPin, onJoin, onForget)
        }
        state.welcome?.let { WelcomeDialog(it.name, it.welcome, onDismissWelcome) }
    }
}

@Composable
private fun Landing(state: EntryViewModel.Content.Landing, onKeyChange: (String) -> Unit, onSubmit: () -> Unit, onCreate: () -> Unit) {
    val brand = stringResource(R.string.brand)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 32.dp).enter(Enter.Rise, durationMs = 420),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(brand, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.landing_line), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DlTextField(
            value = state.key,
            onValueChange = onKeyChange,
            label = stringResource(R.string.key_label),
            errorText = when {
                state.wrong -> stringResource(R.string.key_wrong)
                state.offline -> stringResource(R.string.cant_reach, brand)
                else -> null
            },
            centeredMono = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        )
        DlFilledButton(
            stringResource(R.string.enter),
            onSubmit,
            Modifier.fillMaxWidth(),
            enabled = !state.busy && state.key.isNotBlank(),
            height = 48.dp,
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.have_invite), fontSize = 14.sp)
            TextButton(onClick = onCreate) { StrongLink(stringResource(R.string.create_universe_link)) }
        }
    }
}

@Composable
private fun Cards(state: EntryViewModel.Content.Cards, onOpenPin: (Int) -> Unit, onJoin: () -> Unit, onForget: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        UniverseTitle(state.view.name, Modifier.padding(bottom = 22.dp).enter(Enter.Rise, durationMs = 420))
        // The cards rise in turn (web .dl-entry-cards: 120 ms, 200 ms).
        state.view.users.forEachIndexed { i, u -> Box(Modifier.enter(Enter.Rise, 120 + 80 * i, 380)) { UserCardRow(u, i) { onOpenPin(u.id) } } }
        if (state.view.canJoin) Box(Modifier.enter(Enter.Rise, 120 + 80 * state.view.users.size, 380)) { JoinCardRow(state.view.name, onJoin) }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onForget, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            StrongLink(stringResource(R.string.use_different_key))
        }
    }
}

@Composable
private fun Unreachable(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.brand), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            stringResource(R.string.cant_reach, stringResource(R.string.brand)),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        DlFilledButton(stringResource(R.string.try_again), onRetry)
    }
}

/** The web's strong link: ink, 700, underlined. */
@Composable
internal fun StrongLink(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textDecoration = TextDecoration.Underline)
}
