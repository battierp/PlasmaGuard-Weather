package com.example.data.repository

import android.content.Context
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.model.CitySearchResult
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyMetricSlot
import com.example.data.model.HourlySlot
import com.example.data.model.OpenMeteoAirQualityResponse
import com.example.data.model.OpenMeteoForecastResponse
import com.example.data.model.WeatherAlert
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherSnapshot
import com.example.data.model.WindSpeedUnit
import com.example.data.remote.NetworkModule
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(
  private val prefsRepo: UserPreferencesRepository,
) {
  val settingsFlow: Flow<UserSettings> = prefsRepo.settingsFlow

  suspend fun getWeather(settings: UserSettings): Result<WeatherSnapshot> =
    withContext(Dispatchers.IO) {
      try {
        val activeSettings =
          if (!settings.hasExplicitLocation) {
            detectApproximateLocation()?.let { detected ->
              prefsRepo.saveLocation(
                cityName = detected.name,
                latitude = detected.latitude,
                longitude = detected.longitude,
                admin1 = detected.admin1,
                country = detected.country,
                explicit = true,
              )
              settings.copy(
                cityName = detected.name,
                latitude = detected.latitude,
                longitude = detected.longitude,
                hasExplicitLocation = true,
              )
            } ?: settings
          } else {
            settings
          }

        val windUnit = activeSettings.windSpeedUnit
        val (forecastPair, airQuality) =
          coroutineScope {
            val forecastDeferred = async {
              NetworkModule.forecastApi.getForecast(
                latitude = activeSettings.latitude,
                longitude = activeSettings.longitude,
                temperatureUnit = activeSettings.temperatureUnit.apiParam,
                windSpeedUnit = windUnit.apiParam,
              )
            }
            val alertsDeferred = async {
              if (activeSettings.showSevereWeatherAlerts) {
                fetchNwsAlerts(activeSettings.latitude, activeSettings.longitude)
              } else {
                emptyList()
              }
            }
            val airQualityDeferred = async {
              runCatching {
                NetworkModule.airQualityApi.getAirQuality(
                  latitude = activeSettings.latitude,
                  longitude = activeSettings.longitude,
                )
              }.getOrNull()
            }
            (forecastDeferred.await() to alertsDeferred.await()) to airQualityDeferred.await()
          }

        val (response, nwsAlerts) = forecastPair
        val snapshot = mapToSnapshot(response, airQuality, activeSettings, windUnit, nwsAlerts)
        prefsRepo.saveCachedWeather(snapshot)
        Result.success(snapshot)
      } catch (e: Exception) {
        val cached = prefsRepo.loadCachedWeather()
        if (cached != null) {
          Result.success(cached)
        } else {
          Result.failure(e)
        }
      }
    }

  private suspend fun fetchNwsAlerts(latitude: Double, longitude: Double): List<WeatherAlert> =
    runCatching {
      val point = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
      val response = NetworkModule.nwsAlertApi.getActiveAlerts(point)
      response.features
        .orEmpty()
        .mapNotNull { feature ->
          val props = feature.properties ?: return@mapNotNull null
          if (props.status.equals("Test", ignoreCase = true)) return@mapNotNull null
          val event = props.event?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
          val severity = props.severity?.trim()?.takeIf { it.isNotEmpty() } ?: "Moderate"
          val headline =
            props.headline?.trim()?.takeIf { it.isNotEmpty() }
              ?: "$event in effect"
          WeatherAlert(event = event, severity = severity, headline = headline)
        }
        .distinctBy { "${it.event}:${it.headline}" }
        .take(3)
    }.getOrDefault(emptyList())

  suspend fun detectApproximateLocation(): CitySearchResult? =
    withContext(Dispatchers.IO) {
      runCatching {
        val geo = NetworkModule.ipGeoApi.detectLocation()
        val lat = geo.latitude?.toDoubleOrNull() ?: return@runCatching null
        val lon = geo.longitude?.toDoubleOrNull() ?: return@runCatching null
        val city = geo.city?.takeIf { it.isNotBlank() } ?: return@runCatching null
        CitySearchResult(
          name = city,
          admin1 = geo.region,
          country = geo.country,
          latitude = lat,
          longitude = lon,
        )
      }.getOrNull()
    }

  suspend fun searchCities(query: String): Result<List<CitySearchResult>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val trimmed = com.example.data.model.normalizeCityAutocompleteQuery(query)
          ?: return@runCatching emptyList()
        val response = NetworkModule.geocodingApi.searchCity(trimmed)
        response.results.orEmpty().mapNotNull { item ->
          val name = item.name ?: return@mapNotNull null
          val lat = item.latitude ?: return@mapNotNull null
          val lon = item.longitude ?: return@mapNotNull null
          CitySearchResult(
            name = name,
            admin1 = item.admin1,
            country = item.country,
            latitude = lat,
            longitude = lon,
          )
        }.distinctBy { it.displayName }
      }
    }

  suspend fun loadCached(): WeatherSnapshot? = prefsRepo.loadCachedWeather()

  private fun mapToSnapshot(
    response: OpenMeteoForecastResponse,
    airQuality: OpenMeteoAirQualityResponse?,
    settings: UserSettings,
    windUnit: WindSpeedUnit,
    nwsAlerts: List<WeatherAlert>,
  ): WeatherSnapshot {
    val current = response.current ?: error("Missing current weather payload")
    val isDayNow = (current.isDay ?: 1) == 1
    val currentCode = current.weatherCode ?: 0
    val currentCondition = WeatherCondition.fromWmoCode(currentCode, isDayNow)

    val hourlyDto = response.hourly
    val hourlyTimes = hourlyDto?.time.orEmpty()
    val hourlyTemps = hourlyDto?.temperature2m.orEmpty()
    val hourlyCodes = hourlyDto?.weatherCode.orEmpty()
    val hourlyPrecip = hourlyDto?.precipitationProbability.orEmpty()
    val hourlyIsDay = hourlyDto?.isDay.orEmpty()
    val hourlyWindSpeed = hourlyDto?.windSpeed10m.orEmpty()
    val hourlyWindDir = hourlyDto?.windDirection10m.orEmpty()
    val hourlyWindGusts = hourlyDto?.windGusts10m.orEmpty()

    val aqiByTime: Map<String, Int?> =
      buildMap {
        val aqTimes = airQuality?.hourly?.time.orEmpty()
        val aqValues = airQuality?.hourly?.usAqi.orEmpty()
        aqTimes.forEachIndexed { idx, timeKey ->
          put(timeKey, aqValues.getOrNull(idx))
        }
      }

    val currentTimeStr = current.time.orEmpty()
    val (_, currentMinute) = parseHourMinute(currentTimeStr)
    val currentHourIdx =
      hourlyTimes.indexOfLast { it <= currentTimeStr }.let { idx -> if (idx >= 0) idx else 0 }
    val hourStepFromCurrent = if (currentMinute >= 30) 2 else 1
    val startIdx = (currentHourIdx + hourStepFromCurrent).coerceAtMost((hourlyTimes.size - 1).coerceAtLeast(0))

    // Pick 12 slots every 2 hours starting ~1 hour from now (+1h .. +23h)
    val hourlySlots = buildList {
      var offset = 0
      while (size < 12 && (startIdx + offset) < hourlyTimes.size) {
        val i = startIdx + offset
        val timeStr = hourlyTimes[i]
        val (hour24, minute) = parseHourMinute(timeStr)
        val temp = hourlyTemps.getOrNull(i)?.roundToInt() ?: current.temperature2m?.roundToInt() ?: 0
        val dayFlag = (hourlyIsDay.getOrNull(i) ?: 1) == 1
        val code = hourlyCodes.getOrNull(i) ?: 0
        val precip = hourlyPrecip.getOrNull(i) ?: 0
        add(
          HourlySlot(
            hour24 = hour24,
            minute = minute,
            temperature = temp,
            condition = WeatherCondition.fromWmoCode(code, dayFlag),
            precipProbability = precip.coerceIn(0, 100),
            isDay = dayFlag,
            isNow = false,
          )
        )
        offset += 2
      }
    }

    val windSpeedNow = current.windSpeed10m?.roundToInt() ?: 0
    val windGustsNow = current.windGusts10m?.roundToInt() ?: 0
    val startIdx12h = (currentHourIdx + 1).coerceAtMost((hourlyTimes.size - 1).coerceAtLeast(0))
    val next12Hours = buildList {
      var offset = 0
      while (size < 12 && (startIdx12h + offset) < hourlyTimes.size) {
        val i = startIdx12h + offset
        val timeStr = hourlyTimes[i]
        val (hour24, minute) = parseHourMinute(timeStr)
        val precip = hourlyPrecip.getOrNull(i) ?: 0
        val wSpeed = hourlyWindSpeed.getOrNull(i)?.roundToInt() ?: windSpeedNow
        val wGusts = hourlyWindGusts.getOrNull(i)?.roundToInt() ?: windGustsNow
        val wDir = hourlyWindDir.getOrNull(i) ?: 0
        add(
          HourlyMetricSlot(
            hour24 = hour24,
            minute = minute,
            precipProbability = precip.coerceIn(0, 100),
            windSpeed = wSpeed.coerceAtLeast(0),
            windGusts = wGusts.coerceAtLeast(0),
            windDirectionDeg = wDir,
            usAqi = aqiByTime[timeStr],
          )
        )
        offset += 1
      }
    }

    val dailyDto = response.daily
    val dailyTimes = dailyDto?.time.orEmpty()
    val dailyCodes = dailyDto?.weatherCode.orEmpty()
    val dailyMax = dailyDto?.temperature2mMax.orEmpty()
    val dailyMin = dailyDto?.temperature2mMin.orEmpty()
    val dailyPrecip = dailyDto?.precipitationProbabilityMax.orEmpty()

    val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())

    val todayHigh = dailyMax.firstOrNull()?.roundToInt() ?: current.temperature2m?.roundToInt() ?: 0
    val todayLow = dailyMin.firstOrNull()?.roundToInt() ?: current.temperature2m?.roundToInt() ?: 0

    // 7-day forecast starting from tomorrow (index 1..7)
    val dailyForecasts =
      (1 until dailyTimes.size).take(7).map { index ->
        val dateStr = dailyTimes[index]
        val dayLabel =
          runCatching {
            val parsed = dateParser.parse(dateStr)
            if (parsed != null) dayFormatter.format(parsed) else dateStr
          }.getOrDefault(dateStr)
        val code = dailyCodes.getOrNull(index) ?: 0
        DailyForecast(
          dayLabel = dayLabel,
          condition = WeatherCondition.fromWmoCode(code, isDay = true),
          highTemp = dailyMax.getOrNull(index)?.roundToInt() ?: 0,
          lowTemp = dailyMin.getOrNull(index)?.roundToInt() ?: 0,
          precipProbabilityMax = (dailyPrecip.getOrNull(index) ?: 0).coerceIn(0, 100),
        )
      }

    val windSpeed = current.windSpeed10m?.roundToInt() ?: 0
    val windGusts = current.windGusts10m?.roundToInt() ?: 0
    val nearTermCodes =
      listOf(currentCode) +
        (0 until 12).mapNotNull { delta -> hourlyCodes.getOrNull(currentHourIdx + delta) }

    val resolvedAlerts =
      if (!settings.showSevereWeatherAlerts) {
        emptyList()
      } else if (nwsAlerts.isNotEmpty()) {
        nwsAlerts
      } else {
        deriveMeteorologicalAdvisories(
          cityName = settings.cityName,
          wmoCodes = nearTermCodes,
          windGusts = windGusts,
          windUnit = windUnit,
        )
      }

    val todaySunrise = parseOptionalHourMinute(dailyDto?.sunrise?.firstOrNull())
    val todaySunset = parseOptionalHourMinute(dailyDto?.sunset?.firstOrNull())

    return WeatherSnapshot(
      locationName = settings.cityName,
      latitude = settings.latitude,
      longitude = settings.longitude,
      currentTemp = current.temperature2m?.roundToInt() ?: 0,
      feelsLikeTemp = current.apparentTemperature?.roundToInt() ?: current.temperature2m?.roundToInt() ?: 0,
      todayHighTemp = todayHigh,
      todayLowTemp = todayLow,
      humidityPercent = current.relativeHumidity2m ?: 0,
      windSpeed = windSpeed,
      windGusts = windGusts,
      windUnitLabel = windUnit.displayLabel,
      tempUnit = settings.temperatureUnit,
      currentCondition = currentCondition,
      hourlySlots = hourlySlots,
      next12Hours = next12Hours,
      dailyForecasts = dailyForecasts,
      alerts = resolvedAlerts,
      sunriseHour24 = todaySunrise?.first,
      sunriseMinute = todaySunrise?.second,
      sunsetHour24 = todaySunset?.first,
      sunsetMinute = todaySunset?.second,
      fetchedAtEpochMs = System.currentTimeMillis(),
      isCached = false,
    )
  }

  private fun parseOptionalHourMinute(isoTime: String?): Pair<Int, Int>? {
    if (isoTime.isNullOrBlank()) return null
    val tIndex = isoTime.indexOf('T')
    if (tIndex < 0 || tIndex + 5 >= isoTime.length) return null
    val hh = isoTime.substring(tIndex + 1, tIndex + 3).toIntOrNull() ?: return null
    val mm = isoTime.substring(tIndex + 4, tIndex + 6).toIntOrNull() ?: return null
    if (hh !in 0..23 || mm !in 0..59) return null
    return hh to mm
  }

  private fun parseHourMinute(isoTime: String): Pair<Int, Int> {
    val tIndex = isoTime.indexOf('T')
    if (tIndex < 0 || tIndex + 5 >= isoTime.length) return 12 to 0
    val hh = isoTime.substring(tIndex + 1, tIndex + 3).toIntOrNull() ?: 12
    val mm = isoTime.substring(tIndex + 4, tIndex + 6).toIntOrNull() ?: 0
    return hh to mm
  }

  companion object {
    @Volatile private var instance: WeatherRepository? = null

    fun getInstance(context: Context): WeatherRepository =
      instance ?: synchronized(this) {
        instance ?: WeatherRepository(
          UserPreferencesRepository(context.applicationContext, NetworkModule.moshi)
        ).also { instance = it }
      }
  }
}

