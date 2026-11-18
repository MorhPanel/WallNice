package com.morhpanel.nekowall.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AmoledBlack.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = WallNiceShapes,
        content = content
    )
}
