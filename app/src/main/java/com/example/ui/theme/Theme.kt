package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SpaceCyanPrimary,
    onPrimary = Color(0xFF001A24),
    primaryContainer = SpaceCyanContainer,
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = SpaceAmber,
    onSecondary = Color(0xFF261A00),
    secondaryContainer = Color(0xFF402E00),
    tertiary = SpaceGold,
    background = SpaceDarkBg,
    onBackground = SpaceWhite,
    surface = SpaceDarkSurface,
    onSurface = SpaceWhite,
    surfaceVariant = SpaceDarkSurfaceVariant,
    onSurfaceVariant = SpaceMuted,
    error = SpaceRedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
