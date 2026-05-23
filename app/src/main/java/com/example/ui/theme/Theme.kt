package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.BrowserSettings

@Composable
fun SearchAppTheme(
    settings: BrowserSettings,
    content: @Composable () -> Unit
) {
    val darkTheme = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        when (settings.customThemeColor) {
            1 -> darkColorScheme(
                primary = BlueDarkPrimary,
                secondary = BlueDarkSecondary,
                background = BlueDarkBackground,
                surface = BlueDarkSurface,
                onPrimary = BlueDarkBackground,
                onSurface = Color.White
            )
            2 -> darkColorScheme(
                primary = OrangeDarkPrimary,
                secondary = OrangeDarkSecondary,
                background = OrangeDarkBackground,
                surface = OrangeDarkSurface,
                onPrimary = OrangeDarkBackground,
                onSurface = Color.White
            )
            3 -> darkColorScheme(
                primary = LavenderDarkPrimary,
                secondary = LavenderDarkSecondary,
                background = LavenderDarkBackground,
                surface = LavenderDarkSurface,
                onPrimary = LavenderDarkBackground,
                onSurface = Color.White
            )
            4 -> darkColorScheme(
                primary = SlateDarkPrimary,
                secondary = SlateDarkSecondary,
                background = SlateDarkBackground,
                surface = SlateDarkSurface,
                onPrimary = SlateDarkBackground,
                onSurface = Color.White
            )
            else -> darkColorScheme( // Emerald Green (ColorOS 16 Signature)
                primary = EmeraldDarkPrimary,
                secondary = EmeraldDarkSecondary,
                background = EmeraldDarkBackground,
                surface = EmeraldDarkSurface,
                onPrimary = EmeraldDarkBackground,
                onSurface = Color.White
            )
        }
    } else {
        when (settings.customThemeColor) {
            1 -> lightColorScheme(
                primary = BlueLightPrimary,
                secondary = BlueLightSecondary,
                background = BlueLightBackground,
                surface = BlueLightSurface,
                onPrimary = Color.White,
                onSurface = Color(0xFF131F2A)
            )
            2 -> lightColorScheme(
                primary = OrangeLightPrimary,
                secondary = OrangeLightSecondary,
                background = OrangeLightBackground,
                surface = OrangeLightSurface,
                onPrimary = Color.White,
                onSurface = Color(0xFF281C16)
            )
            3 -> lightColorScheme(
                primary = LavenderLightPrimary,
                secondary = LavenderLightSecondary,
                background = LavenderLightBackground,
                surface = LavenderLightSurface,
                onPrimary = Color.White,
                onSurface = Color(0xFF231A2A)
            )
            4 -> lightColorScheme(
                primary = SlateLightPrimary,
                secondary = SlateLightSecondary,
                background = SlateLightBackground,
                surface = SlateLightSurface,
                onPrimary = Color.White,
                onSurface = Color(0xFF1E2125)
            )
            else -> lightColorScheme( // Emerald Green
                primary = EmeraldLightPrimary,
                secondary = EmeraldLightSecondary,
                background = EmeraldLightBackground,
                surface = EmeraldLightSurface,
                onPrimary = Color.White,
                onSurface = Color(0xFF0D1614)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
