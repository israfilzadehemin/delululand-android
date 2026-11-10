package com.delulutopia.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.delulutopia.core.designsystem.theme.DlShape

/** Snackbars replace the web's toasts (§3): inverse surface, 8 dp corners, optional text action. */
@Composable
fun DlSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> DlSnackbar(data) }
}

@Composable
fun DlSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val label = data.visuals.actionLabel
    Snackbar(
        modifier = modifier,
        shape = DlShape.snackbar,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        tonalElevation = 0.dp,
        action = if (label != null) {
            {
                TextButton(onClick = { data.performAction() }) {
                    Text(label, color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            null
        },
    ) { Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)) }
}

/** Static snackbar for previews and screenshot tests. */
@Composable
fun DlSnackbar(message: String, modifier: Modifier = Modifier, actionLabel: String? = null) {
    Snackbar(
        modifier = modifier,
        shape = DlShape.snackbar,
        containerColor = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        action = if (actionLabel != null) {
            { TextButton(onClick = {}) { Text(actionLabel, color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold) } }
        } else {
            null
        },
    ) { Text(message, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)) }
}
