package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DriftIntensity
import com.example.data.model.TextColorOption
import com.example.ui.theme.AuroraCyan
import com.example.ui.theme.BrightCobalt
import com.example.ui.theme.BrightCoral
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.SeafoamTeal
import com.example.ui.theme.SoftAmethyst
import com.example.ui.theme.SoftFrostText
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

data class PlasmaShieldState(
  val primaryOffsetX: Dp,
  val primaryOffsetY: Dp,
  val primaryTextColor: Color,
  val secondaryTextColor: Color,
  val primaryAccent: Color,
  val secondaryAccent: Color,
  val pulseFraction: Float,
)

private val BrightTextSpectrum =
  listOf(
    AuroraCyan,
    SeafoamTeal,
    BrightGold,
    BrightCoral,
    SoftAmethyst,
    BrightCobalt,
  )

private const val RAINBOW_MS_PER_COLOR = 60_000L

internal fun computeClockwisePerimeterOffset(
  progress: Double,
  maxX: Double,
  maxY: Double,
): Pair<Double, Double> {
  val safeMaxX = maxX.coerceAtLeast(0.0)
  val safeMaxY = maxY.coerceAtLeast(0.0)
  val horizontalEdge = 2.0 * safeMaxX
  val verticalEdge = 2.0 * safeMaxY
  val totalPerimeter = 2.0 * (horizontalEdge + verticalEdge)
  if (totalPerimeter <= 0.0) return 0.0 to 0.0

  val wrapped = ((progress % 1.0) + 1.0) % 1.0
  val distance = wrapped * totalPerimeter

  return when {
    distance < verticalEdge -> {
      // Right edge: top-right (+maxX, -maxY) -> bottom-right (+maxX, +maxY)
      safeMaxX to (-safeMaxY + distance)
    }
    distance < verticalEdge + horizontalEdge -> {
      // Bottom edge: bottom-right (+maxX, +maxY) -> bottom-left (-maxX, +maxY)
      (safeMaxX - (distance - verticalEdge)) to safeMaxY
    }
    distance < verticalEdge + horizontalEdge + verticalEdge -> {
      // Left edge: bottom-left (-maxX, +maxY) -> top-left (-maxX, -maxY)
      -safeMaxX to (safeMaxY - (distance - verticalEdge - horizontalEdge))
    }
    else -> {
      // Top edge: top-left (-maxX, -maxY) -> top-right (+maxX, -maxY)
      (-safeMaxX + (distance - verticalEdge - horizontalEdge - verticalEdge)) to -safeMaxY
    }
  }
}

