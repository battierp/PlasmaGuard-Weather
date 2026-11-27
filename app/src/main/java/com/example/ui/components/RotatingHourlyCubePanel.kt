package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.HourlyMetricSlot
import com.example.data.model.HourlySlot
import com.example.data.model.WeatherSnapshot
import com.example.data.model.aqiCategoryLabel
import com.example.data.model.degreesToCompass
import com.example.data.model.precipCategoryLabel
import com.example.ui.screens.AutoShrinkSingleLineText
import com.example.ui.screens.rememberUniformSingleLineFontSize
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.BrightCoral
import com.example.ui.theme.BrightGold
import com.example.ui.theme.RainBlue
import com.example.ui.theme.SeafoamTeal
import com.example.ui.theme.SoftAmethyst
import com.example.ui.theme.SpaceGroteskFontFamily
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

enum class HourlyCubeFace(val title: String) {
  FORECAST_24H("24-HOUR FORECAST"),
  PRECIPITATION_12H("12-HOUR PRECIPITATION"),
  WIND_12H("12-HOUR WIND SPEED"),
  AIR_QUALITY_12H("12-HOUR AIR QUALITY"),
}

fun resolveEnabledBottomBarFaces(
  showTemp24h: Boolean,
  showPrecipitation: Boolean,
  showWind: Boolean,
  showAirQuality: Boolean,
): List<HourlyCubeFace> =
  buildList {
    if (showTemp24h) add(HourlyCubeFace.FORECAST_24H)
    if (showPrecipitation) add(HourlyCubeFace.PRECIPITATION_12H)
    if (showWind) add(HourlyCubeFace.WIND_12H)
    if (showAirQuality) add(HourlyCubeFace.AIR_QUALITY_12H)
  }

// 4 faces * (14,050ms hold + 950ms 3D roll) = 60,000ms (1 minute) full cycle
private const val FACE_HOLD_DURATION_MS = 14_050L
private const val CUBE_ROLL_DURATION_MS = 950
private val CubeRollEasing = CubicBezierEasing(0.32f, 0.0f, 0.22f, 1.0f)

