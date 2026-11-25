package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MAX_SAVED_LOCATIONS
import com.example.data.local.UserSettings
import com.example.data.local.upsertSavedLocationList
import com.example.data.model.DriftIntensity
import com.example.data.model.DriftMotionMode
import com.example.data.model.SavedLocation
import com.example.data.model.TextColorOption
import com.example.data.model.WeatherCondition
import com.example.data.model.WindSpeedUnit
import com.example.data.model.aqiCategoryLabel
import com.example.data.model.degreesToCompass
import com.example.data.model.precipCategoryLabel
import com.example.data.repository.deriveMeteorologicalAdvisories
import com.example.ui.components.HourlyCubeFace
import com.example.ui.components.computeClockwiseEdgeIndex
import com.example.ui.components.computeClockwisePerimeterOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PlasmaSky", appName)
  }

  @Test
  fun `wmo codes map to expected weather conditions`() {
    assertEquals(WeatherCondition.SUNNY, WeatherCondition.fromWmoCode(0, isDay = true))
    assertEquals(WeatherCondition.CLEAR_NIGHT, WeatherCondition.fromWmoCode(0, isDay = false))
    assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCondition.fromWmoCode(2, isDay = true))
    assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(3, isDay = true))
    assertEquals(WeatherCondition.RAINY, WeatherCondition.fromWmoCode(63, isDay = true))
    assertEquals(WeatherCondition.SNOWY, WeatherCondition.fromWmoCode(73, isDay = false))
    assertEquals(WeatherCondition.STORMY, WeatherCondition.fromWmoCode(95, isDay = true))
  }

  @Test
  fun `text color options cover white default, rainbow modes, and six bright colors`() {
    assertEquals(
      listOf(
        "White",
        "Rainbow Timer",
        "Rainbow Bounce",
        "Electric Cyan",
        "Mint",
        "Solar Gold",
        "Coral",
        "Violet",
        "Cobalt",
      ),
      TextColorOption.entries.map { it.label },
    )
    assertEquals(TextColorOption.WHITE, UserSettings().textColorOption)
    assertEquals(DriftMotionMode.CLOCKWISE, UserSettings().driftMotionMode)
    assertEquals(listOf("Clockwise", "DVD Bounce"), DriftMotionMode.entries.map { it.label })
  }

  @Test
  fun `drift intensity exposes slowest through fastest at 4 to 20 dp per sec with medium default`() {
    assertEquals(
      listOf("Slowest", "Slow", "Medium", "Fast", "Fastest"),
      DriftIntensity.entries.map { it.label },
    )
    assertEquals(
      listOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f),
      DriftIntensity.entries.map { it.speedMultiplier },
    )
    assertEquals(DriftIntensity.BALANCED, UserSettings().driftIntensity)
  }

  @Test
  fun `user settings defaults include fahrenheit, wind mph, 12h clock, forecast panels, alerts, and auto rotate off`() {
    val defaults = UserSettings()
    assertEquals(com.example.data.model.TemperatureUnit.FAHRENHEIT, defaults.temperatureUnit)
    assertEquals(WindSpeedUnit.MPH, defaults.windSpeedUnit)
    assertFalse(defaults.use24HourClock)
    assertEquals(listOf("mph", "kph"), WindSpeedUnit.entries.map { it.label })
    assertTrue(defaults.show7DayForecast)
    assertTrue(defaults.showBottomBar)
    assertTrue(defaults.showBottomBarTemp24h)
    assertTrue(defaults.showBottomBarWind)
    assertTrue(defaults.showBottomBarAirQuality)
    assertTrue(defaults.showBottomBarPrecipitation)
    assertTrue(defaults.showSevereWeatherAlerts)
    assertFalse(defaults.autoRotateLocations)
  }

  @Test
  fun `saved locations cap at 5 and preserve active entry when replacing`() {
    val c1 = SavedLocation("Seattle", "WA", "US", 47.6062, -122.3321)
    val c2 = SavedLocation("Portland", "OR", "US", 45.5152, -122.6784)
    val c3 = SavedLocation("Denver", "CO", "US", 39.7392, -104.9903)
    val c4 = SavedLocation("Chicago", "IL", "US", 41.8781, -87.6298)
    val c5 = SavedLocation("Austin", "TX", "US", 30.2672, -97.7431)
    val c6 = SavedLocation("Boston", "MA", "US", 42.3601, -71.0589)

    val five = listOf(c1, c2, c3, c4, c5)
    val updated =
      upsertSavedLocationList(
        current = five,
        incoming = c6,
        activeLat = c1.latitude,
        activeLon = c1.longitude,
      )

    assertEquals(MAX_SAVED_LOCATIONS, updated.size)
    assertTrue(updated.any { it.name == "Seattle" })
    assertTrue(updated.any { it.name == "Boston" })
    assertFalse(updated.any { it.name == "Portland" })
  }

  @Test
  fun `meteorological advisories trigger only for genuine severe codes or high wind gusts`() {
    val calm =
      deriveMeteorologicalAdvisories(
        cityName = "Seattle",
        wmoCodes = listOf(0, 1, 2, 3),
        windGusts = 14,
        windUnit = WindSpeedUnit.MPH,
      )
    assertTrue(calm.isEmpty())

    val severe =
      deriveMeteorologicalAdvisories(
        cityName = "Denver",
        wmoCodes = listOf(96, 65),
        windGusts = 55,
        windUnit = WindSpeedUnit.MPH,
      )
    assertEquals(3, severe.size)
    assertEquals("Severe Thunderstorm Warning", severe[0].event)
    assertEquals("Heavy Rain Advisory", severe[1].event)
    assertEquals("High Wind Advisory", severe[2].event)
  }

  @Test
  fun `clockwise perimeter drift hugs edges from top-right to bottom-right to bottom-left to top-left`() {
    val maxX = 20.0
    val maxY = 10.0
    // Perimeter = 2*(40 + 20) = 120:
    // 0.0..20.0 (0..1/6): right edge (+20, -10) -> (+20, +10)
    // 20..60 (1/6..3/6): bottom edge (+20, +10) -> (-20, +10)
    // 60..80 (3/6..4/6): left edge (-20, +10) -> (-20, -10)
    // 80..120 (4/6..1.0): top edge (-20, -10) -> (+20, -10)
    val topRight = computeClockwisePerimeterOffset(0.0, maxX, maxY)
    val bottomRight = computeClockwisePerimeterOffset(20.0 / 120.0, maxX, maxY)
    val bottomLeft = computeClockwisePerimeterOffset(60.0 / 120.0, maxX, maxY)
    val topLeft = computeClockwisePerimeterOffset(80.0 / 120.0, maxX, maxY)
    val wrappedTopRight = computeClockwisePerimeterOffset(1.0, maxX, maxY)

    assertEquals(20.0, topRight.first, 0.0001)
    assertEquals(-10.0, topRight.second, 0.0001)

    assertEquals(20.0, bottomRight.first, 0.0001)
    assertEquals(10.0, bottomRight.second, 0.0001)

    assertEquals(-20.0, bottomLeft.first, 0.0001)
    assertEquals(10.0, bottomLeft.second, 0.0001)

    assertEquals(-20.0, topLeft.first, 0.0001)
    assertEquals(-10.0, topLeft.second, 0.0001)

    assertEquals(20.0, wrappedTopRight.first, 0.0001)
    assertEquals(-10.0, wrappedTopRight.second, 0.0001)

    assertEquals(0, computeClockwiseEdgeIndex(0.0, maxX, maxY))
    assertEquals(1, computeClockwiseEdgeIndex(25.0 / 120.0, maxX, maxY))
    assertEquals(2, computeClockwiseEdgeIndex(65.0 / 120.0, maxX, maxY))
    assertEquals(3, computeClockwiseEdgeIndex(85.0 / 120.0, maxX, maxY))
  }

  @Test
  fun `rotating cube cycles four forecast modes and maps wind compass, precip, and aqi labels`() {
    assertEquals(
      listOf(
        "24-HOUR FORECAST",
        "12-HOUR PRECIPITATION",
        "12-HOUR WIND SPEED",
        "12-HOUR AIR QUALITY",
      ),
      HourlyCubeFace.entries.map { it.title },
    )

    assertEquals("N", degreesToCompass(0))
    assertEquals("NE", degreesToCompass(45))
    assertEquals("SW", degreesToCompass(225))
    assertEquals("N", degreesToCompass(360))

    assertEquals("Dry", precipCategoryLabel(5))
    assertEquals("Chance", precipCategoryLabel(30))
    assertEquals("High", precipCategoryLabel(85))

    assertEquals("Good", aqiCategoryLabel(28))
    assertEquals("Mod", aqiCategoryLabel(75))
    assertEquals("Poor", aqiCategoryLabel(165))
    assertEquals("N/A", aqiCategoryLabel(null))
  }
}
