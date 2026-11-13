package com.delulutopia.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.delulutopia.app.R
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.i18n.t
import com.delulutopia.feature.start.EntryDestination
import com.delulutopia.feature.start.startGraph

/** Signed out: key, cards, PIN, create and join (feature:start). */
@Composable
fun StartApp() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = EntryDestination) { startGraph(nav) }
}

/** The start check could not reach the server (no answer, or 5xx). */
@Composable
fun UnreachableScreen(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.brand), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(t("app.cantReachDelulutopia"), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        DlFilledButton(t("entry.tryAgain"), onRetry)
    }
}