@Composable
fun RotatingHourlyCubePanel(
  snapshot: WeatherSnapshot,
  use24Hour: Boolean,
  plasma: PlasmaShieldState,
  isCompact: Boolean,
  isShortHeight: Boolean,
  expandHeight: Boolean,
  enabledFaces: List<HourlyCubeFace> = HourlyCubeFace.entries,
  modifier: Modifier = Modifier,
) {
  if (enabledFaces.isEmpty()) return

  var currentFaceIndex by remember { mutableIntStateOf(0) }
  var nextFaceIndex by remember { mutableIntStateOf(if (enabledFaces.size > 1) 1 else 0) }
  var isAnimating by remember { mutableStateOf(false) }
  val rollProgress = remember { Animatable(0f) }

  LaunchedEffect(enabledFaces) {
    rollProgress.snapTo(0f)
    Snapshot.withMutableSnapshot {
      isAnimating = false
      currentFaceIndex = currentFaceIndex.coerceIn(0, enabledFaces.lastIndex)
      nextFaceIndex = (currentFaceIndex + 1) % enabledFaces.size
    }
    if (enabledFaces.size > 1) {
      while (true) {
        delay(FACE_HOLD_DURATION_MS)
        val upcoming = (currentFaceIndex + 1) % enabledFaces.size
        rollProgress.snapTo(0f)
        Snapshot.withMutableSnapshot {
          nextFaceIndex = upcoming
          isAnimating = true
        }
        rollProgress.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = CUBE_ROLL_DURATION_MS, easing = CubeRollEasing),
        )
        Snapshot.withMutableSnapshot {
          currentFaceIndex = upcoming
          nextFaceIndex = (upcoming + 1) % enabledFaces.size
          isAnimating = false
        }
        rollProgress.snapTo(0f)
      }
    }
  }

  val safeCurrentIndex = currentFaceIndex.coerceIn(0, enabledFaces.lastIndex)
  val safeNextIndex = nextFaceIndex.coerceIn(0, enabledFaces.lastIndex)
  val currentFace = enabledFaces[safeCurrentIndex]
  val nextFace = enabledFaces[safeNextIndex]
  val enabledCount = enabledFaces.size

  Box(
    modifier = modifier.testTag("hourly_forecast_section"),
  ) {
    enabledFaces.forEachIndexed { facePosition, entry ->
      val isCurrent = entry == currentFace
      val isNext = enabledCount > 1 && entry == nextFace && entry != currentFace
      key(entry) {
        CubeFacePanelContent(
          face = entry,
          facePosition = facePosition,
          enabledCount = enabledCount,
          snapshot = snapshot,
          use24Hour = use24Hour,
          plasma = plasma,
          isCompact = isCompact,
          isShortHeight = isShortHeight,
          expandHeight = expandHeight,
          modifier =
            Modifier.fillMaxWidth()
              .then(if (expandHeight) Modifier.fillMaxHeight() else Modifier)
              .zIndex(
                when {
                  isCurrent -> 2f
                  isNext -> 1f
                  else -> 0f
                }
              )
              .graphicsLayer {
                compositingStrategy =
                  if (isCurrent || isNext) CompositingStrategy.Offscreen
                  else CompositingStrategy.Auto
                val h = size.height
                if (!isAnimating || h <= 0f) {
                  transformOrigin = TransformOrigin.Center
                  translationY = 0f
                  rotationX = 0f
                  scaleX = 1f
                  scaleY = 1f
                  // Next face sits pre-rendered directly behind the opaque black current face
                  alpha =
                    when {
                      isCurrent -> 1f
                      isNext -> 0.01f
                      else -> 0f
                    }
                } else if (isCurrent) {
                  val p = rollProgress.value.coerceIn(0f, 1f)
                  val alphaRad = p * (PI.toFloat() / 2f)
                  val seamY = (h / 2f) * (1f - cos(alphaRad) + sin(alphaRad))
                  val protrude = (cos(alphaRad) + sin(alphaRad) - 1f).coerceAtLeast(0f)
                  val depthScale = 1f + 0.03f * protrude
                  transformOrigin = TransformOrigin(0.5f, 0f)
                  translationY = seamY
                  rotationX = -p * 90f
                  scaleX = depthScale
                  scaleY = depthScale
                  cameraDistance = 20f * density
                  alpha = if (p >= 0.995f) 0f else 1f
                } else if (isNext) {
                  val p = rollProgress.value.coerceIn(0f, 1f)
                  val alphaRad = p * (PI.toFloat() / 2f)
                  val seamY = (h / 2f) * (1f - cos(alphaRad) + sin(alphaRad))
                  val protrude = (cos(alphaRad) + sin(alphaRad) - 1f).coerceAtLeast(0f)
                  val depthScale = 1f + 0.03f * protrude
                  transformOrigin = TransformOrigin(0.5f, 1f)
                  translationY = seamY - h
                  rotationX = (1f - p) * 90f
                  scaleX = depthScale
                  scaleY = depthScale
                  cameraDistance = 20f * density
                  alpha = 1f
                } else {
                  alpha = 0f
                }
              }
              .drawWithContent {
                drawContent()
                if (isAnimating && (isCurrent || isNext)) {
                  val p = rollProgress.value.coerceIn(0f, 1f)
                  val alphaRad = p * (PI.toFloat() / 2f)
                  val shadeAlpha =
                    if (isCurrent) (0.95f * sin(alphaRad)).coerceIn(0f, 1f)
                    else (0.95f * cos(alphaRad)).coerceIn(0f, 1f)
                  if (shadeAlpha > 0.005f) {
                    drawRect(Color.Black.copy(alpha = shadeAlpha))
                  }
                }
              },
        )
      }
    }
  }
}

