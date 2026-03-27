package com.animevost.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = OrangePrimary,
    onPrimary        = TextOnOrange,
    primaryContainer = OrangeMuted,
    onPrimaryContainer = OrangeBright,
    secondary        = AccentBlue,
    onSecondary      = Bg0,
    tertiary         = AccentPurple,
    onTertiary       = Bg0,
    background       = Bg1,
    onBackground     = TextPrimary,
    surface          = Bg2,
    onSurface        = TextPrimary,
    surfaceVariant   = Bg3,
    onSurfaceVariant = TextSecondary,
    outline          = Bg4,
    scrim            = ModalScrim,
    error            = ErrorRed,
    onError          = OnError,
)

private val LightColorScheme = lightColorScheme(
    primary          = OrangeDim,
    onPrimary        = OnError,
    primaryContainer = OrangeBright,
    onPrimaryContainer = TextOnOrange,
    secondary        = AccentBlue,
    tertiary         = AccentPurple,
    background       = LightBackground,
    onBackground     = OnLightBg,
    surface          = LightSurface,
    onSurface        = OnLightBg,
    surfaceVariant   = LightSurfaceVar,
    onSurfaceVariant = OnLightBg,
    error            = ErrorRed,
    onError          = OnError,
)

val AnimeVostShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AnimeVostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // App is dark-first; light theme is kept for future optional toggle
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AnimeVostTypography,
        shapes      = AnimeVostShapes,
        content     = content,
    )
}
