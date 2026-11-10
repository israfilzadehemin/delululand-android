package com.delulutopia.core.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the catalog in both themes (A4). CI uploads the PNGs for review against the canvas. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h2400dp-xxhdpi")
class ComponentScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun catalog_light() {
        compose.setContent { DelulutopiaTheme(darkTheme = false) { ComponentCatalog() } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/designsystem_catalog_light.png")
    }

    @Test
    fun catalog_dark() {
        compose.setContent { DelulutopiaTheme(darkTheme = true) { ComponentCatalog() } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/designsystem_catalog_dark.png")
    }
}
