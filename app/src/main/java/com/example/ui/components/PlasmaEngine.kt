package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DriftIntensity
import com.example.data.model.DriftMotionMode
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

@Immutable
data class PlasmaColors(
  val primaryTextColor: Color,
  val secondaryTextColor: Color,
  val primaryAccent: Color,
  val secondaryAccent: Color,
)

@Stable
class PlasmaShieldState(
  initialColors: PlasmaColors,
  private val offsetXDpFloatState: FloatState,
  private val offsetYDpFloatState: FloatState,
  private val pulseState: FloatState,
) {
  var colors by mutableStateOf(initialColors)
    internal set

  val primaryOffsetXDpFloat: Float
    get() = offsetXDpFloatState.floatValue

  val primaryOffsetYDpFloat: Float
    get() = offsetYDpFloatState.floatValue

  val primaryOffsetX: Dp
    get() = offsetXDpFloatState.floatValue.dp

  val primaryOffsetY: Dp
    get() = offsetYDpFloatState.floatValue.dp

  val primaryTextColor: Color
    get() = colors.primaryTextColor

  val secondaryTextColor: Color
    get() = colors.secondaryTextColor

  val primaryAccent: Color
    get() = colors.primaryAccent

  val secondaryAccent: Color
    get() = colors.secondaryAccent

  val pulseFraction: Float
    get() = pulseState.value
}

// Spectrally ordered neighboring hues for Rainbow Timer (least jarring smooth bleed)
private val RainbowTimerSpectrum =
  listOf(
    Color(0xFFFF6B6B), // Warm Coral Red
    Color(0xFFFF9E5E), // Tangerine Peach
    BrightGold,        // Solar Gold
    Color(0xFFA3E635), // Soft Lime
    Color(0xFF34D399), // Mint Green
    SeafoamTeal,       // Seafoam Teal
    AuroraCyan,        // Aurora Cyan
    Color(0xFF60A5FA), // Sky Cobalt
    Color(0xFF818CF8), // Periwinkle Indigo
    SoftAmethyst,      // Soft Amethyst
    Color(0xFFF472B6), // Rose Pink
  )

// Complementary-jump high-contrast palette for Rainbow Bounce (unmistakable color shift per hit)
private val RainbowBounceSpectrum =
  listOf(
    AuroraCyan,        // Cyan (~198°)
    Color(0xFFFF3B5C), // Scarlet Red (~350°)
    Color(0xFF34E87A), // Neon Mint (~144°)
    Color(0xFFB84DFF), // Electric Purple (~277°)
    BrightGold,        // Bright Gold (~47°)
    Color(0xFF2979FF), // Deep Cobalt (~217°)
    Color(0xFFFF7A00), // Blazing Orange (~29°)
  )

private const val RAINBOW_TIMER_MS_PER_COLOR = 12_000L
private const val RAINBOW_COLOR_STEP_MS = 200L

internal class FrameDeltaSmoother {
  private var smoothedDeltaNanos: Double = 0.0
  private var consecutiveOutliers: Int = 0

  fun recordDeltaSeconds(rawDeltaNanos: Long): Double {
    val clamped = rawDeltaNanos.coerceIn(0L, 100_000_000L)
    if (clamped <= 0L) return 0.0
    val raw = clamped.toDouble()
    if (smoothedDeltaNanos <= 0.0) {
      smoothedDeltaNanos = raw
      consecutiveOutliers = 0
      return raw / 1_000_000_000.0
    }
    val lower = smoothedDeltaNanos * 0.60
    val upper = smoothedDeltaNanos * 1.40
    if (raw < lower || raw > upper) {
      consecutiveOutliers++
      if (consecutiveOutliers >= 2) {
        smoothedDeltaNanos = raw
        consecutiveOutliers = 0
      }
      return raw / 1_000_000_000.0
    }
    consecutiveOutliers = 0
    smoothedDeltaNanos = smoothedDeltaNanos * 0.85 + raw * 0.15
    return smoothedDeltaNanos / 1_000_000_000.0
  }
}

internal fun motionModeContentScale(mode: DriftMotionMode): Float =
  when (mode) {
    DriftMotionMode.CLOCKWISE -> 0.90f
    DriftMotionMode.DVD_BOUNCE -> 0.80f
  }

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

