package com.example

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
import com.example.data.model.formatSunTimeLabel
import com.example.data.model.normalizeCityAutocompleteQuery
import com.example.data.model.precipCategoryLabel
import com.example.data.repository.deriveMeteorologicalAdvisories
import com.example.ui.components.HourlyCubeFace
import com.example.ui.components.computeClockwiseEdgeIndex
import com.example.ui.components.computeClockwisePerimeterOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
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
  fun `text color and motion mode options cover decoupled clockwise and dvd bounce`() {
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
    assertEquals("Medium", UserSettings().driftIntensity.label)
  }

  @Test
  fun `saved locations cap at 5 and preserve active entry when replacing`() {
    val c1 = SavedLocation("Seattle", "WA", "US", 47.6062, -122.3321)
    val c2 = SavedLocation("Portland", "OR", "US", 45.5152, -122.6784)
    val c3 = SavedLocation("Denver", "CO", "US", 39.7392, -104.9903)
    val c4 = SavedLocation("Chicago", "IL", "US", 41.8781, -87.6298)
    val c5 = SavedLocation("Austin", "TX", "US", 30.2672, -97.7431)
    val c6 = SavedLocation("Boston", "MA", "US", 42.3601, -71.0589)

    val updated =
      upsertSavedLocationList(
        current = listOf(c1, c2, c3, c4, c5),
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
  }

  @Test
  fun `clockwise perimeter drift hugs edges and reports edge index transitions`() {
    val maxX = 20.0
    val maxY = 10.0
    val topRight = computeClockwisePerimeterOffset(0.0, maxX, maxY)
    val bottomRight = computeClockwisePerimeterOffset(20.0 / 120.0, maxX, maxY)
    val bottomLeft = computeClockwisePerimeterOffset(60.0 / 120.0, maxX, maxY)
    val topLeft = computeClockwisePerimeterOffset(80.0 / 120.0, maxX, maxY)

    assertEquals(20.0, topRight.first, 0.0001)
    assertEquals(-10.0, topRight.second, 0.0001)
    assertEquals(20.0, bottomRight.first, 0.0001)
    assertEquals(10.0, bottomRight.second, 0.0001)
    assertEquals(-20.0, bottomLeft.first, 0.0001)
    assertEquals(10.0, bottomLeft.second, 0.0001)
    assertEquals(-20.0, topLeft.first, 0.0001)
    assertEquals(-10.0, topLeft.second, 0.0001)

    assertEquals(0, computeClockwiseEdgeIndex(0.0, maxX, maxY))
    assertEquals(1, computeClockwiseEdgeIndex(25.0 / 120.0, maxX, maxY))
    assertEquals(2, computeClockwiseEdgeIndex(65.0 / 120.0, maxX, maxY))
    assertEquals(3, computeClockwiseEdgeIndex(85.0 / 120.0, maxX, maxY))
  }

  @Test
  fun `rotating cube cycles four forecast modes and maps labels`() {
    assertEquals(
      listOf(
        "24-HOUR FORECAST",
        "12-HOUR PRECIPITATION",
        "12-HOUR WIND SPEED",
        "12-HOUR AIR QUALITY",
      ),
      HourlyCubeFace.entries.map { it.title },
    )
    assertEquals("SW", degreesToCompass(225))
    assertEquals("Chance", precipCategoryLabel(30))
    assertEquals("Poor", aqiCategoryLabel(165))
    assertEquals("N/A", aqiCategoryLabel(null))
  }

  @Test
  fun `clockwise scales to midpoint 90 percent between full size and 80 percent dvd bounce`() {
    val fullScale = 1.00f
    val dvdScale = com.example.ui.components.motionModeContentScale(DriftMotionMode.DVD_BOUNCE)
    val clockwiseScale = com.example.ui.components.motionModeContentScale(DriftMotionMode.CLOCKWISE)

    assertEquals(0.80f, dvdScale, 0.0001f)
    assertEquals((fullScale + dvdScale) / 2f, clockwiseScale, 0.0001f)
    assertEquals(0.90f, clockwiseScale, 0.0001f)
  }

  @Test
  fun `units default to fahrenheit, mph, 12-hour and bottom bar hides only when all 4 items are off`() {
    val defaults = UserSettings()
    assertEquals(com.example.data.model.TemperatureUnit.FAHRENHEIT, defaults.temperatureUnit)
    assertEquals(WindSpeedUnit.MPH, defaults.windSpeedUnit)
    assertFalse(defaults.use24HourClock)
    assertTrue(defaults.showBottomBar)
    assertEquals(
      HourlyCubeFace.entries,
      com.example.ui.components.resolveEnabledBottomBarFaces(
        showTemp24h = defaults.showBottomBarTemp24h,
        showPrecipitation = defaults.showBottomBarPrecipitation,
        showWind = defaults.showBottomBarWind,
        showAirQuality = defaults.showBottomBarAirQuality,
      ),
    )

    val windOnly =
      defaults.copy(
        showBottomBarTemp24h = false,
        showBottomBarPrecipitation = false,
        showBottomBarAirQuality = false,
      )
    assertTrue(windOnly.showBottomBar)
    assertEquals(
      listOf(HourlyCubeFace.WIND_12H),
      com.example.ui.components.resolveEnabledBottomBarFaces(
        showTemp24h = windOnly.showBottomBarTemp24h,
        showPrecipitation = windOnly.showBottomBarPrecipitation,
        showWind = windOnly.showBottomBarWind,
        showAirQuality = windOnly.showBottomBarAirQuality,
      ),
    )

    val allOff = windOnly.copy(showBottomBarWind = false)
    assertFalse(allOff.showBottomBar)
    assertTrue(
      com.example.ui.components.resolveEnabledBottomBarFaces(
        showTemp24h = allOff.showBottomBarTemp24h,
        showPrecipitation = allOff.showBottomBarPrecipitation,
        showWind = allOff.showBottomBarWind,
        showAirQuality = allOff.showBottomBarAirQuality,
      ).isEmpty(),
    )
  }

  @Test
  fun `sunrise and sunset times format for 12-hour, 24-hour, and missing data`() {
    assertEquals("6:45 AM", formatSunTimeLabel(6, 45, use24Hour = false))
    assertEquals("06:45", formatSunTimeLabel(6, 45, use24Hour = true))
    assertEquals("5:12 PM", formatSunTimeLabel(17, 12, use24Hour = false))
    assertEquals("17:12", formatSunTimeLabel(17, 12, use24Hour = true))
    assertEquals("12:05 AM", formatSunTimeLabel(0, 5, use24Hour = false))
    assertEquals("12:05 PM", formatSunTimeLabel(12, 5, use24Hour = false))
    assertEquals("--:--", formatSunTimeLabel(null, null, use24Hour = false))
  }

  @Test
  fun `autocomplete normalizes query and requires at least 2 non-whitespace characters`() {
    assertEquals(null, normalizeCityAutocompleteQuery(""))
    assertEquals(null, normalizeCityAutocompleteQuery(" "))
    assertEquals(null, normalizeCityAutocompleteQuery("a"))
    assertEquals(null, normalizeCityAutocompleteQuery(" a "))
    assertEquals("se", normalizeCityAutocompleteQuery(" se "))
    assertEquals("Seattle", normalizeCityAutocompleteQuery("  Seattle  "))
  }
}
