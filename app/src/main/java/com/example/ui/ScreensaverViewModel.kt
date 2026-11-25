package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.model.CitySearchResult
import com.example.data.model.DriftIntensity
import com.example.data.model.DriftMotionMode
import com.example.data.model.SavedLocation
import com.example.data.model.TemperatureUnit
import com.example.data.model.TextColorOption
import com.example.data.model.WeatherSnapshot
import com.example.data.model.WindSpeedUnit
import com.example.data.model.normalizeCityAutocompleteQuery
import com.example.data.remote.NetworkModule
import com.example.data.repository.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
  data object Loading : WeatherUiState
  data class Ready(val snapshot: WeatherSnapshot) : WeatherUiState
  data class Error(val message: String) : WeatherUiState
}

data class ClockDisplay(
  val timeText: String,
  val dateText: String,
)

data class CitySearchUiState(
  val query: String = "",
  val isSearching: Boolean = false,
  val results: List<CitySearchResult> = emptyList(),
  val statusMessage: String? = null,
)

private const val AUTO_ROTATE_INTERVAL_MS = 120_000L

class ScreensaverViewModel(application: Application) : AndroidViewModel(application) {
  private val prefsRepo = UserPreferencesRepository(application, NetworkModule.moshi)
  private val weatherRepo = WeatherRepository(prefsRepo)

