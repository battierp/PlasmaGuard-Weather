package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.WeatherCondition
import com.example.ui.theme.CloudMist
import com.example.ui.theme.RainBlue
import com.example.ui.theme.SnowIce
import com.example.ui.theme.StormViolet
import com.example.ui.theme.SunnyGold
import com.example.ui.theme.TwilightIndigo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private class GlyphPathScratch {
  val crescent = Path()
  val cutout = Path()
  val unionResult = Path()
  val bolt = Path()
}

private val StaticZeroPhase: State<Float> = mutableFloatStateOf(0f)
private val FogRowFractions = floatArrayOf(0.22f, 0.50f, 0.78f)
private val SnowXFractions = floatArrayOf(0.24f, 0.50f, 0.76f)

@Composable
fun WeatherGlyph(
  condition: WeatherCondition,
  plasmaAccent: Color,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  animated: Boolean = true,
) {
  val phaseState =
    if (animated) {
      val transition = rememberInfiniteTransition(label = "weather_glyph")
      transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
        label = "glyph_phase",
      )
    } else {
      StaticZeroPhase
    }
  val scratch = remember { GlyphPathScratch() }
  val canvasModifier = if (size == Dp.Unspecified) modifier else modifier.size(size)

  Canvas(modifier = canvasModifier) {
    val w = this.size.width
    val h = this.size.height
    val activePhase = phaseState.value
    val drift = sin(activePhase * 2f * PI.toFloat()) * (w * 0.025f)

    when (condition) {
      WeatherCondition.SUNNY -> drawSunny(w, h, activePhase, plasmaAccent)
      WeatherCondition.CLEAR_NIGHT -> drawClearNight(w, h, activePhase, plasmaAccent, scratch)
      WeatherCondition.PARTLY_CLOUDY -> drawPartlyCloudy(w, h, drift, plasmaAccent)
      WeatherCondition.CLOUDY ->
        drawCloud(w, h, drift, lerp(CloudMist, plasmaAccent, 0.18f), 1.0f, yCenterFrac = 0.57f, sizeScale = 1.12f)
      WeatherCondition.FOGGY -> drawFog(w, h, drift, lerp(CloudMist, plasmaAccent, 0.25f))
      WeatherCondition.DRIZZLE -> drawRainy(w, h, activePhase, drift, plasmaAccent, heavy = false)
      WeatherCondition.RAINY -> drawRainy(w, h, activePhase, drift, plasmaAccent, heavy = true)
      WeatherCondition.SNOWY -> drawSnowy(w, h, activePhase, drift, plasmaAccent)
      WeatherCondition.STORMY -> drawStormy(w, h, activePhase, drift, plasmaAccent, scratch)
    }
  }
}

private fun DrawScope.drawSunny(w: Float, h: Float, phase: Float, accent: Color) {
  val center = Offset(w * 0.5f, h * 0.5f)
  val sunCore = lerp(SunnyGold, accent, 0.22f)
  val pulse = 1f + 0.03f * sin(phase * 2f * PI.toFloat())

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(sunCore.copy(alpha = 0.26f), Color.Transparent),
      center = center,
      radius = w * 0.49f * pulse,
    ),
    radius = w * 0.49f * pulse,
    center = center,
  )

  val rayInner = w * 0.31f
  val rayOuter = w * 0.45f * pulse
  val rotationOffset = phase * (2f * PI.toFloat() / 8f)
  for (i in 0 until 8) {
    val angle = rotationOffset + i * (PI.toFloat() / 4f)
    drawLine(
      color = sunCore.copy(alpha = 0.82f),
      start = Offset(center.x + cos(angle) * rayInner, center.y + sin(angle) * rayInner),
      end = Offset(center.x + cos(angle) * rayOuter, center.y + sin(angle) * rayOuter),
      strokeWidth = (w * 0.068f).coerceAtLeast(2.0f),
      cap = StrokeCap.Round,
    )
  }

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(sunCore.copy(alpha = 0.96f), lerp(sunCore, Color(0xFFD97706), 0.35f)),
      center = Offset(center.x - w * 0.05f, center.y - h * 0.05f),
      radius = w * 0.28f,
    ),
    radius = w * 0.24f,
    center = center,
  )
}

