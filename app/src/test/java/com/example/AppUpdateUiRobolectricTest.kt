package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import com.example.data.local.UserSettings
import com.example.ui.AppUpdateUiState
import com.example.ui.CitySearchUiState
import com.example.ui.screens.SettingsOverlay
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.PlasmaSkyTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateUiRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun `clicking update button transitions cleanly into indeterminate and determinate downloading states`() {
    var updateState by
      mutableStateOf<AppUpdateUiState>(
        AppUpdateUiState.UpdateAvailable(
          currentVersion = "1.06",
          latestVersion = "1.07",
          downloadUrl = "https://github.com/example/repo/releases/download/v1.07/PlasmaSky-v1.07.apk",
        )
      )

    composeTestRule.setContent {
      PlasmaSkyTheme {
        SettingsOverlay(
          settings = UserSettings(),
          citySearchState = CitySearchUiState(),
          activeAlerts = emptyList(),
          accentColor = AuroraCyan,
          onDismiss = {},
          onQueryChange = {},
          onSearchSubmit = {},
          onSelectCity = {},
          onSelectSavedLocation = {},
          onRemoveSavedLocation = {},
          onToggleAutoRotateLocations = {},
          onSelectUnit = {},
          onSelectWindUnit = {},
          onToggle24Hour = {},
          onToggle7DayForecast = {},
          onToggleBottomBarTemp24h = {},
          onToggleBottomBarWind = {},
          onToggleBottomBarAirQuality = {},
          onToggleBottomBarPrecipitation = {},
          onToggleSevereAlerts = {},
          onSelectDriftIntensity = {},
          onSelectDriftMotionMode = {},
          onSelectTextColor = {},
          onRefreshWeather = {},
          appUpdateState = updateState,
          onCheckForAppUpdate = {},
          onDownloadAndInstallUpdate = { latest, _ ->
            updateState =
              AppUpdateUiState.Downloading(
                latestVersion = latest,
                downloadedBytes = 0L,
                totalBytes = -1L,
              )
          },
          onConsumeAutoInstallLaunch = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    val downloadButton = composeTestRule.onNodeWithTag("download_install_update_button")
    downloadButton.performScrollTo()
    downloadButton.assertIsDisplayed()
    downloadButton.performClick()
    composeTestRule.waitForIdle()

    // Simulate progress chunks
    updateState =
      AppUpdateUiState.Downloading(
        latestVersion = "1.07",
        downloadedBytes = 65_536L,
        totalBytes = 12_000_000L,
      )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Downloading v1.07... 0%", substring = true).assertIsDisplayed()

    updateState =
      AppUpdateUiState.Downloading(
        latestVersion = "1.07",
        downloadedBytes = 6_000_000L,
        totalBytes = 12_000_000L,
      )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Downloading v1.07... 50%", substring = true).assertIsDisplayed()

    val tempApk = File.createTempFile("test_update", ".apk")
    try {
      tempApk.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
      updateState =
        AppUpdateUiState.ReadyToInstall(
          latestVersion = "1.07",
          apkFile = tempApk,
          autoLaunchInstall = false,
          incompatibilityReason = null,
        )
      composeTestRule.waitForIdle()
    } finally {
      tempApk.delete()
    }
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun `dpad focus flow from check updates to update button to downloading does not crash`() {
    var updateState by mutableStateOf<AppUpdateUiState>(AppUpdateUiState.Idle)

    composeTestRule.setContent {
      PlasmaSkyTheme {
        SettingsOverlay(
          settings = UserSettings(),
          citySearchState = CitySearchUiState(),
          activeAlerts = emptyList(),
          accentColor = AuroraCyan,
          onDismiss = {},
          onQueryChange = {},
          onSearchSubmit = {},
          onSelectCity = {},
          onSelectSavedLocation = {},
          onRemoveSavedLocation = {},
          onToggleAutoRotateLocations = {},
          onSelectUnit = {},
          onSelectWindUnit = {},
          onToggle24Hour = {},
          onToggle7DayForecast = {},
          onToggleBottomBarTemp24h = {},
          onToggleBottomBarWind = {},
          onToggleBottomBarAirQuality = {},
          onToggleBottomBarPrecipitation = {},
          onToggleSevereAlerts = {},
          onSelectDriftIntensity = {},
          onSelectDriftMotionMode = {},
          onSelectTextColor = {},
          onRefreshWeather = {},
          appUpdateState = updateState,
          onCheckForAppUpdate = { updateState = AppUpdateUiState.Checking },
          onDownloadAndInstallUpdate = { latest, _ ->
            updateState =
              AppUpdateUiState.Downloading(
                latestVersion = latest,
                downloadedBytes = 0L,
                totalBytes = -1L,
              )
          },
          onConsumeAutoInstallLaunch = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    val checkButton = composeTestRule.onNodeWithTag("check_app_update_button")
    checkButton.performScrollTo()
    checkButton.performKeyInput { pressKey(Key.DirectionCenter) }
    composeTestRule.waitForIdle()

    updateState =
      AppUpdateUiState.UpdateAvailable(
        currentVersion = "1.06",
        latestVersion = "1.07",
        downloadUrl = "https://github.com/example/repo/releases/download/v1.07/PlasmaSky-v1.07.apk",
      )
    composeTestRule.waitForIdle()

    val downloadButton = composeTestRule.onNodeWithTag("download_install_update_button")
    downloadButton.performKeyInput { pressKey(Key.DirectionCenter) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `allow app installs shortcut shows in idle state when denied and disappears once granted`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    org.robolectric.Shadows.shadowOf(context.packageManager).setCanRequestPackageInstalls(false)

    lateinit var registry: androidx.lifecycle.LifecycleRegistry
    val lifecycleOwner =
      object : androidx.lifecycle.LifecycleOwner {
        override val lifecycle: androidx.lifecycle.Lifecycle
          get() = registry
      }
    registry = androidx.lifecycle.LifecycleRegistry(lifecycleOwner).apply {
      currentState = androidx.lifecycle.Lifecycle.State.RESUMED
    }

    composeTestRule.setContent {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
      ) {
        PlasmaSkyTheme {
          SettingsOverlay(
            settings = UserSettings(),
            citySearchState = CitySearchUiState(),
            activeAlerts = emptyList(),
            accentColor = AuroraCyan,
            onDismiss = {},
            onQueryChange = {},
            onSearchSubmit = {},
            onSelectCity = {},
            onSelectSavedLocation = {},
            onRemoveSavedLocation = {},
            onToggleAutoRotateLocations = {},
            onSelectUnit = {},
            onSelectWindUnit = {},
            onToggle24Hour = {},
            onToggle7DayForecast = {},
            onToggleBottomBarTemp24h = {},
            onToggleBottomBarWind = {},
            onToggleBottomBarAirQuality = {},
            onToggleBottomBarPrecipitation = {},
            onToggleSevereAlerts = {},
            onSelectDriftIntensity = {},
            onSelectDriftMotionMode = {},
            onSelectTextColor = {},
            onRefreshWeather = {},
            appUpdateState = AppUpdateUiState.Idle,
            onCheckForAppUpdate = {},
            onDownloadAndInstallUpdate = { _, _ -> },
            onConsumeAutoInstallLaunch = {},
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("Grant permission to PlasmaSky to enable updating from within the app directly.")
      .performScrollTo()
      .assertIsDisplayed()
    val permButton = composeTestRule.onNodeWithTag("grant_install_permission_button")
    permButton.performScrollTo()
    permButton.assertIsDisplayed()
    composeTestRule.onNodeWithText("Grant Permission").assertIsDisplayed()

    // Simulate user granting unknown app installs in Android Settings and returning to PlasmaSky
    org.robolectric.Shadows.shadowOf(context.packageManager).setCanRequestPackageInstalls(true)
    composeTestRule.runOnUiThread {
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_PAUSE)
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("grant_install_permission_button").assertDoesNotExist()
  }
}
