package ma.hanout.lite.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** نفس تنسيق الويب: "12.50 DH" */
fun money(value: Double): String = String.format(Locale.US, "%.2f", value) + " DH"

/** كينقّي رقم مغربي وكيرجّع الصيغة الدولية الصالحة لواتساب، ولا null. */
fun normalizeMoroccanPhone(raw: String): String? {
    var digits = raw.filter { it.isDigit() }
    if (digits.startsWith("00")) digits = digits.substring(2)
    digits = when {
        digits.startsWith("212") -> digits
        digits.startsWith("0") -> "212" + digits.substring(1)
        digits.length == 9 && digits.first() in "567" -> "212$digits"
        else -> digits
    }
    return if (Regex("^212[5-7]\\d{8}$").matches(digits)) digits else null
}

fun openWhatsApp(context: Context, phone: String, text: String) {
    val url = "https://wa.me/$phone?text=" + Uri.encode(text)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun shareText(context: Context, body: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    val chooser = Intent.createChooser(send, chooserTitle)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
}

fun dialPhone(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    runCatching { context.startActivity(intent) }
}

/** كيحلّ صورة المنتوج من data URL (base64) فذاكرة الكاش ديال Compose. */
@Composable
fun rememberProductImage(data: String?): ImageBitmap? = produceState<ImageBitmap?>(
    initialValue = null,
    key1 = data
) {
    value = if (data.isNullOrBlank()) null
    else withContext(Dispatchers.IO) { decodeDataUrl(data) }
}.value

private fun decodeDataUrl(data: String): ImageBitmap? {
    val marker = data.indexOf("base64,")
    if (marker < 0) return null
    return runCatching {
        val bytes = Base64.decode(data.substring(marker + 7), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

/** أيقونة/صورة المنتوج: الصورة إلا كانت، وإلا الإيموجي. */
@Composable
fun ProductVisual(
    emoji: String,
    image: String?,
    modifier: Modifier = Modifier,
    emojiSize: Int = 34
) {
    val bitmap = rememberProductImage(image)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14000000)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = emoji, fontSize = emojiSize.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(42.dp))
    }
}
