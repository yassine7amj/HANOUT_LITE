package ma.hanout.lite.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * عميل خفيف لـ Firestore REST API.
 * ------------------------------------------------------------------
 * كنستعملوه بدل Firebase SDK باش:
 *   • ما نحتاجوش ملف google-services.json فمشروع Gradle،
 *   • البناء فالـ CI يبقى بسيط وما يحتاجش إعدادات إضافية.
 *
 * القراءة كتحتاج قواعد أمان تسمح بـ read على merchants/{slug}.
 * الكتابة (الطلبات) كتحتاج allow create على merchants/{slug}/orders.
 */
class FirestoreRest(
    private val projectId: String,
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient()
) {

    private val base =
        "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents"

    /** كيقرا الحانوت + الكتالوغ. كيرجّع null إلا ما كاينش الحانوت. */
    suspend fun getShop(slug: String): Shop? = withContext(Dispatchers.IO) {
        val merchant = readDocument("merchants/$slug")?.let(::fieldsOf)
        val catalog = readDocument("merchants/$slug/data/catalog")?.let(::fieldsOf)
        if (merchant == null && catalog == null) return@withContext null
        Shop(
            slug = slug,
            name = merchant?.let { str(it, "name") } ?: catalog?.let { str(it, "name") }.orEmpty(),
            phone = merchant?.let { str(it, "phone") } ?: catalog?.let { str(it, "phone") }.orEmpty(),
            color = merchant?.let { str(it, "color") }.orEmpty(),
            active = merchant?.let { bool(it, "active") } ?: true,
            products = catalog?.let(::productsOf) ?: emptyList(),
            categories = catalog?.let(::categoriesOf) ?: emptyList()
        )
    }

    /** كيصيفط طلب جديد لـ merchants/{slug}/orders/{order.id} */
    suspend fun createOrder(slug: String, order: Order): Boolean = withContext(Dispatchers.IO) {
        val url = buildUrl("merchants/$slug/orders", mapOf("documentId" to order.id))
        val body = buildJsonObject {
            put("fields", buildJsonObject {
                put("id", stringValue(order.id))
                put("ts", integerValue(order.ts))
                put("buyer", stringValue(order.buyer))
                put("fulfillment", stringValue(order.fulfillment))
                put("note", stringValue(order.note))
                put("total", doubleValue(order.total))
                put("status", stringValue(order.status))
                put("items", buildJsonObject {
                    put("arrayValue", buildJsonObject {
                        put("values", JsonArray(order.items.map { item ->
                            buildJsonObject {
                                put("mapValue", buildJsonObject {
                                    put("fields", buildJsonObject {
                                        put("name", stringValue(item.name))
                                        put("qty", integerValue(item.qty.toLong()))
                                        put("price", doubleValue(item.price))
                                    })
                                })
                            }
                        }))
                    })
                })
            })
        }
        postJson(url, body.toString())
    }

    /* ----------------------- HTTP ----------------------- */

    private fun readDocument(path: String): JsonObject? {
        val request = Request.Builder().url(buildUrl(path)).get().build()
        return client.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> fail(response.code, response.body?.string().orEmpty())
                else -> Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            }
        }
    }

    private fun postJson(url: String, body: String): Boolean {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON_MEDIA))
            .build()
        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) true
            else fail(response.code, response.body?.string().orEmpty())
        }
    }

    private fun buildUrl(path: String, extra: Map<String, String> = emptyMap()): String {
        val params = LinkedHashMap<String, String>(extra)
        if (apiKey.isNotBlank()) params["key"] = apiKey
        if (params.isEmpty()) return "$base/$path"
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${enc(v)}" }
        return "$base/$path?$query"
    }

    private fun fail(code: Int, body: String): Nothing {
        val message = body.take(300).replace('\n', ' ')
        throw IOException("Firestore ($code): $message")
    }

    /* ----------------------- JSON helpers ----------------------- */

    private fun fieldsOf(document: JsonObject): JsonObject? = document["fields"]?.jsonObject

    private fun str(fields: JsonObject, key: String): String? =
        (fields[key] as? JsonObject)?.get("stringValue")?.jsonPrimitive?.contentOrNull

    private fun dbl(fields: JsonObject, key: String): Double? {
        val value = fields[key] as? JsonObject ?: return null
        value["doubleValue"]?.jsonPrimitive?.doubleOrNull?.let { return it }
        return value["integerValue"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
    }

    private fun bool(fields: JsonObject, key: String): Boolean? =
        (fields[key] as? JsonObject)?.get("booleanValue")?.jsonPrimitive?.booleanOrNull

    /** كيرجّع لائحة من الحقول من arrayValue ديال mapValue. */
    private fun mapList(fields: JsonObject, key: String): List<JsonObject> {
        val values = (fields[key] as? JsonObject)
            ?.get("arrayValue")?.jsonObject
            ?.get("values") as? JsonArray ?: return emptyList()
        return values.mapNotNull { entry ->
            (entry as? JsonObject)?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject
        }
    }

    private fun productsOf(fields: JsonObject): List<Product> = mapList(fields, "products").map { row ->
        val name = str(row, "name").orEmpty()
        Product(
            id = str(row, "id").orEmpty().ifBlank { "p" + name.hashCode().toUInt().toString(36) },
            name = name,
            price = dbl(row, "price") ?: 0.0,
            oldPrice = dbl(row, "oldPrice"),
            category = str(row, "category").orEmpty(),
            emoji = str(row, "emoji").orEmpty().ifBlank { "🛍️" },
            available = bool(row, "available") ?: true,
            image = str(row, "image")?.takeIf { it.isNotBlank() }
        )
    }

    private fun categoriesOf(fields: JsonObject): List<Category> =
        mapList(fields, "categories").mapNotNull { row ->
            val name = str(row, "name").orEmpty()
            if (name.isBlank()) null
            else Category(name = name, emoji = str(row, "emoji").orEmpty().ifBlank { "🛍️" })
        }

    private fun stringValue(value: String): JsonObject =
        buildJsonObject { put("stringValue", value) }

    private fun integerValue(value: Long): JsonObject =
        buildJsonObject { put("integerValue", value.toString()) }

    private fun doubleValue(value: Double): JsonObject =
        buildJsonObject { put("doubleValue", value) }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}
