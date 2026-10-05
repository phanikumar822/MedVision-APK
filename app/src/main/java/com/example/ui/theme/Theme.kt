package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClinicalLightColorScheme = lightColorScheme(
    primary = MedicalPrimary,
    onPrimary = Color.White,
    primaryContainer = MedicalPrimaryLight,
    onPrimaryContainer = MedicalPrimaryDark,
    secondary = DarkNavy,
    onSecondary = Color.White,
    secondaryContainer = SurfaceSubtle,
    onSecondaryContainer = SlateSecondary,
    tertiary = SlateSecondary,
    onTertiary = Color.White,
    background = BackgroundClinical,
    onBackground = DarkNavy,
    surface = SurfaceWhite,
    onSurface = DarkNavy,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = SlateSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderStrong,
    error = StatusCritical,
    onError = Color.White,
    errorContainer = StatusCriticalBg,
    onErrorContainer = StatusCritical
)

private val ClinicalDarkColorScheme = darkColorScheme(
    primary = MedicalPrimary,
    onPrimary = Color.White,
    primaryContainer = MedicalPrimaryDark,
    onPrimaryContainer = MedicalPrimaryLight,
    secondary = Color(0xFF94A3B8),
    onSecondary = DarkNavy,
    secondaryContainer = PacsCardSurface,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = SlateSecondary,
    onTertiary = Color.White,
    background = PacsCanvasBlack,
    onBackground = Color(0xFFF1F5F9),
    surface = PacsCardSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = SlateSecondary,
    outline = PacsBorderDark,
    outlineVariant = Color(0xFF334155),
    error = StatusCritical,
    onError = Color.White,
    errorContainer = StatusCriticalBg,
    onErrorContainer = StatusCritical
)

@Composable
fun MedVisionAITheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ClinicalDarkColorScheme else ClinicalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MedVisionAITheme(darkTheme = false, content = content)
}
