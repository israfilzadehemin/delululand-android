package com.delulutopia.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.delulutopia.app.settings.DeviceSettingsRepository
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private fun repository(scope: TestScope): DeviceSettingsRepository {
        val file = File(folder.root, "settings.preferences_pb")
        return DeviceSettingsRepository(PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { file })
    }

    @Test
    fun `theme follows the device until chosen`() = runTest(UnconfinedTestDispatcher()) {
        assertThat(repository(this).themeMode.first()).isEqualTo(ThemeMode.System)
    }

    @Test
    fun `a chosen theme is kept`() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository(this)
        repo.setThemeMode(ThemeMode.Dark)
        assertThat(repo.themeMode.first()).isEqualTo(ThemeMode.Dark)
    }
}
