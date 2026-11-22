package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntSize
import com.example.data.local.UserSettings
import com.example.data.model.DailyForecast
import com.example.data.model.DriftMotionMode
import com.example.data.model.TemperatureUnit
import com.example.data.model.TextColorOption
import com.example.data.model.WeatherAlert
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherSnapshot
import com.example.ui.ClockDisplay
import com.example.ui.ScreensaverViewModel
import com.example.ui.WeatherUiState
import com.example.ui.components.PlasmaShieldState
import com.example.ui.components.RotatingHourlyCubePanel
import com.example.ui.components.WeatherGlyph
import com.example.ui.components.rememberPlasmaShieldState
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.BrightCoral
import com.example.ui.theme.BrightGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftFrostText
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SubtleOutline
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private enum class HeroScaleMode {
  STANDARD,
  EXPANDED_WITH_PANEL,
  WIDE_CENTERED,
  FULL_MAX,
}

@Composable
fun ScreensaverRoute(
  viewModel: ScreensaverViewModel,
  isDreamMode: Boolean,
  showSettings: Boolean,
  onToggleSettings: (Boolean) -> Unit,
) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
  val clockDisplay by viewModel.clockDisplay.collectAsStateWithLifecycle()
  val citySearchState by viewModel.citySearchState.collectAsStateWithLifecycle()
  val locationSwapBanner by viewModel.locationSwapBanner.collectAsStateWithLifecycle()

  var dynamicDriftBounds by remember { mutableStateOf<Pair<Float, Float>?>(null) }

  LaunchedEffect(weatherState is WeatherUiState.Ready) {
    if (weatherState !is WeatherUiState.Ready) {
      dynamicDriftBounds = null
    }
  }

  val plasma =
    rememberPlasmaShieldState(
      driftIntensity = settings.driftIntensity,
      driftMotionMode = settings.driftMotionMode,
      textColorOption = settings.textColorOption,
      maxTravelXDp = dynamicDriftBounds?.first ?: settings.driftIntensity.maxOffsetDp,
      maxTravelYDp = dynamicDriftBounds?.second ?: (settings.driftIntensity.maxOffsetDp * 0.65f),
    )

  var tvFramePreviewOnPhone by rememberSaveable { mutableStateOf(true) }
  var showHintPill by remember { mutableStateOf(!isDreamMode) }
  var showSettingsButton by remember { mutableStateOf(!isDreamMode) }
  var settingsInteractionTick by remember { mutableIntStateOf(0) }

  val rootFocusRequester = remember { FocusRequester() }
  val settingsButtonFocusRequester = remember { FocusRequester() }

  LaunchedEffect(isDreamMode) {
    if (!isDreamMode) {
      delay(6_000L)
      showHintPill = false
    }
  }

  LaunchedEffect(isDreamMode, showSettings, settingsInteractionTick) {
    if (isDreamMode || showSettings) {
      showSettingsButton = false
    } else {
      showSettingsButton = true
      delay(30_000L)
      showSettingsButton = false
    }
  }

  val configuration = LocalConfiguration.current
  val isTvUiMode =
    (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

  BoxWithConstraints(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .testTag("screensaver_root")
  ) {
    val isPortraitPhone = !isDreamMode && !isTvUiMode && maxWidth < 600.dp && maxHeight > maxWidth
    val showTvFrameStage = isPortraitPhone && tvFramePreviewOnPhone

    LaunchedEffect(isDreamMode, showSettings, showSettingsButton, showTvFrameStage) {
      if (!isDreamMode && !showSettings && !showTvFrameStage) {
        if (showSettingsButton) {
          withFrameNanos {}
          runCatching { settingsButtonFocusRequester.requestFocus() }
        } else {
          runCatching { rootFocusRequester.requestFocus() }
        }
      }
    }

    val tapToOpenModifier =
      if (!isDreamMode && !showSettings && !showTvFrameStage) {
        Modifier.clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = { onToggleSettings(true) },
        )
      } else {
        Modifier
      }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .then(
            if (!isDreamMode && !showSettings && !showTvFrameStage) {
              Modifier.focusRequester(rootFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                  when (event.key) {
                    Key.DirectionLeft -> {
                      if (event.type == KeyEventType.KeyDown) {
                        if (settings.savedLocations.size > 1) {
                          viewModel.cycleSavedLocation(step = -1, showSwapToast = true)
                        }
                        showSettingsButton = true
                        settingsInteractionTick++
                      }
                      true
                    }
                    Key.DirectionRight -> {
                      if (event.type == KeyEventType.KeyDown) {
                        if (settings.savedLocations.size > 1) {
                          viewModel.cycleSavedLocation(step = 1, showSwapToast = true)
                        }
                        showSettingsButton = true
                        settingsInteractionTick++
                      }
                      true
                    }
                    Key.DirectionUp,
                    Key.DirectionDown -> {
                      if (event.type == KeyEventType.KeyDown) {
                        showSettingsButton = true
                        settingsInteractionTick++
                      }
                      true
                    }
                    Key.DirectionCenter,
                    Key.Enter,
                    Key.Menu -> {
                      if (event.type == KeyEventType.KeyUp) {
                        onToggleSettings(true)
                      }
                      true
                    }
                    else -> false
                  }
                }
            } else {
              Modifier
            }
          )
          .then(tapToOpenModifier)
    ) {
      if (showTvFrameStage) {
        TvPreviewPhoneStage(
          weatherState = weatherState,
          settings = settings,
          clockDisplay = clockDisplay,
          plasma = plasma,
          onUpdateDriftBounds = { maxX, maxY ->
            val next = maxX to maxY
            if (dynamicDriftBounds != next) {
              dynamicDriftBounds = next
            }
          },
          onOpenSettings = { onToggleSettings(true) },
          onRefresh = viewModel::refreshWeather,
          onPrevLocation = { viewModel.cycleSavedLocation(step = -1, showSwapToast = false) },
          onNextLocation = { viewModel.cycleSavedLocation(step = 1, showSwapToast = false) },
          onToggleUnit = {
            val next =
              if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
                TemperatureUnit.CELSIUS
              } else {
                TemperatureUnit.FAHRENHEIT
              }
            viewModel.setTemperatureUnit(next)
          },
          onToggleDriftMotionMode = {
            val next =
              if (settings.driftMotionMode == DriftMotionMode.CLOCKWISE) {
                DriftMotionMode.DVD_BOUNCE
              } else {
                DriftMotionMode.CLOCKWISE
              }
            viewModel.setDriftMotionMode(next)
          },
          onSelectTextColor = viewModel::setTextColorOption,
          onSwitchToPortraitFill = { tvFramePreviewOnPhone = false },
        )
      } else {
        when (val state = weatherState) {
          is WeatherUiState.Loading -> {
            LoadingWeatherStage(plasma = plasma)
          }

          is WeatherUiState.Error -> {
            ErrorWeatherStage(
              message = state.message,
              plasma = plasma,
              showRetry = !isDreamMode,
              onRetry = viewModel::refreshWeather,
            )
          }

          is WeatherUiState.Ready -> {
            ScreensaverWeatherCanvas(
              snapshot = state.snapshot,
              settings = settings,
              clockDisplay = clockDisplay,
              plasma = plasma,
              forceTvWide = false,
              onUpdateDriftBounds = { maxX, maxY ->
                val next = maxX to maxY
                if (dynamicDriftBounds != next) {
                  dynamicDriftBounds = next
                }
              },
            )
          }
        }

        // Brief location swap feedback badge when D-pad Left/Right swaps saved cities
        AnimatedVisibility(
          visible = !isDreamMode && !showSettings && locationSwapBanner != null,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .safeDrawingPadding()
              .padding(bottom = 18.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.82f),
            border = BorderStroke(1.dp, plasma.primaryAccent.copy(alpha = 0.55f)),
            modifier = Modifier.testTag("location_swap_banner"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = plasma.primaryAccent,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = locationSwapBanner.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = plasma.primaryTextColor,
              )
              Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = plasma.primaryAccent,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        // Discreet interactive settings trigger & fading hint (auto-hides after 30s; wakes on D-pad)
        AnimatedVisibility(
          visible = !isDreamMode && !showSettings && showSettingsButton,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier =
            Modifier.align(Alignment.TopEnd)
              .safeDrawingPadding()
              .padding(20.dp),
        ) {
          var settingsButtonFocused by remember { mutableStateOf(false) }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (isPortraitPhone) {
              OutlinedButton(
                onClick = { tvFramePreviewOnPhone = true },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("switch_tv_frame_button"),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, plasma.primaryAccent.copy(alpha = 0.45f)),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tv,
                  contentDescription = null,
                  tint = plasma.primaryAccent,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "TV 16:9 View",
                  style = MaterialTheme.typography.labelSmall,
                  color = plasma.primaryTextColor,
                )
              }
            } else {
              AnimatedVisibility(visible = showHintPill, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = Color.Black.copy(alpha = 0.72f),
                  border = BorderStroke(1.dp, plasma.primaryAccent.copy(alpha = 0.35f)),
                ) {
                  Text(
                    text = "Tap or press OK for City & Plasma Settings",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    color = plasma.primaryTextColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                  )
                }
              }
            }

            IconButton(
              onClick = { onToggleSettings(true) },
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = if (settingsButtonFocused) 0.9f else 0.55f))
                  .border(
                    width = if (settingsButtonFocused) 2.dp else 1.dp,
                    color = if (settingsButtonFocused) plasma.primaryAccent else SubtleOutline.copy(alpha = 0.5f),
                    shape = CircleShape,
                  )
                  .focusRequester(settingsButtonFocusRequester)
                  .onFocusChanged { settingsButtonFocused = it.isFocused }
                  .focusable()
                  .testTag("open_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Open settings",
                tint = if (settingsButtonFocused) plasma.primaryAccent else plasma.secondaryTextColor,
              )
            }
          }
        }
      }
    }

    AnimatedVisibility(visible = !isDreamMode && showSettings, enter = fadeIn(), exit = fadeOut()) {
      val currentAlerts =
        (weatherState as? WeatherUiState.Ready)?.snapshot?.alerts.orEmpty()
      SettingsOverlay(
        settings = settings,
        citySearchState = citySearchState,
        activeAlerts = currentAlerts,
        accentColor = AuroraCyan,
        onDismiss = { onToggleSettings(false) },
        onQueryChange = viewModel::updateSearchQuery,
        onSearchSubmit = viewModel::submitCitySearch,
        onSelectCity = viewModel::selectCity,
        onSelectSavedLocation = viewModel::selectSavedLocation,
        onRemoveSavedLocation = viewModel::removeSavedLocation,
        onToggleAutoRotateLocations = viewModel::setAutoRotateLocations,
        onSelectUnit = viewModel::setTemperatureUnit,
        onSelectWindUnit = viewModel::setWindSpeedUnit,
        onToggle24Hour = viewModel::setUse24HourClock,
        onToggle7DayForecast = viewModel::setShow7DayForecast,
        onToggle24HourForecast = viewModel::setShow24HourForecast,
        onToggleSevereAlerts = viewModel::setShowSevereWeatherAlerts,
        onSelectDriftIntensity = viewModel::setDriftIntensity,
        onSelectDriftMotionMode = viewModel::setDriftMotionMode,
        onSelectTextColor = viewModel::setTextColorOption,
        onRefreshWeather = viewModel::refreshWeather,
      )
    }
  }
}

