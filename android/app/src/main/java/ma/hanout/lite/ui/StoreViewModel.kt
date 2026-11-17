package ma.hanout.lite.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ma.hanout.lite.R
import ma.hanout.lite.data.Order
import ma.hanout.lite.data.OrderItem
import ma.hanout.lite.data.Product
import ma.hanout.lite.data.Shop
import ma.hanout.lite.data.ShopRepository
import java.util.Locale

enum class SortMode { Default, PriceAsc, PriceDesc, Name }

enum class LoadError { NotFound, Suspended, Failed }

data class UiState(
    val loading: Boolean = true,
    val shop: Shop? = null,
    val error: LoadError? = null,
    val demo: Boolean = false,
    val cloud: Boolean = false,
    val cart: Map<String, Int> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val orders: List<Order> = emptyList(),
    val query: String = "",
    val category: String = "",
    val onlyAvailable: Boolean = false,
    val onlyFavorites: Boolean = false,
    val sort: SortMode = SortMode.Default,
    /** معرّف نص للرسالة اللي كتظهر فالـ snackbar. */
    val message: Int? = null
) {
    /** سطور السلة مع المنتوجات ديالها. */
    val cartLines: List<Pair<Product, Int>>
        get() = shop?.products?.mapNotNull { product ->
            cart[product.id]?.takeIf { it > 0 }?.let { product to it }
        }.orEmpty()

    val cartCount: Int get() = cartLines.sumOf { it.second }
    val cartTotal: Double get() = cartLines.sumOf { it.first.price * it.second }

    /** المنتوجات اللي كتظهر حسب التصفي/الفرز. */
    val visibleProducts: List<Product>
        get() {
            val all = shop?.products.orEmpty()
            val needle = query.trim().lowercase(Locale.getDefault())
            val filtered = all.filter { product ->
                (needle.isEmpty() || product.name.lowercase(Locale.getDefault()).contains(needle)) &&
                    (category.isEmpty() || product.category == category) &&
                    (!onlyAvailable || product.available) &&
                    (!onlyFavorites || product.id in favorites)
            }
            return when (sort) {
                SortMode.Default -> filtered
                SortMode.PriceAsc -> filtered.sortedBy { it.price }
                SortMode.PriceDesc -> filtered.sortedByDescending { it.price }
                SortMode.Name -> filtered.sortedBy { it.name }
            }
        }
}

/** نتيجة محاولة إتمام الطلب. */
sealed interface CheckoutResult {
    data class Ready(val phone: String, val text: String) : CheckoutResult
    data object EmptyCart : CheckoutResult
    data object NoPhone : CheckoutResult
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShopRepository(application)

    private val _state = MutableStateFlow(
        UiState(
            cart = repository.cart,
            favorites = repository.favorites,
            orders = repository.orders,
            cloud = repository.cloudConfigured
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val outcome = runCatching { repository.loadShop() }
            val shop = outcome.getOrNull()
            when {
                outcome.isFailure -> _state.update {
                    it.copy(loading = false, error = LoadError.Failed, shop = null)
                }

                shop == null -> _state.update {
                    it.copy(loading = false, error = LoadError.NotFound, shop = null)
                }

                !shop.active -> _state.update {
                    it.copy(loading = false, error = LoadError.Suspended, shop = null)
                }

                else -> _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        shop = shop,
                        demo = !repository.cloudConfigured,
                        cloud = repository.cloudConfigured
                    )
                }
            }
        }
    }

    /* ----------------------- الفلاتر ----------------------- */

    fun setQuery(value: String) = _state.update { it.copy(query = value) }
    fun setCategory(value: String) = _state.update { it.copy(category = value) }
    fun setOnlyAvailable(value: Boolean) = _state.update { it.copy(onlyAvailable = value) }
    fun setOnlyFavorites(value: Boolean) = _state.update { it.copy(onlyFavorites = value) }
    fun setSort(value: SortMode) = _state.update { it.copy(sort = value) }

    /** كيعرض رسالة فالـ snackbar (resource id ديال النص). */
    fun postMessage(resId: Int) = _state.update { it.copy(message = resId) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    /* ----------------------- السلة والمفضلة ----------------------- */

    fun addToCart(id: String) {
        val product = _state.value.shop?.products?.firstOrNull { it.id == id } ?: return
        if (!product.available) return
        persistCart(_state.value.cart.toMutableMap().apply { this[id] = (this[id] ?: 0) + 1 })
        _state.update { it.copy(message = R.string.product_added) }
    }

    fun changeQuantity(id: String, delta: Int) {
        val next = _state.value.cart.toMutableMap()
        val quantity = (next[id] ?: 0) + delta
        if (quantity > 0) next[id] = quantity else next.remove(id)
        persistCart(next)
    }

    fun clearCart() {
        persistCart(emptyMap())
    }

    private fun persistCart(value: Map<String, Int>) {
        repository.cart = value
        _state.update { it.copy(cart = value) }
    }

    fun toggleFavorite(id: String) {
        val favorites = _state.value.favorites.toMutableSet()
        if (!favorites.add(id)) favorites.remove(id)
        repository.favorites = favorites
        _state.update { it.copy(favorites = favorites) }
    }

    /* ----------------------- الطلبات ----------------------- */

    fun submitOrder(buyer: String, fulfillment: String, note: String): CheckoutResult {
        val current = _state.value
        val lines = current.cartLines
        if (lines.isEmpty()) return CheckoutResult.EmptyCart

        val shop = current.shop ?: return CheckoutResult.EmptyCart
        val phone = normalizeMoroccanPhone(shop.phone) ?: return CheckoutResult.NoPhone

        val total = lines.sumOf { (product, quantity) -> product.price * quantity }
        val order = Order(
            id = ShopRepository.newOrderId(),
            ts = System.currentTimeMillis(),
            buyer = buyer.trim().ifBlank { "غير محدد" },
            fulfillment = fulfillment,
            note = note.trim(),
            items = lines.map { (product, quantity) ->
                OrderItem(name = product.name, qty = quantity, price = product.price)
            },
            total = total,
            status = Order.STATUS_NEW
        )

        val history = (listOf(order) + current.orders).take(200)
        repository.orders = history
        _state.update { it.copy(orders = history) }
        persistCart(emptyMap())

        if (current.cloud) {
            viewModelScope.launch { repository.submitOrder(order) }
        }

        val text = buildString {
            append("السلام عليكم، بغيت نطلب من ")
            append(shop.name)
            append(":\n")
            lines.forEach { (product, quantity) ->
                append("• ${product.name} × $quantity = ${money(product.price * quantity)}\n")
            }
            append("المجموع التقديري: ${money(total)}\n")
            append("الاستلام: $fulfillment\n")
            append("الاسم: ${order.buyer}\n")
            append("ملاحظة: ${order.note.ifBlank { "لا توجد" }}\n")
            append("المرجو تأكيد التوفر والثمن النهائي.")
        }

        return CheckoutResult.Ready(phone = phone, text = text)
    }
}
