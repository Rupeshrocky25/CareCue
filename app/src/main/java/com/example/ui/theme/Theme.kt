package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CareCueDarkColorScheme = darkColorScheme(
    primary = SunsetOrangePrimary,
    onPrimary = Color(0xFF1F0B00),
    primaryContainer = SunsetOrangeContainer,
    onPrimaryContainer = SunsetOrangeOnContainer,
    secondary = IrisAccent,
    onSecondary = Color(0xFF0F102B),
    secondaryContainer = IrisContainer,
    onSecondaryContainer = PureWhite,
    tertiary = SunsetOrangeSecondary,
    onTertiary = Color(0xFF1F0B00),
    background = ObsidianDarkest,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = SlateMuted,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle,
    error = StatusOverdue,
    onError = Color.White
)

@Composable
fun CareCueTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = CareCueDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianDarkest.toArgb()
                window.navigationBarColor = ObsidianDarkest.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
