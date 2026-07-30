package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.BrowserSettings

// ColorOS 16 Aquamorphic Emerald
val EmeraldLightPrimary = Color(0xFF00897B)
val EmeraldLightSecondary = Color(0xFF00695C)
val EmeraldLightBackground = Color(0xFFF0FDFB)
val EmeraldLightSurface = Color(0xFFFFFFFF)
val EmeraldLightOnPrimary = Color(0xFFFFFFFF)

val EmeraldDarkPrimary = Color(0xFF1DE9B6)
val EmeraldDarkSecondary = Color(0xFF00BFA5)
val EmeraldDarkBackground = Color(0xFF0D1614)
val EmeraldDarkSurface = Color(0xFF162220)
val EmeraldDarkOnPrimary = Color(0xFF0D1614)

// Ocean Blue
val BlueLightPrimary = Color(0xFF0288D1)
val BlueLightSecondary = Color(0xFF0277BD)
val BlueLightBackground = Color(0xFFF1F9FD)
val BlueLightSurface = Color(0xFFFFFFFF)

val BlueDarkPrimary = Color(0xFF40C4FF)
val BlueDarkSecondary = Color(0xFF00B0FF)
val BlueDarkBackground = Color(0xFF0A121A)
val BlueDarkSurface = Color(0xFF131F2A)

// Sunset Orange
val OrangeLightPrimary = Color(0xFFE65100)
val OrangeLightSecondary = Color(0xFFEF6C00)
val OrangeLightBackground = Color(0xFFFFF6F0)
val OrangeLightSurface = Color(0xFFFFFFFF)

val OrangeDarkPrimary = Color(0xFFFFAB40)
val OrangeDarkSecondary = Color(0xFFFF6D00)
val OrangeDarkBackground = Color(0xFF1C130F)
val OrangeDarkSurface = Color(0xFF281C16)

// Cyber Lavender
val LavenderLightPrimary = Color(0xFF6A1B9A)
val LavenderLightSecondary = Color(0xFF4A148C)
val LavenderLightBackground = Color(0xFFFAF3FD)
val LavenderLightSurface = Color(0xFFFFFFFF)

val LavenderDarkPrimary = Color(0xFFE1bee7)
val LavenderDarkSecondary = Color(0xFFCE93D8)
val LavenderDarkBackground = Color(0xFF17101C)
val LavenderDarkSurface = Color(0xFF231A2A)

// Obsidian Slate
val SlateLightPrimary = Color(0xFF263238)
val SlateLightSecondary = Color(0xFF37474F)
val SlateLightBackground = Color(0xFFF3F5F6)
val SlateLightSurface = Color(0xFFFFFFFF)

val SlateDarkPrimary = Color(0xFF90A4AE)
val SlateDarkSecondary = Color(0xFFB0BEC5)
val SlateDarkBackground = Color(0xFF121417)
val SlateDarkSurface = Color(0xFF1E2125)

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
  )

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

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
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

