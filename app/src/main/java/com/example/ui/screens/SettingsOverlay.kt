package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.data.local.MAX_SAVED_LOCATIONS
import com.example.data.local.UserSettings
import com.example.data.model.CitySearchResult
import com.example.data.model.DriftIntensity
import com.example.data.model.DriftMotionMode
import com.example.data.model.SavedLocation
import com.example.data.model.TemperatureUnit
import com.example.data.model.TextColorOption
import com.example.data.model.WeatherAlert
import com.example.data.model.WindSpeedUnit
import com.example.data.model.normalizeVersionTag
import com.example.data.update.AppUpdateManager
import com.example.ui.AppUpdateUiState
import com.example.ui.CitySearchUiState
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.BrightCobalt
import com.example.ui.theme.BrightCoral
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepCardSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SeafoamTeal
import com.example.ui.theme.SoftAmethyst
import com.example.ui.theme.SoftFrostText
import com.example.ui.theme.SubtleOutline
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsOverlay(
  settings: UserSettings,
  citySearchState: CitySearchUiState,
  activeAlerts: List<WeatherAlert>,
  accentColor: Color,
  onDismiss: () -> Unit,
  onQueryChange: (String) -> Unit,
  onSearchSubmit: () -> Unit,
  onSelectCity: (CitySearchResult) -> Unit,
  onSelectSavedLocation: (SavedLocation) -> Unit,
  onRemoveSavedLocation: (SavedLocation) -> Unit,
  onToggleAutoRotateLocations: (Boolean) -> Unit,
  onSelectUnit: (TemperatureUnit) -> Unit,
  onSelectWindUnit: (WindSpeedUnit) -> Unit,
  onToggle24Hour: (Boolean) -> Unit,
  onToggle7DayForecast: (Boolean) -> Unit,
  onToggleBottomBarTemp24h: (Boolean) -> Unit,
  onToggleBottomBarWind: (Boolean) -> Unit,
  onToggleBottomBarAirQuality: (Boolean) -> Unit,
  onToggleBottomBarPrecipitation: (Boolean) -> Unit,
  onToggleSevereAlerts: (Boolean) -> Unit,
  onSelectDriftIntensity: (DriftIntensity) -> Unit,
  onSelectDriftMotionMode: (DriftMotionMode) -> Unit,
  onSelectTextColor: (TextColorOption) -> Unit,
  onRefreshWeather: () -> Unit,
  appUpdateState: AppUpdateUiState = AppUpdateUiState.Idle,
  onCheckForAppUpdate: () -> Unit = {},
  onDownloadAndInstallUpdate: (String, String) -> Unit = { _, _ -> },
  onConsumeAutoInstallLaunch: () -> Unit = {},
) {
  BackHandler(onBack = onDismiss)

  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val closeButtonFocusRequester = remember { FocusRequester() }
  val searchBoxFocusRequester = remember { FocusRequester() }
  val searchFieldFocusRequester = remember { FocusRequester() }
  val firstResultFocusRequester = remember { FocusRequester() }

  var isSearchEditing by remember { mutableStateOf(false) }
  var searchBoxFocused by remember { mutableStateOf(false) }
  var searchFieldFocused by remember { mutableStateOf(false) }
  var searchBtnFocused by remember { mutableStateOf(false) }
  var searchFieldAcquiredFocus by remember { mutableStateOf(false) }
  var pendingFocusFirstResult by remember { mutableStateOf(false) }
  var pendingFocusSearchAfterSelect by remember { mutableStateOf(false) }

  // Ensure initial focus lands on the Close button so the search box / keyboard never auto-opens
  LaunchedEffect(Unit) {
    keyboardController?.hide()
    withFrameNanos {}
    runCatching { closeButtonFocusRequester.requestFocus() }
  }

  LaunchedEffect(isSearchEditing) {
    if (isSearchEditing) {
      searchFieldAcquiredFocus = false
      for (attempt in 0 until 6) {
        withFrameNanos {}
        if (searchFieldFocused) break
        runCatching { searchFieldFocusRequester.requestFocus() }
        if (!searchFieldFocused && attempt >= 2) {
          delay(24L)
        }
      }
      keyboardController?.show()
    } else {
      keyboardController?.hide()
      if (pendingFocusFirstResult && !searchBtnFocused && !searchBoxFocused) {
        withFrameNanos {}
        runCatching { searchBoxFocusRequester.requestFocus() }
      }
    }
  }

  LaunchedEffect(isSearchEditing, searchFieldFocused) {
    if (isSearchEditing && !searchFieldFocused && searchFieldAcquiredFocus) {
      delay(80L)
      if (!searchFieldFocused) {
        searchFieldAcquiredFocus = false
        isSearchEditing = false
      }
    }
  }

  LaunchedEffect(pendingFocusFirstResult, citySearchState.isSearching, citySearchState.results) {
    if (pendingFocusFirstResult && !citySearchState.isSearching) {
      if (citySearchState.results.isNotEmpty()) {
        withFrameNanos {}
        runCatching { firstResultFocusRequester.requestFocus() }
      }
      pendingFocusFirstResult = false
    }
  }

  LaunchedEffect(pendingFocusSearchAfterSelect, citySearchState.results) {
    if (pendingFocusSearchAfterSelect && citySearchState.results.isEmpty()) {
      searchFieldFocused = false
      searchFieldAcquiredFocus = false
      isSearchEditing = false
      withFrameNanos {}
      runCatching { searchBoxFocusRequester.requestFocus() }
      pendingFocusSearchAfterSelect = false
    }
  }

  BackHandler(enabled = isSearchEditing) {
    searchFieldFocused = false
    searchFieldAcquiredFocus = false
    isSearchEditing = false
    keyboardController?.hide()
    runCatching { searchBoxFocusRequester.requestFocus() }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(ObsidianBlack.copy(alpha = 0.84f))
        .safeDrawingPadding()
        .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth().widthIn(max = 760.dp),
      shape = RoundedCornerShape(24.dp),
      color = MidnightSlate.copy(alpha = 0.96f),
      border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
      tonalElevation = 8.dp,
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "PlasmaSky Controls",
              style = MaterialTheme.typography.headlineMedium,
              color = SoftFrostText,
            )
            Text(
              text = "Current city: ${settings.cityName}",
              style = MaterialTheme.typography.bodyMedium,
              color = SoftFrostText,
            )
          }
          var closeFocused by remember { mutableStateOf(false) }
          val closeScale by animateFloatAsState(targetValue = if (closeFocused) 1.08f else 1f, label = "close_scale")
          IconButton(
            onClick = {
              keyboardController?.hide()
              onDismiss()
            },
            modifier =
              Modifier.scale(closeScale)
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(if (closeFocused) lerp(ElevatedSurface, BrightGold, 0.20f) else Color.Transparent, CircleShape)
                .border(
                  width = if (closeFocused) 3.dp else 1.dp,
                  color = if (closeFocused) BrightGold else SubtleOutline,
                  shape = CircleShape,
                )
                .focusRequester(closeButtonFocusRequester)
                .onFocusChanged { closeFocused = it.isFocused || it.hasFocus }
                .testTag("close_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Resume screensaver",
              tint = if (closeFocused) BrightGold else SoftFrostText,
            )
          }
        }

        HorizontalDivider(color = SubtleOutline)

        // Location Search & Saved Locations (Up to 5)
        SectionHeader(
          icon = Icons.Rounded.LocationOn,
          title = "Locations (${settings.savedLocations.size}/$MAX_SAVED_LOCATIONS Saved)",
          tint = accentColor,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val isSearchHighlighted = searchBoxFocused || isSearchEditing || searchFieldFocused
          val searchScale by animateFloatAsState(
            targetValue = if (isSearchHighlighted) 1.015f else 1f,
            label = "search_scale",
          )
          Box(
            modifier = Modifier.weight(1f).scale(searchScale).defaultMinSize(minHeight = 56.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            // Non-nested sibling activation layer for D-pad focus & OK press when not actively typing
            Box(
              modifier =
                Modifier.matchParentSize()
                  .clip(RoundedCornerShape(14.dp))
                  .focusRequester(searchBoxFocusRequester)
                  .focusProperties { canFocus = !searchFieldFocused }
                  .onFocusChanged { searchBoxFocused = it.isFocused }
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                  ) {
                    if (!isSearchEditing) {
                      searchFieldAcquiredFocus = false
                      isSearchEditing = true
                    }
                  }
                  .then(if (isSearchEditing) Modifier else Modifier.testTag("city_search_input"))
            )

            // Persistent visual Surface + permanently mounted BasicTextField (never detached between searches)
            Surface(
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp),
              shape = RoundedCornerShape(14.dp),
              color = if (isSearchHighlighted) lerp(DeepCardSurface, BrightGold, 0.16f) else DeepCardSurface,
              border =
                BorderStroke(
                  width = if (isSearchHighlighted) 3.dp else 1.dp,
                  color = if (isSearchHighlighted) BrightGold else SubtleOutline,
                ),
            ) {
              Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                BasicTextField(
                  value = citySearchState.query,
                  onValueChange = onQueryChange,
                  enabled = isSearchEditing,
                  readOnly = !isSearchEditing,
                  modifier =
                    Modifier.fillMaxWidth()
                      .focusRequester(searchFieldFocusRequester)
                      .onFocusChanged { focusState ->
                        val nowFocused = focusState.isFocused || focusState.hasFocus
                        searchFieldFocused = nowFocused
                        if (nowFocused) {
                          searchFieldAcquiredFocus = true
                        }
                      }
                      .then(if (isSearchEditing) Modifier.testTag("city_search_input") else Modifier),
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyLarge.copy(color = SoftFrostText),
                  cursorBrush = SolidColor(BrightGold),
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                  keyboardActions =
                    KeyboardActions(
                      onSearch = {
                        pendingFocusFirstResult = true
                        searchFieldFocused = false
                        searchFieldAcquiredFocus = false
                        isSearchEditing = false
                        keyboardController?.hide()
                        onSearchSubmit()
                      }
                    ),
                  decorationBox = { innerTextField ->
                    Box(
                      modifier = Modifier.fillMaxWidth(),
                      contentAlignment = Alignment.CenterStart,
                    ) {
                      if (citySearchState.query.isEmpty()) {
                        Text(
                          text =
                            if (isSearchEditing) "Type city or zip code..."
                            else "Search city or zip code (press OK to type)",
                          style = MaterialTheme.typography.bodyLarge,
                          color = SoftFrostText.copy(alpha = 0.65f),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                      innerTextField()
                    }
                  },
                )
              }
            }
          }
          val searchBtnScale by animateFloatAsState(targetValue = if (searchBtnFocused) 1.06f else 1f, label = "search_btn_scale")
          Button(
            onClick = {
              if (!citySearchState.isSearching) {
                pendingFocusFirstResult = true
                searchFieldFocused = false
                searchFieldAcquiredFocus = false
                isSearchEditing = false
                keyboardController?.hide()
                onSearchSubmit()
              }
            },
            modifier =
              Modifier.height(54.dp)
                .scale(searchBtnScale)
                .onFocusChanged { searchBtnFocused = it.isFocused || it.hasFocus }
                .testTag("city_search_button"),
            shape = RoundedCornerShape(14.dp),
            border = if (searchBtnFocused) BorderStroke(3.dp, BrightGold) else null,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = ObsidianBlack),
          ) {
            if (citySearchState.isSearching) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianBlack)
            } else {
              Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search city or zip code")
            }
          }
        }

        if (citySearchState.statusMessage != null) {
          Text(
            text = citySearchState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = SoftFrostText,
          )
        }

        if (citySearchState.results.isNotEmpty()) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DeepCardSurface)
                .border(1.dp, SubtleOutline, RoundedCornerShape(14.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "Suggestions (press Down or Search to select)",
              style = MaterialTheme.typography.labelSmall,
              color = SoftFrostText.copy(alpha = 0.72f),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
            citySearchState.results.take(5).forEachIndexed { index, city ->
              var focused by remember { mutableStateOf(false) }
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (focused) lerp(ElevatedSurface, BrightGold, 0.16f) else Color.Transparent)
                    .border(
                      width = if (focused) 3.dp else 0.dp,
                      color = if (focused) BrightGold else Color.Transparent,
                      shape = RoundedCornerShape(10.dp),
                    )
                    .then(if (index == 0) Modifier.focusRequester(firstResultFocusRequester) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable {
                      searchFieldFocused = false
                      searchFieldAcquiredFocus = false
                      isSearchEditing = false
                      pendingFocusSearchAfterSelect = true
                      runCatching { searchBoxFocusRequester.requestFocus() }
                      onSelectCity(city)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("city_result_item"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(text = city.displayName, style = MaterialTheme.typography.bodyLarge, color = SoftFrostText)
                Text(text = "Save & Select", style = MaterialTheme.typography.labelLarge, color = SoftFrostText)
              }
            }
          }
        }

        if (settings.savedLocations.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            settings.savedLocations.forEach { saved ->
              val isCurrent = saved.matches(settings.latitude, settings.longitude)
              SavedLocationPill(
                location = saved,
                selected = isCurrent,
                canRemove = settings.savedLocations.size > 1,
                accentColor = accentColor,
                onSelect = { onSelectSavedLocation(saved) },
                onRemove = { onRemoveSavedLocation(saved) },
              )
            }
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TvSelectableChip(
            label = if (settings.autoRotateLocations) "Auto-Rotate Cities: ON (2 min)" else "Auto-Rotate Cities: OFF",
            selected = settings.autoRotateLocations,
            accentColor = accentColor,
            leadingIcon = Icons.Rounded.Sync,
            testTag = "auto_rotate_locations_chip",
            onClick = { onToggleAutoRotateLocations(!settings.autoRotateLocations) },
          )
        }
        Text(
          text =
            if (settings.savedLocations.size > 1) {
              "Tip: Press Left / Right on D-pad to swap between your ${settings.savedLocations.size} saved cities."
            } else {
              "Save up to 5 locations using search above, then swap with D-pad Left / Right or enable Auto-Rotate."
            },
          style = MaterialTheme.typography.labelSmall,
          color = SoftFrostText.copy(alpha = 0.78f),
        )

        HorizontalDivider(color = SubtleOutline)

        // Units & Clock (Single-press toggles)
        SectionHeader(icon = Icons.Rounded.Schedule, title = "Units & Clock", tint = accentColor)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TvSelectableChip(
            label =
              if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) "Temp: °F Fahrenheit"
              else "Temp: °C Celsius",
            selected = true,
            accentColor = accentColor,
            leadingIcon = Icons.Rounded.SwapHoriz,
            testTag = "unit_toggle_chip",
            onClick = {
              val next =
                if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) TemperatureUnit.CELSIUS
                else TemperatureUnit.FAHRENHEIT
              onSelectUnit(next)
            },
          )
          TvSelectableChip(
            label =
              if (settings.windSpeedUnit == WindSpeedUnit.MPH) "Wind: mph"
              else "Wind: kph",
            selected = true,
            accentColor = accentColor,
            leadingIcon = Icons.Rounded.SwapHoriz,
            testTag = "wind_unit_toggle_chip",
            onClick = {
              val next =
                if (settings.windSpeedUnit == WindSpeedUnit.MPH) WindSpeedUnit.KPH
                else WindSpeedUnit.MPH
              onSelectWindUnit(next)
            },
          )
          TvSelectableChip(
            label = if (settings.use24HourClock) "Clock: 24-Hour" else "Clock: 12-Hour",
            selected = true,
            accentColor = accentColor,
            leadingIcon = Icons.Rounded.SwapHoriz,
            testTag = "clock_toggle_chip",
            onClick = { onToggle24Hour(!settings.use24HourClock) },
          )
        }

        HorizontalDivider(color = SubtleOutline)

        // Forecast Panels, Bottom Bar & Severe Alerts
        SectionHeader(
          icon = Icons.Rounded.DashboardCustomize,
          title = "Display Panels & Weather Features",
          tint = accentColor,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TvSelectableChip(
            label = if (settings.show7DayForecast) "7-Day Forecast: ON" else "7-Day Forecast: OFF",
            selected = settings.show7DayForecast,
            accentColor = accentColor,
            testTag = "toggle_7day_chip",
            onClick = { onToggle7DayForecast(!settings.show7DayForecast) },
          )
          TvSelectableChip(
            label = if (settings.showSevereWeatherAlerts) "Severe Alerts Banner: ON" else "Severe Alerts Banner: OFF",
            selected = settings.showSevereWeatherAlerts,
            accentColor = accentColor,
            leadingIcon = Icons.Rounded.WarningAmber,
            testTag = "toggle_severe_alerts_chip",
            onClick = { onToggleSevereAlerts(!settings.showSevereWeatherAlerts) },
          )
        }

        // Bottom Bar on its own line with bullet-point toggles directly beneath it
        val enabledBottomBarCount =
          listOf(
            settings.showBottomBarTemp24h,
            settings.showBottomBarWind,
            settings.showBottomBarAirQuality,
            settings.showBottomBarPrecipitation,
          ).count { it }
        Column(
          modifier = Modifier.fillMaxWidth().testTag("bottom_bar_settings_group"),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Bottom Bar",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = SoftFrostText,
            )
            Text(
              text =
                if (enabledBottomBarCount == 0) "(Hidden — all items off)"
                else "($enabledBottomBarCount/4 enabled)",
              style = MaterialTheme.typography.labelSmall,
              color = SoftFrostText.copy(alpha = 0.72f),
            )
          }

          Column(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            BottomBarBulletToggle(
              label = if (settings.showBottomBarTemp24h) "24h Temp: ON" else "24h Temp: OFF",
              selected = settings.showBottomBarTemp24h,
              accentColor = accentColor,
              testTag = "toggle_bottom_bar_temp_24h_chip",
              onClick = { onToggleBottomBarTemp24h(!settings.showBottomBarTemp24h) },
            )
            BottomBarBulletToggle(
              label = if (settings.showBottomBarPrecipitation) "Precipitation: ON" else "Precipitation: OFF",
              selected = settings.showBottomBarPrecipitation,
              accentColor = accentColor,
              testTag = "toggle_bottom_bar_precipitation_chip",
              onClick = { onToggleBottomBarPrecipitation(!settings.showBottomBarPrecipitation) },
            )
            BottomBarBulletToggle(
              label = if (settings.showBottomBarWind) "Wind: ON" else "Wind: OFF",
              selected = settings.showBottomBarWind,
              accentColor = accentColor,
              testTag = "toggle_bottom_bar_wind_chip",
              onClick = { onToggleBottomBarWind(!settings.showBottomBarWind) },
            )
            BottomBarBulletToggle(
              label = if (settings.showBottomBarAirQuality) "Air Quality: ON" else "Air Quality: OFF",
              selected = settings.showBottomBarAirQuality,
              accentColor = accentColor,
              testTag = "toggle_bottom_bar_air_quality_chip",
              onClick = { onToggleBottomBarAirQuality(!settings.showBottomBarAirQuality) },
            )
          }
        }

        if (settings.showSevereWeatherAlerts) {
          val alertSummary =
            if (activeAlerts.isNotEmpty()) {
              "Active advisory for ${settings.cityName}: ${activeAlerts.joinToString(" • ") { it.event }}"
            } else {
              "No active severe weather alerts or advisories for ${settings.cityName}."
            }
          Text(
            text = alertSummary,
            style = MaterialTheme.typography.labelSmall,
            color = SoftFrostText.copy(alpha = 0.85f),
          )
        }

        HorizontalDivider(color = SubtleOutline)

        // Dedicated Motion Mode Toggle (Clockwise vs DVD Bounce)
        SectionHeader(
          icon = Icons.Rounded.OpenWith,
          title = "Motion Mode",
          tint = accentColor,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          DriftMotionMode.entries.forEach { motionMode ->
            val selected = settings.driftMotionMode == motionMode
            var focused by remember { mutableStateOf(false) }
            val motionScale by animateFloatAsState(targetValue = if (focused) 1.03f else 1f, label = "motion_scale")
            val containerColor =
              when {
                focused && selected -> lerp(accentColor.copy(alpha = 0.24f), BrightGold, 0.18f)
                focused -> lerp(ElevatedSurface, BrightGold, 0.16f)
                selected -> accentColor.copy(alpha = 0.22f)
                else -> DeepCardSurface
              }
            Surface(
              modifier =
                Modifier.weight(1f)
                  .scale(motionScale)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(12.dp))
                  .onFocusChanged { focused = it.isFocused }
                  .clickable { onSelectDriftMotionMode(motionMode) }
                  .testTag("motion_mode_${motionMode.name.lowercase()}_chip"),
              shape = RoundedCornerShape(12.dp),
              color = containerColor,
              border =
                BorderStroke(
                  width =
                    when {
                      focused -> 3.dp
                      selected -> 1.5.dp
                      else -> 1.dp
                    },
                  color =
                    when {
                      focused -> BrightGold
                      selected -> accentColor.copy(alpha = 0.72f)
                      else -> SubtleOutline
                    },
                ),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                if (selected) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                  text = motionMode.label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal,
                  color = SoftFrostText,
                )
              }
            }
          }
        }
        Text(
          text =
            "Clockwise scales elements to 90% and glides along the outer edges; DVD Bounce scales to 80% and bounces off edges.",
          style = MaterialTheme.typography.labelSmall,
          color = SoftFrostText.copy(alpha = 0.78f),
        )

        // Plasma Burn-in Protection Speed Controls
        SectionHeader(
          icon = Icons.Rounded.Shield,
          title = "Motion Drift Speed (Plasma Shield)",
          tint = accentColor,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DriftIntensity.entries.forEach { mode ->
            TvSelectableChip(
              label = mode.label,
              selected = settings.driftIntensity == mode,
              accentColor = accentColor,
              testTag = "drift_${mode.name.lowercase()}_chip",
              onClick = { onSelectDriftIntensity(mode) },
            )
          }
        }

        SectionHeader(
          icon = Icons.Rounded.Palette,
          title = "Text Color",
          tint = accentColor,
        )
        TextColorDropdown(
          selectedOption = settings.textColorOption,
          accentColor = accentColor,
          onSelectOption = onSelectTextColor,
        )

        HorizontalDivider(color = SubtleOutline)

        // Actions
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          var refreshFocused by remember { mutableStateOf(false) }
          val refreshScale by animateFloatAsState(targetValue = if (refreshFocused) 1.05f else 1f, label = "refresh_scale")
          OutlinedButton(
            onClick = onRefreshWeather,
            modifier =
              Modifier.scale(refreshScale)
                .minimumInteractiveComponentSize()
                .onFocusChanged { refreshFocused = it.isFocused || it.hasFocus }
                .testTag("refresh_weather_button"),
            shape = RoundedCornerShape(14.dp),
            border =
              BorderStroke(
                width = if (refreshFocused) 3.dp else 1.dp,
                color = if (refreshFocused) BrightGold else SubtleOutline,
              ),
            colors =
              ButtonDefaults.outlinedButtonColors(
                containerColor = if (refreshFocused) lerp(DeepCardSurface, BrightGold, 0.16f) else Color.Transparent,
              ),
          ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, tint = accentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Weather", color = SoftFrostText)
          }

          var resumeFocused by remember { mutableStateOf(false) }
          val resumeScale by animateFloatAsState(targetValue = if (resumeFocused) 1.05f else 1f, label = "resume_scale")
          Button(
            onClick = {
              keyboardController?.hide()
              onDismiss()
            },
            modifier =
              Modifier.scale(resumeScale)
                .minimumInteractiveComponentSize()
                .onFocusChanged { resumeFocused = it.isFocused || it.hasFocus }
                .testTag("resume_screensaver_button"),
            shape = RoundedCornerShape(14.dp),
            border = if (resumeFocused) BorderStroke(3.dp, BrightGold) else null,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = ObsidianBlack),
          ) {
            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Resume Screensaver", fontWeight = FontWeight.SemiBold)
          }
        }

        Text(
          text = "Forecast Data: Open-Meteo & NOAA/NWS Live Alerts",
          style = MaterialTheme.typography.labelSmall,
          color = SoftFrostText.copy(alpha = 0.78f),
        )

        HorizontalDivider(color = SubtleOutline)

        // App Updates (very last setting at the bottom)
        val installedVersion = normalizeVersionTag(BuildConfig.VERSION_NAME).ifBlank { "1.01" }
        var installActionError by remember(appUpdateState) { mutableStateOf<String?>(null) }
        var canInstallUnknownApps by remember {
          mutableStateOf(AppUpdateManager.canRequestPackageInstalls(context))
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, context) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              canInstallUnknownApps = AppUpdateManager.canRequestPackageInstalls(context)
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(appUpdateState, canInstallUnknownApps) {
          if (
            appUpdateState is AppUpdateUiState.ReadyToInstall &&
              appUpdateState.autoLaunchInstall &&
              canInstallUnknownApps
          ) {
            onConsumeAutoInstallLaunch()
            AppUpdateManager.launchApkInstall(context, appUpdateState.apkFile).onFailure { err ->
              installActionError = err.localizedMessage ?: "Unable to launch package installer."
            }
          }
        }

        SectionHeader(
          icon = Icons.Rounded.SystemUpdate,
          title = "App Updates",
          tint = accentColor,
        )
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = DeepCardSurface,
          border = BorderStroke(1.dp, SubtleOutline),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Installed version: v$installedVersion",
              style = MaterialTheme.typography.labelLarge,
              color = SoftFrostText,
            )

            val statusLine =
              when (appUpdateState) {
                is AppUpdateUiState.Idle ->
                  "Check GitHub Releases to download and install updates in-app."
                is AppUpdateUiState.Checking ->
                  "Checking GitHub Releases..."
                is AppUpdateUiState.UpToDate ->
                  "You are up to date (v${appUpdateState.currentVersion})."
                is AppUpdateUiState.UpdateAvailable ->
                  "New version ready: v${appUpdateState.latestVersion}"
                is AppUpdateUiState.Downloading -> {
                  val pct =
                    if (appUpdateState.totalBytes > 0L) {
                      ((appUpdateState.downloadedBytes * 100L) / appUpdateState.totalBytes)
                        .toInt()
                        .coerceIn(0, 100)
                    } else {
                      null
                    }
                  val dlMb = appUpdateState.downloadedBytes / (1024f * 1024f)
                  if (pct != null) {
                    val totalMb = appUpdateState.totalBytes / (1024f * 1024f)
                    "Downloading v${appUpdateState.latestVersion}... $pct% (%.1f / %.1f MB)".format(dlMb, totalMb)
                  } else {
                    "Downloading v${appUpdateState.latestVersion}... %.1f MB".format(dlMb)
                  }
                }
                is AppUpdateUiState.ReadyToInstall ->
                  if (canInstallUnknownApps) {
                    "v${appUpdateState.latestVersion} downloaded and ready to install."
                  } else {
                    "v${appUpdateState.latestVersion} downloaded. Allow PlasmaSky to install updates in Android Settings, then press Install."
                  }
                is AppUpdateUiState.Error ->
                  appUpdateState.message
                is AppUpdateUiState.RepoNotConfigured ->
                  "In-app updates are enabled on APKs built by GitHub Actions."
              }

            Text(
              text = installActionError ?: statusLine,
              style = MaterialTheme.typography.bodyMedium,
              color =
                when {
                  installActionError != null -> BrightCoral
                  appUpdateState is AppUpdateUiState.UpdateAvailable -> BrightGold
                  appUpdateState is AppUpdateUiState.Downloading -> accentColor
                  appUpdateState is AppUpdateUiState.ReadyToInstall ->
                    if (canInstallUnknownApps) SeafoamTeal else BrightGold
                  appUpdateState is AppUpdateUiState.Error -> BrightCoral
                  appUpdateState is AppUpdateUiState.UpToDate -> SeafoamTeal
                  else -> MutedSlateText
                },
            )

            if (appUpdateState is AppUpdateUiState.Downloading) {
              if (appUpdateState.totalBytes > 0L) {
                val fraction =
                  (appUpdateState.downloadedBytes.toFloat() / appUpdateState.totalBytes.toFloat())
                    .coerceIn(0f, 1f)
                LinearProgressIndicator(
                  progress = { fraction },
                  modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                  color = accentColor,
                  trackColor = SubtleOutline,
                )
              } else {
                LinearProgressIndicator(
                  modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                  color = accentColor,
                  trackColor = SubtleOutline,
                )
              }
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              val isBusy =
                appUpdateState is AppUpdateUiState.Checking ||
                  appUpdateState is AppUpdateUiState.Downloading
              var checkFocused by remember { mutableStateOf(false) }
              val checkScale by animateFloatAsState(
                targetValue = if (checkFocused) 1.05f else 1f,
                label = "update_check_scale",
              )
              OutlinedButton(
                onClick = {
                  installActionError = null
                  onCheckForAppUpdate()
                },
                enabled = !isBusy,
                modifier =
                  Modifier.scale(checkScale)
                    .minimumInteractiveComponentSize()
                    .onFocusChanged { checkFocused = it.isFocused || it.hasFocus }
                    .testTag("check_app_update_button"),
                shape = RoundedCornerShape(14.dp),
                border =
                  BorderStroke(
                    width = if (checkFocused) 3.dp else 1.dp,
                    color = if (checkFocused) BrightGold else SubtleOutline,
                  ),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    containerColor =
                      if (checkFocused) lerp(DeepCardSurface, BrightGold, 0.16f) else Color.Transparent,
                  ),
              ) {
                if (isBusy) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = accentColor,
                  )
                } else {
                  Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    when (appUpdateState) {
                      is AppUpdateUiState.Checking -> "Checking..."
                      is AppUpdateUiState.Downloading -> "Downloading..."
                      else -> "Check for Updates"
                    },
                  color = SoftFrostText,
                )
              }

              if (appUpdateState is AppUpdateUiState.UpdateAvailable) {
                var installNowFocused by remember { mutableStateOf(false) }
                val installNowScale by animateFloatAsState(
                  targetValue = if (installNowFocused) 1.05f else 1f,
                  label = "download_install_scale",
                )
                Button(
                  onClick = {
                    installActionError = null
                    onDownloadAndInstallUpdate(
                      appUpdateState.latestVersion,
                      appUpdateState.downloadUrl,
                    )
                  },
                  modifier =
                    Modifier.scale(installNowScale)
                      .minimumInteractiveComponentSize()
                      .onFocusChanged { installNowFocused = it.isFocused || it.hasFocus }
                      .testTag("download_install_update_button"),
                  shape = RoundedCornerShape(14.dp),
                  border = if (installNowFocused) BorderStroke(3.dp, BrightGold) else null,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = BrightGold,
                      contentColor = ObsidianBlack,
                    ),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Update to v${appUpdateState.latestVersion}",
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }

              if (appUpdateState is AppUpdateUiState.ReadyToInstall) {
                if (!canInstallUnknownApps) {
                  var permFocused by remember { mutableStateOf(false) }
                  val permScale by animateFloatAsState(
                    targetValue = if (permFocused) 1.05f else 1f,
                    label = "perm_install_scale",
                  )
                  Button(
                    onClick = {
                      installActionError = null
                      runCatching {
                        context.startActivity(
                          AppUpdateManager.createUnknownSourcesPermissionIntent(context)
                        )
                      }.onFailure { err ->
                        installActionError =
                          err.localizedMessage ?: "Unable to open Install Unknown Apps settings."
                      }
                    },
                    modifier =
                      Modifier.scale(permScale)
                        .minimumInteractiveComponentSize()
                        .onFocusChanged { permFocused = it.isFocused || it.hasFocus }
                        .testTag("grant_install_permission_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = if (permFocused) BorderStroke(3.dp, BrightGold) else null,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = BrightGold,
                        contentColor = ObsidianBlack,
                      ),
                  ) {
                    Text(
                      text = "Allow App Installs",
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                } else {
                  var installFocused by remember { mutableStateOf(false) }
                  val installScale by animateFloatAsState(
                    targetValue = if (installFocused) 1.05f else 1f,
                    label = "install_apk_scale",
                  )
                  Button(
                    onClick = {
                      installActionError = null
                      AppUpdateManager.launchApkInstall(context, appUpdateState.apkFile).onFailure { err ->
                        installActionError =
                          err.localizedMessage ?: "Unable to start Android package installer."
                      }
                    },
                    modifier =
                      Modifier.scale(installScale)
                        .minimumInteractiveComponentSize()
                        .onFocusChanged { installFocused = it.isFocused || it.hasFocus }
                        .testTag("install_downloaded_apk_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = if (installFocused) BorderStroke(3.dp, BrightGold) else null,
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = SeafoamTeal,
                        contentColor = ObsidianBlack,
                      ),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.SystemUpdate,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Install v${appUpdateState.latestVersion}",
                      fontWeight = FontWeight.SemiBold,
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
}

@Composable
private fun SavedLocationPill(
  location: SavedLocation,
  selected: Boolean,
  canRemove: Boolean,
  accentColor: Color,
  onSelect: () -> Unit,
  onRemove: () -> Unit,
) {
  var selectFocused by remember { mutableStateOf(false) }
  var removeFocused by remember { mutableStateOf(false) }
  val pillScale by
    animateFloatAsState(
      targetValue = if (selectFocused || removeFocused) 1.04f else 1f,
      label = "saved_pill_scale",
    )
  val bgColor by
    animateColorAsState(
      targetValue =
        when {
          selected -> accentColor.copy(alpha = 0.20f)
          else -> DeepCardSurface
        },
      label = "saved_pill_bg",
    )
  val outerBorderColor =
    when {
      selected -> accentColor.copy(alpha = 0.72f)
      else -> SubtleOutline
    }

  Surface(
    modifier = Modifier.scale(pillScale),
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, outerBorderColor),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Row(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selectFocused) lerp(ElevatedSurface, BrightGold, 0.18f) else Color.Transparent)
            .border(
              width = if (selectFocused) 3.dp else 0.dp,
              color = if (selectFocused) BrightGold else Color.Transparent,
              shape = RoundedCornerShape(12.dp),
            )
            .onFocusChanged { selectFocused = it.isFocused }
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = if (canRemove) 8.dp else 12.dp, top = 8.dp, bottom = 8.dp)
            .testTag("saved_location_chip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (selected) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Active location",
            tint = accentColor,
            modifier = Modifier.size(16.dp),
          )
        }
        Text(
          text = location.displayLabel,
          style = MaterialTheme.typography.labelLarge,
          color = if (selected || selectFocused) SoftFrostText else SoftFrostText.copy(alpha = 0.85f),
        )
      }

      if (canRemove) {
        IconButton(
          onClick = onRemove,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(CircleShape)
              .background(if (removeFocused) lerp(ElevatedSurface, BrightGold, 0.18f) else Color.Transparent, CircleShape)
              .border(
                width = if (removeFocused) 3.dp else 0.dp,
                color = if (removeFocused) BrightGold else Color.Transparent,
                shape = CircleShape,
              )
              .onFocusChanged { removeFocused = it.isFocused || it.hasFocus }
              .testTag("remove_saved_location_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove ${location.shortLabel}",
            tint = if (removeFocused) BrightGold else SoftFrostText.copy(alpha = 0.78f),
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, tint: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    Text(text = title, style = MaterialTheme.typography.titleMedium, color = SoftFrostText)
  }
}

@Composable
private fun BottomBarBulletToggle(
  label: String,
  selected: Boolean,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = "•",
      style = MaterialTheme.typography.titleMedium,
      color = if (selected) accentColor else SoftFrostText.copy(alpha = 0.65f),
    )
    TvSelectableChip(
      label = label,
      selected = selected,
      accentColor = accentColor,
      testTag = testTag,
      onClick = onClick,
    )
  }
}

