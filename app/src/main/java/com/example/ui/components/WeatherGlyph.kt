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
import androidx.compose.ui.graphics.PathOperation
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
      WeatherCondition.CLOUDY ->
        drawCloud(w, h, drift, lerp(CloudMist, plasmaAccent, 0.18f), 1.0f, yCenterFrac = 0.54f)
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

private fun DrawScope.drawClearNight(w: Float, h: Float, phase: Float, accent: Color) {
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
  val result = Path().apply { op(crescent, cutout, PathOperation.Difference) }
  drawPath(result, color = lerp(Color(0xFFE2E8F0), moonTint, 0.30f).copy(alpha = 0.92f))

  val star1Alpha = 0.45f + 0.5f * (0.5f + 0.5f * sin(phase * 2f * PI.toFloat()))
  val star2Alpha = 0.45f + 0.5f * (0.5f + 0.5f * cos(phase * 2f * PI.toFloat()))
  drawCircle(color = moonTint.copy(alpha = star1Alpha), radius = w * 0.045f, center = Offset(w * 0.80f, h * 0.22f))
  drawCircle(color = Color.White.copy(alpha = star2Alpha * 0.8f), radius = w * 0.030f, center = Offset(w * 0.20f, h * 0.25f))
}

private fun DrawScope.drawPartlyCloudy(w: Float, h: Float, phase: Float, drift: Float, accent: Color) {
  val sunCore = lerp(SunnyGold, accent, 0.18f)
  val sunCenter = Offset(w * 0.70f - drift * 0.35f, h * 0.32f)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(sunCore.copy(alpha = 0.24f), Color.Transparent),
      center = sunCenter,
      radius = w * 0.34f,
    ),
    radius = w * 0.34f,
    center = sunCenter,
  )
  drawCircle(color = sunCore.copy(alpha = 0.94f), radius = w * 0.20f, center = sunCenter)
  drawCloud(w, h, drift, lerp(CloudMist, accent, 0.16f), 0.96f, yCenterFrac = 0.64f)
}

private fun DrawScope.drawCloud(
  w: Float,
  h: Float,
  drift: Float,
  color: Color,
  alpha: Float,
  yCenterFrac: Float,
) {
  val cx = w * 0.49f + drift
  val cy = h * yCenterFrac
  val baseWidth = w * 0.86f
  val baseHeight = h * 0.30f

  drawRoundRect(
    color = color.copy(alpha = 0.88f * alpha),
    topLeft = Offset(cx - baseWidth * 0.48f, cy - baseHeight * 0.25f),
    size = Size(baseWidth * 0.96f, baseHeight),
    cornerRadius = CornerRadius(baseHeight * 0.5f, baseHeight * 0.5f),
  )
  drawCircle(
    color = color.copy(alpha = 0.92f * alpha),
    radius = w * 0.24f,
    center = Offset(cx - w * 0.09f, cy - h * 0.10f),
  )
  drawCircle(
    color = color.copy(alpha = 0.87f * alpha),
    radius = w * 0.18f,
    center = Offset(cx + w * 0.17f, cy - h * 0.03f),
  )
}

private fun DrawScope.drawFog(w: Float, h: Float, drift: Float, color: Color) {
  val strokeW = (h * 0.10f).coerceAtLeast(2.2f)
  val rows = listOf(0.28f, 0.50f, 0.72f)
  rows.forEachIndexed { idx, yFrac ->
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
  val xs = listOf(0.24f, 0.50f, 0.76f)
  xs.forEachIndexed { idx, xFrac ->
    val fall = (phase * 1.8f + idx * 0.33f) % 1f
    val sway = sin((fall * 2f * PI).toFloat() + idx) * (w * 0.04f)
    drawCircle(
      color = flakeColor.copy(alpha = 0.88f * (1f - fall * 0.30f)),
      radius = (w * 0.055f).coerceAtLeast(1.8f),
      center = Offset(w * xFrac + sway, h * (0.62f + fall * 0.26f)),
    )
  }
}

private fun DrawScope.drawStormy(w: Float, h: Float, phase: Float, drift: Float, accent: Color) {
  drawCloud(w, h, drift, lerp(Color(0xFF94A3B8), accent, 0.26f), 0.97f, yCenterFrac = 0.38f)
  val boltColor = lerp(SunnyGold, StormViolet, 0.25f)
  val pulseAlpha = 0.58f + 0.42f * sin(phase * 4f * PI.toFloat()).coerceAtLeast(0f)

  val bolt =
    Path().apply {
      moveTo(w * 0.55f, h * 0.48f)
      lineTo(w * 0.38f, h * 0.71f)
      lineTo(w * 0.52f, h * 0.71f)
      lineTo(w * 0.42f, h * 0.95f)
      lineTo(w * 0.65f, h * 0.64f)
      lineTo(w * 0.51f, h * 0.64f)
      close()
    }
  drawPath(bolt, color = boltColor.copy(alpha = pulseAlpha))
  drawPath(bolt, color = Color.White.copy(alpha = pulseAlpha * 0.25f), style = Stroke(width = 1f))
}