internal fun computeClockwiseEdgeIndex(
  progress: Double,
  maxX: Double,
  maxY: Double,
): Int {
  val safeMaxX = maxX.coerceAtLeast(0.0)
  val safeMaxY = maxY.coerceAtLeast(0.0)
  val horizontalEdge = 2.0 * safeMaxX
  val verticalEdge = 2.0 * safeMaxY
  val totalPerimeter = 2.0 * (horizontalEdge + verticalEdge)
  if (totalPerimeter <= 0.0) return 0

  val wrapped = ((progress % 1.0) + 1.0) % 1.0
  val distance = wrapped * totalPerimeter
  return when {
    distance < verticalEdge -> 0
    distance < verticalEdge + horizontalEdge -> 1
    distance < verticalEdge + horizontalEdge + verticalEdge -> 2
    else -> 3
  }
}

@Composable
fun rememberPlasmaShieldState(
  driftIntensity: DriftIntensity,
  driftMotionMode: DriftMotionMode,
  textColorOption: TextColorOption,
  maxTravelXDp: Float = driftIntensity.maxOffsetDp,
  maxTravelYDp: Float = driftIntensity.maxOffsetDp * 0.65f,
): PlasmaShieldState {
  var rainbowBucketMs by remember { mutableLongStateOf(0L) }
  var bounceColorIndex by remember { mutableIntStateOf(0) }
  val offsetXDpFloatState = remember { mutableFloatStateOf(0f) }
  val offsetYDpFloatState = remember { mutableFloatStateOf(0f) }
  val pulseFractionState = remember { mutableFloatStateOf(0.5f) }

  val currentSpeedMultiplier by rememberUpdatedState(driftIntensity.speedMultiplier)
  val currentMotionMode by rememberUpdatedState(driftMotionMode)
  val currentTextColorOption by rememberUpdatedState(textColorOption)
  val currentMaxXDp by rememberUpdatedState(maxTravelXDp)
  val currentMaxYDp by rememberUpdatedState(maxTravelYDp)

  LaunchedEffect(Unit) {
    val startFrameNanos = withFrameNanos { it }
    var lastFrameNanos = startFrameNanos
    val deltaSmoother = FrameDeltaSmoother()
    var perimeterProgress = 0.0
    var bounceX = 0.0
    var bounceY = 0.0
    var vxSign = 1.0
    var vySign = 1.0
    var lastEdgeIdx = 0

    while (true) {
      withFrameNanos { frameTimeNanos ->
        val deltaSec = deltaSmoother.recordDeltaSeconds(frameTimeNanos - lastFrameNanos)
        lastFrameNanos = frameTimeNanos
        val elapsedMs = ((frameTimeNanos - startFrameNanos) / 1_000_000L).coerceAtLeast(0L)

        if (currentTextColorOption == TextColorOption.RAINBOW) {
          val nextBucket = (elapsedMs / RAINBOW_COLOR_STEP_MS) * RAINBOW_COLOR_STEP_MS
          if (nextBucket != rainbowBucketMs) {
            rainbowBucketMs = nextBucket
          }
        }

        val maxX = currentMaxXDp.toDouble().coerceAtLeast(1.0)
        val maxY = currentMaxYDp.toDouble().coerceAtLeast(1.0)

        // Wide travel (90% Clockwise, 80% DVD Bounce, or Hero-only TV edge travel) uses smooth visible screen traversal
        // at 4, 8, 12, 16, and 20 dp/sec (1x..5x); tight anti-burn-in drift scales cleanly by 1x..5x.
        val isWideTravel = maxX > 30.0 || maxY > 24.0
        val speedX =
          if (isWideTravel) 3.2 * currentSpeedMultiplier
          else (maxX * 4.0 / 40.0) * currentSpeedMultiplier
        val speedY =
          if (isWideTravel) 2.4 * currentSpeedMultiplier
          else (maxY * 4.0 / 53.0) * currentSpeedMultiplier

        if (currentMotionMode == DriftMotionMode.DVD_BOUNCE) {
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
            bounceColorIndex = (bounceColorIndex + 1) % RainbowBounceSpectrum.size
          }
          lastEdgeIdx = computeClockwiseEdgeIndex(perimeterProgress, maxX, maxY)
          offsetXDpFloatState.floatValue = bounceX.toFloat()
          offsetYDpFloatState.floatValue = bounceY.toFloat()
        } else {
          val totalPerimeter = (4.0 * maxX + 4.0 * maxY).coerceAtLeast(1.0)
          val perimeterSpeedDpPerSec =
            if (isWideTravel) 4.0 * currentSpeedMultiplier
            else hypot(speedX, speedY)
          perimeterProgress =
            ((perimeterProgress + (perimeterSpeedDpPerSec * deltaSec) / totalPerimeter) % 1.0 + 1.0) % 1.0
          val (edgeX, edgeY) = computeClockwisePerimeterOffset(perimeterProgress, maxX, maxY)
          bounceX = edgeX
          bounceY = edgeY
          val nextEdgeIdx = computeClockwiseEdgeIndex(perimeterProgress, maxX, maxY)
          if (nextEdgeIdx != lastEdgeIdx) {
            lastEdgeIdx = nextEdgeIdx
            bounceColorIndex = (bounceColorIndex + 1) % RainbowBounceSpectrum.size
          }
          offsetXDpFloatState.floatValue = edgeX.toFloat()
          offsetYDpFloatState.floatValue = edgeY.toFloat()
        }

        val tSec = elapsedMs / 1000.0
        pulseFractionState.floatValue = (0.5f + 0.5f * sin(tSec * (2.0 * PI / 6.0))).toFloat()
      }
    }
  }

  val animatedBounceColor by
    animateColorAsState(
      targetValue = RainbowBounceSpectrum[bounceColorIndex % RainbowBounceSpectrum.size],
      animationSpec = tween(durationMillis = 90),
      label = "rainbow_bounce_color",
    )

  val colors =
    when (textColorOption) {
      TextColorOption.WHITE ->
        PlasmaColors(
          primaryTextColor = SoftFrostText,
          secondaryTextColor = MutedSlateText,
          primaryAccent = AuroraCyan,
          secondaryAccent = SeafoamTeal,
        )

      TextColorOption.RAINBOW -> {
        val cycleMs = RainbowTimerSpectrum.size * RAINBOW_TIMER_MS_PER_COLOR
        val phase = ((rainbowBucketMs % cycleMs).toDouble() / cycleMs.toDouble()).toFloat()
        val activeColor = sampleCyclicPalette(RainbowTimerSpectrum, phase)
        PlasmaColors(
          primaryTextColor = activeColor,
          secondaryTextColor = activeColor.copy(alpha = 0.80f),
          primaryAccent = activeColor,
          secondaryAccent = activeColor.copy(alpha = 0.90f),
        )
      }

      TextColorOption.RAINBOW_BOUNCE ->
        PlasmaColors(
          primaryTextColor = animatedBounceColor,
          secondaryTextColor = animatedBounceColor.copy(alpha = 0.80f),
          primaryAccent = animatedBounceColor,
          secondaryAccent = animatedBounceColor.copy(alpha = 0.90f),
        )

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
        PlasmaColors(
          primaryTextColor = fixedColor,
          secondaryTextColor = fixedColor.copy(alpha = 0.80f),
          primaryAccent = fixedColor,
          secondaryAccent = fixedColor.copy(alpha = 0.90f),
        )
      }
    }

  val shieldState = remember {
    PlasmaShieldState(
      initialColors = colors,
      offsetXDpFloatState = offsetXDpFloatState,
      offsetYDpFloatState = offsetYDpFloatState,
      pulseState = pulseFractionState,
    )
  }
  shieldState.colors = colors
  return shieldState
}

private fun sampleCyclicPalette(palette: List<Color>, fraction: Float): Color {
  val count = palette.size
  val scaled = (fraction.coerceIn(0f, 1f)) * count
  val idx = scaled.toInt() % count
  val nextIdx = (idx + 1) % count
  val localT = scaled - scaled.toInt()
  // Smoothstep easing between neighboring hue anchors so transitions bleed smoothly
  val smoothT = localT * localT * (3f - 2f * localT)
  return lerp(palette[idx], palette[nextIdx], smoothT)
}
