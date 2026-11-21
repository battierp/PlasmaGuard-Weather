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
import kotlin.math.abs
import kotlin.math.cos
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

@Composable
fun rememberPlasmaShieldState(
  driftIntensity: DriftIntensity,
  textColorOption: TextColorOption,
): PlasmaShieldState {
  var elapsedMs by remember { mutableLongStateOf(0L) }
  var driftPhaseSec by remember { mutableDoubleStateOf(0.0) }
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
        driftPhaseSec += deltaSec * currentSpeedMultiplier

        if (currentOption == TextColorOption.RAINBOW_BOUNCE) {
          val maxX = currentMaxDp.toDouble().coerceAtLeast(1.0)
          val maxY = (currentMaxDp * 0.65).coerceAtLeast(1.0)
          val speedX = (maxX * 4.0 / 40.0) * currentSpeedMultiplier
          val speedY = (maxY * 4.0 / 53.0) * currentSpeedMultiplier

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
          // Keep bounce coordinates synced to current Lissajous position so switching modes feels seamless
          val maxX = currentMaxDp.toDouble()
          val maxY = currentMaxDp * 0.65
          bounceX = sin(driftPhaseSec * (2.0 * PI / 53.0)) * maxX
          bounceY = cos(driftPhaseSec * (2.0 * PI / 85.0) + 0.7) * maxY
          vxSign = if (cos(driftPhaseSec * (2.0 * PI / 53.0)) >= 0.0) 1.0 else -1.0
          vySign = if (-sin(driftPhaseSec * (2.0 * PI / 85.0) + 0.7) >= 0.0) 1.0 else -1.0
        }
      }
    }
  }

  val tSec = elapsedMs / 1000.0
  val dSec = driftPhaseSec
  val maxDp = driftIntensity.maxOffsetDp

  // Pinball straight-line border bounce for Rainbow Bounce; smooth Lissajous orbit otherwise
  val primaryX =
    if (textColorOption == TextColorOption.RAINBOW_BOUNCE) bounceX.toFloat().dp
    else (sin(dSec * (2.0 * PI / 53.0)) * maxDp).toFloat().dp
  val primaryY =
    if (textColorOption == TextColorOption.RAINBOW_BOUNCE) bounceY.toFloat().dp
    else (cos(dSec * (2.0 * PI / 85.0) + 0.7) * (maxDp * 0.65f)).toFloat().dp

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
