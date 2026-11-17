package ma.hanout.lite.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/* ------------------------------------------------------------------
 * الألوان ديال العلامة (نفس لوحة ألوان نسخة الويب):
 *   الأزرق #003DA5 • الأصفر #FFC220 • الأخضر #00843D • الأحمر #E01A2B
 * ------------------------------------------------------------------ */

private val LightScheme = lightColorScheme(
    primary = Color(0xFF003DA5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF001A41),
    inversePrimary = Color(0xFFB0C6FF),

    secondary = Color(0xFF00696D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB2ECEF),
    onSecondaryContainer = Color(0xFF002021),

    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261A00),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF5F8FD),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FD),
    surfaceContainer = Color(0xFFF0F4FB),
    surfaceContainerHigh = Color(0xFFEBEFF7),
    surfaceContainerHighest = Color(0xFFE4E9F2),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),

    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC5C6D0),
    scrim = Color(0xFF000000)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF00419E),
    onPrimaryContainer = Color(0xFFDCE3FF),
    inversePrimary = Color(0xFF003DA5),

    secondary = Color(0xFF4FD8DE),
    onSecondary = Color(0xFF003739),
    secondaryContainer = Color(0xFF004F52),
    onSecondaryContainer = Color(0xFFB2ECEF),

    tertiary = Color(0xFFEFC03C),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5C4500),
    onTertiaryContainer = Color(0xFFFFE08A),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFF0C0E11),
    surfaceContainerLow = Color(0xFF191C1E),
    surfaceContainer = Color(0xFF1D2023),
    surfaceContainerHigh = Color(0xFF272A2E),
    surfaceContainerHighest = Color(0xFF323539),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2F3033),

    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    scrim = Color(0xFF000000)
)

/** أشكال «مدوّرة» عصرية (Material 3). */
private val HanoutShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/** كيحوّل لون التاجر (#RRGGBB ولا #AARRGGBB) للون Compose. */
fun parseHexColor(hex: String): Color? = runCatching {
    val clean = hex.trim().removePrefix("#")
    val normalized = when (clean.length) {
        6 -> "#FF$clean"
        8 -> "#$clean"
        else -> return@runCatching null
    }
    Color(android.graphics.Color.parseColor(normalized))
}.getOrNull()

/** خلط لونين (t = 0 → a، t = 1 → b). */
private fun blend(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)

/** كيبدّل الألوان الأساسية بلون الحانوت باش التطبيق ياخد هوية التاجر. */
private fun ColorScheme.branded(brand: Color, dark: Boolean): ColorScheme {
    val lightBrand = brand.luminance() > 0.55f
    val onBrand = if (lightBrand) Color(0xFF10192B) else Color.White
    return copy(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = if (dark) blend(brand, surface, 0.55f) else blend(brand, Color.White, 0.86f),
        onPrimaryContainer = if (dark) blend(brand, Color.White, 0.80f) else blend(brand, Color.Black, 0.30f),
        inversePrimary = if (dark) brand else blend(brand, Color.White, 0.45f)
    )
}

/**
 * ثيم التطبيق.
 *   • [brandColor] : لون الحانوت — إلا كان، كيتّبع فكل التطبيق.
 *   • الوضع (فاتح/غامق) وألوان Material You كيتقراو من [ThemePrefs].
 */
@Composable
fun HanoutTheme(
    brandColor: Color? = null,
    content: @Composable () -> Unit
) {
    val dark = when (ThemePrefs.mode) {
        ThemePrefs.Mode.Light -> false
        ThemePrefs.Mode.Dark -> true
        ThemePrefs.Mode.System -> isSystemInDarkTheme()
    }
    val dynamicColor = ThemePrefs.dynamic
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        brandColor != null -> (if (dark) DarkScheme else LightScheme).branded(brandColor, dark)

        else -> if (dark) DarkScheme else LightScheme
    }
    MaterialTheme(colorScheme = scheme, shapes = HanoutShapes, content = content)
}