fun deriveMeteorologicalAdvisories(
  cityName: String,
  wmoCodes: List<Int>,
  windGusts: Int,
  windUnit: WindSpeedUnit,
): List<WeatherAlert> {
  val advisories = mutableListOf<WeatherAlert>()
  val hasHailStorm = wmoCodes.any { it == 96 || it == 99 }
  val hasStorm = wmoCodes.any { it == 95 }
  val hasFreezing = wmoCodes.any { it == 56 || it == 57 || it == 66 || it == 67 }
  val hasHeavySnow = wmoCodes.any { it == 75 || it == 86 }
  val hasHeavyRain = wmoCodes.any { it == 65 || it == 82 }
  val highGustThreshold = if (windUnit == WindSpeedUnit.MPH) 45 else 72

  when {
    hasHailStorm ->
      advisories += WeatherAlert(
        event = "Severe Thunderstorm Warning",
        severity = "Severe",
        headline = "Severe thunderstorm with hail forecast for $cityName in the next 12 hours",
      )
    hasStorm ->
      advisories += WeatherAlert(
        event = "Thunderstorm Advisory",
        severity = "Moderate",
        headline = "Thunderstorm activity forecast for $cityName in the next 12 hours",
      )
  }

  if (hasFreezing) {
    advisories += WeatherAlert(
      event = "Freezing Rain Advisory",
      severity = "Severe",
      headline = "Freezing rain or icy conditions forecast for $cityName",
    )
  }

  if (hasHeavySnow) {
    advisories += WeatherAlert(
      event = "Heavy Snow Advisory",
      severity = "Moderate",
      headline = "Heavy snow expected in $cityName over the next 12 hours",
    )
  } else if (hasHeavyRain) {
    advisories += WeatherAlert(
      event = "Heavy Rain Advisory",
      severity = "Moderate",
      headline = "Heavy downpours forecast in $cityName over the next 12 hours",
    )
  }

  if (windGusts >= highGustThreshold) {
    advisories += WeatherAlert(
      event = "High Wind Advisory",
      severity = "Moderate",
      headline = "Strong wind gusts up to $windGusts ${windUnit.displayLabel} reported in $cityName",
    )
  }

  return advisories.take(3)
}