@Composable
private fun CubeFacePanelContent(
  face: HourlyCubeFace,
  facePosition: Int,
  enabledCount: Int,
  snapshot: WeatherSnapshot,
  use24Hour: Boolean,
  plasma: PlasmaShieldState,
  isCompact: Boolean,
  isShortHeight: Boolean,
  expandHeight: Boolean,
  modifier: Modifier = Modifier,
) {
  val forecastSlots = remember(snapshot.hourlySlots) { snapshot.hourlySlots.take(12) }
  val metricSlots =
    remember(snapshot.next12Hours, snapshot.hourlySlots, snapshot.windSpeed, snapshot.windGusts) {
      if (snapshot.next12Hours.isNotEmpty()) {
        snapshot.next12Hours.take(12)
      } else {
        snapshot.hourlySlots.take(12).map { slot ->
          HourlyMetricSlot(
            hour24 = slot.hour24,
            minute = slot.minute,
            precipProbability = slot.precipProbability,
            windSpeed = snapshot.windSpeed,
            windGusts = snapshot.windGusts,
            windDirectionDeg = 0,
            usAqi = null,
          )
        }
      }
    }

  val slotCount = if (face == HourlyCubeFace.FORECAST_24H) forecastSlots.size else metricSlots.size
  val useTwoRows = isCompact && slotCount > 6
  val cardCompact = isCompact || isShortHeight
  val cardSpacing = if (useTwoRows) 4.dp else 3.dp
  val cardHorizontalPadding = 1.dp

  val formattedCards =
    remember(face, forecastSlots, metricSlots, use24Hour, snapshot.windUnitLabel) {
      when (face) {
        HourlyCubeFace.FORECAST_24H ->
          forecastSlots.map { slot ->
            val timeLabel = formatHourlyTimeLabel(slot.hour24, slot.minute, use24Hour, slot.isNow)
            val condLabel =
              if (slot.precipProbability >= 20) "${slot.condition.shortLabel} ${slot.precipProbability}%"
              else slot.condition.shortLabel
            CubeCardStrings(
              timeLabel = timeLabel,
              valueLabel = "${slot.temperature}°",
              subLabel = condLabel,
              highlightTime = slot.isNow,
              highlightSub = slot.precipProbability >= 30,
            )
          }

        HourlyCubeFace.PRECIPITATION_12H ->
          metricSlots.map { slot ->
            val timeLabel = formatHourlyTimeLabel(slot.hour24, slot.minute, use24Hour)
            CubeCardStrings(
              timeLabel = timeLabel,
              valueLabel = "${slot.precipProbability}%",
              subLabel = precipCategoryLabel(slot.precipProbability),
              highlightTime = false,
              highlightSub = slot.precipProbability >= 30,
            )
          }

        HourlyCubeFace.WIND_12H ->
          metricSlots.map { slot ->
            val timeLabel = formatHourlyTimeLabel(slot.hour24, slot.minute, use24Hour)
            CubeCardStrings(
              timeLabel = timeLabel,
              valueLabel = "${slot.windSpeed}",
              subLabel = "${snapshot.windUnitLabel} ${degreesToCompass(slot.windDirectionDeg)}",
              highlightTime = false,
              highlightSub = slot.windSpeed >= 20,
            )
          }

        HourlyCubeFace.AIR_QUALITY_12H ->
          metricSlots.map { slot ->
            val timeLabel = formatHourlyTimeLabel(slot.hour24, slot.minute, use24Hour)
            CubeCardStrings(
              timeLabel = timeLabel,
              valueLabel = slot.usAqi?.toString() ?: "--",
              subLabel = aqiCategoryLabel(slot.usAqi),
              highlightTime = false,
              highlightSub = (slot.usAqi ?: 0) > 100,
            )
          }
      }
    }

  val timeLabels = remember(formattedCards) { formattedCards.map { it.timeLabel } }
  val valueLabels = remember(formattedCards) { formattedCards.map { it.valueLabel } }
  val subLabels = remember(formattedCards) { formattedCards.map { it.subLabel } }

  val baseTimeStyle =
    MaterialTheme.typography.labelLarge.copy(
      fontSize = if (cardCompact) 16.sp else 18.sp,
      lineHeight = if (cardCompact) 18.sp else 20.sp,
    )
  val baseValueStyle =
    MaterialTheme.typography.headlineMedium.copy(
      fontFamily = SpaceGroteskFontFamily,
      fontSize = if (cardCompact) 22.sp else 33.sp,
      lineHeight = if (cardCompact) 24.sp else 35.sp,
      letterSpacing = (-0.5).sp,
    )
  val baseSubStyle =
    MaterialTheme.typography.labelSmall.copy(
      fontSize = if (cardCompact) 13.sp else 15.sp,
      lineHeight = if (cardCompact) 15.sp else 17.sp,
      letterSpacing = (-0.2).sp,
    )

  Box(
    modifier = modifier.background(Color.Black),
  ) {
    Column(
      modifier =
        Modifier.then(if (expandHeight) Modifier.fillMaxSize() else Modifier)
          .padding(
            horizontal = 8.dp,
            vertical = if (cardCompact) 6.dp else 8.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(if (isShortHeight) 4.dp else 6.dp),
    ) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      ) {
        AutoShrinkSingleLineText(
          text = face.title,
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontSize = 18.sp,
              lineHeight = 20.sp,
            ),
          color = plasma.primaryTextColor,
          minFontSize = 12.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 42.dp),
        )

        if (enabledCount > 1) {
          Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            for (idx in 0 until enabledCount) {
              val selected = idx == facePosition
              Box(
                modifier =
                  Modifier.height(4.dp)
                    .width(if (selected) 12.dp else 4.dp)
                    .clip(CircleShape)
                    .background(
                      if (selected) plasma.primaryTextColor.copy(alpha = 0.85f)
                      else plasma.secondaryTextColor.copy(alpha = 0.30f)
                    )
              )
            }
          }
        }
      }

      BoxWithConstraints(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (expandHeight && !useTwoRows) Modifier.weight(1f) else Modifier),
      ) {
        val density = LocalDensity.current
        val cardsPerRow = if (useTwoRows) 6 else slotCount.coerceAtLeast(1)
        val gapPx = with(density) { cardSpacing.roundToPx() }
        val innerPadPx = with(density) { (cardHorizontalPadding * 2).roundToPx() }
        val cardInnerWidthPx =
          ((constraints.maxWidth - gapPx * (cardsPerRow - 1)) / cardsPerRow - innerPadPx)
            .coerceAtLeast(1)

        val uniformTimeSize =
          rememberUniformSingleLineFontSize(
            texts = timeLabels,
            style = baseTimeStyle,
            maxWidthPx = cardInnerWidthPx,
            minFontSize = 10.sp,
          )
        val uniformValueSize =
          rememberUniformSingleLineFontSize(
            texts = valueLabels,
            style = baseValueStyle,
            maxWidthPx = cardInnerWidthPx,
            minFontSize = if (cardCompact) 14.sp else 20.sp,
          )
        val uniformSubSize =
          rememberUniformSingleLineFontSize(
            texts = subLabels,
            style = baseSubStyle,
            maxWidthPx = cardInnerWidthPx,
            minFontSize = 8.5.sp,
          )

        val uniformTimeStyle =
          baseTimeStyle.copy(
            fontSize = uniformTimeSize,
            lineHeight = (uniformTimeSize.value + 2f).sp,
          )
        val uniformValueStyle =
          baseValueStyle.copy(
            fontSize = uniformValueSize,
            lineHeight = (uniformValueSize.value + 2f).sp,
          )
        val uniformSubStyle =
          baseSubStyle.copy(
            fontSize = uniformSubSize,
            lineHeight = (uniformSubSize.value + 2f).sp,
          )

        val cardInnerWidthDp = with(density) { cardInnerWidthPx.toDp() }
        val widthBasedGlyphDp = (cardInnerWidthDp - 2.dp).coerceAtLeast(18.dp)
        val glyphSize =
          if (expandHeight && !useTwoRows && constraints.hasBoundedHeight) {
            val availableHeightDp = with(density) { constraints.maxHeight.toDp() }
            val cardVertPadDp = if (isShortHeight) 6.dp else 8.dp
            val textBudgetDp =
              with(density) {
                uniformTimeStyle.lineHeight.toDp() +
                  uniformValueStyle.lineHeight.toDp() +
                  uniformSubStyle.lineHeight.toDp()
              }
            val spacingBudgetDp = 8.dp
            val heightBasedGlyphDp =
              (availableHeightDp - cardVertPadDp - textBudgetDp - spacingBudgetDp).coerceAtLeast(18.dp)
            minOf(widthBasedGlyphDp, heightBasedGlyphDp)
          } else {
            widthBasedGlyphDp.coerceAtMost(if (useTwoRows) 34.dp else 44.dp)
          }

        if (useTwoRows) {
          val indices = (0 until slotCount).toList()
          val topIndices = indices.take(6)
          val bottomIndices = indices.drop(6).take(6)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            ) {
              topIndices.forEach { idx ->
                CubeSlotCard(
                  face = face,
                  strings = formattedCards[idx],
                  forecastSlot = forecastSlots.getOrNull(idx),
                  metricSlot = metricSlots.getOrNull(idx),
                  windUnitLabel = snapshot.windUnitLabel,
                  plasma = plasma,
                  timeStyle = uniformTimeStyle,
                  valueStyle = uniformValueStyle,
                  subStyle = uniformSubStyle,
                  glyphSize = glyphSize,
                  isCompact = true,
                  fillVertical = false,
                  modifier = Modifier.weight(1f),
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            ) {
              bottomIndices.forEach { idx ->
                CubeSlotCard(
                  face = face,
                  strings = formattedCards[idx],
                  forecastSlot = forecastSlots.getOrNull(idx),
                  metricSlot = metricSlots.getOrNull(idx),
                  windUnitLabel = snapshot.windUnitLabel,
                  plasma = plasma,
                  timeStyle = uniformTimeStyle,
                  valueStyle = uniformValueStyle,
                  subStyle = uniformSubStyle,
                  glyphSize = glyphSize,
                  isCompact = true,
                  fillVertical = false,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        } else {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .then(if (expandHeight) Modifier.fillMaxHeight() else Modifier),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
          ) {
            (0 until slotCount).forEach { idx ->
              CubeSlotCard(
                face = face,
                strings = formattedCards[idx],
                forecastSlot = forecastSlots.getOrNull(idx),
                metricSlot = metricSlots.getOrNull(idx),
                windUnitLabel = snapshot.windUnitLabel,
                plasma = plasma,
                timeStyle = uniformTimeStyle,
                valueStyle = uniformValueStyle,
                subStyle = uniformSubStyle,
                glyphSize = glyphSize,
                isCompact = isShortHeight,
                fillVertical = expandHeight,
                modifier =
                  Modifier.weight(1f)
                    .then(if (expandHeight) Modifier.fillMaxHeight() else Modifier),
              )
            }
          }
        }
      }
    }
  }
}

