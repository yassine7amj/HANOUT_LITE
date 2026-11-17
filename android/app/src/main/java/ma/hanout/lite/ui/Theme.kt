package ma.hanout.lite.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toColorInt

private val LightScheme = lightColorScheme(
    primary = Color(0xFF003DA5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF00696D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB2ECEF),
    onSecondaryContainer = Color(0xFF002021),
    background = Color(0xFFF5F8FD),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF757780),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB0C6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF00419E),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFF4FD8DE),
    onSecondary = Color(0xFF003739),
    secondaryContainer = Color(0xFF004F52),
    onSecondaryContainer = Color(0xFFB2ECEF),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/** كيحوّل لون التاجر (#RRGGBB ولا #AARRGGBB) للون Compose. */
fun parseHexColor(hex: String): Color? = runCatching {
    val clean = hex.trim().removePrefix("#")
    val normalized = when (clean.length) {
        6 -> "#FF$clean"
        8 -> "#$clean"
        else -> return@runCatching null
    }
    Color(normalized.toColorInt())
}.getOrNull()

@Composable
fun HanoutTheme(brandColor: Color? = null, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) DarkScheme else LightScheme
    val scheme = if (brandColor == null) base else base.copy(
        primary = brandColor,
        onPrimary = if (dark) Color(0xFF00201F) else Color(0xFFFFFFFF),
        primaryContainer = brandColor.copy(alpha = if (dark) 0.35f else 0.14f),
        onPrimaryContainer = if (dark) Color(0xFFDCE3FF) else Color(0xFF001A41)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
