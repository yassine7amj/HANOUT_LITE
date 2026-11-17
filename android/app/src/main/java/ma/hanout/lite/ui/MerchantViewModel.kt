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
import ma.hanout.lite.data.Category
import ma.hanout.lite.data.DemoData
import ma.hanout.lite.data.MerchantRepository
import ma.hanout.lite.data.Order
import ma.hanout.lite.data.Product
import ma.hanout.lite.data.Shop
import java.util.Calendar

/** حالة تطبيق التاجر. */
data class MerchantState(
    val signedIn: Boolean = false,
    /** وضع تجريبي: ما كاينش Firebase ولا التاجر داخّل بلا حساب. */
    val demo: Boolean = false,
    val busy: Boolean = false,
    val loading: Boolean = false,
    val email: String = "",
    val slug: String = "",
    val error: String? = null,
    val message: String? = null,
    val shop: Shop? = null,
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val ordersQuery: String = "",
    val statusFilter: String = "",
    val catalogQuery: String = "",
    /** المنتوج اللي كيتعدّل دابا (null = ما كاين حتى dialog). */
    val draft: Product? = null,
    val draftIsNew: Boolean = false,
    /** واش كاين تغييرات محلية ما تسجلوش فالسحابة. */
    val dirty: Boolean = false
) {
    val newOrders: Int get() = orders.count { it.isNew }
    val revenue: Double get() = orders.sumOf { it.total }
    val availableProducts: Int get() = products.count { it.available }

    val todayOrders: List<Order>
        get() {
            val start = startOfToday()
            return orders.filter { it.ts >= start }
        }
    val todayRevenue: Double get() = todayOrders.sumOf { it.total }

    val visibleOrders: List<Order>
        get() {
            val needle = ordersQuery.trim().lowercase()
            return orders.filter { order ->
                val okStatus = statusFilter.isEmpty() || order.status == statusFilter
                val okQuery = needle.isEmpty() ||
                    order.buyer.lowercase().contains(needle) ||
                    order.items.any { it.name.lowercase().contains(needle) }
                okStatus && okQuery
            }
        }

    val visibleProducts: List<Product>
        get() {
            val needle = catalogQuery.trim().lowercase()
            return products.filter {
                needle.isEmpty() ||
                    it.name.lowercase().contains(needle) ||
                    it.category.lowercase().contains(needle)
            }
        }
}

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** منطق تطبيق التاجر: الدخول، الطلبات، الكتالوغ ومعلومات الحانوت. */
class MerchantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MerchantRepository(application)

    private val _state = MutableStateFlow(
        MerchantState(
            signedIn = repository.configured && repository.session != null,
            email = repository.session?.email.orEmpty(),
            slug = repository.slug
        )
    )
    val state: StateFlow<MerchantState> = _state.asStateFlow()

    init {
        if (_state.value.signedIn) refresh()
    }

    private fun str(resId: Int): String = getApplication<Application>().getString(resId)

    /* ----------------------- الدخول ----------------------- */

    fun signIn(email: String, password: String) {
        val mail = email.trim()
        if (mail.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = str(R.string.merchant_email_required)) }
            return
        }
        if (!repository.configured) {
            enterDemo()
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.signIn(mail, password) }.fold(
                onSuccess = { session ->
                    _state.update {
                        it.copy(
                            busy = false,
                            signedIn = true,
                            demo = false,
                            error = null,
                            email = session.email,
                            slug = repository.slug
                        )
                    }
                    refresh()
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(busy = false, error = failure.message ?: str(R.string.error_load))
                    }
                }
            )
        }
    }

    /** كيدخل بلا حساب بمعطيات تجريبية — باش الواجهة تبان كاملة. */
    fun enterDemo() {
        val target = repository.slug.ifBlank { "hanout-demo" }
        repository.useSlug(target)
        _state.update {
            it.copy(
                signedIn = true,
                demo = true,
                busy = false,
                loading = false,
                error = null,
                email = it.email.ifBlank { "demo@hanweb.hub" },
                slug = target,
                shop = DemoData.shop(target),
                products = DemoData.products,
                categories = DemoData.categories,
                orders = DemoData.orders,
                dirty = false
            )
        }
    }

    fun signOut() {
        repository.signOut()
        _state.value = MerchantState(slug = repository.slug)
    }

    fun useSlug(value: String) {
        repository.useSlug(value)
        _state.update { it.copy(slug = repository.slug) }
    }

    /* ----------------------- التحميل ----------------------- */

    fun refresh() {
        val current = _state.value
        if (!current.signedIn) return
        if (current.demo) {
            _state.update { it.copy(message = str(R.string.merchant_demo_badge)) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val shop = repository.loadShop()
                val orders = repository.listOrders()
                shop to orders
            }.fold(
                onSuccess = { (shop, orders) ->
                    if (shop == null) {
                        _state.update {
                            it.copy(
                                loading = false,
                                shop = null,
                                orders = orders,
                                error = str(R.string.dashboard_no_shop)
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                loading = false,
                                error = null,
                                shop = shop,
                                orders = orders,
                                products = shop.products.ifEmpty { it.products },
                                categories = shop.categories.ifEmpty { it.categories },
                                dirty = false
                            )
                        }
                    }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(loading = false, error = failure.message ?: str(R.string.error_load))
                    }
                }
            )
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /* ----------------------- الطلبات ----------------------- */

    fun setOrdersQuery(value: String) = _state.update { it.copy(ordersQuery = value) }

    fun setStatusFilter(value: String) = _state.update { it.copy(statusFilter = value) }

    fun toggleOrderStatus(order: Order) {
        val next = if (order.isNew) Order.STATUS_DONE else Order.STATUS_NEW
        _state.update { state ->
            state.copy(orders = state.orders.map { if (it.id == order.id) it.copy(status = next) else it })
        }
        if (_state.value.demo) {
            _state.update { it.copy(message = str(R.string.order_updated)) }
            return
        }
        viewModelScope.launch {
            val ok = runCatching { repository.setOrderStatus(order.id, next) }.getOrDefault(false)
            _state.update {
                it.copy(message = str(if (ok) R.string.order_updated else R.string.error_load))
            }
        }
    }

    fun removeOrder(order: Order) {
        _state.update { state -> state.copy(orders = state.orders.filterNot { it.id == order.id }) }
        if (_state.value.demo) {
            _state.update { it.copy(message = str(R.string.order_deleted)) }
            return
        }
        viewModelScope.launch {
            val ok = runCatching { repository.deleteOrder(order.id) }.getOrDefault(false)
            _state.update {
                it.copy(message = str(if (ok) R.string.order_deleted else R.string.error_load))
            }
        }
    }

    /* ----------------------- الكتالوغ ----------------------- */

    fun setCatalogQuery(value: String) = _state.update { it.copy(catalogQuery = value) }

    fun startNewProduct() = _state.update {
        it.copy(
            draft = Product(id = "p" + System.currentTimeMillis().toString(36), emoji = "🛍️"),
            draftIsNew = true
        )
    }

    fun editProduct(product: Product) = _state.update {
        it.copy(draft = product, draftIsNew = false)
    }

    fun closeDraft() = _state.update { it.copy(draft = null) }

    fun upsertProduct(product: Product) {
        val exists = _state.value.products.any { it.id == product.id }
        _state.update { state ->
            state.copy(
                products = if (exists) {
                    state.products.map { if (it.id == product.id) product else it }
                } else {
                    state.products + product
                },
                categories = if (product.category.isNotBlank() &&
                    state.categories.none { it.name == product.category }
                ) {
                    state.categories + Category(product.category, "🛍️")
                } else {
                    state.categories
                },
                draft = null,
                dirty = true,
                message = str(R.string.product_saved)
            )
        }
    }

    fun removeProduct(id: String) {
        _state.update {
            it.copy(
                products = it.products.filterNot { product -> product.id == id },
                dirty = true,
                message = str(R.string.product_deleted)
            )
        }
    }

    fun toggleAvailable(id: String) {
        _state.update { state ->
            state.copy(
                products = state.products.map {
                    if (it.id == id) it.copy(available = !it.available) else it
                },
                dirty = true
            )
        }
    }

    fun saveCatalog() {
        val current = _state.value
        if (current.demo || !repository.configured) {
            _state.update { it.copy(message = str(R.string.merchant_demo_badge)) }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { repository.saveCatalog(current.products, current.categories) }.fold(
                onSuccess = { ok ->
                    _state.update {
                        it.copy(
                            busy = false,
                            dirty = !ok,
                            message = str(if (ok) R.string.catalog_saved else R.string.error_load)
                        )
                    }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(
                            busy = false,
                            message = str(R.string.save_failed).format(failure.message.orEmpty())
                        )
                    }
                }
            )
        }
    }

    /* ----------------------- الحانوت ----------------------- */

    fun saveShop(name: String, phone: String, color: String, active: Boolean) {
        _state.update {
            it.copy(shop = it.shop?.copy(name = name, phone = phone, color = color, active = active))
        }
        if (_state.value.demo || !repository.configured) {
            _state.update { it.copy(message = str(R.string.shop_saved)) }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { repository.saveShop(name, phone, color, active) }.fold(
                onSuccess = { ok ->
                    _state.update {
                        it.copy(
                            busy = false,
                            message = str(if (ok) R.string.shop_saved else R.string.error_load)
                        )
                    }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(
                            busy = false,
                            message = str(R.string.save_failed).format(failure.message.orEmpty())
                        )
                    }
                }
            )
        }
    }

    /* ----------------------- رسائل ----------------------- */

    fun postMessage(resId: Int) = _state.update { it.copy(message = str(resId)) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
