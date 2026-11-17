package ma.hanout.lite.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** جلسة التاجر بعد تسجيل الدخول (email + password). */
@Serializable
data class Session(
    val email: String = "",
    val uid: String = "",
    val idToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0L
) {
    val valid: Boolean get() = idToken.isNotBlank()
    fun expired(now: Long = System.currentTimeMillis()): Boolean = now > expiresAt - 60_000L
}

/**
 * مصادقة Firebase عبر REST (بديل SDK).
 * ------------------------------------------------------------------
 * كنستعملوها فـ تطبيق التاجر باش يقدر يقرا/يكتب الطلبات والكتالوغ.
 * ملاحظة: خاص Authentication يكون مفعّل فمشروع Firebase.
 */
class AuthRest(
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient()
) {

    /** تسجيل الدخول بالبريد وكلمة السر. */
    suspend fun signIn(email: String, password: String): Session = withContext(Dispatchers.IO) {
        val body = jsonBody(
            "email" to JsonPrimitive(email.trim()),
            "password" to JsonPrimitive(password),
            "returnSecureToken" to JsonPrimitive(true)
        )
        val request = Request.Builder()
            .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey")
            .post(body)
            .build()
        val json = execute(request)
        Session(
            email = json["email"]?.jsonPrimitive?.contentOrNull ?: email.trim(),
            uid = json["localId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            idToken = json["idToken"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            refreshToken = json["refreshToken"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            expiresAt = System.currentTimeMillis() +
                ((json["expiresIn"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L) * 1000L)
        )
    }

    /** كيجدّد الـ idToken بمفتاح التحديث. */
    suspend fun refresh(refreshToken: String): Session = withContext(Dispatchers.IO) {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()
        val request = Request.Builder()
            .url("https://securetoken.googleapis.com/v1/token?key=$apiKey")
            .post(form)
            .build()
        val json = execute(request)
        Session(
            email = "",
            uid = json["user_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            idToken = json["access_token"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            refreshToken = json["refresh_token"]?.jsonPrimitive?.contentOrNull ?: refreshToken,
            expiresAt = System.currentTimeMillis() +
                ((json["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L) * 1000L)
        )
    }

    /* ----------------------- داخلي ----------------------- */

    private fun jsonBody(vararg pairs: Pair<String, JsonElement>): okhttp3.RequestBody {
        val obj = buildJsonObject { pairs.forEach { (key, value) -> put(key, value) } }
        return obj.toString().toRequestBody(JSON_MEDIA)
    }

    private fun execute(request: Request): JsonObject {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    Json.parseToJsonElement(text).jsonObject["error"]?.jsonObject
                        ?.get("message")?.jsonPrimitive?.contentOrNull
                }.getOrNull() ?: text.take(200)
                throw IOException("Auth (${response.code}): $message")
            }
            return Json.parseToJsonElement(text).jsonObject
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}
