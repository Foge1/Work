package com.loaderapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald80,
    onPrimary = Color(0xFF003840),
    primaryContainer = Color(0xFF004F5A),
    onPrimaryContainer = Color(0xFFB2EEF8),
    secondary = EmeraldGrey80,
    onSecondary = Color(0xFF003840),
    tertiary = Mint80,
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF3F484D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EEF8),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = EmeraldGrey40,
    onSecondary = Color.White,
    tertiary = Mint40,
    background = Color(0xFFF5FBFC),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF5FBFC),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFD8E8EC),
    onSurfaceVariant = Color(0xFF3F484D),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun LoaderAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