private data class CubeCardStrings(
  val timeLabel: String,
  val valueLabel: String,
  val subLabel: String,
  val highlightTime: Boolean,
  val highlightSub: Boolean,
)

@Composable
private fun CubeSlotCard(
  face: HourlyCubeFace,
  strings: CubeCardStrings,
  forecastSlot: HourlySlot?,
  metricSlot: HourlyMetricSlot?,
  windUnitLabel: String,
  plasma: PlasmaShieldState,
  timeStyle: TextStyle,
  valueStyle: TextStyle,
  subStyle: TextStyle,
  glyphSize: Dp,
  isCompact: Boolean,
  fillVertical: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    Column(
      modifier =
        Modifier.then(if (fillVertical) Modifier.fillMaxSize() else Modifier)
          .padding(horizontal = 1.dp, vertical = if (isCompact) 3.dp else 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement =
        if (fillVertical) Arrangement.SpaceBetween
        else Arrangement.spacedBy(if (isCompact) 2.dp else 3.dp),
    ) {
      Text(
        text = strings.timeLabel,
        style = timeStyle,
        color = if (strings.highlightTime) plasma.primaryAccent else plasma.secondaryTextColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )

      when (face) {
        HourlyCubeFace.FORECAST_24H -> {
          if (forecastSlot != null) {
            WeatherGlyph(
              condition = forecastSlot.condition,
              plasmaAccent = plasma.primaryAccent,
              size = glyphSize,
              animated = false,
            )
          }
        }

        HourlyCubeFace.PRECIPITATION_12H -> {
          PrecipitationGlyph(
            precipProbability = metricSlot?.precipProbability ?: 0,
            plasmaAccent = plasma.primaryAccent,
            size = glyphSize,
          )
        }

        HourlyCubeFace.WIND_12H -> {
          WindCompassGlyph(
            windSpeed = metricSlot?.windSpeed ?: 0,
            windDirectionDeg = metricSlot?.windDirectionDeg ?: 0,
            windUnitLabel = windUnitLabel,
            plasmaAccent = plasma.primaryAccent,
            size = glyphSize,
          )
        }

        HourlyCubeFace.AIR_QUALITY_12H -> {
          AqiRingGlyph(
            usAqi = metricSlot?.usAqi,
            plasmaAccent = plasma.primaryAccent,
            size = glyphSize,
          )
        }
      }

      Text(
        text = strings.valueLabel,
        style = valueStyle,
        color = plasma.primaryTextColor,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = strings.subLabel,
        style = subStyle,
        color =
          if (face == HourlyCubeFace.AIR_QUALITY_12H) {
            aqiAccentColor(metricSlot?.usAqi, plasma.secondaryTextColor)
          } else if (strings.highlightSub) {
            plasma.primaryAccent
          } else {
            plasma.secondaryTextColor
          },
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PrecipitationGlyph(
  precipProbability: Int,
  plasmaAccent: Color,
  size: Dp,
) {
  Box(
    modifier =
      Modifier.size(size).drawWithCache {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val topY = h * 0.06f
        val bottomY = h * 0.94f
        val dropHalfW = w * 0.34f
        val bellyY = h * 0.62f

        val droplet =
          Path().apply {
            moveTo(cx, topY)
            cubicTo(
              cx + dropHalfW * 0.55f,
              h * 0.30f,
              cx + dropHalfW,
              h * 0.48f,
              cx + dropHalfW,
              bellyY,
            )
            cubicTo(
              cx + dropHalfW,
              h * 0.82f,
              cx + dropHalfW * 0.55f,
              bottomY,
              cx,
              bottomY,
            )
            cubicTo(
              cx - dropHalfW * 0.55f,
              bottomY,
              cx - dropHalfW,
              h * 0.82f,
              cx - dropHalfW,
              bellyY,
            )
            cubicTo(
              cx - dropHalfW,
              h * 0.48f,
              cx - dropHalfW * 0.55f,
              h * 0.30f,
              cx,
              topY,
            )
            close()
          }

        val strokeColor = lerp(RainBlue, plasmaAccent, 0.28f)
        val fillFrac = (precipProbability.coerceIn(0, 100) / 100f)
        val effectiveFrac = (fillFrac * 0.90f + 0.06f).coerceIn(0f, 1f)
        val liquidTop = bottomY - (bottomY - topY) * effectiveFrac
        val liquidBrush =
          Brush.verticalGradient(
            colors = listOf(AuroraCyan.copy(alpha = 0.92f), RainBlue.copy(alpha = 0.96f)),
            startY = liquidTop,
            endY = bottomY,
          )
        val borderStroke = Stroke(width = (w * 0.055f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
        val highlightWidth = (w * 0.045f).coerceAtLeast(1.5f)
        val highlightStart = Offset(cx - dropHalfW * 0.52f, h * 0.50f)
        val highlightEnd = Offset(cx - dropHalfW * 0.58f, h * 0.66f)

        onDrawBehind {
          drawPath(droplet, color = strokeColor.copy(alpha = 0.14f))
          if (fillFrac > 0f) {
            clipPath(droplet) {
              drawRect(
                brush = liquidBrush,
                topLeft = Offset(0f, liquidTop),
                size = Size(w, bottomY - liquidTop + 2f),
              )
            }
          }
          drawPath(path = droplet, color = strokeColor.copy(alpha = 0.90f), style = borderStroke)
          drawLine(
            color = Color.White.copy(alpha = 0.50f),
            start = highlightStart,
            end = highlightEnd,
            strokeWidth = highlightWidth,
            cap = StrokeCap.Round,
          )
        }
      }
  )
}

@Composable
private fun WindCompassGlyph(
  windSpeed: Int,
  windDirectionDeg: Int,
  windUnitLabel: String,
  plasmaAccent: Color,
  size: Dp,
) {
  val arrowPath = remember { Path() }
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val strokeW = (w * 0.08f).coerceAtLeast(2.2f)
    val pad = strokeW * 0.8f
    val diameter = minOf(w, h) - pad * 2f
    val topLeft = Offset((w - diameter) / 2f, (h - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val cx = w / 2f
    val cy = h / 2f
    val radius = diameter / 2f

    // Track circle
    drawCircle(
      color = plasmaAccent.copy(alpha = 0.22f),
      radius = radius,
      center = Offset(cx, cy),
      style = Stroke(width = strokeW * 0.85f),
    )

    // Speed gauge sweep (up to 40 mph or 65 kph = full ring)
    val maxRef = if (windUnitLabel == "kph") 65f else 40f
    val speedFrac = (windSpeed.toFloat() / maxRef).coerceIn(0.08f, 1f)
    val windColor =
      when {
        speedFrac >= 0.75f -> BrightCoral
        speedFrac >= 0.45f -> BrightGold
        else -> lerp(SeafoamTeal, AuroraCyan, 0.45f)
      }

    drawArc(
      color = windColor.copy(alpha = 0.92f),
      startAngle = -90f,
      sweepAngle = 360f * speedFrac,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeW, cap = StrokeCap.Round),
    )

    // 4 subtle cardinal ticks (N/E/S/W)
    for (i in 0 until 4) {
      val rad = i * (PI.toFloat() / 2f)
      val rIn = radius * 0.78f
      val rOut = radius * 0.95f
      drawLine(
        color = Color.White.copy(alpha = 0.40f),
        start = Offset(cx + cos(rad) * rIn, cy + sin(rad) * rIn),
        end = Offset(cx + cos(rad) * rOut, cy + sin(rad) * rOut),
        strokeWidth = (w * 0.04f).coerceAtLeast(1.4f),
        cap = StrokeCap.Round,
      )
    }

    // Wind arrow pointing in the direction the wind blows (meteorological direction + 180°)
    val arrowLen = radius * 0.56f
    val wingW = radius * 0.30f
    arrowPath.reset()
    arrowPath.moveTo(cx, cy - arrowLen)
    arrowPath.lineTo(cx + wingW, cy + arrowLen * 0.58f)
    arrowPath.lineTo(cx, cy + arrowLen * 0.22f)
    arrowPath.lineTo(cx - wingW, cy + arrowLen * 0.58f)
    arrowPath.close()
    rotate(degrees = (windDirectionDeg + 180).toFloat(), pivot = Offset(cx, cy)) {
      drawPath(arrowPath, color = windColor.copy(alpha = 0.95f))
    }
  }
}

@Composable
private fun AqiRingGlyph(
  usAqi: Int?,
  plasmaAccent: Color,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val strokeW = (w * 0.11f).coerceAtLeast(2.6f)
    val pad = strokeW * 0.75f
    val diameter = minOf(w, h) - pad * 2f
    val topLeft = Offset((w - diameter) / 2f, (h - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val cx = w / 2f
    val cy = h / 2f
    val radius = diameter / 2f

    val ringColor = aqiAccentColor(usAqi, lerp(SeafoamTeal, plasmaAccent, 0.25f))

    // 270-degree gauge track
    drawArc(
      color = ringColor.copy(alpha = 0.20f),
      startAngle = 135f,
      sweepAngle = 270f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeW, cap = StrokeCap.Round),
    )

    val aqiSweep =
      if (usAqi == null) 0f
      else (usAqi.toFloat() / 200f).coerceIn(0.10f, 1f) * 270f

    if (aqiSweep > 0f) {
      drawArc(
        color = ringColor.copy(alpha = 0.95f),
        startAngle = 135f,
        sweepAngle = aqiSweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
      )

      val endRad = (135f + aqiSweep) * (PI.toFloat() / 180f)
      drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        radius = strokeW * 0.42f,
        center = Offset(cx + cos(endRad) * radius, cy + sin(endRad) * radius),
      )
    }

    // Subtle clean-air breeze lines in center
    val lineW = (w * 0.055f).coerceAtLeast(1.6f)
    val xs = radius * 0.42f
    val ys = listOf(cy - radius * 0.16f, cy + radius * 0.04f, cy + radius * 0.24f)
    ys.forEachIndexed { idx, y ->
      val shift = if (idx == 1) radius * 0.08f else 0f
      drawLine(
        color = ringColor.copy(alpha = 0.72f - idx * 0.12f),
        start = Offset(cx - xs + shift, y),
        end = Offset(cx + xs - (if (idx == 2) radius * 0.14f else 0f), y),
        strokeWidth = lineW,
        cap = StrokeCap.Round,
      )
    }
  }
}

private fun aqiAccentColor(usAqi: Int?, fallback: Color): Color =
  when {
    usAqi == null -> fallback
    usAqi <= 50 -> SeafoamTeal
    usAqi <= 100 -> BrightGold
    usAqi <= 150 -> Color(0xFFFB923C)
    usAqi <= 200 -> BrightCoral
    else -> SoftAmethyst
  }

private fun formatHourlyTimeLabel(
  hour24: Int,
  minute: Int,
  use24Hour: Boolean,
  isNow: Boolean = false,
): String =
  when {
    isNow -> "Now"
    use24Hour -> String.format(Locale.getDefault(), "%02d:%02d", hour24, minute)
    else -> {
      val h12 =
        when {
          hour24 == 0 -> 12
          hour24 > 12 -> hour24 - 12
          else -> hour24
        }
      val amPm = if (hour24 < 12) "AM" else "PM"
      "$h12 $amPm"
    }
  }
