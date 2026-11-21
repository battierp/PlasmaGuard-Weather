package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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

@Composable
fun WeatherGlyph(
  condition: WeatherCondition,
  plasmaAccent: Color,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  animated: Boolean = true,
) {
  val transition = rememberInfiniteTransition(label = "weather_glyph")
  val phase by
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
  val activePhase = if (animated) phase else 0f

  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val drift = sin(activePhase * 2f * PI.toFloat()) * (w * 0.025f)

    when (condition) {
      WeatherCondition.SUNNY -> drawSunny(w, h, activePhase, plasmaAccent)
      WeatherCondition.CLEAR_NIGHT -> drawClearNight(w, h, activePhase, plasmaAccent)
      WeatherCondition.PARTLY_CLOUDY -> drawPartlyCloudy(w, h, activePhase, drift, plasmaAccent)
      WeatherCondition.CLOUDY -> drawCloud(w, h, drift, lerp(CloudMist, plasmaAccent, 0.18f), 1.0f)
      WeatherCondition.FOGGY -> drawFog(w, h, drift, lerp(CloudMist, plasmaAccent, 0.25f))
      WeatherCondition.DRIZZLE -> drawRainy(w, h, activePhase, drift, plasmaAccent, heavy = false)
      WeatherCondition.RAINY -> drawRainy(w, h, activePhase, drift, plasmaAccent, heavy = true)
      WeatherCondition.SNOWY -> drawSnowy(w, h, activePhase, drift, plasmaAccent)
      WeatherCondition.STORMY -> drawStormy(w, h, activePhase, drift, plasmaAccent)
    }
  }
}

private fun DrawScope.drawSunny(w: Float, h: Float, phase: Float, accent: Color) {
  val center = Offset(w * 0.5f, h * 0.5f)
  val sunCore = lerp(SunnyGold, accent, 0.22f)
  val pulse = 1f + 0.04f * sin(phase * 2f * PI.toFloat())

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(sunCore.copy(alpha = 0.26f), Color.Transparent),
      center = center,
      radius = w * 0.48f * pulse,
    ),
    radius = w * 0.48f * pulse,
    center = center,
  )

  val rayInner = w * 0.27f
  val rayOuter = w * 0.38f * pulse
  val rotationOffset = phase * (2f * PI.toFloat() / 8f)
  for (i in 0 until 8) {
    val angle = rotationOffset + i * (PI.toFloat() / 4f)
    drawLine(
      color = sunCore.copy(alpha = 0.78f),
      start = Offset(center.x + cos(angle) * rayInner, center.y + sin(angle) * rayInner),
      end = Offset(center.x + cos(angle) * rayOuter, center.y + sin(angle) * rayOuter),
      strokeWidth = (w * 0.055f).coerceAtLeast(1.8f),
      cap = StrokeCap.Round,
    )
  }

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(sunCore.copy(alpha = 0.95f), lerp(sunCore, Color(0xFFD97706), 0.35f)),
      center = Offset(center.x - w * 0.05f, center.y - h * 0.05f),
      radius = w * 0.24f,
    ),
    radius = w * 0.20f,
    center = center,
  )
}

private fun DrawScope.drawClearNight(w: Float, h: Float, phase: Float, accent: Color) {
  val moonTint = lerp(TwilightIndigo, accent, 0.35f)
  val center = Offset(w * 0.48f, h * 0.52f)
  val moonRadius = w * 0.24f

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(moonTint.copy(alpha = 0.22f), Color.Transparent),
      center = center,
      radius = w * 0.46f,
    ),
    radius = w * 0.46f,
    center = center,
  )

  // Crescent via layered path approximation using arcs/circles
  val crescent =
    Path().apply {
      addOval(
        androidx.compose.ui.geometry.Rect(
          center.x - moonRadius,
          center.y - moonRadius,
          center.x + moonRadius,
          center.y + moonRadius,
        )
      )
    }
  val cutout =
    Path().apply {
      addOval(
        androidx.compose.ui.geometry.Rect(
          center.x - moonRadius * 0.35f,
          center.y - moonRadius * 1.15f,
          center.x + moonRadius * 1.35f,
          center.y + moonRadius * 0.55f,
        )
      )
    }
  val result = Path().apply { op(crescent, cutout, androidx.compose.ui.graphics.PathOperation.Difference) }
  drawPath(result, color = lerp(Color(0xFFE2E8F0), moonTint, 0.30f).copy(alpha = 0.9f))

  // Two gentle twinkling stars
  val star1Alpha = 0.4f + 0.5f * (0.5f + 0.5f * sin(phase * 2f * PI.toFloat()))
  val star2Alpha = 0.4f + 0.5f * (0.5f + 0.5f * cos(phase * 2f * PI.toFloat()))
  drawCircle(color = moonTint.copy(alpha = star1Alpha), radius = w * 0.032f, center = Offset(w * 0.74f, h * 0.28f))
  drawCircle(color = Color.White.copy(alpha = star2Alpha * 0.75f), radius = w * 0.022f, center = Offset(w * 0.26f, h * 0.30f))
}

