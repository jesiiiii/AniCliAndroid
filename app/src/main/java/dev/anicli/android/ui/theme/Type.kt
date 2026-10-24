package dev.anicli.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// The reference app pairs a display face with a body face. System faces keep
// Arabic shaping intact and avoid shipping font binaries; the weights and
// hierarchy match the reference Typography contract.
private val display = FontFamily.SansSerif
private val body = FontFamily.SansSerif
private val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    displayMedium = baseline.displayMedium.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    displaySmall = baseline.displaySmall.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    titleLarge = baseline.titleLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),
    titleMedium = baseline.titleMedium.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontFamily = display, fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = body, fontWeight = FontWeight.Medium),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = body, fontWeight = FontWeight.Medium),
    bodySmall = baseline.bodySmall.copy(fontFamily = body, fontWeight = FontWeight.Medium),
    labelLarge = baseline.labelLarge.copy(fontFamily = body, fontWeight = FontWeight.SemiBold),
    labelMedium = baseline.labelMedium.copy(fontFamily = body, fontWeight = FontWeight.Medium),
    labelSmall = baseline.labelSmall.copy(fontFamily = body, fontWeight = FontWeight.Medium)
)