  val settings: StateFlow<UserSettings> =
    prefsRepo.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = UserSettings(),
    )

  private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
  val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

  private val _clockDisplay = MutableStateFlow(buildClockDisplay(use24Hour = false))
  val clockDisplay: StateFlow<ClockDisplay> = _clockDisplay.asStateFlow()

  private val _citySearchState = MutableStateFlow(CitySearchUiState())
  val citySearchState: StateFlow<CitySearchUiState> = _citySearchState.asStateFlow()

  private val _locationSwapBanner = MutableStateFlow<String?>(null)
  val locationSwapBanner: StateFlow<String?> = _locationSwapBanner.asStateFlow()
  private var swapBannerJob: Job? = null
  private var searchJob: Job? = null
  private var lastCompletedSearchQuery: String = ""

  init {
    viewModelScope.launch {
      val cached = weatherRepo.loadCached()
      if (cached != null) {
        _weatherState.value = WeatherUiState.Ready(cached)
      }
    }

    viewModelScope.launch {
      prefsRepo.settingsFlow
        .map { it.use24HourClock }
        .distinctUntilChanged()
        .collect { use24Hour ->
          _clockDisplay.value = buildClockDisplay(use24Hour)
        }
    }

    viewModelScope.launch {
      prefsRepo.settingsFlow
        .distinctUntilChangedBy {
          "${it.latitude}:${it.longitude}:${it.temperatureUnit.name}:${it.windSpeedUnit.name}:${it.showSevereWeatherAlerts}:${it.hasExplicitLocation}"
        }
        .collectLatest { currentSettings ->
          refreshWeatherInternal(currentSettings)
        }
    }

    // Auto-rotate through saved locations when enabled and >1 location is saved
    viewModelScope.launch {
      prefsRepo.settingsFlow
        .map { it.autoRotateLocations to it.savedLocations }
        .distinctUntilChanged()
        .collectLatest { (autoRotate, locations) ->
          if (autoRotate && locations.size > 1) {
            while (isActive) {
              delay(AUTO_ROTATE_INTERVAL_MS)
              cycleSavedLocation(step = 1, showSwapToast = false)
            }
          }
        }
    }

    // Tick clock every 10 seconds
    viewModelScope.launch {
      while (isActive) {
        _clockDisplay.value = buildClockDisplay(settings.value.use24HourClock)
        delay(10_000L)
      }
    }

    // Refresh weather automatically every 20 minutes while screensaver is active
    viewModelScope.launch {
      while (isActive) {
        delay(20 * 60 * 1000L)
        refreshWeatherInternal(settings.value)
      }
    }
  }

  fun refreshWeather() {
    viewModelScope.launch { refreshWeatherInternal(settings.value) }
  }

  private suspend fun refreshWeatherInternal(currentSettings: UserSettings) {
    if (_weatherState.value !is WeatherUiState.Ready) {
      _weatherState.value = WeatherUiState.Loading
    }
    weatherRepo
      .getWeather(currentSettings)
      .onSuccess { snapshot -> _weatherState.value = WeatherUiState.Ready(snapshot) }
      .onFailure { error ->
        if (_weatherState.value !is WeatherUiState.Ready) {
          _weatherState.value =
            WeatherUiState.Error(
              error.localizedMessage ?: "Unable to fetch forecast. Check network connection."
            )
        }
      }
  }

  fun updateSearchQuery(query: String) {
    searchJob?.cancel()
    val normalized = normalizeCityAutocompleteQuery(query)
    if (normalized == null) {
      lastCompletedSearchQuery = ""
      _citySearchState.update {
        it.copy(
          query = query,
          isSearching = false,
          results = emptyList(),
          statusMessage = null,
        )
      }
      return
    }

    _citySearchState.update { it.copy(query = query, statusMessage = null) }
    searchJob =
      viewModelScope.launch {
        delay(320L)
        executeCitySearch(query = normalized, showNoResultsMessage = false)
      }
  }

  fun submitCitySearch() {
    searchJob?.cancel()
    val normalized = normalizeCityAutocompleteQuery(_citySearchState.value.query)
    if (normalized == null) {
      _citySearchState.update {
        it.copy(
          isSearching = false,
          results = emptyList(),
          statusMessage = "Enter at least 2 characters to search",
        )
      }
      return
    }
    if (lastCompletedSearchQuery == normalized && _citySearchState.value.results.isNotEmpty()) {
      _citySearchState.update { it.copy(isSearching = false, statusMessage = null) }
      return
    }
    searchJob =
      viewModelScope.launch {
        executeCitySearch(query = normalized, showNoResultsMessage = true)
      }
  }

  private suspend fun executeCitySearch(
    query: String,
    showNoResultsMessage: Boolean,
  ) {
    _citySearchState.update { it.copy(isSearching = true, statusMessage = null) }
    weatherRepo
      .searchCities(query)
      .onSuccess { list ->
        if (_citySearchState.value.query.trim() != query) return@onSuccess
        lastCompletedSearchQuery = query
        _citySearchState.update {
          it.copy(
            isSearching = false,
            results = list,
            statusMessage =
              if (list.isEmpty() && (showNoResultsMessage || query.length >= 3)) {
                "No matching locations found"
              } else {
                null
              },
          )
        }
      }
      .onFailure {
        if (_citySearchState.value.query.trim() != query) return@onFailure
        _citySearchState.update {
          it.copy(
            isSearching = false,
            statusMessage =
              if (showNoResultsMessage) "Location search failed — check internet" else null,
          )
        }
      }
  }

  fun selectCity(city: CitySearchResult) {
    searchJob?.cancel()
    lastCompletedSearchQuery = ""
    viewModelScope.launch {
      prefsRepo.saveLocation(
        cityName = city.name,
        latitude = city.latitude,
        longitude = city.longitude,
        admin1 = city.admin1,
        country = city.country,
        explicit = true,
      )
      _citySearchState.update {
        it.copy(
          query = "",
          isSearching = false,
          results = emptyList(),
          statusMessage = "Location set & saved: ${city.name}",
        )
      }
    }
  }

  fun selectSavedLocation(location: SavedLocation) {
    viewModelScope.launch {
      prefsRepo.selectSavedLocation(location)
      _citySearchState.update {
        it.copy(statusMessage = "Active location: ${location.shortLabel}")
      }
    }
  }

  fun removeSavedLocation(location: SavedLocation) {
    viewModelScope.launch {
      prefsRepo.removeSavedLocation(location)
    }
  }

  fun cycleSavedLocation(step: Int, showSwapToast: Boolean = true) {
    val currentSettings = settings.value
    val locations = currentSettings.savedLocations
    if (locations.size <= 1) return

    val currentIdx =
      locations.indexOfFirst { it.matches(currentSettings.latitude, currentSettings.longitude) }
        .let { if (it >= 0) it else 0 }
    val nextIdx = ((currentIdx + step) % locations.size + locations.size) % locations.size
    val target = locations[nextIdx]

    viewModelScope.launch {
      prefsRepo.selectSavedLocation(target)
    }

    if (showSwapToast) {
      swapBannerJob?.cancel()
      _locationSwapBanner.value = "${target.shortLabel} (${nextIdx + 1}/${locations.size})"
      swapBannerJob =
        viewModelScope.launch {
          delay(2_800L)
          _locationSwapBanner.value = null
        }
    }
  }

  fun setTemperatureUnit(unit: TemperatureUnit) {
    viewModelScope.launch { prefsRepo.setTemperatureUnit(unit) }
  }

  fun setWindSpeedUnit(unit: WindSpeedUnit) {
    viewModelScope.launch { prefsRepo.setWindSpeedUnit(unit) }
  }

  fun setUse24HourClock(use24Hour: Boolean) {
    viewModelScope.launch {
      prefsRepo.setUse24HourClock(use24Hour)
      _clockDisplay.value = buildClockDisplay(use24Hour)
    }
  }

  fun setDriftIntensity(intensity: DriftIntensity) {
    viewModelScope.launch { prefsRepo.setDriftIntensity(intensity) }
  }

  fun setDriftMotionMode(mode: DriftMotionMode) {
    viewModelScope.launch { prefsRepo.setDriftMotionMode(mode) }
  }

  fun setTextColorOption(option: TextColorOption) {
    viewModelScope.launch { prefsRepo.setTextColorOption(option) }
  }

  fun setShow7DayForecast(show: Boolean) {
    viewModelScope.launch { prefsRepo.setShow7DayForecast(show) }
  }

  fun setShowBottomBarTemp24h(show: Boolean) {
    viewModelScope.launch { prefsRepo.setShowBottomBarTemp24h(show) }
  }

  fun setShowBottomBarWind(show: Boolean) {
    viewModelScope.launch { prefsRepo.setShowBottomBarWind(show) }
  }

  fun setShowBottomBarAirQuality(show: Boolean) {
    viewModelScope.launch { prefsRepo.setShowBottomBarAirQuality(show) }
  }

  fun setShowBottomBarPrecipitation(show: Boolean) {
    viewModelScope.launch { prefsRepo.setShowBottomBarPrecipitation(show) }
  }

  fun setShowSevereWeatherAlerts(show: Boolean) {
    viewModelScope.launch { prefsRepo.setShowSevereWeatherAlerts(show) }
  }

  fun setAutoRotateLocations(enabled: Boolean) {
    viewModelScope.launch { prefsRepo.setAutoRotateLocations(enabled) }
  }

  private fun buildClockDisplay(use24Hour: Boolean): ClockDisplay {
    val now = Date()
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    val datePattern = "EEE, MMM d"
    return ClockDisplay(
      timeText = SimpleDateFormat(timePattern, Locale.getDefault()).format(now),
      dateText = SimpleDateFormat(datePattern, Locale.getDefault()).format(now),
    )
  }
}
