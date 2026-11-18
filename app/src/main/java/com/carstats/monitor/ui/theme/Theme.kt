package com.carstats.monitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = CockpitBackground,
    secondary = SportAmber,
    onSecondary = CockpitBackground,
    tertiary = EcoGreen,
    background = CockpitBackground,
    surface = CockpitSurface,
    surfaceVariant = CockpitSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = SportRed
)

@Composable
fun CarStatsMonitorTheme(
    darkTheme: Boolean = true, // Завжди темна спортивна тема
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
