package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CineDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = ElectricVioletDark,
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = ElectricVioletLight,
    onSecondary = Color(0xFF1E1035),
    secondaryContainer = SurfaceCard,
    onSecondaryContainer = TextPrimary,
    tertiary = RatingGold,
    onTertiary = Color(0xFF452B00),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceElevated,
    outline = BorderSubtle,
    outlineVariant = DividerDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Dark theme is default as per design requirement
    dynamicColor: Boolean = false, // Keep intentional cinematic branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CineDarkColorScheme,
        typography = Typography,
        content = content
    )
}
