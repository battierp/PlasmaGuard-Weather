package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PlasmaDarkColorScheme =
  darkColorScheme(
    primary = AuroraCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = ElevatedSurface,
    onPrimaryContainer = SoftFrostText,
    secondary = TwilightIndigo,
    onSecondary = ObsidianBlack,
    secondaryContainer = DeepCardSurface,
    onSecondaryContainer = SoftFrostText,
    tertiary = WarmAmber,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = SoftFrostText,
    surface = MidnightSlate,
    onSurface = SoftFrostText,
    surfaceVariant = DeepCardSurface,
    onSurfaceVariant = MutedSlateText,
    outline = SubtleOutline,
    error = Color(0xFFF87171),
    onError = ObsidianBlack,
  )

@Composable
fun PlasmaSkyTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = PlasmaDarkColorScheme,
    typography = PlasmaTypography,
    content = content,
  )
}