@Composable
private fun LoadingWeatherStage(plasma: PlasmaShieldState) {
  BoxWithConstraints(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .padding(horizontal = 24.dp, vertical = 20.dp)
        .testTag("loading_weather_stage"),
    contentAlignment = Alignment.Center,
  ) {
    val isCompact = maxWidth < 560.dp
    val graphicWidth = if (isCompact) 140.dp else 200.dp
    val graphicHeight = if (isCompact) 105.dp else 148.dp
    val titleSp = if (isCompact) 26.sp else 38.sp
    val loadingSp = if (isCompact) 20.sp else 28.sp

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp),
      modifier =
        Modifier.fillMaxWidth()
          .offset(x = plasma.primaryOffsetX, y = plasma.primaryOffsetY),
    ) {
      // Matches the cold-boot launch graphic (Sun, Cloud, 3 Raindrops) so cold boot transitions seamlessly
      Canvas(modifier = Modifier.size(width = graphicWidth, height = graphicHeight)) {
        val w = size.width
        val h = size.height
        val sunCenter = Offset(w * 0.68f, h * 0.34f)
        drawCircle(
          color = BrightGold.copy(alpha = 0.22f),
          radius = w * 0.20f,
          center = sunCenter,
        )
        drawCircle(
          color = BrightGold,
          radius = w * 0.145f,
          center = sunCenter,
        )

        val cloudColor = Color(0xFFF4F7FF)
        drawCircle(color = cloudColor, radius = w * 0.165f, center = Offset(w * 0.44f, h * 0.40f))
        drawCircle(color = cloudColor, radius = w * 0.135f, center = Offset(w * 0.31f, h * 0.48f))
        drawCircle(color = cloudColor, radius = w * 0.135f, center = Offset(w * 0.58f, h * 0.48f))
        drawRoundRect(
          color = cloudColor,
          topLeft = Offset(w * 0.20f, h * 0.41f),
          size = Size(w * 0.50f, h * 0.23f),
          cornerRadius = CornerRadius(h * 0.12f, h * 0.12f),
        )

        val dropW = w * 0.036f
        val dropH = h * 0.14f
        val dropRadius = CornerRadius(dropW / 2f, dropW / 2f)
        drawRoundRect(
          color = AuroraCyan,
          topLeft = Offset(w * 0.34f, h * 0.72f),
          size = Size(dropW, dropH),
          cornerRadius = dropRadius,
        )
        drawRoundRect(
          color = AuroraCyan,
          topLeft = Offset(w * 0.46f, h * 0.77f),
          size = Size(dropW, dropH),
          cornerRadius = dropRadius,
        )
        drawRoundRect(
          color = AuroraCyan,
          topLeft = Offset(w * 0.58f, h * 0.72f),
          size = Size(dropW, dropH),
          cornerRadius = dropRadius,
        )
      }

      AutoShrinkSingleLineText(
        text = "PlasmaGuard Weather",
        style =
          MaterialTheme.typography.displaySmall.copy(
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = titleSp,
            lineHeight = (titleSp.value + 4f).sp,
            letterSpacing = (-0.5).sp,
          ),
        color = SoftFrostText,
        minFontSize = 18.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().testTag("loading_brand_title"),
      )

      Text(
        text = "LOADING...",
        style =
          MaterialTheme.typography.headlineSmall.copy(
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = loadingSp,
            lineHeight = (loadingSp.value + 4f).sp,
            letterSpacing = 1.5.sp,
          ),
        color = AuroraCyan.copy(alpha = 0.65f + 0.35f * plasma.pulseFraction),
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun ErrorWeatherStage(
  message: String,
  plasma: PlasmaShieldState,
  showRetry: Boolean,
  onRetry: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().background(Color.Black).padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.offset(x = plasma.primaryOffsetX, y = plasma.primaryOffsetY),
    ) {
      Text(
        text = "Weather Feed Unavailable",
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 36.sp, lineHeight = 42.sp),
        color = plasma.primaryTextColor,
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, lineHeight = 30.sp),
        color = plasma.secondaryTextColor,
      )
      if (showRetry) {
        Button(
          onClick = onRetry,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("retry_weather_button"),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = plasma.primaryAccent,
              contentColor = ObsidianBlack,
            ),
        ) {
          Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Retry")
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TvPreviewPhoneStage(
  weatherState: WeatherUiState,
  settings: UserSettings,
  clockDisplay: ClockDisplay,
  plasma: PlasmaShieldState,
  onUpdateDriftBounds: (Float, Float) -> Unit,
  onOpenSettings: () -> Unit,
  onRefresh: () -> Unit,
  onPrevLocation: () -> Unit,
  onNextLocation: () -> Unit,
  onToggleUnit: () -> Unit,
  onToggleDriftMotionMode: () -> Unit,
  onSelectTextColor: (TextColorOption) -> Unit,
  onSwitchToPortraitFill: () -> Unit,
) {
  var showColorDropdown by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .safeDrawingPadding()
        .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Top Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Tv,
          contentDescription = null,
          tint = plasma.primaryAccent,
          modifier = Modifier.size(20.dp),
        )
        Column {
          Text(
            text = "PlasmaSky TV Preview",
            style = MaterialTheme.typography.titleMedium,
            color = SoftFrostText,
          )
          Text(
            text = "16:9 OLED / Plasma TV Simulation",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlateText,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = BorderStroke(1.dp, plasma.primaryAccent.copy(alpha = 0.45f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier =
              Modifier.size(7.dp)
                .clip(CircleShape)
                .background(plasma.primaryAccent.copy(alpha = 0.45f + 0.55f * plasma.pulseFraction))
          )
          Text(
            text = "DRIFT ACTIVE",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = plasma.primaryTextColor,
          )
        }
      }
    }

    // 16:9 Android TV Bezel Rendering Pure Black Widescreen Layout
    Surface(
      modifier = Modifier.fillMaxWidth().testTag("tv_bezel_preview"),
      shape = RoundedCornerShape(14.dp),
      color = Color.Black,
      border = BorderStroke(2.dp, Color(0xFF202736)),
      shadowElevation = 12.dp,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .padding(6.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color.Black)
              .clickable(onClick = onOpenSettings)
        ) {
          ScaledTvViewport(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
              when (weatherState) {
                is WeatherUiState.Loading ->
                  LoadingWeatherStage(plasma = plasma)
                is WeatherUiState.Error ->
                  ErrorWeatherStage(
                    message = weatherState.message,
                    plasma = plasma,
                    showRetry = false,
                    onRetry = onRefresh,
                  )
                is WeatherUiState.Ready ->
                  ScreensaverWeatherCanvas(
                    snapshot = weatherState.snapshot,
                    settings = settings,
                    clockDisplay = clockDisplay,
                    plasma = plasma,
                    forceTvWide = true,
                    onUpdateDriftBounds = onUpdateDriftBounds,
                  )
              }
            }
          }
        }
        // Subtle TV bezel chin & standby LED
        Box(
          modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(width = 18.dp, height = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(plasma.primaryAccent.copy(alpha = 0.6f))
          )
        }
      }
    }

    // Quick TV Preview Companion Controls
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(18.dp),
      color = MidnightSlate,
      border = BorderStroke(1.dp, SubtleOutline),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "TV Remote & Preview Controls",
          style = MaterialTheme.typography.titleMedium,
          color = SoftFrostText,
        )
        Text(
          text =
            "This emulator runs a phone profile, so PlasmaSky displays the true 16:9 Android TV screensaver above. On an Android TV or System Screensaver (DreamService), it fills the entire display automatically.",
          style = MaterialTheme.typography.bodyMedium,
          color = MutedSlateText,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onOpenSettings,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("open_settings_button"),
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = plasma.primaryAccent,
                contentColor = ObsidianBlack,
              ),
          ) {
            Icon(imageVector = Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("City & Plasma Settings", fontWeight = FontWeight.SemiBold)
          }

          if (settings.savedLocations.size > 1) {
            val currentIdx =
              settings.savedLocations
                .indexOfFirst { it.matches(settings.latitude, settings.longitude) }
                .let { if (it >= 0) it else 0 }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.Transparent,
              border = BorderStroke(1.dp, SubtleOutline),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = onPrevLocation,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("preview_prev_city_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Previous saved city",
                    tint = plasma.primaryAccent,
                  )
                }
                Text(
                  text = "${settings.cityName} (${currentIdx + 1}/${settings.savedLocations.size})",
                  style = MaterialTheme.typography.labelLarge,
                  color = SoftFrostText,
                )
                IconButton(
                  onClick = onNextLocation,
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("preview_next_city_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Next saved city",
                    tint = plasma.primaryAccent,
                  )
                }
              }
            }
          }

          OutlinedButton(
            onClick = onToggleUnit,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_unit_toggle_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SubtleOutline),
          ) {
            Text(
              text = "Unit: ${settings.temperatureUnit.symbol}",
              color = SoftFrostText,
            )
          }

          OutlinedButton(
            onClick = onToggleDriftMotionMode,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_motion_mode_toggle_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AuroraCyan.copy(alpha = 0.55f)),
          ) {
            Icon(
              imageVector = Icons.Rounded.OpenWith,
              contentDescription = null,
              tint = AuroraCyan,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Motion: ${settings.driftMotionMode.label}",
              color = SoftFrostText,
            )
          }

          Box {
            OutlinedButton(
              onClick = { showColorDropdown = true },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_color_shift_dropdown"),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, SubtleOutline),
            ) {
              Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = plasma.primaryAccent,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Text Color: ${settings.textColorOption.label}",
                color = SoftFrostText,
              )
              Spacer(modifier = Modifier.width(2.dp))
              Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = plasma.primaryAccent,
                modifier = Modifier.size(18.dp),
              )
            }

            DropdownMenu(
              expanded = showColorDropdown,
              onDismissRequest = { showColorDropdown = false },
              modifier = Modifier.background(ElevatedSurface),
            ) {
              TextColorOption.entries.forEach { option ->
                val isSelected = option == settings.textColorOption
                DropdownMenuItem(
                  text = {
                    Text(
                      text = option.label,
                      style = MaterialTheme.typography.bodyMedium,
                      color = SoftFrostText,
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                  },
                  trailingIcon =
                    if (isSelected) {
                      {
                        Icon(
                          imageVector = Icons.Rounded.Check,
                          contentDescription = "Selected",
                          tint = AuroraCyan,
                        )
                      }
                    } else {
                      null
                    },
                  onClick = {
                    onSelectTextColor(option)
                    showColorDropdown = false
                  },
                )
              }
            }
          }

          OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("quick_refresh_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SubtleOutline),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              tint = plasma.primaryAccent,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Refresh", color = SoftFrostText)
          }

          OutlinedButton(
            onClick = onSwitchToPortraitFill,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("switch_portrait_fill_button"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SubtleOutline),
          ) {
            Icon(
              imageVector = Icons.Rounded.Smartphone,
              contentDescription = null,
              tint = MutedSlateText,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Fill Portrait Screen", color = SoftFrostText)
          }
        }
      }
    }
  }
}

