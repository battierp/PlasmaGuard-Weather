package com.example.data.remote

import com.example.data.model.GeoJsResponseDto
import com.example.data.model.GeocodingResponseDto
import com.example.data.model.NwsAlertsResponseDto
import com.example.data.model.OpenMeteoForecastResponse
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface OpenMeteoForecastApi {
  @GET("v1/forecast")
  suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current")
    current: String =
      "temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m,wind_gusts_10m",
    @Query("hourly")
    hourly: String = "temperature_2m,weather_code,precipitation_probability,is_day",
    @Query("daily")
    daily: String =
      "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
    @Query("temperature_unit") temperatureUnit: String,
    @Query("wind_speed_unit") windSpeedUnit: String,
    @Query("timezone") timezone: String = "auto",
    @Query("forecast_days") forecastDays: Int = 8,
  ): OpenMeteoForecastResponse
}

interface OpenMeteoGeocodingApi {
  @GET("v1/search")
  suspend fun searchCity(
    @Query("name") query: String,
    @Query("count") count: Int = 8,
    @Query("language") language: String = "en",
    @Query("format") format: String = "json",
  ): GeocodingResponseDto
}

interface IpGeoApi {
  @GET("v1/ip/geo.json")
  suspend fun detectLocation(): GeoJsResponseDto
}

interface NwsAlertApi {
  @Headers(
    "Accept: application/geo+json",
    "User-Agent: PlasmaSky/1.0 (Weather Screensaver)",
  )
  @GET("alerts/active")
  suspend fun getActiveAlerts(@Query("point") point: String): NwsAlertsResponseDto
}

object NetworkModule {
  val moshi: Moshi by lazy { Moshi.Builder().build() }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()
  }

  val forecastApi: OpenMeteoForecastApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoForecastApi::class.java)
  }

  val geocodingApi: OpenMeteoGeocodingApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://geocoding-api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoGeocodingApi::class.java)
  }

  val ipGeoApi: IpGeoApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://get.geojs.io/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(IpGeoApi::class.java)
  }

  val nwsAlertApi: NwsAlertApi by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.weather.gov/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(NwsAlertApi::class.java)
  }
}