private fun DrawScope.drawPartlyCloudy(w: Float, h: Float, phase: Float, drift: Float, accent: Color) {
  val sunCore = lerp(SunnyGold, accent, 0.18f)
  val sunCenter = Offset(w * 0.64f - drift * 0.4f, h * 0.36f)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(sunCore.copy(alpha = 0.24f), Color.Transparent),
      center = sunCenter,
      radius = w * 0.34f,
    ),
    radius = w * 0.34f,
    center = sunCenter,
  )
  drawCircle(color = sunCore.copy(alpha = 0.92f), radius = w * 0.16f, center = sunCenter)
  drawCloud(w, h * 1.06f, drift, lerp(CloudMist, accent, 0.16f), 0.95f)
}

private fun DrawScope.drawCloud(w: Float, h: Float, drift: Float, color: Color, alpha: Float) {
  val cx = w * 0.48f + drift
  val cy = h * 0.58f
  val baseWidth = w * 0.58f
  val baseHeight = h * 0.24f

  drawRoundRect(
    color = color.copy(alpha = 0.86f * alpha),
    topLeft = Offset(cx - baseWidth * 0.46f, cy - baseHeight * 0.35f),
    size = Size(baseWidth * 0.92f, baseHeight),
    cornerRadius = CornerRadius(baseHeight * 0.5f, baseHeight * 0.5f),
  )
  drawCircle(
    color = color.copy(alpha = 0.90f * alpha),
    radius = w * 0.17f,
    center = Offset(cx - w * 0.06f, cy - h * 0.08f),
  )
  drawCircle(
    color = color.copy(alpha = 0.85f * alpha),
    radius = w * 0.13f,
    center = Offset(cx + w * 0.12f, cy - h * 0.02f),
  )
}

private fun DrawScope.drawFog(w: Float, h: Float, drift: Float, color: Color) {
  val strokeW = (h * 0.065f).coerceAtLeast(2f)
  val rows = listOf(0.34f, 0.50f, 0.66f)
  rows.forEachIndexed { idx, yFrac ->
    val dir = if (idx % 2 == 0) 1f else -1f
    val xStart = w * 0.18f + drift * dir
    val xEnd = w * 0.82f + drift * dir
    drawLine(
      color = color.copy(alpha = 0.72f - idx * 0.12f),
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
  drawCloud(w, h * 0.86f, drift, lerp(CloudMist, accent, 0.22f), 0.95f)
  val dropColor = lerp(RainBlue, accent, 0.28f)
  val dropCount = if (heavy) 4 else 2
  val spacing = w * 0.46f / (dropCount.coerceAtLeast(1))
  val startX = w * 0.30f

  for (i in 0 until dropCount) {
    val dropPhase = (phase * 2.5f + i * 0.27f) % 1f
    val x = startX + i * spacing + drift * 0.3f
    val yTop = h * (0.60f + dropPhase * 0.16f)
    val len = h * 0.13f
    drawLine(
      color = dropColor.copy(alpha = 0.88f * (1f - dropPhase * 0.35f)),
      start = Offset(x, yTop),
      end = Offset(x - w * 0.04f, yTop + len),
      strokeWidth = (w * 0.055f).coerceAtLeast(1.8f),
      cap = StrokeCap.Round,
    )
  }
}

private fun DrawScope.drawSnowy(w: Float, h: Float, phase: Float, drift: Float, accent: Color) {
  drawCloud(w, h * 0.86f, drift, lerp(CloudMist, accent, 0.18f), 0.94f)
  val flakeColor = lerp(SnowIce, accent, 0.20f)
  val xs = listOf(0.32f, 0.50f, 0.68f)
  xs.forEachIndexed { idx, xFrac ->
    val fall = (phase * 1.8f + idx * 0.33f) % 1f
    val sway = sin((fall * 2f * PI).toFloat() + idx) * (w * 0.03f)
    drawCircle(
      color = flakeColor.copy(alpha = 0.85f * (1f - fall * 0.35f)),
      radius = (w * 0.038f).coerceAtLeast(1.6f),
      center = Offset(w * xFrac + sway, h * (0.63f + fall * 0.22f)),
    )
  }
}

private fun DrawScope.drawStormy(w: Float, h: Float, phase: Float, drift: Float, accent: Color) {
  drawCloud(w, h * 0.86f, drift, lerp(Color(0xFF94A3B8), accent, 0.26f), 0.96f)
  val boltColor = lerp(SunnyGold, StormViolet, 0.25f)
  val pulseAlpha = 0.55f + 0.45f * sin(phase * 4f * PI.toFloat()).coerceAtLeast(0f)

  val bolt =
    Path().apply {
      moveTo(w * 0.53f, h * 0.54f)
      lineTo(w * 0.43f, h * 0.70f)
      lineTo(w * 0.52f, h * 0.70f)
      lineTo(w * 0.45f, h * 0.87f)
      lineTo(w * 0.60f, h * 0.66f)
      lineTo(w * 0.51f, h * 0.66f)
      close()
    }
  drawPath(bolt, color = boltColor.copy(alpha = pulseAlpha))
  drawPath(bolt, color = Color.White.copy(alpha = pulseAlpha * 0.25f), style = Stroke(width = 1f))
}