private fun DrawScope.drawClearNight(
  w: Float,
  h: Float,
  phase: Float,
  accent: Color,
  scratch: GlyphPathScratch,
) {
  val moonTint = lerp(TwilightIndigo, accent, 0.35f)
  val center = Offset(w * 0.48f, h * 0.54f)
  val moonRadius = w * 0.36f

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(moonTint.copy(alpha = 0.22f), Color.Transparent),
      center = center,
      radius = w * 0.48f,
    ),
    radius = w * 0.48f,
    center = center,
  )

  scratch.crescent.reset()
  scratch.crescent.addOval(
    Rect(
      center.x - moonRadius,
      center.y - moonRadius,
      center.x + moonRadius,
      center.y + moonRadius,
    )
  )
  scratch.cutout.reset()
  scratch.cutout.addOval(
    Rect(
      center.x - moonRadius * 0.35f,
      center.y - moonRadius * 1.15f,
      center.x + moonRadius * 1.35f,
      center.y + moonRadius * 0.55f,
    )
  )
  scratch.unionResult.reset()
  scratch.unionResult.op(scratch.crescent, scratch.cutout, PathOperation.Difference)
  drawPath(scratch.unionResult, color = lerp(Color(0xFFE2E8F0), moonTint, 0.30f).copy(alpha = 0.92f))

  val star1Alpha = 0.45f + 0.5f * (0.5f + 0.5f * sin(phase * 2f * PI.toFloat()))
  val star2Alpha = 0.45f + 0.5f * (0.5f + 0.5f * cos(phase * 2f * PI.toFloat()))
  drawCircle(color = moonTint.copy(alpha = star1Alpha), radius = w * 0.045f, center = Offset(w * 0.80f, h * 0.22f))
  drawCircle(color = Color.White.copy(alpha = star2Alpha * 0.8f), radius = w * 0.030f, center = Offset(w * 0.20f, h * 0.25f))
}

private fun DrawScope.drawPartlyCloudy(w: Float, h: Float, drift: Float, accent: Color) {
  val sunCore = lerp(SunnyGold, accent, 0.18f)
  val sunCenter = Offset(w * 0.70f - drift * 0.35f, h * 0.30f)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(sunCore.copy(alpha = 0.24f), Color.Transparent),
      center = sunCenter,
      radius = w * 0.34f,
    ),
    radius = w * 0.34f,
    center = sunCenter,
  )
  drawCircle(color = sunCore.copy(alpha = 0.94f), radius = w * 0.21f, center = sunCenter)
  drawCloud(w, h, drift, lerp(CloudMist, accent, 0.16f), 0.96f, yCenterFrac = 0.67f, sizeScale = 1.05f)
}

private fun DrawScope.drawCloud(
  w: Float,
  h: Float,
  drift: Float,
  color: Color,
  alpha: Float,
  yCenterFrac: Float,
  sizeScale: Float = 1f,
) {
  val cx = w * 0.49f + drift
  val cy = h * yCenterFrac
  val baseWidth = w * 0.86f * sizeScale
  val baseHeight = h * 0.30f * sizeScale

  drawRoundRect(
    color = color.copy(alpha = 0.88f * alpha),
    topLeft = Offset(cx - baseWidth * 0.48f, cy - baseHeight * 0.25f),
    size = Size(baseWidth * 0.96f, baseHeight),
    cornerRadius = CornerRadius(baseHeight * 0.5f, baseHeight * 0.5f),
  )
  drawCircle(
    color = color.copy(alpha = 0.92f * alpha),
    radius = w * 0.24f * sizeScale,
    center = Offset(cx - w * 0.09f * sizeScale, cy - h * 0.10f * sizeScale),
  )
  drawCircle(
    color = color.copy(alpha = 0.87f * alpha),
    radius = w * 0.18f * sizeScale,
    center = Offset(cx + w * 0.17f * sizeScale, cy - h * 0.03f * sizeScale),
  )
}

