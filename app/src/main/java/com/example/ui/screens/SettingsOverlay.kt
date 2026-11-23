package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
  onToggle24HourForecast: (Boolean) -> Unit,
  onToggleSevereAlerts: (Boolean) -> Unit,
  onSelectDriftIntensity: (DriftIntensity) -> Unit,
  onSelectDriftMotionMode: (DriftMotionMode) -> Unit,
  onSelectTextColor: (TextColorOption) -> Unit,
  onRefreshWeather: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  val keyboardController = LocalSoftwareKeyboardController.current
  val closeButtonFocusRequester = remember { FocusRequester() }
  val searchBoxFocusRequester = remember { FocusRequester() }
  val searchFieldFocusRequester = remember { FocusRequester() }

  var isSearchEditing by remember { mutableStateOf(false) }
  var searchBoxFocused by remember { mutableStateOf(false) }
  var searchFieldAcquiredFocus by remember { mutableStateOf(false) }

  // Ensure initial focus lands on the Close button so the search box / keyboard never auto-opens
  LaunchedEffect(Unit) {
    keyboardController?.hide()
    withFrameNanos {}
    runCatching { closeButtonFocusRequester.requestFocus() }
  }

  LaunchedEffect(isSearchEditing) {
    if (isSearchEditing) {
      searchFieldAcquiredFocus = false
      withFrameNanos {}
      runCatching { searchFieldFocusRequester.requestFocus() }
      keyboardController?.show()
    } else {
      keyboardController?.hide()
    }
  }

  BackHandler(enabled = isSearchEditing) {
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
          IconButton(
            onClick = {
              keyboardController?.hide()
              onDismiss()
            },
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .focusRequester(closeButtonFocusRequester)
                .testTag("close_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Resume screensaver",
              tint = SoftFrostText,
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
          if (isSearchEditing) {
            OutlinedTextField(
              value = citySearchState.query,
              onValueChange = onQueryChange,
              modifier =
                Modifier.weight(1f)
                  .focusRequester(searchFieldFocusRequester)
                  .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                      searchFieldAcquiredFocus = true
                    } else if (searchFieldAcquiredFocus) {
                      isSearchEditing = false
                    }
                  }
                  .testTag("city_search_input"),
              placeholder = {
                Text(
                  "Search city or zip code (e.g. Seattle, 90210)",
                  color = SoftFrostText.copy(alpha = 0.65f),
                )
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions =
                KeyboardActions(
                  onSearch = {
                    isSearchEditing = false
                    keyboardController?.hide()
                    onSearchSubmit()
                  }
                ),
              shape = RoundedCornerShape(14.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedTextColor = SoftFrostText,
                  unfocusedTextColor = SoftFrostText,
                  focusedBorderColor = accentColor,
                  unfocusedBorderColor = SubtleOutline,
                  focusedContainerColor = DeepCardSurface,
                  unfocusedContainerColor = DeepCardSurface,
                ),
            )
          } else {
            Surface(
              modifier =
                Modifier.weight(1f)
                  .defaultMinSize(minHeight = 56.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .focusRequester(searchBoxFocusRequester)
                  .onFocusChanged { searchBoxFocused = it.isFocused }
                  .clickable { isSearchEditing = true }
                  .testTag("city_search_input"),
              shape = RoundedCornerShape(14.dp),
              color = DeepCardSurface,
              border =
                BorderStroke(
                  width = if (searchBoxFocused) 2.dp else 1.dp,
                  color = if (searchBoxFocused) accentColor else SubtleOutline,
                ),
            ) {
              Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                Text(
                  text =
                    citySearchState.query.ifBlank {
                      "Search city or zip code (press OK to type)"
                    },
                  style = MaterialTheme.typography.bodyLarge,
                  color =
                    if (citySearchState.query.isNotBlank()) SoftFrostText
                    else SoftFrostText.copy(alpha = 0.65f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
          Button(
            onClick = {
              isSearchEditing = false
              keyboardController?.hide()
              onSearchSubmit()
            },
            enabled = !citySearchState.isSearching,
            modifier = Modifier.height(54.dp).testTag("city_search_button"),
            shape = RoundedCornerShape(14.dp),
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
            citySearchState.results.take(5).forEach { city ->
              var focused by remember { mutableStateOf(false) }
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (focused) ElevatedSurface else Color.Transparent)
                    .border(
                      width = if (focused) 1.5.dp else 0.dp,
                      color = if (focused) accentColor else Color.Transparent,
                      shape = RoundedCornerShape(10.dp),
                    )
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .clickable { onSelectCity(city) }
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

        // Units & Clock
        SectionHeader(icon = Icons.Rounded.Schedule, title = "Units & Clock", tint = accentColor)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TvSelectableChip(
            label = "°F Fahrenheit",
            selected = settings.temperatureUnit == TemperatureUnit.FAHRENHEIT,
            accentColor = accentColor,
            testTag = "unit_fahrenheit_chip",
            onClick = { onSelectUnit(TemperatureUnit.FAHRENHEIT) },
          )
          TvSelectableChip(
            label = "°C Celsius",
            selected = settings.temperatureUnit == TemperatureUnit.CELSIUS,
            accentColor = accentColor,
            testTag = "unit_celsius_chip",
            onClick = { onSelectUnit(TemperatureUnit.CELSIUS) },
          )
          TvSelectableChip(
            label = "Wind: mph",
            selected = settings.windSpeedUnit == WindSpeedUnit.MPH,
            accentColor = accentColor,
            testTag = "wind_unit_mph_chip",
            onClick = { onSelectWindUnit(WindSpeedUnit.MPH) },
          )
          TvSelectableChip(
            label = "Wind: kph",
            selected = settings.windSpeedUnit == WindSpeedUnit.KPH,
            accentColor = accentColor,
            testTag = "wind_unit_kph_chip",
            onClick = { onSelectWindUnit(WindSpeedUnit.KPH) },
          )
          TvSelectableChip(
            label = "12-Hour",
            selected = !settings.use24HourClock,
            accentColor = accentColor,
            testTag = "clock_12h_chip",
            onClick = { onToggle24Hour(false) },
          )
          TvSelectableChip(
            label = "24-Hour",
            selected = settings.use24HourClock,
            accentColor = accentColor,
            testTag = "clock_24h_chip",
            onClick = { onToggle24Hour(true) },
          )
        }

        HorizontalDivider(color = SubtleOutline)

        // Forecast Panels, Severe Alerts & Reactive Visuals
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
            label = if (settings.show24HourForecast) "24-Hour Forecast: ON" else "24-Hour Forecast: OFF",
            selected = settings.show24HourForecast,
            accentColor = accentColor,
            testTag = "toggle_24hour_chip",
            onClick = { onToggle24HourForecast(!settings.show24HourForecast) },
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
            Surface(
              modifier =
                Modifier.weight(1f)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(12.dp))
                  .onFocusChanged { focused = it.isFocused }
                  .focusable()
                  .clickable { onSelectDriftMotionMode(motionMode) }
                  .testTag("motion_mode_${motionMode.name.lowercase()}_chip"),
              shape = RoundedCornerShape(12.dp),
              color =
                when {
                  selected -> accentColor.copy(alpha = 0.24f)
                  focused -> ElevatedSurface
                  else -> DeepCardSurface
                },
              border =
                BorderStroke(
                  width = if (focused || selected) 2.dp else 1.dp,
                  color =
                    when {
                      focused -> accentColor
                      selected -> accentColor.copy(alpha = 0.80f)
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
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  color = SoftFrostText,
                )
              }
            }
          }
        }
        Text(
          text =
            "Clockwise glides continuously along the outer edges; DVD Bounce shrinks elements by 20% and bounces off edges.",
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
          OutlinedButton(
            onClick = onRefreshWeather,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_weather_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, SubtleOutline),
          ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, tint = accentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Weather", color = SoftFrostText)
          }

          Button(
            onClick = {
              keyboardController?.hide()
              onDismiss()
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("resume_screensaver_button"),
            shape = RoundedCornerShape(14.dp),
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
  val bgColor by
    animateColorAsState(
      targetValue =
        when {
          selected -> accentColor.copy(alpha = 0.20f)
          selectFocused -> ElevatedSurface
          else -> DeepCardSurface
        },
      label = "saved_pill_bg",
    )
  val borderColor =
    when {
      selectFocused || removeFocused -> accentColor
      selected -> accentColor.copy(alpha = 0.72f)
      else -> SubtleOutline
    }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    border = BorderStroke(if (selectFocused || removeFocused) 2.dp else 1.dp, borderColor),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Row(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .onFocusChanged { selectFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onSelect)
            .padding(start = 12.dp, end = if (canRemove) 6.dp else 12.dp, top = 8.dp, bottom = 8.dp)
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
              .onFocusChanged { removeFocused = it.isFocused }
              .focusable()
              .testTag("remove_saved_location_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Remove ${location.shortLabel}",
            tint = if (removeFocused) BrightCoral else SoftFrostText.copy(alpha = 0.78f),
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
          selected -> accentColor.copy(alpha = 0.22f)
          focused -> ElevatedSurface
          else -> DeepCardSurface
        },
      label = "chip_bg",
    )
  val borderColor =
    when {
      focused -> accentColor
      selected -> accentColor.copy(alpha = 0.7f)
      else -> SubtleOutline
    }

  Surface(
    modifier =
      Modifier.scale(scale)
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(12.dp))
        .onFocusChanged { focused = it.isFocused }
        .focusable()
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
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
          tint = if (selected || focused) accentColor else MutedSlateText,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
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

  Box(modifier = Modifier.fillMaxWidth()) {
    Surface(
      modifier =
        Modifier.fillMaxWidth()
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(14.dp))
          .onFocusChanged { focused = it.isFocused }
          .focusable()
          .clickable { expanded = true }
          .testTag("text_color_dropdown"),
      shape = RoundedCornerShape(14.dp),
      color = DeepCardSurface,
      border =
        BorderStroke(
          width = if (focused || expanded) 2.dp else 1.dp,
          color = if (focused || expanded) accentColor else SubtleOutline,
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
          tint = accentColor,
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
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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
          modifier = Modifier.testTag("text_color_option_${option.name.lowercase()}"),
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
