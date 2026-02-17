package com.loaderapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD8E8),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F5A),
    onPrimaryContainer = Color(0xFFB2F0F8),
    secondary = Color(0xFF5EC8A0),
    onSecondary = Color(0xFF003826),
    tertiary = Color(0xFFA8DCE8),
    background = Color(0xFF0F1416),
    onBackground = Color(0xFFE4E8EA),
    surface = Color(0xFF161C1F),
    onSurface = Color(0xFFE4E8EA),
    surfaceVariant = Color(0xFF253238),
    onSurfaceVariant = Color(0xFFB8C4C8),
    outline = Color(0xFF4A5C62),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0097A7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBF2F8),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF27AE60),
    onSecondary = Color.White,
    tertiary = Color(0xFF00838F),
    background = Color(0xFFF4FAFB),
    onBackground = Color(0xFF161C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF161C1E),
    surfaceVariant = Color(0xFFE0F0F3),
    onSurfaceVariant = Color(0xFF3D5055),
    outline = Color(0xFF6F8D93),
    error = Color(0xFFE53935),
    onError = Color.White
)

@Composable
fun LoaderAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
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
    MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = AppShapes, content = content)
}