private fun DrawScope.drawFog(w: Float, h: Float, drift: Float, color: Color) {
  val strokeW = (h * 0.10f).coerceAtLeast(2.2f)
  for (idx in FogRowFractions.indices) {
    val yFrac = FogRowFractions[idx]
    val dir = if (idx % 2 == 0) 1f else -1f
    val xStart = w * 0.09f + drift * dir
    val xEnd = w * 0.91f + drift * dir
    drawLine(
      color = color.copy(alpha = 0.78f - idx * 0.12f),
      start = Offset(xStart, h * yFrac),
      end = Offset(xEnd, h * yFrac),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
  }
}

private fun DrawScope.drawRainy(
  w: Float,
  h: Float,
  phase: Float,
  drift: Float,
  accent: Color,
  heavy: Boolean,
) {
  drawCloud(w, h, drift, lerp(CloudMist, accent, 0.22f), 0.96f, yCenterFrac = 0.40f)
  val dropColor = lerp(RainBlue, accent, 0.28f)
  val dropCount = if (heavy) 4 else 2
  val spacing = w * 0.58f / (dropCount.coerceAtLeast(1))
  val startX = w * 0.24f

  for (i in 0 until dropCount) {
    val dropPhase = (phase * 2.5f + i * 0.27f) % 1f
    val x = startX + i * spacing + drift * 0.3f
    val yTop = h * (0.58f + dropPhase * 0.18f)
    val len = h * 0.18f
    drawLine(
      color = dropColor.copy(alpha = 0.90f * (1f - dropPhase * 0.30f)),
      start = Offset(x, yTop),
      end = Offset(x - w * 0.06f, yTop + len),
      strokeWidth = (w * 0.072f).coerceAtLeast(2.0f),
      cap = StrokeCap.Round,
    )
  }
}

private fun DrawScope.drawSnowy(w: Float, h: Float, phase: Float, drift: Float, accent: Color) {
  drawCloud(w, h, drift, lerp(CloudMist, accent, 0.18f), 0.95f, yCenterFrac = 0.40f)
  val flakeColor = lerp(SnowIce, accent, 0.20f)
  for (idx in SnowXFractions.indices) {
    val xFrac = SnowXFractions[idx]
    val fall = (phase * 1.8f + idx * 0.33f) % 1f
    val sway = sin((fall * 2f * PI).toFloat() + idx) * (w * 0.04f)
    drawCircle(
      color = flakeColor.copy(alpha = 0.88f * (1f - fall * 0.30f)),
      radius = (w * 0.055f).coerceAtLeast(1.8f),
      center = Offset(w * xFrac + sway, h * (0.62f + fall * 0.26f)),
    )
  }
}

private fun DrawScope.drawStormy(
  w: Float,
  h: Float,
  phase: Float,
  drift: Float,
  accent: Color,
  scratch: GlyphPathScratch,
) {
  drawCloud(w, h, drift, lerp(Color(0xFF94A3B8), accent, 0.26f), 0.97f, yCenterFrac = 0.38f)
  val boltColor = lerp(SunnyGold, StormViolet, 0.25f)
  val pulseAlpha = 0.58f + 0.42f * sin(phase * 4f * PI.toFloat()).coerceAtLeast(0f)

  scratch.bolt.reset()
  scratch.bolt.moveTo(w * 0.55f, h * 0.48f)
  scratch.bolt.lineTo(w * 0.38f, h * 0.71f)
  scratch.bolt.lineTo(w * 0.52f, h * 0.71f)
  scratch.bolt.lineTo(w * 0.42f, h * 0.95f)
  scratch.bolt.lineTo(w * 0.65f, h * 0.64f)
  scratch.bolt.lineTo(w * 0.51f, h * 0.64f)
  scratch.bolt.close()
  drawPath(scratch.bolt, color = boltColor.copy(alpha = pulseAlpha))
  drawPath(scratch.bolt, color = Color.White.copy(alpha = pulseAlpha * 0.25f), style = Stroke(width = 1f))
}

@Composable
fun SunriseSunGlyph(
  accent: Color,
  modifier: Modifier = Modifier,
  size: Dp = 20.dp,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .semantics { contentDescription = "Sunrise" }
        .drawWithCache {
          val w = this.size.width
          val h = this.size.height
          val center = Offset(w * 0.5f, h * 0.5f)
          val sunColor = lerp(SunnyGold, accent, 0.26f)
          val glowBrush =
            Brush.radialGradient(
              colors = listOf(sunColor.copy(alpha = 0.28f), Color.Transparent),
              center = center,
              radius = w * 0.48f,
            )
          val rayInner = w * 0.31f
          val rayOuter = w * 0.45f
          val rayStroke = (w * 0.085f).coerceAtLeast(1.6f)
          val discRadius = w * 0.23f
          val rayStarts =
            Array(8) { i ->
              val angle = i * (PI.toFloat() / 4f)
              Offset(center.x + cos(angle) * rayInner, center.y + sin(angle) * rayInner)
            }
          val rayEnds =
            Array(8) { i ->
              val angle = i * (PI.toFloat() / 4f)
              Offset(center.x + cos(angle) * rayOuter, center.y + sin(angle) * rayOuter)
            }

          onDrawBehind {
            drawCircle(brush = glowBrush, radius = w * 0.48f, center = center)
            for (i in 0 until 8) {
              drawLine(
                color = sunColor.copy(alpha = 0.90f),
                start = rayStarts[i],
                end = rayEnds[i],
                strokeWidth = rayStroke,
                cap = StrokeCap.Round,
              )
            }
            drawCircle(color = sunColor, radius = discRadius, center = center)
          }
        }
  )
}