@Composable
private fun ScaledTvViewport(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val density = LocalDensity.current
  val tvDensity = remember(density.density) { Density(density = density.density, fontScale = 1f) }
  val virtualWidthPx = with(tvDensity) { 960.dp.roundToPx() }
  val virtualHeightPx = with(tvDensity) { 540.dp.roundToPx() }

  CompositionLocalProvider(LocalDensity provides tvDensity) {
    Layout(
      content = content,
      modifier = modifier.clipToBounds(),
    ) { measurables, constraints ->
      val targetWidth = constraints.maxWidth.coerceAtLeast(1)
      val scale = targetWidth.toFloat() / virtualWidthPx.toFloat()
      val targetHeight = (virtualHeightPx * scale).roundToInt().coerceAtLeast(1)

      val placeables =
        measurables.map { measurable ->
          measurable.measure(Constraints.fixed(virtualWidthPx, virtualHeightPx))
        }

      layout(targetWidth, targetHeight) {
        placeables.forEach { placeable ->
          placeable.placeWithLayer(0, 0) {
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = scale
            scaleY = scale
          }
        }
      }
    }
  }
}

@Composable
private fun ScaledContentBox(
  scale: Float,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Layout(
    content = content,
    modifier = modifier,
  ) { measurables, constraints ->
    val placeables = measurables.map { it.measure(constraints) }
    val rawW = placeables.maxOfOrNull { it.width } ?: 0
    val rawH = placeables.maxOfOrNull { it.height } ?: 0
    val scaledW = (rawW * scale).roundToInt().coerceAtLeast(1)
    val scaledH = (rawH * scale).roundToInt().coerceAtLeast(1)

    layout(scaledW, scaledH) {
      placeables.forEach { placeable ->
        placeable.placeWithLayer(0, 0) {
          transformOrigin = TransformOrigin(0f, 0f)
          scaleX = scale
          scaleY = scale
        }
      }
    }
  }
}

