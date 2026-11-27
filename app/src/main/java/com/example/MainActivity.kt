package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.model.normalizeVersionTag
import com.example.data.update.AppUpdateManager
import com.example.ui.ScreensaverViewModel
import com.example.ui.screens.ScreensaverRoute
import com.example.ui.theme.PlasmaSkyTheme

class MainActivity : ComponentActivity() {
  private val viewModel: ScreensaverViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    maybeShowPostUpdateToast(intent)

    setContent {
      PlasmaSkyTheme {
        var showSettings by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(showSettings) {
          val controller = WindowCompat.getInsetsController(window, window.decorView)
          controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
          if (showSettings) {
            controller.show(WindowInsetsCompat.Type.systemBars())
          } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
          }
        }

        ScreensaverRoute(
          viewModel = viewModel,
          isDreamMode = false,
          showSettings = showSettings,
          onToggleSettings = { showSettings = it },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    maybeShowPostUpdateToast(intent)
  }

  private fun maybeShowPostUpdateToast(launchIntent: Intent?) {
    val rawVersion =
      launchIntent?.getStringExtra(AppUpdateManager.EXTRA_RELAUNCHED_FROM_UPDATE_VERSION) ?: return
    launchIntent.removeExtra(AppUpdateManager.EXTRA_RELAUNCHED_FROM_UPDATE_VERSION)
    val versionTag = normalizeVersionTag(rawVersion.ifBlank { BuildConfig.VERSION_NAME })
    Toast.makeText(this, "PlasmaSky updated to v$versionTag", Toast.LENGTH_LONG).show()
  }
}
