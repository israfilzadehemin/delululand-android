package com.delulutopia.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.app.ui.DelulutopiaApp
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.delulutopia.core.designsystem.theme.isDark
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
                DelulutopiaApp(themeMode = ready.themeMode, onThemeModeChange = viewModel::setThemeMode)
            }
        }
    }
}
