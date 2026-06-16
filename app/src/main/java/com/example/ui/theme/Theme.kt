package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = RedPrimary,
    secondary = RedSecondary,
    tertiary = NeonGold,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onBackground = WhiteText,
    onSurface = WhiteText,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = GreyText
  )

private val LightColorScheme =
  lightColorScheme(
    primary = RedPrimary,
    secondary = RedDark,
    tertiary = NeonGold,
    background = DarkBg, // Keep deep gaming dark background even in light theme for gaming consistency
    surface = DarkSurface,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onBackground = WhiteText,
    onSurface = WhiteText,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = GreyText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For esports aesthetics, disable system-wide dynamic colors by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> DarkColorScheme // Enforce stunning dark esports mode by default
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
