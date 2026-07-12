package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = RoyalNavy,
    primaryContainer = GoldDark,
    onPrimaryContainer = RoyalNavy,
    secondary = RoyalBlue,
    onSecondary = WarmIvory,
    background = RoyalNavy,
    onBackground = WarmIvory,
    surface = VelvetSurface,
    onSurface = WarmIvory,
    error = CoralRed,
    onError = WarmIvory
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = RoyalNavy,
    primaryContainer = RoyalGold,
    onPrimaryContainer = RoyalNavy,
    secondary = RoyalBlue,
    onSecondary = RoyalNavy,
    background = WarmIvory,
    onBackground = RoyalNavy,
    surface = VelvetSurface,
    onSurface = WarmIvory,
    error = CoralRed,
    onError = WarmIvory
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default for immersive game aesthetics
    dynamicColor: Boolean = false, // Disable dynamic system colors to preserve game brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
