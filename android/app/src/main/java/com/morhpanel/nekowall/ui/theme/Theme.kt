package com.morhpanel.nekowall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkColorScheme = darkColorScheme(
    primary = NeonPink,
    secondary = NeonCyan,
    background = AmoledBlack,
    surface = SurfaceMid,
    surfaceVariant = SurfaceHigh,
    onPrimary = TextPrimary,
    onSecondary = AmoledBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = OutlineStrong,
    scrim = AmoledBlack
)

private val WallNiceShapes = Shapes(
    small = RoundedCornerShape(WallNiceRadii.small),
    medium = RoundedCornerShape(WallNiceRadii.medium),
    large = RoundedCornerShape(WallNiceRadii.large)
)

@Composable
fun NekoWallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = WallNiceShapes,
        content = content
    )
}
