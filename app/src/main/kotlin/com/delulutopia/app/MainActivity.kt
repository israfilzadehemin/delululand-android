package com.delulutopia.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.app.notify.PendingOpen
import com.delulutopia.app.ui.SignedInApp
import com.delulutopia.app.ui.StartApp
import com.delulutopia.app.ui.UnreachableScreen
import com.delulutopia.core.data.SessionState
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.motion.LocalPageLanding
import com.delulutopia.core.designsystem.motion.Motion
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.delulutopia.core.designsystem.theme.isDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        takeOpen(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = state as? MainUiState.Ready ?: return@setContent
            val dark = ready.themeMode.isDark()

            // P1: edge-to-edge everywhere; bar icons follow the app theme, not the device's.
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            DelulutopiaTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalServerBaseUrl provides serverUrl()) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Column {
                            viewModel.testBanner?.let { TestBanner(it) }
                            Box(
                                Modifier.weight(1f).then(
                                    if (viewModel.testBanner !=
                                        null
                                    ) {
                                        Modifier.consumeWindowInsets(WindowInsets.statusBars)
                                    } else {
                                        Modifier
                                    },
                                ),
                            ) {
                                // Into the universe (PIN accepted): the start screens grow past you as the app comes up from just behind them; log out reverses it.
                                AnimatedContent(
                                    targetState = ready.session,
                                    contentKey = { it::class },
                                    transitionSpec = {
                                        val inward = targetState is SessionState.SignedIn
                                        val ms = Motion.STACK_MS
                                        (
                                            fadeIn(tween(ms, easing = Motion.EmphasizedDecelerate)) +
                                                scaleIn(tween(ms, easing = Motion.EmphasizedDecelerate), initialScale = if (inward) 0.94f else 1.06f)
                                            ) togetherWith (
                                            fadeOut(tween(ms / 2, easing = Motion.EmphasizedAccelerate)) +
                                                scaleOut(tween(ms, easing = Motion.EmphasizedAccelerate), targetScale = if (inward) 1.06f else 0.94f)
                                            )
                                    },
                                    label = "session",
                                ) { session ->
                                    // Its pages arrive with the zoom: their own enter animations wait for it (LocalPageLanding).
                                    val arriving = transition.targetState == EnterExitState.Visible && transition.currentState != EnterExitState.Visible
                                    val landing = remember { if (arriving) SystemClock.uptimeMillis() + Motion.STACK_MS else null }
                                    CompositionLocalProvider(LocalPageLanding provides landing) {
                                        when (session) {
                                            is SessionState.SignedIn -> SignedInApp(
                                                session = session.session,
                                                themeMode = ready.themeMode,
                                                onThemeModeChange = viewModel::setThemeMode,
                                                onLogout = viewModel::logout,
                                            )
                                            SessionState.Unreachable -> UnreachableScreen(onRetry = viewModel::refreshSession)
                                            else -> StartApp()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeOpen(intent)
    }

    /** A tapped notification's target, opened once Home is up. */
    private fun takeOpen(intent: Intent?) {
        intent?.getStringExtra(PendingOpen.EXTRA)?.let { PendingOpen.path.value = it }
        intent?.removeExtra(PendingOpen.EXTRA)
    }
}

/** Shown on every screen while the API runs with shortened timers (web TestBanner). */
@Composable
private fun TestBanner(text: String) {
    Text(
        text,
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
    )
}
