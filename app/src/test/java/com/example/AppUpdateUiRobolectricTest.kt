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
  fun `permission helper text and shortcuts adapt across all four permission states and vanish when both are granted`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    org.robolectric.Shadows.shadowOf(context.packageManager).setCanRequestPackageInstalls(false)
    org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)

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

    val updatesSentence = "Allow Updates lets PlasmaSky install releases directly from inside the app."
    val relaunchSentence =
      "Allow Auto-Relaunch lets PlasmaSky reopen automatically after an update instead of closing until you launch it manually."

    // 1. Neither permission granted: both helper sentences and both buttons visible
    composeTestRule.waitForIdle()
    val helperNode = composeTestRule.onNodeWithTag("update_permissions_helper_text")
    helperNode.performScrollTo()
    helperNode.assertIsDisplayed()
    composeTestRule
      .onNodeWithText("$updatesSentence $relaunchSentence")
      .assertIsDisplayed()
    val updatesBtn = composeTestRule.onNodeWithTag("grant_install_permission_button")
    updatesBtn.performScrollTo()
    updatesBtn.assertIsDisplayed()
    composeTestRule.onNodeWithText("Allow Updates").assertIsDisplayed()
    val relaunchBtn = composeTestRule.onNodeWithTag("grant_relaunch_permission_button")
    relaunchBtn.performScrollTo()
    relaunchBtn.assertIsDisplayed()
    composeTestRule.onNodeWithText("Allow Auto-Relaunch").assertIsDisplayed()

    // 2. Updates granted, Auto-Relaunch still missing: only relaunch sentence & button visible
    org.robolectric.Shadows.shadowOf(context.packageManager).setCanRequestPackageInstalls(true)
    composeTestRule.runOnUiThread {
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_PAUSE)
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("grant_install_permission_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("grant_relaunch_permission_button").assertIsDisplayed()
    composeTestRule.onNodeWithText(relaunchSentence).assertIsDisplayed()

    // 3. Auto-Relaunch granted, Updates missing: only updates sentence & button visible
    org.robolectric.Shadows.shadowOf(context.packageManager).setCanRequestPackageInstalls(false)
    org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
    composeTestRule.runOnUiThread {
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_PAUSE)
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("grant_install_permission_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("grant_relaunch_permission_button").assertDoesNotExist()
    composeTestRule.onNodeWithText(updatesSentence).assertIsDisplayed()

    // 4. Both granted: helper text and both permission buttons disappear completely
    org.robolectric.Shadows.shadowOf(context.packageManager).setCanRequestPackageInstalls(true)
    composeTestRule.runOnUiThread {
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_PAUSE)
      registry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("grant_install_permission_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("grant_relaunch_permission_button").assertDoesNotExist()
    composeTestRule.onNodeWithTag("update_permissions_helper_text").assertDoesNotExist()
  }

  @Test
  fun `package replaced receiver launches MainActivity only for valid non-expired in-app updates`() {
    val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    val receiver = com.example.data.update.AppPackageReplacedReceiver()

    // Unrelated update (no pending marker) must not launch MainActivity
    com.example.data.update.AppUpdateManager.clearPendingUpdateRelaunch(app)
    receiver.onReceive(app, android.content.Intent(android.content.Intent.ACTION_MY_PACKAGE_REPLACED))
    org.junit.Assert.assertNull(org.robolectric.Shadows.shadowOf(app).nextStartedActivity)

    // Expired marker (>10 min) must not launch MainActivity
    com.example.data.update.AppUpdateManager.markPendingUpdateRelaunch(
      context = app,
      targetVersion = "1.12",
      nowMs = System.currentTimeMillis() - (com.example.data.update.AppUpdateManager.PENDING_RELAUNCH_TTL_MS + 5_000L),
    )
    receiver.onReceive(app, android.content.Intent(android.content.Intent.ACTION_MY_PACKAGE_REPLACED))
    org.junit.Assert.assertNull(org.robolectric.Shadows.shadowOf(app).nextStartedActivity)

    // Valid recent update marker triggers one-shot relaunch of MainActivity with version extra
    com.example.data.update.AppUpdateManager.markPendingUpdateRelaunch(
      context = app,
      targetVersion = "1.12",
    )
    receiver.onReceive(app, android.content.Intent(android.content.Intent.ACTION_MY_PACKAGE_REPLACED))
    val launched = org.robolectric.Shadows.shadowOf(app).nextStartedActivity
    org.junit.Assert.assertNotNull(launched)
    org.junit.Assert.assertEquals(MainActivity::class.java.name, launched.component?.className)
    org.junit.Assert.assertEquals(
      "1.12",
      launched.getStringExtra(com.example.data.update.AppUpdateManager.EXTRA_RELAUNCHED_FROM_UPDATE_VERSION),
    )

    // Subsequent duplicate broadcast must not launch again (one-shot consumption)
    receiver.onReceive(app, android.content.Intent(android.content.Intent.ACTION_MY_PACKAGE_REPLACED))
    org.junit.Assert.assertNull(org.robolectric.Shadows.shadowOf(app).nextStartedActivity)
  }
}
