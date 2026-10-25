package dev.anicli.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Palette lifted 1:1 from the reference project (Anime_App warm cream/gold).
val primaryLight = Color(0xFF27345D)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFDDE3FF)
val onPrimaryContainerLight = Color(0xFF101A3C)
val secondaryLight = Color(0xFF5B6078)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFE5E7F2)
val onSecondaryContainerLight = Color(0xFF20243A)
val tertiaryLight = Color(0xFFB94D42)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFFFDAD3)
val onTertiaryContainerLight = Color(0xFF3F0806)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFFAF8F4)
val onBackgroundLight = Color(0xFF191B24)
val surfaceLight = Color(0xFFFAF8F4)
val onSurfaceLight = Color(0xFF191B24)
val surfaceVariantLight = Color(0xFFECEAF0)
val onSurfaceVariantLight = Color(0xFF555765)
val outlineLight = Color(0xFF777986)
val outlineVariantLight = Color(0xFFD7D7E0)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF2E303B)
val inverseOnSurfaceLight = Color(0xFFF1F0F7)
val inversePrimaryLight = Color(0xFFBCC7FF)
val surfaceDimLight = Color(0xFFE5E2DC)
val surfaceBrightLight = Color(0xFFFAF8F4)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF4F1EC)
val surfaceContainerLight = Color(0xFFEFEDF0)
val surfaceContainerHighLight = Color(0xFFE9E7EC)
val surfaceContainerHighestLight = Color(0xFFE3E1E8)

val primaryDark = Color(0xFFB9B5FF)
val onPrimaryDark = Color(0xFF211D52)
val primaryContainerDark = Color(0xFF312E81)
val onPrimaryContainerDark = Color(0xFFE0E7FF)
val secondaryDark = Color(0xFFC8C5E8)
val onSecondaryDark = Color(0xFF24214F)
val secondaryContainerDark = Color(0xFF37336D)
val onSecondaryContainerDark = Color(0xFFEAE8FF)
val tertiaryDark = Color(0xFFFFB8AE)
val onTertiaryDark = Color(0xFF5A1713)
val tertiaryContainerDark = Color(0xFF7D302B)
val onTertiaryContainerDark = Color(0xFFFFE2DD)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF15152C)
val onBackgroundDark = Color(0xFFF0EFFB)
val surfaceDark = Color(0xFF15152C)
val onSurfaceDark = Color(0xFFF0EFFB)
val surfaceVariantDark = Color(0xFF565577)
val onSurfaceVariantDark = Color(0xFFDCD9ED)
val outlineDark = Color(0xFF9D9AB9)
val outlineVariantDark = Color(0xFF565577)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFF0EFFB)
val inverseOnSurfaceDark = Color(0xFF202044)
val inversePrimaryDark = Color(0xFF3730A3)
val surfaceDimDark = Color(0xFF15152C)
val surfaceBrightDark = Color(0xFF333354)
val surfaceContainerLowestDark = Color(0xFF111125)
val surfaceContainerLowDark = Color(0xFF202044)
val surfaceContainerDark = Color(0xFF242442)
val surfaceContainerHighDark = Color(0xFF303052)
val surfaceContainerHighestDark = Color(0xFF3C3B61)

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark
)

@Composable
fun UchihaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
