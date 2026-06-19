package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Saffron,
    onPrimary = White,
    primaryContainer = SaffronLight,
    onPrimaryContainer = Saffron,
    secondary = Navy,
    onSecondary = White,
    secondaryContainer = Cream,
    onSecondaryContainer = Navy,
    tertiary = Green,
    onTertiary = White,
    tertiaryContainer = GreenLight,
    onTertiaryContainer = Green,
    background = Cream,
    surface = White,
    onBackground = Navy,
    onSurface = Navy,
    outline = Muted,
    surfaceVariant = Cream,
    onSurfaceVariant = NavyMid
)

private val DarkColorScheme = darkColorScheme(
    primary = SaffronMid,
    onPrimary = Navy,
    primaryContainer = NavyMid,
    onPrimaryContainer = SaffronLight,
    secondary = Saffron,
    onSecondary = White,
    secondaryContainer = Navy,
    onSecondaryContainer = Cream,
    tertiary = GreenMid,
    onTertiary = Navy,
    onBackground = Cream,
    background = Navy,
    surface = NavyMid,
    onSurface = Cream,
    outline = Muted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
