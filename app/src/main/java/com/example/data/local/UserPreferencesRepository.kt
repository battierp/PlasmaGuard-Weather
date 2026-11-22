package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DriftIntensity
import com.example.data.model.SavedLocation
import com.example.data.model.TemperatureUnit
import com.example.data.model.TextColorOption
import com.example.data.model.WeatherSnapshot
import com.example.data.model.WindSpeedUnit
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "plasmasky_prefs")

const val MAX_SAVED_LOCATIONS = 5

data class UserSettings(
  val cityName: String = "Mountain View",
  val latitude: Double = 37.3861,
  val longitude: Double = -122.0839,
  val hasExplicitLocation: Boolean = false,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
  val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.MPH,
  val use24HourClock: Boolean = false,
  val driftIntensity: DriftIntensity = DriftIntensity.BALANCED,
  val textColorOption: TextColorOption = TextColorOption.WHITE,
  val show7DayForecast: Boolean = true,
  val show24HourForecast: Boolean = true,
  val showSevereWeatherAlerts: Boolean = true,
  val savedLocations: List<SavedLocation> = emptyList(),
  val autoRotateLocations: Boolean = false,
)

class UserPreferencesRepository(
  private val context: Context,
  private val moshi: Moshi,
) {
  private object Keys {
    val CITY_NAME = stringPreferencesKey("city_name")
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
    val HAS_EXPLICIT_LOCATION = booleanPreferencesKey("has_explicit_location")
    val USE_CELSIUS = booleanPreferencesKey("use_celsius")
    val WIND_SPEED_UNIT = stringPreferencesKey("wind_speed_unit")
    val USE_24H_CLOCK = booleanPreferencesKey("use_24h_clock")
    val DRIFT_INTENSITY = stringPreferencesKey("drift_intensity")
    val TEXT_COLOR_OPTION = stringPreferencesKey("text_color_option")
    val SHOW_7_DAY_FORECAST = booleanPreferencesKey("show_7_day_forecast")
    val SHOW_24_HOUR_FORECAST = booleanPreferencesKey("show_24_hour_forecast")
    val SHOW_SEVERE_WEATHER_ALERTS = booleanPreferencesKey("show_severe_weather_alerts")
    val SAVED_LOCATIONS_JSON = stringPreferencesKey("saved_locations_v1_json")
    val AUTO_ROTATE_LOCATIONS = booleanPreferencesKey("auto_rotate_locations")
    val CACHED_WEATHER_JSON = stringPreferencesKey("cached_weather_v6_json")
  }

  private val snapshotAdapter by lazy { moshi.adapter(WeatherSnapshot::class.java) }
  private val savedLocationsAdapter by lazy {
    val listType = Types.newParameterizedType(List::class.java, SavedLocation::class.java)
    moshi.adapter<List<SavedLocation>>(listType)
  }

  val settingsFlow: Flow<UserSettings> =
    context.dataStore.data.map { prefs ->
      val activeCity = (prefs[Keys.CITY_NAME] ?: "Mountain View").substringBefore(",").trim()
      val activeLat = prefs[Keys.LATITUDE] ?: 37.3861
      val activeLon = prefs[Keys.LONGITUDE] ?: -122.0839
      val parsedSaved = parseSavedLocations(prefs[Keys.SAVED_LOCATIONS_JSON])
      val effectiveSaved =
        if (parsedSaved.isNotEmpty()) {
          parsedSaved.take(MAX_SAVED_LOCATIONS)
        } else {
          listOf(SavedLocation(name = activeCity, latitude = activeLat, longitude = activeLon))
        }

      UserSettings(
        cityName = activeCity,
        latitude = activeLat,
        longitude = activeLon,
        hasExplicitLocation = prefs[Keys.HAS_EXPLICIT_LOCATION] ?: false,
        temperatureUnit =
          if (prefs[Keys.USE_CELSIUS] == true) TemperatureUnit.CELSIUS
          else TemperatureUnit.FAHRENHEIT,
        windSpeedUnit =
          prefs[Keys.WIND_SPEED_UNIT]?.let { name ->
            WindSpeedUnit.entries.firstOrNull { it.name == name }
          } ?: WindSpeedUnit.MPH,
        use24HourClock = prefs[Keys.USE_24H_CLOCK] ?: false,
        driftIntensity =
          prefs[Keys.DRIFT_INTENSITY]?.let { name ->
            DriftIntensity.entries.firstOrNull { it.name == name }
          } ?: DriftIntensity.BALANCED,
        textColorOption =
          prefs[Keys.TEXT_COLOR_OPTION]?.let { name ->
            TextColorOption.entries.firstOrNull { it.name == name }
          } ?: TextColorOption.WHITE,
        show7DayForecast = prefs[Keys.SHOW_7_DAY_FORECAST] ?: true,
        show24HourForecast = prefs[Keys.SHOW_24_HOUR_FORECAST] ?: true,
        showSevereWeatherAlerts = prefs[Keys.SHOW_SEVERE_WEATHER_ALERTS] ?: true,
        savedLocations = effectiveSaved,
        autoRotateLocations = prefs[Keys.AUTO_ROTATE_LOCATIONS] ?: false,
      )
    }

  suspend fun saveLocation(
    cityName: String,
    latitude: Double,
    longitude: Double,
    admin1: String? = null,
    country: String? = null,
    explicit: Boolean = true,
  ) {
    val cleanName = cityName.substringBefore(",").trim()
    context.dataStore.edit { prefs ->
      val previousLat = prefs[Keys.LATITUDE] ?: 37.3861
      val previousLon = prefs[Keys.LONGITUDE] ?: -122.0839
      val existing = parseSavedLocations(prefs[Keys.SAVED_LOCATIONS_JSON])
      val incoming =
        SavedLocation(
          name = cleanName,
          admin1 = admin1,
          country = country,
          latitude = latitude,
          longitude = longitude,
        )
      val updatedList =
        upsertSavedLocationList(
          current = existing,
          incoming = incoming,
          activeLat = previousLat,
          activeLon = previousLon,
        )
      prefs[Keys.CITY_NAME] = cleanName
      prefs[Keys.LATITUDE] = latitude
      prefs[Keys.LONGITUDE] = longitude
      prefs[Keys.HAS_EXPLICIT_LOCATION] = explicit
      prefs[Keys.SAVED_LOCATIONS_JSON] = savedLocationsAdapter.toJson(updatedList)
    }
  }

  suspend fun selectSavedLocation(location: SavedLocation) {
    context.dataStore.edit { prefs ->
      prefs[Keys.CITY_NAME] = location.shortLabel
      prefs[Keys.LATITUDE] = location.latitude
      prefs[Keys.LONGITUDE] = location.longitude
      prefs[Keys.HAS_EXPLICIT_LOCATION] = true
    }
  }

  suspend fun removeSavedLocation(location: SavedLocation) {
    context.dataStore.edit { prefs ->
      val existing = parseSavedLocations(prefs[Keys.SAVED_LOCATIONS_JSON])
      if (existing.size <= 1) return@edit

      val remaining = existing.filterNot { it.matches(location.latitude, location.longitude) }
      if (remaining.isEmpty()) return@edit

      prefs[Keys.SAVED_LOCATIONS_JSON] = savedLocationsAdapter.toJson(remaining)

      val activeLat = prefs[Keys.LATITUDE] ?: 37.3861
      val activeLon = prefs[Keys.LONGITUDE] ?: -122.0839
      if (location.matches(activeLat, activeLon)) {
        val fallback = remaining.first()
        prefs[Keys.CITY_NAME] = fallback.shortLabel
        prefs[Keys.LATITUDE] = fallback.latitude
        prefs[Keys.LONGITUDE] = fallback.longitude
        prefs[Keys.HAS_EXPLICIT_LOCATION] = true
      }
    }
  }

  suspend fun setTemperatureUnit(unit: TemperatureUnit) {
    context.dataStore.edit { prefs ->
      prefs[Keys.USE_CELSIUS] = (unit == TemperatureUnit.CELSIUS)
    }
  }

  suspend fun setWindSpeedUnit(unit: WindSpeedUnit) {
    context.dataStore.edit { prefs ->
      prefs[Keys.WIND_SPEED_UNIT] = unit.name
    }
  }

  suspend fun setUse24HourClock(use24Hour: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.USE_24H_CLOCK] = use24Hour
    }
  }

  suspend fun setDriftIntensity(intensity: DriftIntensity) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DRIFT_INTENSITY] = intensity.name
    }
  }

  suspend fun setTextColorOption(option: TextColorOption) {
    context.dataStore.edit { prefs ->
      prefs[Keys.TEXT_COLOR_OPTION] = option.name
    }
  }

  suspend fun setShow7DayForecast(show: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SHOW_7_DAY_FORECAST] = show
    }
  }

  suspend fun setShow24HourForecast(show: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SHOW_24_HOUR_FORECAST] = show
    }
  }

  suspend fun setShowSevereWeatherAlerts(show: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SHOW_SEVERE_WEATHER_ALERTS] = show
    }
  }

  suspend fun setAutoRotateLocations(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.AUTO_ROTATE_LOCATIONS] = enabled
    }
  }

  suspend fun saveCachedWeather(snapshot: WeatherSnapshot) {
    val json = snapshotAdapter.toJson(snapshot)
    context.dataStore.edit { prefs ->
      prefs[Keys.CACHED_WEATHER_JSON] = json
    }
  }

  suspend fun loadCachedWeather(): WeatherSnapshot? {
    val json = context.dataStore.data.first()[Keys.CACHED_WEATHER_JSON] ?: return null
    return runCatching { snapshotAdapter.fromJson(json)?.copy(isCached = true) }.getOrNull()
  }

  private fun parseSavedLocations(json: String?): List<SavedLocation> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { savedLocationsAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
  }
}

fun upsertSavedLocationList(
  current: List<SavedLocation>,
  incoming: SavedLocation,
  activeLat: Double,
  activeLon: Double,
  maxCount: Int = MAX_SAVED_LOCATIONS,
): List<SavedLocation> {
  val existingIdx = current.indexOfFirst { it.matches(incoming.latitude, incoming.longitude) }
  if (existingIdx >= 0) {
    return current.toMutableList().apply { set(existingIdx, incoming) }
  }
  if (current.size < maxCount) {
    return current + incoming
  }
  val mutable = current.toMutableList()
  val replaceIdx = mutable.indexOfFirst { !it.matches(activeLat, activeLon) }.let { idx ->
    if (idx >= 0) idx else 0
  }
  mutable.removeAt(replaceIdx)
  mutable.add(incoming)
  return mutable
}
