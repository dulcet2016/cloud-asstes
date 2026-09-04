package com.manha.eventassettracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    secondary = BrandGreen,
    onSecondary = Color.White,
    background = SurfaceLight,
    surface = SurfaceCard,
    error = BrandRed,
    onBackground = InkPrimary,
    onSurface = InkPrimary,
    outline = LineColor
)

@Composable
fun EventAssetTrackerTheme(
    content: @Composable () -> Unit
) {
    // Always light — a dark theme was explicitly ruled out for this app.
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
