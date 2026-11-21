package com.example.dream

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.ScreensaverViewModel
import com.example.ui.screens.ScreensaverRoute
import com.example.ui.theme.PlasmaSkyTheme

class WeatherDreamService :
  DreamService(),
  LifecycleOwner,
  SavedStateRegistryOwner,
  ViewModelStoreOwner {

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val savedStateRegistryController = SavedStateRegistryController.create(this)
  private val dreamViewModelStore = ViewModelStore()

  override val lifecycle: Lifecycle
    get() = lifecycleRegistry

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateRegistryController.savedStateRegistry

  override val viewModelStore: ViewModelStore
    get() = dreamViewModelStore

  override fun onCreate() {
    super.onCreate()
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    isInteractive = false
    isFullscreen = true
    isScreenBright = true

    val decorView = window.decorView
    decorView.setViewTreeLifecycleOwner(this)
    decorView.setViewTreeSavedStateRegistryOwner(this)
    decorView.setViewTreeViewModelStoreOwner(this)

    val viewModel =
      ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(application),
      )[ScreensaverViewModel::class.java]

    val composeView =
      ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
          PlasmaSkyTheme {
            ScreensaverRoute(
              viewModel = viewModel,
              isDreamMode = true,
              showSettings = false,
              onToggleSettings = {},
            )
          }
        }
      }

    setContentView(composeView)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
  }

  override fun onDreamingStarted() {
    super.onDreamingStarted()
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
  }

  override fun onDreamingStopped() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    super.onDreamingStopped()
  }

  override fun onDetachedFromWindow() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    super.onDetachedFromWindow()
  }

  override fun onDestroy() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    dreamViewModelStore.clear()
    super.onDestroy()
  }
}
