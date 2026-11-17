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
 * القراءة العامة (الحانوت/الكتالوغ) ما كتحتاجش حساب.
 * أما كتابة الطلبات وتسيير الحانوت فكتحتاج توكن التاجر ([token]) —
 * كيتعطى بعد تسجيل الدخول بـ [AuthRest].
 */
class FirestoreRest(
    private val projectId: String,
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient()
) {

    /** توكن التاجر (Firebase ID token). كيتحدّث بعد كل تسجيل دخول. */
    var token: String? = null

    private val base =
        "https://firestore.googleapis.com/v1/projects/$projectId/databases/$DATABASE/documents"

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
            plan = merchant?.let { str(it, "plan") }.orEmpty().ifBlank { "free" },
            active = merchant?.let { bool(it, "active") } ?: true,
            products = catalog?.let(::productsOf) ?: emptyList(),
            categories = catalog?.let(::categoriesOf) ?: emptyList()
        )
    }

    /* ----------------------- حساب التاجر ----------------------- */

    /** كيقرا حساب المستعمل من users/{uid} (مسموح غير لصاحب الحساب). */
    suspend fun getProfile(uid: String): MerchantProfile? = withContext(Dispatchers.IO) {
        val fields = readDocument("users/$uid")?.let(::fieldsOf) ?: return@withContext null
        MerchantProfile(
            uid = uid,
            email = str(fields, "email").orEmpty(),
            slug = str(fields, "slug").orEmpty(),
            role = str(fields, "role").orEmpty().ifBlank { "merchant" }
        )
    }

    /**
     * كيلقى slug الحانوت من صاحب الحساب (ownerUid ولا ownerEmail).
     * احتياط ملي ما تكونش وثيقة users/{uid} فيها slug.
     */
    suspend fun findMerchantSlug(uid: String, email: String): String? =
        withContext(Dispatchers.IO) {
            val json = readObject(buildUrl("merchants", listOf("pageSize" to "300")))
                ?: return@withContext null
            val documents = json["documents"] as? JsonArray ?: return@withContext null
            documents.forEach { element ->
                val doc = element as? JsonObject ?: return@forEach
                val fields = fieldsOf(doc) ?: return@forEach
                val ownerUid = str(fields, "ownerUid").orEmpty()
                val ownerEmail = str(fields, "ownerEmail").orEmpty()
                val match = (uid.isNotBlank() && ownerUid == uid) ||
                    (email.isNotBlank() && ownerEmail.equals(email, ignoreCase = true))
                if (match) return@withContext nameOf(doc).substringAfterLast('/')
            }
            null
        }

    /* ----------------------- الطلبات ----------------------- */

    /** كيصيفط طلب جديد لـ merchants/{slug}/orders/{order.id} */
    suspend fun createOrder(slug: String, order: Order, tokenOverride: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            val url = buildUrl("merchants/$slug/orders", listOf("documentId" to order.id))
            val body = buildJsonObject {
                put("fields", orderFields(order))
            }
            val previous = token
            if (tokenOverride != null) token = tokenOverride
            try {
                postJson(url, body.toString())
            } finally {
                token = previous
            }
        }

    /** كيقرا كل الطلبات (خاص التاجر يكون داخل). */
    suspend fun listOrders(slug: String, limit: Int = 200): List<Order> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(
                "merchants/$slug/orders",
                listOf("pageSize" to limit.toString())
            )
            val json = readObject(url) ?: return@withContext emptyList()
            val documents = json["documents"] as? JsonArray ?: return@withContext emptyList()
            documents.mapNotNull { element ->
                val doc = element as? JsonObject ?: return@mapNotNull null
                val fields = fieldsOf(doc) ?: return@mapNotNull null
                val id = str(fields, "id").orEmpty().ifBlank { nameOf(doc).substringAfterLast('/') }
                orderOf(id, fields)
            }.sortedByDescending { it.ts }
        }

    /** كيبدّل حالة الطلب (جديد / تم / ملغى). */
    suspend fun updateOrderStatus(slug: String, orderId: String, status: String): Boolean =
        withContext(Dispatchers.IO) {
            val url = buildUrl(
                "merchants/$slug/orders/$orderId",
                listOf("updateMask.fieldPaths" to "status")
            )
            val body = buildJsonObject {
                put("fields", buildJsonObject { put("status", stringValue(status)) })
            }
            patchJson(url, body.toString())
        }

    /** كيحيد طلب. */
    suspend fun deleteOrder(slug: String, orderId: String): Boolean = withContext(Dispatchers.IO) {
        val url = buildUrl("merchants/$slug/orders/$orderId")
        deleteJson(url)
    }

    /* ----------------------- الكتالوغ والحانوت ----------------------- */

    /** كيكتب الكتالوغ (المنتوجات + الأصناف) فـ merchants/{slug}/data/catalog. */
    suspend fun saveCatalog(
        slug: String,
        products: List<Product>,
        categories: List<Category>
    ): Boolean = withContext(Dispatchers.IO) {
        val url = buildUrl(
            "merchants/$slug/data/catalog",
            listOf(
                "updateMask.fieldPaths" to "products",
                "updateMask.fieldPaths" to "categories"
            )
        )
        val body = buildJsonObject {
            put(
                "fields",
                buildJsonObject {
                    put("products", arrayValue(products.map(::productFields)))
                    put("categories", arrayValue(categories.map(::categoryFields)))
                }
            )
        }
        patchJson(url, body.toString())
    }

    /** كيحدّث معلومات الحانوت (الاسم / الواتساب / اللون / الحالة). */
    suspend fun updateMerchant(
        slug: String,
        name: String? = null,
        phone: String? = null,
        color: String? = null,
        active: Boolean? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val fields = buildJsonObject {
            name?.let { put("name", stringValue(it)) }
            phone?.let { put("phone", stringValue(it)) }
            color?.let { put("color", stringValue(it)) }
            active?.let { put("active", booleanValue(it)) }
        }
        val masks = buildList {
            if (name != null) add("name")
            if (phone != null) add("phone")
            if (color != null) add("color")
            if (active != null) add("active")
        }
        if (masks.isEmpty()) return@withContext true
        val url = buildUrl("merchants/$slug", masks.map { "updateMask.fieldPaths" to it })
        val body = buildJsonObject { put("fields", fields) }
        patchJson(url, body.toString())
    }

    /* ----------------------- HTTP ----------------------- */

    private fun readDocument(path: String): JsonObject? = readObject(buildUrl(path))

    private fun readObject(url: String): JsonObject? {
        val request = builder(url).get().build()
        return client.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> fail(response.code, response.body?.string().orEmpty())
                else -> Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            }
        }
    }

    private fun postJson(url: String, body: String): Boolean = sendJson("POST", url, body)

    private fun patchJson(url: String, body: String): Boolean = sendJson("PATCH", url, body)

    private fun sendJson(method: String, url: String, body: String): Boolean {
        val request = builder(url).method(method, body.toRequestBody(JSON_MEDIA)).build()
        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) true
            else fail(response.code, response.body?.string().orEmpty())
        }
    }

    private fun deleteJson(url: String): Boolean {
        val request = builder(url).delete().build()
        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) true
            else fail(response.code, response.body?.string().orEmpty())
        }
    }

    private fun builder(url: String): Request.Builder {
        val builder = Request.Builder().url(url)
        token?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        return builder
    }

    /**
     * كيبني الرابط. [extra] لائحة مفتاح/قيمة (ماشي Map) حيت Firestore كيقبل
     * نفس المعامل مكرّر عدة مرات — بحال `updateMask.fieldPaths`.
     */
    private fun buildUrl(path: String, extra: List<Pair<String, String>> = emptyList()): String {
        val params = ArrayList<Pair<String, String>>(extra.size + 1)
        if (apiKey.isNotBlank()) params.add("key" to apiKey)
        params.addAll(extra)
        val query = params.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        return "$base/$path?$query"
    }

    private fun fail(code: Int, body: String): Nothing {
        val message = body.take(300).replace('\n', ' ')
        throw IOException("Firestore ($code): $message")
    }

    /* ----------------------- JSON helpers ----------------------- */

    private fun fieldsOf(document: JsonObject): JsonObject? = document["fields"]?.jsonObject

    private fun nameOf(document: JsonObject): String =
        document["name"]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun str(fields: JsonObject, key: String): String? =
        (fields[key] as? JsonObject)?.get("stringValue")?.jsonPrimitive?.contentOrNull

    private fun dbl(fields: JsonObject, key: String): Double? {
        val value = fields[key] as? JsonObject ?: return null
        value["doubleValue"]?.jsonPrimitive?.doubleOrNull?.let { return it }
        return value["integerValue"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
    }

    private fun lng(fields: JsonObject, key: String): Long? =
        (fields[key] as? JsonObject)?.get("integerValue")?.jsonPrimitive?.contentOrNull?.toLongOrNull()

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

    private fun orderOf(id: String, fields: JsonObject): Order = Order(
        id = id,
        ts = lng(fields, "ts") ?: 0L,
        buyer = str(fields, "buyer").orEmpty(),
        fulfillment = str(fields, "fulfillment").orEmpty(),
        note = str(fields, "note").orEmpty(),
        total = dbl(fields, "total") ?: 0.0,
        status = str(fields, "status").orEmpty().ifBlank { Order.STATUS_NEW },
        items = mapList(fields, "items").map { row ->
            OrderItem(
                name = str(row, "name").orEmpty(),
                qty = (lng(row, "qty") ?: 0L).toInt(),
                price = dbl(row, "price") ?: 0.0
            )
        }
    )

    private fun orderFields(order: Order): JsonObject = buildJsonObject {
        put("id", stringValue(order.id))
        put("ts", integerValue(order.ts))
        put("buyer", stringValue(order.buyer))
        put("fulfillment", stringValue(order.fulfillment))
        put("note", stringValue(order.note))
        put("total", doubleValue(order.total))
        put("status", stringValue(order.status))
        put("items", arrayValue(order.items.map { item ->
            buildJsonObject {
                put("name", stringValue(item.name))
                put("qty", integerValue(item.qty.toLong()))
                put("price", doubleValue(item.price))
            }
        }))
    }

    private fun productFields(product: Product): JsonObject = buildJsonObject {
        put("id", stringValue(product.id))
        put("name", stringValue(product.name))
        put("price", doubleValue(product.price))
        product.oldPrice?.let { put("oldPrice", doubleValue(it)) }
        put("category", stringValue(product.category))
        put("emoji", stringValue(product.emoji))
        put("available", booleanValue(product.available))
        product.image?.let { put("image", stringValue(it)) }
    }

    private fun categoryFields(category: Category): JsonObject = buildJsonObject {
        put("name", stringValue(category.name))
        put("emoji", stringValue(category.emoji))
    }

    private fun arrayValue(values: List<JsonObject>): JsonObject = buildJsonObject {
        put("arrayValue", buildJsonObject {
            put("values", JsonArray(values.map { row ->
                buildJsonObject { put("mapValue", buildJsonObject { put("fields", row) }) }
            }))
        })
    }

    private fun stringValue(value: String): JsonObject =
        buildJsonObject { put("stringValue", value) }

    private fun integerValue(value: Long): JsonObject =
        buildJsonObject { put("integerValue", value.toString()) }

    private fun doubleValue(value: Double): JsonObject =
        buildJsonObject { put("doubleValue", value) }

    private fun booleanValue(value: Boolean): JsonObject =
        buildJsonObject { put("booleanValue", value) }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    companion object {
        /** اسم قاعدة البيانات فمشروع Firebase (المشروع كيستعمل قاعدة مسمّاة «default»). */
        private const val DATABASE = "default"

        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}
