package ma.hanout.lite.data

import kotlinx.serialization.Serializable

/** منتوج واحد فالكتالوغ (نفس البنية ديال الويب: products[]). */
@Serializable
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val oldPrice: Double? = null,
    val category: String = "",
    val emoji: String = "🛍️",
    val available: Boolean = true,
    /** صورة اختيارية على شكل data URL (base64) */
    val image: String? = null
) {
    val hasDiscount: Boolean get() = oldPrice != null && oldPrice > price

    /** نسبة التخفيض (%) — 0 إلا ما كانش تخفيض. */
    val discountPercent: Int
        get() {
            val old = oldPrice ?: return 0
            if (old <= price || old <= 0.0) return 0
            return (((old - price) / old) * 100).toInt()
        }
}

/** صنف (catégories[]) */
@Serializable
data class Category(
    val name: String = "",
    val emoji: String = "🛍️"
)

/** الحانوت مع كتالوغه — نفس اللي كيرجّع HanoutStore.loadShop(slug). */
@Serializable
data class Shop(
    val slug: String = "",
    val name: String = "",
    val phone: String = "",
    val color: String = "",
    val plan: String = "free",
    val active: Boolean = true,
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList()
)

/** سطر فالطلب. */
@Serializable
data class OrderItem(
    val name: String = "",
    val qty: Int = 0,
    val price: Double = 0.0
)

/** طلب زبون — نفس البنية ديال الويب: merchants/{slug}/orders/{id} */
@Serializable
data class Order(
    val id: String = "",
    val ts: Long = 0L,
    val buyer: String = "",
    val fulfillment: String = "",
    val note: String = "",
    val items: List<OrderItem> = emptyList(),
    val total: Double = 0.0,
    val status: String = STATUS_NEW
) {
    val isNew: Boolean get() = status != STATUS_DONE
    val itemCount: Int get() = items.sumOf { it.qty }

    companion object {
        const val STATUS_NEW = "جديد"
        const val STATUS_DONE = "تم"
    }
}

/** حساب التاجر كما هو مخزّن فـ users/{uid}. */
data class MerchantProfile(
    val uid: String = "",
    val email: String = "",
    val slug: String = "",
    val role: String = "merchant"
)
