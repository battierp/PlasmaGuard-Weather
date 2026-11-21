package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
  @Json(name = "current") val current: CurrentWeatherDto?,
  @Json(name = "hourly") val hourly: HourlyWeatherDto?,
  @Json(name = "daily") val daily: DailyWeatherDto?,
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
  @Json(name = "time") val time: String?,
  @Json(name = "temperature_2m") val temperature2m: Double?,
  @Json(name = "apparent_temperature") val apparentTemperature: Double?,
  @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int?,
  @Json(name = "is_day") val isDay: Int?,
  @Json(name = "weather_code") val weatherCode: Int?,
  @Json(name = "wind_speed_10m") val windSpeed10m: Double?,
  @Json(name = "wind_gusts_10m") val windGusts10m: Double? = null,
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
  @Json(name = "time") val time: List<String>?,
  @Json(name = "temperature_2m") val temperature2m: List<Double>?,
  @Json(name = "weather_code") val weatherCode: List<Int>?,
  @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>?,
  @Json(name = "is_day") val isDay: List<Int>?,
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
  @Json(name = "time") val time: List<String>?,
  @Json(name = "weather_code") val weatherCode: List<Int>?,
  @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>?,
  @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>?,
  @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?>?,
)

@JsonClass(generateAdapter = true)
data class GeocodingResponseDto(
  @Json(name = "results") val results: List<GeocodingResultDto>?,
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
  @Json(name = "name") val name: String?,
  @Json(name = "admin1") val admin1: String?,
  @Json(name = "country") val country: String?,
  @Json(name = "latitude") val latitude: Double?,
  @Json(name = "longitude") val longitude: Double?,
)

@JsonClass(generateAdapter = true)
data class GeoJsResponseDto(
  @Json(name = "city") val city: String?,
  @Json(name = "region") val region: String?,
  @Json(name = "country") val country: String?,
  @Json(name = "latitude") val latitude: String?,
  @Json(name = "longitude") val longitude: String?,
)

@JsonClass(generateAdapter = true)
data class NwsAlertsResponseDto(
  @Json(name = "features") val features: List<NwsAlertFeatureDto>?,
)

@JsonClass(generateAdapter = true)
data class NwsAlertFeatureDto(
  @Json(name = "properties") val properties: NwsAlertPropertiesDto?,
)

@JsonClass(generateAdapter = true)
data class NwsAlertPropertiesDto(
  @Json(name = "event") val event: String?,
  @Json(name = "severity") val severity: String?,
  @Json(name = "headline") val headline: String?,
  @Json(name = "status") val status: String?,
)