@Composable
private fun ScreensaverWeatherCanvas(
  snapshot: WeatherSnapshot,
  settings: UserSettings,
  clockDisplay: ClockDisplay,
  plasma: PlasmaShieldState,
  forceTvWide: Boolean,
  onUpdateDriftBounds: (Float, Float) -> Unit,
) {
  val showAlertsBanner = settings.showSevereWeatherAlerts && snapshot.alerts.isNotEmpty()
  val show7Day = settings.show7DayForecast
  val show24Hour = settings.show24HourForecast
  val isHeroOnly = !show7Day && !show24Hour
  val isDvdBounce = settings.driftMotionMode == DriftMotionMode.DVD_BOUNCE
  val contentScale = if (isDvdBounce) 0.80f else 1.0f

  BoxWithConstraints(
    modifier =
      Modifier.fillMaxSize()
        .then(if (forceTvWide) Modifier else Modifier.safeDrawingPadding())
        .padding(horizontal = 6.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    val density = LocalDensity.current
    val stageWidthDp = maxWidth.value
    val stageHeightDp = maxHeight.value
    val isTvWide = forceTvWide || maxWidth >= 600.dp
    val isShortLandscape = isTvWide && maxHeight < 470.dp

    var renderedSizePx by remember { mutableStateOf(IntSize.Zero) }

    val freeX =
      if (renderedSizePx.width > 0) {
        ((stageWidthDp - with(density) { renderedSizePx.width.toDp().value }) / 2f - 4f)
          .coerceAtLeast(1f)
      } else {
        settings.driftIntensity.maxOffsetDp
      }
    val freeY =
      if (renderedSizePx.height > 0) {
        ((stageHeightDp - with(density) { renderedSizePx.height.toDp().value }) / 2f - 4f)
          .coerceAtLeast(1f)
      } else {
        settings.driftIntensity.maxOffsetDp * 0.65f
      }

    val targetMaxX =
      if (isDvdBounce || isHeroOnly) freeX
      else minOf(freeX, settings.driftIntensity.maxOffsetDp)
    val targetMaxY =
      if (isDvdBounce || isHeroOnly) freeY
      else minOf(
        freeY,
        if (isShortLandscape) settings.driftIntensity.maxOffsetDp * 0.45f
        else settings.driftIntensity.maxOffsetDp * 0.65f,
      )

    LaunchedEffect(targetMaxX, targetMaxY) {
      onUpdateDriftBounds(targetMaxX, targetMaxY)
    }

    // Reserve gentle outer drift margin in Clockwise panel mode; DVD Bounce already shrinks 20%,
    // and Hero-Only hugs minimal borders so it hits all 4 edges of the TV.
    val clockwisePanelHPad =
      if (!isDvdBounce && !isHeroOnly) settings.driftIntensity.maxOffsetDp.dp else 0.dp
    val clockwisePanelVPad =
      if (!isDvdBounce && !isHeroOnly) {
        (if (isShortLandscape) settings.driftIntensity.maxOffsetDp * 0.45f else settings.driftIntensity.maxOffsetDp * 0.65f).dp
      } else {
        0.dp
      }

    // When both 7-Day and 24-Hour are OFF, show any active alert banner pinned at top so Hero keeps minimal bounds
    if (showAlertsBanner && isHeroOnly) {
      SevereWeatherAlertBanner(
        alerts = snapshot.alerts,
        plasma = plasma,
        isCompact = !isTvWide || isShortLandscape,
        modifier =
          Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }

    ScaledContentBox(
      scale = contentScale,
      modifier =
        Modifier.offset(x = plasma.primaryOffsetX, y = plasma.primaryOffsetY)
          .onSizeChanged { renderedSizePx = it },
    ) {
      if (isHeroOnly) {
        // Both 7-Day & 24-Hour OFF -> tight-wrap Hero with minimal borders for maximum TV-edge travel
        HeroWeatherCluster(
          snapshot = snapshot,
          clockDisplay = clockDisplay,
          plasma = plasma,
          isCompact = !isTvWide || isShortLandscape,
          scaleMode = HeroScaleMode.FULL_MAX,
          tightWrap = true,
          modifier = Modifier.wrapContentSize(),
        )
      } else {
        val fillsFullHeight = show24Hour
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .then(if (fillsFullHeight) Modifier.fillMaxHeight() else Modifier)
              .padding(horizontal = clockwisePanelHPad, vertical = clockwisePanelVPad),
        ) {
          if (showAlertsBanner) {
            SevereWeatherAlertBanner(
              alerts = snapshot.alerts,
              plasma = plasma,
              isCompact = !isTvWide || isShortLandscape,
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(if (isShortLandscape) 6.dp else 10.dp))
          }

          if (isTvWide) {
            when {
              show7Day && show24Hour -> {
                // Standard 60 / 40 split: Hero Left + 7-Day Right | 24-Hour Bottom
                Row(
                  modifier = Modifier.weight(0.60f).fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier.weight(0.90f).padding(start = 2.dp, end = 10.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    HeroWeatherCluster(
                      snapshot = snapshot,
                      clockDisplay = clockDisplay,
                      plasma = plasma,
                      isCompact = isShortLandscape,
                      scaleMode = HeroScaleMode.STANDARD,
                    )
                  }

                  MultiDayForecastPanel(
                    days = snapshot.dailyForecasts,
                    plasma = plasma,
                    isCompact = false,
                    isShortHeight = isShortLandscape,
                    fillVertical = !isShortLandscape,
                    expandedVertical = false,
                    modifier =
                      Modifier.weight(1.45f)
                        .then(
                          if (isShortLandscape) Modifier
                          else Modifier.fillMaxHeight().padding(top = 8.dp)
                        ),
                  )
                }

                Spacer(modifier = Modifier.height(if (isShortLandscape) 6.dp else 10.dp))

                RotatingHourlyCubePanel(
                  snapshot = snapshot,
                  use24Hour = settings.use24HourClock,
                  plasma = plasma,
                  isCompact = false,
                  isShortHeight = isShortLandscape,
                  expandHeight = true,
                  modifier = Modifier.weight(0.40f).fillMaxWidth(),
                )
              }

              show7Day && !show24Hour -> {
                // 24-Hour OFF -> Keep Hero & 7-Day side-by-side at matching compact height with minimal vertical spacing
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier.weight(0.90f).padding(start = 2.dp, end = 10.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    HeroWeatherCluster(
                      snapshot = snapshot,
                      clockDisplay = clockDisplay,
                      plasma = plasma,
                      isCompact = isShortLandscape,
                      scaleMode = HeroScaleMode.STANDARD,
                    )
                  }

                  MultiDayForecastPanel(
                    days = snapshot.dailyForecasts,
                    plasma = plasma,
                    isCompact = false,
                    isShortHeight = isShortLandscape,
                    fillVertical = false,
                    expandedVertical = false,
                    modifier = Modifier.weight(1.45f),
                  )
                }
              }

              else -> {
                // 7-Day OFF, 24-Hour ON -> Hero cluster centered cleanly above 24-Hour strip with zero bottom clipping
                Box(
                  modifier = Modifier.weight(0.62f).fillMaxWidth(),
                  contentAlignment = Alignment.Center,
                ) {
                  HeroWeatherCluster(
                    snapshot = snapshot,
                    clockDisplay = clockDisplay,
                    plasma = plasma,
                    isCompact = isShortLandscape,
                    scaleMode = HeroScaleMode.WIDE_CENTERED,
                  )
                }

                Spacer(modifier = Modifier.height(if (isShortLandscape) 4.dp else 6.dp))

                RotatingHourlyCubePanel(
                  snapshot = snapshot,
                  use24Hour = settings.use24HourClock,
                  plasma = plasma,
                  isCompact = false,
                  isShortHeight = isShortLandscape,
                  expandHeight = true,
                  modifier = Modifier.weight(0.38f).fillMaxWidth(),
                )
              }
            }
          } else {
            // Compact / Portrait Non-Scrollable Layout
            when {
              show7Day && show24Hour -> {
                Column(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  HeroWeatherCluster(
                    snapshot = snapshot,
                    clockDisplay = clockDisplay,
                    plasma = plasma,
                    isCompact = true,
                    scaleMode = HeroScaleMode.STANDARD,
                    modifier = Modifier.fillMaxWidth(),
                  )

                  MultiDayForecastPanel(
                    days = snapshot.dailyForecasts,
                    plasma = plasma,
                    isCompact = true,
                    isShortHeight = false,
                    fillVertical = false,
                    expandedVertical = false,
                    modifier = Modifier.fillMaxWidth(),
                  )

                  RotatingHourlyCubePanel(
                    snapshot = snapshot,
                    use24Hour = settings.use24HourClock,
                    plasma = plasma,
                    isCompact = true,
                    isShortHeight = false,
                    expandHeight = false,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }

              show7Day && !show24Hour -> {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  HeroWeatherCluster(
                    snapshot = snapshot,
                    clockDisplay = clockDisplay,
                    plasma = plasma,
                    isCompact = true,
                    scaleMode = HeroScaleMode.STANDARD,
                    modifier = Modifier.fillMaxWidth(),
                  )

                  MultiDayForecastPanel(
                    days = snapshot.dailyForecasts,
                    plasma = plasma,
                    isCompact = true,
                    isShortHeight = false,
                    fillVertical = false,
                    expandedVertical = false,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }

              else -> {
                // 7-Day OFF, 24-Hour ON (Compact)
                Column(
                  modifier = Modifier.weight(1f).fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                  ) {
                    HeroWeatherCluster(
                      snapshot = snapshot,
                      clockDisplay = clockDisplay,
                      plasma = plasma,
                      isCompact = true,
                      scaleMode = HeroScaleMode.WIDE_CENTERED,
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }

                  RotatingHourlyCubePanel(
                    snapshot = snapshot,
                    use24Hour = settings.use24HourClock,
                    plasma = plasma,
                    isCompact = true,
                    isShortHeight = false,
                    expandHeight = false,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SevereWeatherAlertBanner(
  alerts: List<WeatherAlert>,
  plasma: PlasmaShieldState,
  isCompact: Boolean,
  modifier: Modifier = Modifier,
) {
  val hasSevere =
    alerts.any {
      it.severity.equals("Extreme", ignoreCase = true) ||
        it.severity.equals("Severe", ignoreCase = true)
    }
  val alertTint = if (hasSevere) BrightCoral else BrightGold
  val tickerText =
    remember(alerts) {
      alerts.joinToString("     •     ") { alert ->
        "${alert.event.uppercase(Locale.getDefault())}: ${alert.headline}"
      }
    }

  Surface(
    modifier = modifier.testTag("severe_weather_alert_banner"),
    shape = RoundedCornerShape(14.dp),
    color = Color.Black.copy(alpha = 0.84f),
    border = BorderStroke(1.2.dp, alertTint.copy(alpha = 0.72f)),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = if (isCompact) 7.dp else 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = alertTint.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, alertTint.copy(alpha = 0.55f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = "Weather Alert",
            tint = alertTint,
            modifier = Modifier.size(if (isCompact) 16.dp else 20.dp),
          )
          Text(
            text = if (hasSevere) "ALERT" else "ADVISORY",
            style =
              MaterialTheme.typography.labelLarge.copy(
                fontSize = if (isCompact) 13.sp else 16.sp,
                lineHeight = if (isCompact) 15.sp else 18.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = alertTint,
            maxLines = 1,
          )
        }
      }

      Text(
        text = tickerText,
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontSize = if (isCompact) 16.sp else 21.sp,
            lineHeight = if (isCompact) 19.sp else 24.sp,
          ),
        color = plasma.primaryTextColor,
        maxLines = 1,
        softWrap = false,
        modifier =
          Modifier.weight(1f)
            .basicMarquee(
              iterations = Int.MAX_VALUE,
              velocity = 54.dp,
            ),
      )
    }
  }
}

@Composable
private fun HeroWeatherCluster(
  snapshot: WeatherSnapshot,
  clockDisplay: ClockDisplay,
  plasma: PlasmaShieldState,
  isCompact: Boolean,
  scaleMode: HeroScaleMode = HeroScaleMode.STANDARD,
  tightWrap: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val factor =
    when (scaleMode) {
      HeroScaleMode.STANDARD -> 1f
      HeroScaleMode.EXPANDED_WITH_PANEL -> if (isCompact) 1.10f else 1.15f
      HeroScaleMode.WIDE_CENTERED -> 1f
      HeroScaleMode.FULL_MAX -> if (isCompact) 1.10f else 1.24f
    }
  HeroWeatherClusterContent(
    snapshot = snapshot,
    clockDisplay = clockDisplay,
    plasma = plasma,
    isCompact = isCompact,
    scaleFactor = factor,
    tightWrap = tightWrap,
    modifier = modifier,
  )
}

@Composable
private fun HeroWeatherClusterContent(
  snapshot: WeatherSnapshot,
  clockDisplay: ClockDisplay,
  plasma: PlasmaShieldState,
  isCompact: Boolean,
  scaleFactor: Float,
  tightWrap: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val citySp = ((if (isCompact) 24f else 34f) * scaleFactor).sp
  val cityMinSp = ((if (isCompact) 17f else 22f) * scaleFactor).sp
  val dateSp = ((if (isCompact) 19f else 26f) * scaleFactor).sp
  val dateMinSp = ((if (isCompact) 14f else 18f) * scaleFactor).sp
  val glyphDp = ((if (isCompact) 40f else 56f) * scaleFactor).dp
  val condSp = ((if (isCompact) 19f else 26f) * scaleFactor).sp
  val condMinSp = ((if (isCompact) 13f else 18f) * scaleFactor).sp
  val tempSp = ((if (isCompact) 54f else 94f) * scaleFactor).sp
  val unitSp = ((if (isCompact) 26f else 38f) * scaleFactor).sp
  val detailsSp = ((if (isCompact) 17f else 22f) * scaleFactor).sp
  val detailsMinSp = ((if (isCompact) 12f else 15f) * scaleFactor).sp
  val locIconDp = ((if (isCompact) 18f else 27f) * scaleFactor).dp
  val detailIconDp = ((if (isCompact) 15f else 20f) * scaleFactor).dp

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy((if (isCompact) 1f else 2f * scaleFactor).dp),
  ) {
    // Location alone on top line | Day/Date + Clock below with matching fonts
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy((2f * scaleFactor).dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((if (isCompact) 5f else 8f * scaleFactor).dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.LocationOn,
          contentDescription = "Location",
          tint = plasma.primaryAccent,
          modifier = Modifier.size(locIconDp),
        )
        AutoShrinkSingleLineText(
          text = snapshot.locationName.substringBefore(",").trim(),
          style =
            MaterialTheme.typography.titleLarge.copy(
              fontSize = citySp,
              lineHeight = (citySp.value + 3f).sp,
            ),
          color = plasma.primaryTextColor,
          minFontSize = cityMinSp,
          modifier = Modifier.testTag("location_label"),
        )
      }

      val dateTimeStyle =
        MaterialTheme.typography.titleMedium.copy(
          fontSize = dateSp,
          lineHeight = (dateSp.value + 3f).sp,
        )

      val dateAndTimeText =
        remember(
          clockDisplay.dateText,
          clockDisplay.timeText,
          plasma.secondaryTextColor,
          plasma.primaryTextColor,
        ) {
          buildAnnotatedString {
            withStyle(SpanStyle(color = plasma.secondaryTextColor)) {
              append(clockDisplay.dateText)
            }
            append("   ")
            withStyle(SpanStyle(color = plasma.primaryTextColor)) {
              append(clockDisplay.timeText)
            }
          }
        }

      AutoShrinkSingleLineAnnotatedText(
        text = dateAndTimeText,
        style = dateTimeStyle,
        minFontSize = dateMinSp,
        modifier = Modifier.testTag("clock_time_text"),
      )
    }

    // Weather Graphic + Condition Label directly underneath beside Bold Hero Temperature (F/C in degree spot)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy((if (isCompact) 8f else 10f * scaleFactor).dp),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
      ) {
        WeatherGlyph(
          condition = snapshot.currentCondition,
          plasmaAccent = plasma.primaryAccent,
          size = glyphDp,
        )
        AutoShrinkSingleLineText(
          text = snapshot.currentCondition.label,
          style =
            MaterialTheme.typography.titleMedium.copy(
              fontSize = condSp,
              lineHeight = (condSp.value + 2f).sp,
            ),
          color = plasma.primaryAccent,
          minFontSize = condMinSp,
          modifier =
            Modifier.widthIn(max = ((if (isCompact) 124f else 136f) * scaleFactor).dp)
              .testTag("current_condition_text"),
        )
      }

      Row(verticalAlignment = Alignment.Top) {
        Text(
          text = "${snapshot.currentTemp}",
          style =
            MaterialTheme.typography.displayLarge.copy(
              fontSize = tempSp,
              lineHeight = (tempSp.value * 0.86f).sp,
              lineHeightStyle =
                LineHeightStyle(
                  alignment = LineHeightStyle.Alignment.Center,
                  trim = LineHeightStyle.Trim.Both,
                ),
            ),
          color = plasma.primaryTextColor,
          maxLines = 1,
          modifier = Modifier.testTag("current_temp_text"),
        )
        Text(
          text = snapshot.tempUnit.symbol.removePrefix("°"),
          style =
            MaterialTheme.typography.headlineMedium.copy(
              fontSize = unitSp,
              lineHeight = (unitSp.value + 2f).sp,
            ),
          color = plasma.primaryAccent,
          modifier = Modifier.padding(top = ((if (isCompact) 2f else 4f) * scaleFactor).dp, start = 2.dp),
        )
      }
    }

    // Stacked Secondary Atmospheric Details: Feels/L/H above Humidity/Wind
    val detailsContainerModifier =
      if (tightWrap) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()

    BoxWithConstraints(
      modifier = detailsContainerModifier,
      contentAlignment = Alignment.Center,
    ) {
      val density = LocalDensity.current
      val fallbackMaxPx = with(density) { (360f * scaleFactor).dp.roundToPx() }
      val effectiveMaxWidthPx =
        if (constraints.hasBoundedWidth) constraints.maxWidth else fallbackMaxPx

      val detailsBaseStyle =
        MaterialTheme.typography.bodyLarge.copy(
          fontSize = detailsSp,
          lineHeight = (detailsSp.value + 2f).sp,
        )

      val feelsRangeString =
        "Feels ${snapshot.feelsLikeTemp}°   L ${snapshot.todayLowTemp}° · H ${snapshot.todayHighTemp}°"
      val humidityString = "${snapshot.humidityPercent}%"
      val windString = "${snapshot.windSpeed} ${snapshot.windUnitLabel}"
      val detailStrings =
        remember(feelsRangeString, humidityString, windString) {
          listOf(feelsRangeString, humidityString, windString)
        }

      val uniformDetailsSize =
        rememberUniformSingleLineFontSize(
          texts = detailStrings,
          style = detailsBaseStyle,
          maxWidthPx = effectiveMaxWidthPx,
          minFontSize = detailsMinSp,
        )
      val detailsStyle =
        detailsBaseStyle.copy(
          fontSize = uniformDetailsSize,
          lineHeight = (uniformDetailsSize.value + 2f).sp,
        )

      val feelsRangeText =
        remember(
          snapshot.feelsLikeTemp,
          snapshot.todayLowTemp,
          snapshot.todayHighTemp,
          plasma.secondaryTextColor,
          plasma.primaryTextColor,
        ) {
          buildAnnotatedString {
            withStyle(SpanStyle(color = plasma.secondaryTextColor)) {
              append("Feels ${snapshot.feelsLikeTemp}°")
            }
            append("   ")
            withStyle(
              SpanStyle(
                color = plasma.primaryTextColor,
                fontWeight = FontWeight.Medium,
              )
            ) {
              append("L ${snapshot.todayLowTemp}° · H ${snapshot.todayHighTemp}°")
            }
          }
        }

      Column(
        modifier = if (tightWrap) Modifier.width(IntrinsicSize.Max) else Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((if (isCompact) 1f else 2f * scaleFactor).dp),
      ) {
        Text(
          text = feelsRangeText,
          style = detailsStyle,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy((if (isCompact) 14f else 18f * scaleFactor).dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((if (isCompact) 4f else 6f * scaleFactor).dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WaterDrop,
              contentDescription = "Humidity",
              tint = plasma.secondaryAccent,
              modifier = Modifier.size(detailIconDp),
            )
            Text(
              text = humidityString,
              style = detailsStyle,
              color = plasma.secondaryTextColor,
              maxLines = 1,
              softWrap = false,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((if (isCompact) 4f else 6f * scaleFactor).dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Air,
              contentDescription = "Wind speed",
              tint = plasma.secondaryAccent,
              modifier = Modifier.size(detailIconDp),
            )
            Text(
              text = windString,
              style = detailsStyle,
              color = plasma.secondaryTextColor,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MultiDayForecastPanel(
  days: List<DailyForecast>,
  plasma: PlasmaShieldState,
  isCompact: Boolean,
  isShortHeight: Boolean,
  fillVertical: Boolean,
  expandedVertical: Boolean,
  modifier: Modifier = Modifier,
) {
  val cardCompact = isCompact || isShortHeight
  val cardSpacing = 4.dp
  val cardHorizontalPadding = 1.dp
  val expandBoost = if (expandedVertical) 1.18f else 1f

  val dayLabels = remember(days) { days.map { it.dayLabel } }
  val condLabels = remember(days) { days.map { it.condition.shortLabel } }
  val highLabels = remember(days) { days.map { "H ${it.highTemp}°" } }
  val lowLabels = remember(days) { days.map { "L ${it.lowTemp}°" } }

  val baseDayStyle =
    MaterialTheme.typography.titleMedium.copy(
      fontSize = ((if (cardCompact) 21f else 27f) * expandBoost).sp,
      lineHeight = ((if (cardCompact) 23f else 29f) * expandBoost).sp,
    )
  val baseCondStyle =
    MaterialTheme.typography.labelSmall.copy(
      fontSize = ((if (cardCompact) 16f else 18f) * expandBoost).sp,
      lineHeight = ((if (cardCompact) 18f else 20f) * expandBoost).sp,
      letterSpacing = (-0.2).sp,
    )
  val baseHighStyle =
    MaterialTheme.typography.titleLarge.copy(
      fontFamily = SpaceGroteskFontFamily,
      fontSize = ((if (cardCompact) 24f else 33f) * expandBoost).sp,
      lineHeight = ((if (cardCompact) 26f else 35f) * expandBoost).sp,
      letterSpacing = (-0.5).sp,
      fontWeight = FontWeight.SemiBold,
    )
  val baseLowStyle =
    MaterialTheme.typography.bodyLarge.copy(
      fontFamily = SpaceGroteskFontFamily,
      fontSize = ((if (cardCompact) 18f else 24f) * expandBoost).sp,
      lineHeight = ((if (cardCompact) 20f else 26f) * expandBoost).sp,
      letterSpacing = (-0.3).sp,
    )

  Surface(
    modifier = modifier.testTag("multiday_forecast_section"),
    shape = RoundedCornerShape(22.dp),
    color = Color.Transparent,
    border = null,
  ) {
    Column(
      modifier =
        Modifier.then(if (fillVertical) Modifier.fillMaxSize() else Modifier)
          .padding(
            horizontal = 8.dp,
            vertical = if (cardCompact) 6.dp else if (expandedVertical) 12.dp else 8.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(if (isShortHeight) 4.dp else 8.dp),
    ) {
      Text(
        text = "${days.size}-DAY FORECAST",
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontSize = (18f * if (expandedVertical) 1.1f else 1f).sp,
            lineHeight = 22.sp,
          ),
        color = plasma.secondaryAccent,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      )

      BoxWithConstraints(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (fillVertical) Modifier.weight(1f) else Modifier),
      ) {
        val density = LocalDensity.current
        val dayCount = days.size.coerceAtLeast(1)
        val gapPx = with(density) { cardSpacing.roundToPx() }
        val innerPadPx = with(density) { (cardHorizontalPadding * 2).roundToPx() }
        val cardInnerWidthPx =
          ((constraints.maxWidth - gapPx * (dayCount - 1)) / dayCount - innerPadPx)
            .coerceAtLeast(1)

        val uniformDaySize =
          rememberUniformSingleLineFontSize(
            texts = dayLabels,
            style = baseDayStyle,
            maxWidthPx = cardInnerWidthPx,
            minFontSize = if (cardCompact) 13.sp else 18.sp,
          )
        val uniformCondSize =
          rememberUniformSingleLineFontSize(
            texts = condLabels,
            style = baseCondStyle,
            maxWidthPx = cardInnerWidthPx,
            minFontSize = 9.5.sp,
          )
        val uniformHighSize =
          rememberUniformSingleLineFontSize(
            texts = highLabels,
            style = baseHighStyle,
            maxWidthPx = cardInnerWidthPx,
            minFontSize = if (cardCompact) 14.sp else 20.sp,
          )
        val uniformLowSize =
          rememberUniformSingleLineFontSize(
            texts = lowLabels,
            style = baseLowStyle,
            maxWidthPx = cardInnerWidthPx,
            minFontSize = if (cardCompact) 11.sp else 15.sp,
          )

        val uniformDayStyle =
          baseDayStyle.copy(
            fontSize = uniformDaySize,
            lineHeight = (uniformDaySize.value + 2f).sp,
          )
        val uniformCondStyle =
          baseCondStyle.copy(
            fontSize = uniformCondSize,
            lineHeight = (uniformCondSize.value + 2f).sp,
          )
        val uniformHighStyle =
          baseHighStyle.copy(
            fontSize = uniformHighSize,
            lineHeight = (uniformHighSize.value + 2f).sp,
          )
        val uniformLowStyle =
          baseLowStyle.copy(
            fontSize = uniformLowSize,
            lineHeight = (uniformLowSize.value + 2f).sp,
          )

        val cardInnerWidthDp = with(density) { cardInnerWidthPx.toDp() }
        val widthBasedGlyphDp = (cardInnerWidthDp - 2.dp).coerceAtLeast(20.dp)
        val dailyGlyphSize =
          if (fillVertical && constraints.hasBoundedHeight) {
            val availableHeightDp = with(density) { constraints.maxHeight.toDp() }
            val cardVertPadDp =
              when {
                cardCompact -> 8.dp
                expandedVertical -> 24.dp
                else -> 12.dp
              }
            val textBudgetDp =
              with(density) {
                uniformDayStyle.lineHeight.toDp() +
                  uniformCondStyle.lineHeight.toDp() +
                  uniformHighStyle.lineHeight.toDp() +
                  uniformLowStyle.lineHeight.toDp()
              }
            val spacingBudgetDp = if (expandedVertical) 24.dp else 14.dp
            val heightBasedGlyphDp =
              (availableHeightDp - cardVertPadDp - textBudgetDp - spacingBudgetDp).coerceAtLeast(22.dp)
            minOf(widthBasedGlyphDp, heightBasedGlyphDp)
          } else {
            widthBasedGlyphDp.coerceAtMost(if (cardCompact) 38.dp else 60.dp)
          }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .then(if (fillVertical) Modifier.fillMaxHeight() else Modifier),
          horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        ) {
          days.forEach { day ->
            DailyForecastCard(
              day = day,
              plasma = plasma,
              dayStyle = uniformDayStyle,
              condStyle = uniformCondStyle,
              highStyle = uniformHighStyle,
              lowStyle = uniformLowStyle,
              glyphSize = dailyGlyphSize,
              isCompact = cardCompact,
              fillVertical = fillVertical,
              expandedVertical = expandedVertical,
              modifier =
                Modifier.weight(1f)
                  .then(if (fillVertical) Modifier.fillMaxHeight() else Modifier),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DailyForecastCard(
  day: DailyForecast,
  plasma: PlasmaShieldState,
  dayStyle: TextStyle,
  condStyle: TextStyle,
  highStyle: TextStyle,
  lowStyle: TextStyle,
  glyphSize: androidx.compose.ui.unit.Dp,
  isCompact: Boolean,
  fillVertical: Boolean,
  expandedVertical: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = Color.Transparent,
    border = null,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .then(if (fillVertical) Modifier.fillMaxHeight() else Modifier)
          .padding(
            horizontal = 1.dp,
            vertical =
              when {
                isCompact -> 4.dp
                expandedVertical -> 12.dp
                else -> 6.dp
              },
          ),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement =
        if (fillVertical) Arrangement.SpaceBetween
        else Arrangement.spacedBy(if (isCompact) 3.dp else 6.dp),
    ) {
      Text(
        text = day.dayLabel,
        style = dayStyle,
        color = plasma.primaryTextColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (expandedVertical) 6.dp else 2.dp),
      ) {
        WeatherGlyph(
          condition = day.condition,
          plasmaAccent = plasma.primaryAccent,
          size = glyphSize,
        )
        Text(
          text = day.condition.shortLabel,
          style = condStyle,
          color = plasma.secondaryTextColor,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (expandedVertical) 4.dp else 2.dp),
      ) {
        Text(
          text = "H ${day.highTemp}°",
          style = highStyle,
          color = plasma.primaryTextColor,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "L ${day.lowTemp}°",
          style = lowStyle,
          color = plasma.secondaryTextColor,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
internal fun rememberUniformSingleLineFontSize(
  texts: List<String>,
  style: TextStyle,
  maxWidthPx: Int,
  minFontSize: TextUnit,
): TextUnit {
  val textMeasurer = rememberTextMeasurer()
  val density = LocalDensity.current

  return remember(texts, style, maxWidthPx, minFontSize, density) {
    if (maxWidthPx <= 0 || texts.isEmpty()) {
      style.fontSize
    } else {
      val maxSp = style.fontSize.value
      val minSp = minFontSize.value.coerceAtMost(maxSp)
      var candidateSp = maxSp
      while (candidateSp > minSp) {
        val candidateStyle = style.copy(fontSize = candidateSp.sp)
        val allFit =
          texts.all { label ->
            val measured =
              textMeasurer.measure(
                text = label,
                style = candidateStyle,
                maxLines = 1,
                softWrap = false,
              )
            !measured.hasVisualOverflow && measured.size.width <= maxWidthPx
          }
        if (allFit) break
        candidateSp = (candidateSp - 0.5f).coerceAtLeast(minSp)
      }
      candidateSp.sp
    }
  }
}

@Composable
internal fun AutoShrinkSingleLineText(
  text: String,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  minFontSize: TextUnit = 10.sp,
  fontWeight: FontWeight? = null,
  textAlign: TextAlign? = null,
) {
  var multiplier by remember(text, style.fontSize) { mutableFloatStateOf(1f) }
  val scaledFontSize = (style.fontSize.value * multiplier).coerceAtLeast(minFontSize.value).sp
  val scaledLineHeight =
    if (style.lineHeight.isSp) {
      (style.lineHeight.value * multiplier).coerceAtLeast(minFontSize.value + 2f).sp
    } else {
      style.lineHeight
    }

  Text(
    text = text,
    style =
      style.copy(
        fontSize = scaledFontSize,
        lineHeight = scaledLineHeight,
        fontWeight = fontWeight ?: style.fontWeight,
      ),
    color = color,
    textAlign = textAlign,
    maxLines = 1,
    softWrap = false,
    overflow = TextOverflow.Ellipsis,
    onTextLayout = { result ->
      if (result.hasVisualOverflow && multiplier > 0.55f) {
        val next = (multiplier * 0.92f).coerceAtLeast(0.55f)
        if ((style.fontSize.value * next) >= minFontSize.value - 0.5f) {
          multiplier = next
        }
      }
    },
    modifier = modifier,
  )
}

@Composable
private fun AutoShrinkSingleLineAnnotatedText(
  text: AnnotatedString,
  style: TextStyle,
  modifier: Modifier = Modifier,
  minFontSize: TextUnit = 10.sp,
) {
  var multiplier by remember(text.text, style.fontSize) { mutableFloatStateOf(1f) }
  val scaledFontSize = (style.fontSize.value * multiplier).coerceAtLeast(minFontSize.value).sp
  val scaledLineHeight =
    if (style.lineHeight.isSp) {
      (style.lineHeight.value * multiplier).coerceAtLeast(minFontSize.value + 2f).sp
    } else {
      style.lineHeight
    }

  Text(
    text = text,
    style = style.copy(fontSize = scaledFontSize, lineHeight = scaledLineHeight),
    maxLines = 1,
    softWrap = false,
    overflow = TextOverflow.Ellipsis,
    onTextLayout = { result ->
      if (result.hasVisualOverflow && multiplier > 0.55f) {
        val next = (multiplier * 0.92f).coerceAtLeast(0.55f)
        if ((style.fontSize.value * next) >= minFontSize.value - 0.5f) {
          multiplier = next
        }
      }
    },
    modifier = modifier,
  )
}
