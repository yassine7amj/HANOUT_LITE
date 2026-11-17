package ma.hanout.lite.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ma.hanout.lite.BuildConfig

/**
 * مستودع البيانات: كيجيب الحانوت من Firestore (إلا كان مهيّأ) ولا كيرجع
 * لوضع تجريبي محلي، وكيخزّن السلة/المفضلة/الطلبات ديال الجهاز فـ SharedPreferences.
 */
class ShopRepository(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("hanout_lite_v1", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** واش التطبيق مربوط مع مشروع Firebase؟ */
    val cloudConfigured: Boolean = BuildConfig.FB_PROJECT_ID.isNotBlank()

    /** slug الحانوت اللي تبنى عليه هاد التطبيق (كيتبنى فيه). */
    val slug: String = BuildConfig.SHOP_SLUG.trim()

    private val rest: FirestoreRest? by lazy {
        if (cloudConfigured) FirestoreRest(BuildConfig.FB_PROJECT_ID, BuildConfig.FB_API_KEY) else null
    }

    /**
     * كيرجّع الحانوت:
     *   • cloud مهيّأ + slug  → من Firestore (ولا null إلا ما كاينش/موقوف)
     *   • غير مهيّأ            → معطيات تجريبية
     */
    suspend fun loadShop(): Shop? {
        val client = rest
        if (client == null || slug.isBlank()) return DemoData.shop(slug)
        val shop = client.getShop(slug) ?: return null
        return shop.copy(
            name = shop.name.ifBlank { "حانوت" },
            categories = shop.categories.ifEmpty { deriveCategories(shop.products) }
        )
    }

    /** كيصيفط الطلب للسحابة. كيرجّع false إلا ما كانش السيرفر مهيّأ. */
    suspend fun submitOrder(order: Order): Boolean {
        val client = rest ?: return false
        if (slug.isBlank()) return false
        return runCatching { client.createOrder(slug, order) }.getOrDefault(false)
    }

    /* ----------------------- تخزين محلي ----------------------- */

    var cart: Map<String, Int>
        get() = prefs.getString(KEY_CART, null)
            ?.let { runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrNull() }
            .orEmpty()
        set(value) = prefs.edit()
            .putString(KEY_CART, json.encodeToString(value.filterValues { it > 0 }))
            .apply()

    var favorites: Set<String>
        get() = prefs.getString(KEY_FAVS, null)
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?.toSet()
            .orEmpty()
        set(value) = prefs.edit().putString(KEY_FAVS, json.encodeToString(value.toList())).apply()

    var orders: List<Order>
        get() = prefs.getString(KEY_ORDERS, null)
            ?.let { runCatching { json.decodeFromString<List<Order>>(it) }.getOrNull() }
            .orEmpty()
        set(value) = prefs.edit().putString(KEY_ORDERS, json.encodeToString(value)).apply()

    companion object {
        private const val KEY_CART = "cart"
        private const val KEY_FAVS = "favorites"
        private const val KEY_ORDERS = "orders"

        /** كيولّد معرّف طلب بنفس صيغة الويب: o + timestamp + عشوائي. */
        fun newOrderId(): String =
            "o" + System.currentTimeMillis().toString(36) +
                (1000..9999).random().toString(36)

        private fun deriveCategories(products: List<Product>): List<Category> =
            products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                .map { name ->
                    Category(name, products.firstOrNull { it.category == name }?.emoji ?: "🛍️")
                }
    }
}
