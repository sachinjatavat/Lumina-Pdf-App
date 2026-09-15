package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LuminaPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFF1F5F9),
    tertiary = LuminaAccent,
    background = LuminaBgDark,
    surface = LuminaSurfaceDark,
    surfaceVariant = LuminaSurfaceVariantDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    outline = LuminaOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = LuminaPrimary,
    onPrimary = Color.White,
    primaryContainer = LuminaPrimaryContainer,
    onPrimaryContainer = LuminaOnPrimaryContainer,
    secondary = LuminaSecondary,
    onSecondary = Color.White,
    secondaryContainer = LuminaSecondaryContainer,
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = LuminaAccent,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9)
)

@Composable
fun LuminaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Pure white/light theme enforced as requested
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

// Alias to maintain compatibility if referenced
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LuminaTheme(darkTheme = false, dynamicColor = false, content = content)
}
