package com.delulutopia.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.app.ui.SignedInApp
import com.delulutopia.app.ui.StartApp
import com.delulutopia.app.ui.UnreachableScreen
import com.delulutopia.core.data.SessionState
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
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
                CompositionLocalProvider(LocalServerBaseUrl provides BuildConfig.BASE_URL) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        when (val session = ready.session) {
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
