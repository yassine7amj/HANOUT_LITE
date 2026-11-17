package ma.hanout.lite.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ma.hanout.lite.BuildConfig

/**
 * مستودع تطبيق التاجر.
 * ------------------------------------------------------------------
 * كيجمّع جوج خدمات:
 *   • [AuthRest]       : تسجيل الدخول (Firebase Authentication عبر REST)
 *   • [FirestoreRest]  : قراءة/كتابة الطلبات، الكتالوغ ومعلومات الحانوت
 *
 * الجلسة كتّخزّن محلياً فـ SharedPreferences، والتوكن كيتجدّد أوتوماتيكياً
 * ملي كينتهي. إلا كان البناء بلا Firebase، التطبيق كيخدم فوض تجريبي.
 */
class MerchantRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("hanout_merchant_v1", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** واش التطبيق مربوط مع مشروع Firebase؟ */
    val configured: Boolean = BuildConfig.FB_PROJECT_ID.isNotBlank()

    private val auth: AuthRest? by lazy {
        if (configured) AuthRest(BuildConfig.FB_API_KEY) else null
    }
    private val rest: FirestoreRest? by lazy {
        if (configured) FirestoreRest(BuildConfig.FB_PROJECT_ID, BuildConfig.FB_API_KEY) else null
    }

    /** slug الحانوت: كيتعطى وقت البناء، ولا كيتقرا من حساب التاجر بعد الدخول. */
    var slug: String = BuildConfig.SHOP_SLUG.trim()
        private set

    private var current: Session? = read()

    val session: Session? get() = current

    init {
        rest?.token = current?.idToken
    }

    /* ----------------------- الجلسة ----------------------- */

    private fun read(): Session? = prefs.getString(KEY_SESSION, null)
        ?.let { runCatching { json.decodeFromString<Session>(it) }.getOrNull() }

    private fun write(value: Session?) {
        current = value
        if (value == null) prefs.edit().remove(KEY_SESSION).apply()
        else prefs.edit().putString(KEY_SESSION, json.encodeToString(value)).apply()
        rest?.token = value?.idToken
    }

    /** تسجيل دخول التاجر + تحديد الحانوت ديالو. */
    suspend fun signIn(email: String, password: String): Session {
        val client = auth ?: error("هاد البناء ماشي مربوط مع Firebase.")
        val session = client.signIn(email, password)
        write(session)
        if (slug.isBlank()) resolveSlug(session)
        return session
    }

    fun signOut() = write(null)

    /** كيتأكد أن التوكن مازال صالح — وكيجدّدو إلا انتهى. */
    suspend fun ensureToken() {
        val session = current ?: return
        if (!session.expired()) {
            rest?.token = session.idToken
            return
        }
        val client = auth ?: return
        if (session.refreshToken.isBlank()) return
        val fresh = runCatching { client.refresh(session.refreshToken) }.getOrNull() ?: return
        write(fresh.copy(email = session.email.ifBlank { fresh.email }))
    }

    private suspend fun resolveSlug(session: Session) {
        val client = rest ?: return
        val fromProfile = client.getProfile(session.uid)?.slug?.takeIf { it.isNotBlank() }
        val found = fromProfile
            ?: client.findMerchantSlug(session.uid, session.email)?.takeIf { it.isNotBlank() }
        if (found != null) slug = found
    }

    /** كيحدّد الحانوت يدوياً (كيستعمل فالتجربة ولا ملي ما كانش فالإعدادات). */
    fun useSlug(value: String) {
        val clean = value.trim()
        if (clean.isNotBlank()) slug = clean
    }

    /* ----------------------- القراءة ----------------------- */

    suspend fun loadShop(target: String = slug): Shop? {
        if (!configured || target.isBlank()) return null
        ensureToken()
        return rest?.getShop(target)
    }

    suspend fun listOrders(target: String = slug): List<Order> {
        if (!configured || target.isBlank()) return emptyList()
        ensureToken()
        return rest?.listOrders(target).orEmpty()
    }

    /* ----------------------- الكتابة ----------------------- */

    suspend fun setOrderStatus(orderId: String, status: String, target: String = slug): Boolean {
        if (!configured) return false
        ensureToken()
        return rest?.updateOrderStatus(target, orderId, status) == true
    }

    suspend fun deleteOrder(orderId: String, target: String = slug): Boolean {
        if (!configured) return false
        ensureToken()
        return rest?.deleteOrder(target, orderId) == true
    }

    suspend fun saveCatalog(
        products: List<Product>,
        categories: List<Category>,
        target: String = slug
    ): Boolean {
        if (!configured) return false
        ensureToken()
        return rest?.saveCatalog(target, products, categories) == true
    }

    suspend fun saveShop(
        name: String,
        phone: String,
        color: String,
        active: Boolean,
        target: String = slug
    ): Boolean {
        if (!configured) return false
        ensureToken()
        return rest?.updateMerchant(target, name, phone, color, active) == true
    }

    companion object {
        private const val KEY_SESSION = "session"
    }
}