@Composable
private fun TvSelectableChip(
  label: String,
  selected: Boolean,
  accentColor: Color,
  testTag: String,
  leadingIcon: ImageVector? = null,
  onClick: () -> Unit,
) {
  var focused by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(targetValue = if (focused) 1.05f else 1f, label = "chip_scale")
  val bgColor by
    animateColorAsState(
      targetValue =
        when {
          focused && selected -> lerp(accentColor.copy(alpha = 0.22f), BrightGold, 0.18f)
          focused -> lerp(ElevatedSurface, BrightGold, 0.18f)
          selected -> accentColor.copy(alpha = 0.22f)
          else -> DeepCardSurface
        },
      label = "chip_bg",
    )
  val borderColor =
    when {
      focused -> BrightGold
      selected -> accentColor.copy(alpha = 0.72f)
      else -> SubtleOutline
    }

  Surface(
    modifier =
      Modifier.scale(scale)
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(12.dp))
        .onFocusChanged { focused = it.isFocused }
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    border =
      BorderStroke(
        width =
          when {
            focused -> 3.dp
            selected -> 1.5.dp
            else -> 1.dp
          },
        color = borderColor,
      ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint =
            when {
              focused -> BrightGold
              selected -> accentColor
              else -> MutedSlateText
            },
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected || focused) SoftFrostText else SoftFrostText.copy(alpha = 0.85f),
      )
    }
  }
}

