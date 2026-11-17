package ma.hanout.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ma.hanout.lite.R

/** كارت بسيط بعنوان اختياري — الوحدة الأساسية فالتصميم. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null || trailing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    trailing?.invoke()
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

/** كارت إحصائية (رقم كبير + وصف + أيقونة). */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** حالة فارغة (أيقونة + نص). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(54.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** شرابة حالة صغيرة (Badge). */
@Composable
fun StatusPill(text: String, container: Color, contentColor: Color) {
    Surface(color = container, contentColor = contentColor, shape = CircleShape) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** سطر «عنوان: قيمة». */
@Composable
fun KeyValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** صف اختيار لون (لهوية الحانوت). */
@Composable
fun ColorChips(colors: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { hex ->
            val color = parseHexColor(hex) ?: MaterialTheme.colorScheme.primary
            val active = selected.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (active) 3.dp else 1.dp,
                        color = if (active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

/** كتبدّل الطابع الزمني (ms) لتاريخ مقروء. */
@Composable
fun rememberDateTime(ts: Long): String {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()) }
    return remember(ts) { if (ts <= 0L) "" else formatter.format(Date(ts)) }
}

/** كارت إعدادات المظهر: الوضع (فاتح/غامق/النظام) + ألوان Material You. */
@Composable
fun ThemeSettingsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SectionCard(modifier = modifier, title = stringResource(R.string.ui_title)) {
        Text(
            text = stringResource(R.string.ui_mode),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemePrefs.Mode.entries.forEach { mode ->
                FilterChip(
                    selected = ThemePrefs.mode == mode,
                    onClick = { ThemePrefs.setMode(context, mode) },
                    label = {
                        Text(
                            text = stringResource(
                                when (mode) {
                                    ThemePrefs.Mode.System -> R.string.ui_mode_system
                                    ThemePrefs.Mode.Light -> R.string.ui_mode_light
                                    ThemePrefs.Mode.Dark -> R.string.ui_mode_dark
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ui_dynamic),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.ui_dynamic_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = ThemePrefs.dynamic,
                onCheckedChange = { ThemePrefs.setDynamic(context, it) }
            )
        }
    }
}
