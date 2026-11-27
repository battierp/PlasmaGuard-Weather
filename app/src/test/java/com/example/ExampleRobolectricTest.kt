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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyForecast
import com.example.data.model.HourlyMetricSlot
import com.example.data.model.HourlySlot
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherAlert
import com.example.data.model.WeatherSnapshot
import com.example.ui.ClockDisplay
import com.example.ui.components.PlasmaColors
import com.example.ui.components.PlasmaShieldState
import com.example.ui.screens.ScreensaverWeatherCanvas
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.PlasmaSkyTheme
import com.example.ui.theme.SeafoamTeal
import com.example.ui.theme.SoftFrostText
import org.junit.Rule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

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

  @Test
  fun `weather alert banner stays pinned at top in full dashboard mode while plasma drift moves panels`() {
    val offsetX = mutableFloatStateOf(0f)
    val offsetY = mutableFloatStateOf(0f)
    val pulse = mutableFloatStateOf(1f)
    val plasma =
      PlasmaShieldState(
        initialColors =
          PlasmaColors(
            primaryTextColor = SoftFrostText,
            secondaryTextColor = MutedSlateText,
            primaryAccent = AuroraCyan,
            secondaryAccent = SeafoamTeal,
          ),
        offsetXDpFloatState = offsetX,
        offsetYDpFloatState = offsetY,
        pulseState = pulse,
      )
    val snapshot =
      WeatherSnapshot(
        locationName = "Denver",
        latitude = 39.7392,
        longitude = -104.9903,
        currentTemp = 48,
        feelsLikeTemp = 44,
        todayHighTemp = 54,
        todayLowTemp = 32,
        humidityPercent = 40,
        windSpeed = 12,
        windGusts = 48,
        windUnitLabel = "mph",
        tempUnit = TemperatureUnit.FAHRENHEIT,
        currentCondition = WeatherCondition.PARTLY_CLOUDY,
        hourlySlots =
          (0 until 12).map { i ->
            HourlySlot(
              hour24 = (i * 2) % 24,
              minute = 0,
              temperature = 45 + i,
              condition = WeatherCondition.PARTLY_CLOUDY,
              precipProbability = 10,
              isDay = true,
              isNow = i == 0,
            )
          },
        next12Hours =
          (0 until 12).map { i ->
            HourlyMetricSlot(
              hour24 = i,
              minute = 0,
              precipProbability = 10,
              windSpeed = 12,
              windGusts = 22,
              windDirectionDeg = 180,
              usAqi = 35,
            )
          },
        dailyForecasts =
          listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon").map { day ->
            DailyForecast(day, WeatherCondition.SUNNY, 58, 34, 10)
          },
        alerts =
          listOf(
            WeatherAlert(
              event = "High Wind Advisory",
              severity = "Moderate",
              headline = "Strong wind gusts up to 48 mph expected",
            )
          ),
        sunriseHour24 = 6,
        sunriseMinute = 42,
        sunsetHour24 = 16,
        sunsetMinute = 50,
        fetchedAtEpochMs = 1_000_000L,
      )

    composeTestRule.mainClock.autoAdvance = false
    var bannerTopBefore = -1f
    var bannerTopAfter = -1f
    var tempXBefore = -1f
    var tempXAfter = -1f

    composeTestRule.setContent {
      PlasmaSkyTheme {
        Box(modifier = Modifier.size(960.dp, 540.dp)) {
          ScreensaverWeatherCanvas(
            snapshot = snapshot,
            settings =
              UserSettings(
                show7DayForecast = true,
                showBottomBarTemp24h = true,
                showSevereWeatherAlerts = true,
                driftMotionMode = DriftMotionMode.DVD_BOUNCE,
              ),
            clockDisplay = ClockDisplay("5:12 PM", "Thu, Nov 26"),
            plasma = plasma,
            forceTvWide = true,
            onUpdateDriftBounds = { _, _ -> },
          )
        }
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.waitForIdle()
    val bannerNode =
      composeTestRule.onNode(
        androidx.compose.ui.test.hasTestTag("severe_weather_alert_banner")
      )
    val tempNode =
      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("current_temp_text"))
    bannerTopBefore = bannerNode.fetchSemanticsNode().boundsInRoot.top
    tempXBefore = tempNode.fetchSemanticsNode().boundsInRoot.left

    offsetX.floatValue = 22f
    offsetY.floatValue = 12f
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.waitForIdle()
    bannerTopAfter = bannerNode.fetchSemanticsNode().boundsInRoot.top
    tempXAfter = tempNode.fetchSemanticsNode().boundsInRoot.left

    assertEquals(bannerTopBefore, bannerTopAfter, 0.5f)
    assertTrue("Expected weather panel to drift horizontally", kotlin.math.abs(tempXAfter - tempXBefore) > 5f)

    // Even when drifted upward to maximum negative Y, panels must stay strictly below the pinned alert banner
    val bannerBottom = bannerNode.fetchSemanticsNode().boundsInRoot.bottom
    offsetY.floatValue = -40f
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.waitForIdle()
    val multiDayTop =
      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("multiday_forecast_section"))
        .fetchSemanticsNode()
        .boundsInRoot
        .top
    assertTrue(
      "Expected drifting panels (top=$multiDayTop) to remain below banner (bottom=$bannerBottom)",
      multiDayTop >= bannerBottom - 1f,
    )
  }

  @Test
  fun `weather alert banner stays pinned at top in 7-day-only and bottom-bar-only modes while badge scrolls`() {
    val offsetX = mutableFloatStateOf(0f)
    val offsetY = mutableFloatStateOf(0f)
    val plasma =
      PlasmaShieldState(
        initialColors =
          PlasmaColors(
            primaryTextColor = SoftFrostText,
            secondaryTextColor = SoftFrostText.copy(alpha = 0.8f),
            primaryAccent = AuroraCyan,
            secondaryAccent = AuroraCyan.copy(alpha = 0.8f),
          ),
        offsetXDpFloatState = offsetX,
        offsetYDpFloatState = offsetY,
        pulseState = mutableFloatStateOf(0.5f),
      )

    val snapshot =
      WeatherSnapshot(
        locationName = "Seattle, WA",
        latitude = 47.6062,
        longitude = -122.3321,
        currentTemp = 54,
        feelsLikeTemp = 51,
        todayHighTemp = 58,
        todayLowTemp = 44,
        currentCondition = WeatherCondition.RAINY,
        tempUnit = TemperatureUnit.FAHRENHEIT,
        windSpeed = 12,
        windGusts = 28,
        windUnitLabel = "mph",
        humidityPercent = 78,
        hourlySlots =
          (0 until 12).map { idx ->
            HourlySlot(idx, 0, 50 + idx, WeatherCondition.RAINY, 60, isDay = true, isNow = idx == 0)
          },
        next12Hours =
          (0 until 12).map { idx ->
            HourlyMetricSlot(idx, 0, 60, 12, 28, 180, 28)
          },
        dailyForecasts =
          listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon").map { day ->
            DailyForecast(day, WeatherCondition.RAINY, 58, 44, 60)
          },
        alerts =
          listOf(
            WeatherAlert(
              event = "Wind Advisory",
              severity = "Moderate",
              headline = "Gusts up to 45 mph expected",
            )
          ),
        sunriseHour24 = 7,
        sunriseMinute = 10,
        sunsetHour24 = 16,
        sunsetMinute = 35,
        fetchedAtEpochMs = 1_000_000L,
      )

    val settingsState =
      androidx.compose.runtime.mutableStateOf(
        UserSettings(
          show7DayForecast = true,
          showBottomBarTemp24h = false,
          showBottomBarPrecipitation = false,
          showBottomBarWind = false,
          showBottomBarAirQuality = false,
          showSevereWeatherAlerts = true,
        )
      )

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      PlasmaSkyTheme {
        Box(modifier = Modifier.size(960.dp, 540.dp)) {
          ScreensaverWeatherCanvas(
            snapshot = snapshot,
            settings = settingsState.value,
            clockDisplay = ClockDisplay("5:12 PM", "Thu, Nov 26"),
            plasma = plasma,
            forceTvWide = true,
            onUpdateDriftBounds = { _, _ -> },
          )
        }
      }
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.waitForIdle()
    val bannerNode =
      composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("severe_weather_alert_banner"))
    val bannerTop7DayOnly = bannerNode.fetchSemanticsNode().boundsInRoot.top

    // Switch to Bottom-bar-only mode and drift panels
    settingsState.value =
      settingsState.value.copy(
        show7DayForecast = false,
        showBottomBarTemp24h = true,
      )
    offsetX.floatValue = 18f
    offsetY.floatValue = 14f
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.waitForIdle()

    val bannerTopBottomBarOnly = bannerNode.fetchSemanticsNode().boundsInRoot.top
    assertEquals(bannerTop7DayOnly, bannerTopBottomBarOnly, 0.5f)
  }
}