@Composable
private fun TextColorDropdown(
  selectedOption: TextColorOption,
  accentColor: Color,
  onSelectOption: (TextColorOption) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  var focused by remember { mutableStateOf(false) }
  val dropdownScale by animateFloatAsState(targetValue = if (focused) 1.015f else 1f, label = "dropdown_scale")

  Box(modifier = Modifier.fillMaxWidth()) {
    Surface(
      modifier =
        Modifier.fillMaxWidth()
          .scale(dropdownScale)
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(14.dp))
          .onFocusChanged { focused = it.isFocused }
          .clickable { expanded = true }
          .testTag("text_color_dropdown"),
      shape = RoundedCornerShape(14.dp),
      color = if (focused) lerp(DeepCardSurface, BrightGold, 0.14f) else DeepCardSurface,
      border =
        BorderStroke(
          width = if (focused || expanded) 3.dp else 1.dp,
          color = if (focused || expanded) BrightGold else SubtleOutline,
        ),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          TextColorSwatch(option = selectedOption)
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Display Text Color",
              style = MaterialTheme.typography.labelSmall,
              color = SoftFrostText.copy(alpha = 0.78f),
            )
            Text(
              text =
                when (selectedOption) {
                  TextColorOption.WHITE -> "White (Default)"
                  TextColorOption.RAINBOW -> "Rainbow Timer (Smooth Similar Blend)"
                  TextColorOption.RAINBOW_BOUNCE -> "Rainbow Bounce (Contrasting Hit)"
                  else -> selectedOption.label
                },
              style = MaterialTheme.typography.bodyLarge,
              color = SoftFrostText,
            )
          }
        }
        Icon(
          imageVector = Icons.Rounded.ArrowDropDown,
          contentDescription = "Choose display text color",
          tint = if (focused || expanded) BrightGold else accentColor,
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(ElevatedSurface),
    ) {
      TextColorOption.entries.forEach { option ->
        val isSelected = option == selectedOption
        var itemFocused by remember { mutableStateOf(false) }
        DropdownMenuItem(
          leadingIcon = { TextColorSwatch(option = option) },
          text = {
            Text(
              text =
                when (option) {
                  TextColorOption.RAINBOW -> "Rainbow Timer (Smooth Blend)"
                  TextColorOption.RAINBOW_BOUNCE -> "Rainbow Bounce (Contrast Hit)"
                  else -> option.label
                },
              style = MaterialTheme.typography.bodyLarge,
              color = SoftFrostText,
              fontWeight = if (isSelected || itemFocused) FontWeight.SemiBold else FontWeight.Normal,
            )
          },
          trailingIcon =
            if (isSelected) {
              {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Selected",
                  tint = accentColor,
                )
              }
            } else {
              null
            },
          modifier =
            Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (itemFocused) lerp(ElevatedSurface, BrightGold, 0.20f) else Color.Transparent,
                RoundedCornerShape(10.dp),
              )
              .border(
                width = if (itemFocused) 3.dp else 0.dp,
                color = if (itemFocused) BrightGold else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
              )
              .onFocusChanged { itemFocused = it.isFocused || it.hasFocus }
              .testTag("text_color_option_${option.name.lowercase()}"),
          onClick = {
            onSelectOption(option)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun TextColorSwatch(option: TextColorOption) {
  val swatchModifier =
    Modifier.size(14.dp)
      .clip(CircleShape)
      .border(1.dp, SubtleOutline, CircleShape)

  if (option == TextColorOption.RAINBOW || option == TextColorOption.RAINBOW_BOUNCE) {
    Box(
      modifier =
        swatchModifier.background(
          Brush.sweepGradient(
            listOf(
              AuroraCyan,
              SeafoamTeal,
              BrightGold,
              BrightCoral,
              SoftAmethyst,
              BrightCobalt,
              AuroraCyan,
            )
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (option == TextColorOption.RAINBOW_BOUNCE) {
        Box(
          modifier =
            Modifier.size(5.dp)
              .clip(CircleShape)
              .background(ObsidianBlack)
        )
      }
    }
  } else {
    val swatchColor =
      when (option) {
        TextColorOption.WHITE -> SoftFrostText
        TextColorOption.ELECTRIC_CYAN -> AuroraCyan
        TextColorOption.MINT -> SeafoamTeal
        TextColorOption.SOLAR_GOLD -> BrightGold
        TextColorOption.CORAL -> BrightCoral
        TextColorOption.VIOLET -> SoftAmethyst
        TextColorOption.COBALT -> BrightCobalt
        TextColorOption.RAINBOW,
        TextColorOption.RAINBOW_BOUNCE -> SoftFrostText
      }
    Box(modifier = swatchModifier.background(swatchColor))
  }
}
