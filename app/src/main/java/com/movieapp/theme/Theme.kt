package com.movieapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Global theme controller managing Theme Mode (Light / Dark).
 * Both Light and Dark themes natively support eye-comfort / night light tones.
 */
object AppThemeController {
    var isDarkMode by mutableStateOf(false)

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }
}

val LocalNeoColors = staticCompositionLocalOf { LightNeoColors }

val MaterialTheme.neoColors: NeoColors
    @Composable
    @ReadOnlyComposable
    get() = LocalNeoColors.current

@Composable
fun MovieAppTheme(
    darkTheme: Boolean = AppThemeController.isDarkMode,
    content: @Composable () -> Unit
) {
    val neoColors = if (darkTheme) DarkNeoColors else LightNeoColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = neoColors.primary,
            onPrimary = neoColors.onPrimary,
            secondary = neoColors.secondary,
            onSecondary = neoColors.onSecondary,
            tertiary = neoColors.tertiary,
            onTertiary = NeoBlack,
            background = neoColors.background,
            onBackground = neoColors.textPrimary,
            surface = neoColors.surface,
            onSurface = neoColors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = neoColors.primary,
            onPrimary = neoColors.onPrimary,
            secondary = neoColors.secondary,
            onSecondary = neoColors.onSecondary,
            tertiary = neoColors.tertiary,
            onTertiary = NeoBlack,
            background = neoColors.background,
            onBackground = neoColors.textPrimary,
            surface = neoColors.surface,
            onSurface = neoColors.textPrimary
        )
    }

    CompositionLocalProvider(
        LocalNeoColors provides neoColors,
        LocalContentColor provides neoColors.textPrimary
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
