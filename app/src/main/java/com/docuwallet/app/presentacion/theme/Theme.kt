package com.docuwallet.app.presentacion.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===== ESQUEMA DE COLORES OSCURO =====
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = TextOnPrimary,
    secondary = AccentOrange,
    onSecondary = TextOnPrimary,
    tertiary = CategoryTravel,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    error = ErrorRed,
    onError = TextOnPrimary
)

// ===== ESQUEMA DE COLORES CLARO =====
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = InfoBlueLight,
    onPrimaryContainer = PrimaryBlueDark,

    secondary = AccentOrange,
    onSecondary = TextOnPrimary,
    secondaryContainer = WarningOrangeLight,
    onSecondaryContainer = AccentOrangeDark,

    tertiary = CategoryTravel,
    onTertiary = TextOnPrimary,

    background = BackgroundWhite,
    onBackground = TextPrimary,

    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = TextOnPrimary,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRed,

    outline = BorderLight,
    outlineVariant = BorderMedium
)

// ===== TEMA PRINCIPAL DE DOCUWALLET =====
@Composable
fun MyApplicationDocuWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