@Composable
fun rememberPlasmaShieldState(
  driftIntensity: DriftIntensity,
  textColorOption: TextColorOption,
): PlasmaShieldState {
  var elapsedMs by remember { mutableLongStateOf(0L) }
  var perimeterProgress by remember { mutableDoubleStateOf(0.0) }
  var bounceX by remember { mutableDoubleStateOf(0.0) }
  var bounceY by remember { mutableDoubleStateOf(0.0) }
  var bounceColorIndex by remember { mutableIntStateOf(0) }

  val currentSpeedMultiplier by rememberUpdatedState(driftIntensity.speedMultiplier)
  val currentMaxDp by rememberUpdatedState(driftIntensity.maxOffsetDp)
  val currentOption by rememberUpdatedState(textColorOption)

  LaunchedEffect(Unit) {
    val startFrame = withFrameMillis { it }
    var lastFrame = startFrame
    var vxSign = 1.0
    var vySign = 1.0
    while (true) {
      withFrameMillis { frameTime ->
        val deltaSec = ((frameTime - lastFrame).coerceIn(0L, 250L)) / 1000.0
        lastFrame = frameTime
        elapsedMs = (frameTime - startFrame).coerceAtLeast(0L)

        val maxX = currentMaxDp.toDouble().coerceAtLeast(1.0)
        val maxY = (currentMaxDp * 0.65).coerceAtLeast(1.0)
        val speedX = (maxX * 4.0 / 40.0) * currentSpeedMultiplier
        val speedY = (maxY * 4.0 / 53.0) * currentSpeedMultiplier

        if (currentOption == TextColorOption.RAINBOW_BOUNCE) {
          var nextX = bounceX + vxSign * speedX * deltaSec
          var nextY = bounceY + vySign * speedY * deltaSec
          var hitBorder = false

          if (nextX >= maxX) {
            nextX = maxX - (nextX - maxX).coerceAtMost(maxX)
            vxSign = -1.0
            hitBorder = true
          } else if (nextX <= -maxX) {
            nextX = -maxX + (-maxX - nextX).coerceAtMost(maxX)
            vxSign = 1.0
            hitBorder = true
          }

          if (nextY >= maxY) {
            nextY = maxY - (nextY - maxY).coerceAtMost(maxY)
            vySign = -1.0
            hitBorder = true
          } else if (nextY <= -maxY) {
            nextY = -maxY + (-maxY - nextY).coerceAtMost(maxY)
            vySign = 1.0
            hitBorder = true
          }

          bounceX = nextX.coerceIn(-maxX, maxX)
          bounceY = nextY.coerceIn(-maxY, maxY)
          if (hitBorder) {
            bounceColorIndex = (bounceColorIndex + 1) % BrightTextSpectrum.size
          }
        } else {
          val totalPerimeter = 4.0 * maxX + 4.0 * maxY
          val perimeterSpeedDpPerSec = hypot(speedX, speedY)
          perimeterProgress =
            ((perimeterProgress + (perimeterSpeedDpPerSec * deltaSec) / totalPerimeter) % 1.0 + 1.0) % 1.0
          val (edgeX, edgeY) = computeClockwisePerimeterOffset(perimeterProgress, maxX, maxY)
          bounceX = edgeX
          bounceY = edgeY
        }
      }
    }
  }

  val tSec = elapsedMs / 1000.0
  val maxX = driftIntensity.maxOffsetDp.toDouble()
  val maxY = driftIntensity.maxOffsetDp * 0.65

  // Pinball straight-line border bounce for Rainbow Bounce; continuous clockwise edge-hug loop otherwise
  val (primaryXVal, primaryYVal) =
    if (textColorOption == TextColorOption.RAINBOW_BOUNCE) {
      bounceX to bounceY
    } else {
      computeClockwisePerimeterOffset(perimeterProgress, maxX, maxY)
    }
  val primaryX = primaryXVal.toFloat().dp
  val primaryY = primaryYVal.toFloat().dp

  val animatedBounceColor by
    animateColorAsState(
      targetValue = BrightTextSpectrum[bounceColorIndex % BrightTextSpectrum.size],
      animationSpec = tween(durationMillis = 240),
      label = "rainbow_bounce_color",
    )

  val primaryTextColor: Color
  val secondaryTextColor: Color
  val primaryAccent: Color
  val secondaryAccent: Color

  when (textColorOption) {
    TextColorOption.WHITE -> {
      primaryTextColor = SoftFrostText
      secondaryTextColor = MutedSlateText
      primaryAccent = AuroraCyan
      secondaryAccent = SeafoamTeal
    }

    TextColorOption.RAINBOW -> {
      val cycleMs = BrightTextSpectrum.size * RAINBOW_MS_PER_COLOR
      val phase = ((elapsedMs % cycleMs).toDouble() / cycleMs.toDouble()).toFloat()
      val activeColor = sampleCyclicPalette(phase)
      primaryTextColor = activeColor
      secondaryTextColor = activeColor.copy(alpha = 0.80f)
      primaryAccent = activeColor
      secondaryAccent = activeColor.copy(alpha = 0.90f)
    }

    TextColorOption.RAINBOW_BOUNCE -> {
      primaryTextColor = animatedBounceColor
      secondaryTextColor = animatedBounceColor.copy(alpha = 0.80f)
      primaryAccent = animatedBounceColor
      secondaryAccent = animatedBounceColor.copy(alpha = 0.90f)
    }

    else -> {
      val fixedColor =
        when (textColorOption) {
          TextColorOption.ELECTRIC_CYAN -> AuroraCyan
          TextColorOption.MINT -> SeafoamTeal
          TextColorOption.SOLAR_GOLD -> BrightGold
          TextColorOption.CORAL -> BrightCoral
          TextColorOption.VIOLET -> SoftAmethyst
          TextColorOption.COBALT -> BrightCobalt
          TextColorOption.WHITE,
          TextColorOption.RAINBOW,
          TextColorOption.RAINBOW_BOUNCE -> SoftFrostText
        }
      primaryTextColor = fixedColor
      secondaryTextColor = fixedColor.copy(alpha = 0.80f)
      primaryAccent = fixedColor
      secondaryAccent = fixedColor.copy(alpha = 0.90f)
    }
  }

  val pulse = (0.5f + 0.5f * sin(tSec * (2.0 * PI / 6.0))).toFloat()

  return PlasmaShieldState(
    primaryOffsetX = primaryX,
    primaryOffsetY = primaryY,
    primaryTextColor = primaryTextColor,
    secondaryTextColor = secondaryTextColor,
    primaryAccent = primaryAccent,
    secondaryAccent = secondaryAccent,
    pulseFraction = pulse,
  )
}

private fun sampleCyclicPalette(fraction: Float): Color {
  val count = BrightTextSpectrum.size
  val scaled = (fraction.coerceIn(0f, 1f)) * count
  val idx = scaled.toInt() % count
  val nextIdx = (idx + 1) % count
  val localT = scaled - scaled.toInt()
  // Smoothstep easing between bright color anchors
  val smoothT = localT * localT * (3f - 2f * localT)
  return lerp(BrightTextSpectrum[idx], BrightTextSpectrum[nextIdx], smoothT)
}