@Composable
fun SunsetMoonGlyph(
  accent: Color,
  modifier: Modifier = Modifier,
  size: Dp = 20.dp,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .semantics { contentDescription = "Sunset" }
        .drawWithCache {
          val w = this.size.width
          val h = this.size.height
          val center = Offset(w * 0.46f, h * 0.53f)
          val moonRadius = w * 0.36f
          val moonTint = lerp(TwilightIndigo, accent, 0.35f)
          val moonFill = lerp(Color(0xFFE2E8F0), moonTint, 0.32f).copy(alpha = 0.95f)
          val glowBrush =
            Brush.radialGradient(
              colors = listOf(moonTint.copy(alpha = 0.24f), Color.Transparent),
              center = center,
              radius = w * 0.47f,
            )
          val baseOval =
            Path().apply {
              addOval(
                Rect(
                  center.x - moonRadius,
                  center.y - moonRadius,
                  center.x + moonRadius,
                  center.y + moonRadius,
                )
              )
            }
          val cutoutOval =
            Path().apply {
              addOval(
                Rect(
                  center.x - moonRadius * 0.30f,
                  center.y - moonRadius * 1.12f,
                  center.x + moonRadius * 1.38f,
                  center.y + moonRadius * 0.56f,
                )
              )
            }
          val crescentPath =
            Path().apply {
              op(baseOval, cutoutOval, PathOperation.Difference)
            }
          val starCenter = Offset(w * 0.78f, h * 0.25f)
          val starRadius = (w * 0.055f).coerceAtLeast(1.2f)

          onDrawBehind {
            drawCircle(brush = glowBrush, radius = w * 0.47f, center = center)
            drawPath(crescentPath, color = moonFill)
            drawCircle(
              color = moonTint.copy(alpha = 0.85f),
              radius = starRadius,
              center = starCenter,
            )
          }
        }
  )
}
