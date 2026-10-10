package com.example.minilims.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A6A), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1F0), onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7), onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4B607C), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E4FF), onTertiaryContainer = Color(0xFF041C35),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4FBFA), onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF4FBFA), onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE5E4), onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7979), outlineVariant = Color(0xFFBEC9C8),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFEEF5F4),
    surfaceContainer = Color(0xFFE9EFEF), surfaceContainerHigh = Color(0xFFE3E9E9),
    surfaceContainerHighest = Color(0xFFDDE4E3),
    inverseSurface = Color(0xFF2B3231), inverseOnSurface = Color(0xFFECF2F1), inversePrimary = Color(0xFF4CDADA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4CDADA), onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F), onPrimaryContainer = Color(0xFF6FF7F6),
    secondary = Color(0xFFB0CCCB), onSecondary = Color(0xFF1B3534),
    secondaryContainer = Color(0xFF324B4B), onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFB3C8E8), onTertiary = Color(0xFF1C314B),
    tertiaryContainer = Color(0xFF334863), onTertiaryContainer = Color(0xFFD3E4FF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1514), onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1514), onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4948), onSurfaceVariant = Color(0xFFBEC9C8),
    outline = Color(0xFF899392), outlineVariant = Color(0xFF3F4948),
    surfaceContainerLowest = Color(0xFF090F0F), surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = Color(0xFF1A2121), surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF303636),
    inverseSurface = Color(0xFFDDE4E3), inverseOnSurface = Color(0xFF2B3231), inversePrimary = Color(0xFF006A6A)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val baseType = Typography()
private val AppTypography = baseType.copy(
    headlineMedium = baseType.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = baseType.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseType.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseType.titleMedium.copy(fontWeight = FontWeight.SemiBold)
)

/** Material 3 theme: wallpaper-based colours on Android 12+, teal palette otherwise; follows system dark mode. */
@Composable
fun LimsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, shapes = AppShapes, typography = AppTypography, content = content)
}
