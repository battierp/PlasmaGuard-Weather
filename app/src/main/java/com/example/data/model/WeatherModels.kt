package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.Locale
import kotlin.math.abs

enum class WeatherCondition(val label: String, val shortLabel: String) {
  SUNNY("Sunny", "Sunny"),
  CLEAR_NIGHT("Clear Night", "Clear"),
  PARTLY_CLOUDY("Partly Cloudy", "P. Cloudy"),
  CLOUDY("Cloudy", "Cloudy"),
  FOGGY("Foggy", "Foggy"),
  DRIZZLE("Light Drizzle", "Drizzle"),
  RAINY("Rainy", "Rainy"),
  SNOWY("Snowy", "Snowy"),
  STORMY("Thunderstorm", "Stormy");

  companion object {
    fun fromWmoCode(code: Int, isDay: Boolean): WeatherCondition =
      when (code) {
        0 -> if (isDay) SUNNY else CLEAR_NIGHT
        1, 2 -> PARTLY_CLOUDY
        3 -> CLOUDY
        45, 48 -> FOGGY
        51, 53, 55, 56, 57 -> DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> RAINY
        71, 73, 75, 77, 85, 86 -> SNOWY
        95, 96, 99 -> STORMY
        else -> CLOUDY
      }
  }
}

enum class TemperatureUnit(val symbol: String, val apiParam: String) {
  FAHRENHEIT("°F", "fahrenheit"),
  CELSIUS("°C", "celsius"),
}

enum class WindSpeedUnit(
  val label: String,
  val apiParam: String,
  val displayLabel: String,
) {
  MPH("mph", "mph", "mph"),
  KPH("kph", "kmh", "kph"),
}

enum class DriftIntensity(
  val label: String,
  val maxOffsetDp: Float,
  val speedMultiplier: Float,
) {
  SLOWEST("Slowest", 8f, 1.0f),
  SUBTLE("Slow", 10f, 2.0f),
  BALANCED("Medium", 16f, 3.0f),
  WIDE("Fast", 24f, 4.0f),
  FASTEST("Fastest", 24f, 5.0f),
}

enum class DriftMotionMode(val label: String) {
  CLOCKWISE("Clockwise"),
  DVD_BOUNCE("DVD Bounce"),
}

enum class TextColorOption(val label: String) {
  WHITE("White"),
  RAINBOW("Rainbow Timer"),
  RAINBOW_BOUNCE("Rainbow Bounce"),
  ELECTRIC_CYAN("Electric Cyan"),
  MINT("Mint"),
  SOLAR_GOLD("Solar Gold"),
  CORAL("Coral"),
  VIOLET("Violet"),
  COBALT("Cobalt"),
}

@JsonClass(generateAdapter = true)
data class SavedLocation(
  val name: String,
  val admin1: String? = null,
  val country: String? = null,
  val latitude: Double,
  val longitude: Double,
) {
  val shortLabel: String
    get() = name.substringBefore(",").trim()

  val displayLabel: String
    get() = listOfNotNull(shortLabel, admin1?.takeIf { it.isNotBlank() })
      .distinct()
      .joinToString(", ")

  fun matches(lat: Double, lon: Double): Boolean =
    abs(latitude - lat) < 0.01 && abs(longitude - lon) < 0.01
}

@JsonClass(generateAdapter = true)
data class WeatherAlert(
  val event: String,
  val severity: String,
  val headline: String,
)

@JsonClass(generateAdapter = true)
data class HourlySlot(
  val hour24: Int,
  val minute: Int,
  val temperature: Int,
  val condition: WeatherCondition,
  val precipProbability: Int,
  val isDay: Boolean,
  val isNow: Boolean,
)

@JsonClass(generateAdapter = true)
data class HourlyMetricSlot(
  val hour24: Int,
  val minute: Int,
  val precipProbability: Int,
  val windSpeed: Int,
  val windGusts: Int = 0,
  val windDirectionDeg: Int = 0,
  val usAqi: Int? = null,
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
  val dayLabel: String,
  val condition: WeatherCondition,
  val highTemp: Int,
  val lowTemp: Int,
  val precipProbabilityMax: Int,
)

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
  val locationName: String,
  val latitude: Double,
  val longitude: Double,
  val currentTemp: Int,
  val feelsLikeTemp: Int,
  val todayHighTemp: Int,
  val todayLowTemp: Int,
  val humidityPercent: Int,
  val windSpeed: Int,
  val windGusts: Int = 0,
  val windUnitLabel: String,
  val tempUnit: TemperatureUnit,
  val currentCondition: WeatherCondition,
  val hourlySlots: List<HourlySlot>,
  val next12Hours: List<HourlyMetricSlot> = emptyList(),
  val dailyForecasts: List<DailyForecast>,
  val alerts: List<WeatherAlert> = emptyList(),
  val sunriseHour24: Int? = null,
  val sunriseMinute: Int? = null,
  val sunsetHour24: Int? = null,
  val sunsetMinute: Int? = null,
  val fetchedAtEpochMs: Long,
  val isCached: Boolean = false,
)

data class CitySearchResult(
  val name: String,
  val admin1: String?,
  val country: String?,
  val latitude: Double,
  val longitude: Double,
) {
  val displayName: String
    get() = listOfNotNull(name, admin1?.takeIf { it.isNotBlank() }, country?.takeIf { it.isNotBlank() })
      .distinct()
      .joinToString(", ")
}

fun degreesToCompass(degrees: Int): String {
  val normalized = ((degrees % 360) + 360) % 360
  val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
  return directions[((normalized + 22) / 45) % 8]
}

fun precipCategoryLabel(percent: Int): String =
  when {
    percent >= 75 -> "High"
    percent >= 50 -> "Likely"
    percent >= 25 -> "Chance"
    percent >= 10 -> "Slight"
    else -> "Dry"
  }

fun aqiCategoryLabel(usAqi: Int?): String =
  when {
    usAqi == null -> "N/A"
    usAqi <= 50 -> "Good"
    usAqi <= 100 -> "Mod"
    usAqi <= 150 -> "Sens"
    usAqi <= 200 -> "Poor"
    usAqi <= 300 -> "VPoor"
    else -> "Haz"
  }

fun formatSunTimeLabel(hour24: Int?, minute: Int?, use24Hour: Boolean): String {
  if (hour24 == null || minute == null) return "--:--"
  return if (use24Hour) {
    String.format(Locale.getDefault(), "%02d:%02d", hour24, minute)
  } else {
    val h12 =
      when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
      }
    val amPm = if (hour24 < 12) "AM" else "PM"
    String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm)
  }
}

fun normalizeCityAutocompleteQuery(raw: String): String? {
  val trimmed = raw.trim()
  return if (trimmed.length >= 2) trimmed else null
}
