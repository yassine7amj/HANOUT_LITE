package ma.hanout.lite.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * تفضيلات الواجهة: وضع المظهر (فاتح/غامق/حسب النظام) وألوان Material You.
 * مخزّنة محلياً فـ SharedPreferences وكمراقبة فـ Compose.
 */
object ThemePrefs {

    enum class Mode { System, Light, Dark }

    var mode: Mode by mutableStateOf(Mode.System)
        private set

    var dynamic: Boolean by mutableStateOf(false)
        private set

    fun load(context: Context) {
        val prefs = prefs(context)
        mode = runCatching { Mode.valueOf(prefs.getString(KEY_MODE, "").orEmpty()) }
            .getOrDefault(Mode.System)
        dynamic = prefs.getBoolean(KEY_DYNAMIC, false)
    }

    fun setMode(context: Context, value: Mode) {
        mode = value
        prefs(context).edit().putString(KEY_MODE, value.name).apply()
    }

    fun setDynamic(context: Context, value: Boolean) {
        dynamic = value
        prefs(context).edit().putBoolean(KEY_DYNAMIC, value).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("hanout_ui_v1", Context.MODE_PRIVATE)

    private const val KEY_MODE = "mode"
    private const val KEY_DYNAMIC = "dynamic"
}
