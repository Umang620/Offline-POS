package com.umang620.offline_pos.ui.register

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.AddOnEntity
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.local.ProductEntity
import com.umang620.offline_pos.data.repository.PosRepository
import com.umang620.offline_pos.domain.model.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CheckoutEvent {
    data class Success(val orderId: Long, val isUnpaid: Boolean) : CheckoutEvent()
    data class Error(val message: String) : CheckoutEvent()
}

class PosViewModel(private val repository: PosRepository, context: Context) : ViewModel() {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)

    private val _isAddOnsEnabled = MutableStateFlow(prefs.getBoolean("enable_item_add_ons", true))
    val isAddOnsEnabled: StateFlow<Boolean> = _isAddOnsEnabled.asStateFlow()

    private val _isOrderTypesEnabled = MutableStateFlow(prefs.getBoolean("enable_order_types", true))
    val isOrderTypesEnabled: StateFlow<Boolean> = _isOrderTypesEnabled.asStateFlow()

    private val _isSurchargeEnabled = MutableStateFlow(prefs.getBoolean("enable_surcharge_fee", true))
    val isSurchargeEnabled: StateFlow<Boolean> = _isSurchargeEnabled.asStateFlow()

    private val _surchargeName = MutableStateFlow(prefs.getString("surcharge_name", "Delivery Fee") ?: "Delivery Fee")
    val surchargeName: StateFlow<String> = _surchargeName.asStateFlow()

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "enable_item_add_ons" -> _isAddOnsEnabled.value = sharedPreferences.getBoolean("enable_item_add_ons", true)
            "enable_order_types" -> _isOrderTypesEnabled.value = sharedPreferences.getBoolean("enable_order_types", true)
            "enable_surcharge_fee" -> _isSurchargeEnabled.value = sharedPreferences.getBoolean("enable_surcharge_fee", true)
            "surcharge_name" -> _surchargeName.value = sharedPreferences.getString("surcharge_name", "Delivery Fee") ?: "Delivery Fee"
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _editingOrder = MutableStateFlow<OrderWithItems?>(null)
    val editingOrder: StateFlow<OrderWithItems?> = _editingOrder.asStateFlow()

    private val _orderName = MutableStateFlow("")
    val orderName: StateFlow<String> = _orderName.asStateFlow()

    private val initialQuantities = mutableMapOf<Long, Int>()

    private val _checkoutEvent = MutableSharedFlow<CheckoutEvent>()
    val checkoutEvent: SharedFlow<CheckoutEvent> = _checkoutEvent.asSharedFlow()

    val availableCategories: StateFlow<List<String>> = repository.categories.map { cats ->
        listOf("⭐ Best Sellers") + cats
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("⭐ Best Sellers"))

    val activeAddOns: Flow<List<AddOnEntity>> = repository.activeAddOns

    val filteredProducts = combine(
        repository.activeProducts,
        repository.allOrdersWithItems,
        _searchQuery,
        _selectedCategory
    ) { products, allOrders, query, category ->
        var list = products.filter { product ->
            query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true) ||
                    product.sku.contains(query, ignoreCase = true)
        }
        if (category == "⭐ Best Sellers") {
            val salesCountMap = mutableMapOf<Long, Int>()
            allOrders.filter { it.order.status != "VOIDED" }.forEach { owi ->
                owi.items.forEach { item ->
                    salesCountMap[item.productId] = (salesCountMap[item.productId] ?: 0) + item.quantity
                }
            }
            list = list.sortedByDescending { product -> salesCountMap[product.id] ?: 0 }.take(8)
        } else if (category != null) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        list
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setOrderName(name: String) {
        _orderName.value = name
    }

    fun loadUnpaidOrderForEditing(orderWithItems: OrderWithItems) {
        _editingOrder.value = orderWithItems
        val existingNumber = orderWithItems.order.orderNumber
        val extractedName = if (existingNumber.contains(" - ORD-")) {
            existingNumber.substringBefore(" - ORD-")
        } else ""
        _orderName.value = extractedName

        initialQuantities.clear()
        orderWithItems.items.forEach { item ->
            initialQuantities[item.productId] = item.quantity
        }

        viewModelScope.launch {
            val currentProducts = repository.activeProducts.firstOrNull() ?: emptyList()
            val currentAddOns = repository.activeAddOns.firstOrNull() ?: emptyList()

            val cartList = orderWithItems.items.map { orderItem ->
                val existingProduct = currentProducts.find { it.id == orderItem.productId }
                    ?: ProductEntity(
                        id = orderItem.productId,
                        name = orderItem.productName,
                        price = orderItem.unitPrice,
                        category = "General",
                        stockQuantity = orderItem.quantity
                    )

                val matchedAddOns = mutableListOf<AddOnEntity>()
                val summary = orderItem.addOnsSummary.orEmpty()
                if (summary.isNotBlank()) {
                    val addOnsPart = if (summary.contains("; Note: ")) {
                        summary.substringBefore("; Note: ")
                    } else if (summary.startsWith("Note: ")) {
                        ""
                    } else {
                        summary
                    }

                    if (addOnsPart.isNotBlank()) {
                        val addOnTokens = addOnsPart.split(", ")
                        addOnTokens.forEach { token ->
                            val addOnName = token.substringBefore(" (+₱").trim()
                            val foundAddOn = currentAddOns.find { it.name.equals(addOnName, ignoreCase = true) }
                            if (foundAddOn != null) {
                                matchedAddOns.add(foundAddOn)
                            }
                        }
                    }
                }

                val note = if (summary.contains("Note: ")) {
                    summary.substringAfter("Note: ").trim()
                } else ""

                val matchedAddOnsTotal = matchedAddOns.sumOf { it.price }
                val expectedBasePrice = orderItem.unitPrice - matchedAddOnsTotal
                val adjustedProduct = if (expectedBasePrice > 0 && Math.abs(existingProduct.price - expectedBasePrice) > 0.01) {
                    existingProduct.copy(price = expectedBasePrice)
                } else {
                    existingProduct
                }

                CartItem(
                    product = adjustedProduct,
                    selectedAddOns = matchedAddOns,
                    itemNote = note,
                    quantity = orderItem.quantity
                )
            }
            _cartItems.value = cartList
        }
    }

    fun cancelEditingOrder() {
        _editingOrder.value = null
        _cartItems.value = emptyList()
        _orderName.value = ""
        initialQuantities.clear()
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun addToCart(
        product: ProductEntity,
        selectedAddOns: List<AddOnEntity> = emptyList(),
        itemNote: String = ""
    ) {
        val currentCart = _cartItems.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst {
            it.product.id == product.id &&
                    it.selectedAddOns == selectedAddOns &&
                    it.itemNote == itemNote
        }
        val maxStock = product.stockQuantity + (initialQuantities[product.id] ?: 0)

        if (existingIndex >= 0) {
            val existingItem = currentCart[existingIndex]
            if (existingItem.quantity < maxStock) {
                currentCart[existingIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
            }
        } else {
            if (maxStock > 0) {
                currentCart.add(
                    CartItem(
                        product = product,
                        selectedAddOns = selectedAddOns,
                        itemNote = itemNote,
                        quantity = 1
                    )
                )
            }
        }
        _cartItems.value = currentCart
    }

    fun updateCartQuantity(index: Int, delta: Int) {
        val currentCart = _cartItems.value.toMutableList()
        if (index in currentCart.indices) {
            val item = currentCart[index]
            val maxStock = item.product.stockQuantity + (initialQuantities[item.product.id] ?: 0)
            val newQuantity = item.quantity + delta
            if (newQuantity <= 0) {
                currentCart.removeAt(index)
            } else if (newQuantity <= maxStock) {
                currentCart[index] = item.copy(quantity = newQuantity)
            }
            _cartItems.value = currentCart
        }
    }

    fun removeFromCart(index: Int) {
        val currentCart = _cartItems.value.toMutableList()
        if (index in currentCart.indices) {
            currentCart.removeAt(index)
            _cartItems.value = currentCart
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    suspend fun getOrderWithItemsById(orderId: Long): OrderWithItems? = repository.getOrderWithItemsById(orderId)

    fun processCheckout(
        paymentMethod: String,
        isUnpaid: Boolean = false,
        cashReceived: Double? = null,
        changeAmount: Double? = null,
        gcashRefNumber: String? = null,
        customerName: String = "",
        orderType: String? = null,
        surchargeFee: Double = 0.0,
        surchargeName: String = "Delivery Fee"
    ) {
        viewModelScope.launch {
            val editingId = _editingOrder.value?.order?.id
            val result = repository.processCheckout(
                cartItems = _cartItems.value,
                paymentMethod = paymentMethod,
                isUnpaid = isUnpaid,
                cashReceived = cashReceived,
                changeAmount = changeAmount,
                gcashRefNumber = gcashRefNumber,
                orderName = customerName.ifBlank { _orderName.value },
                editingOrderId = editingId,
                orderType = orderType,
                surchargeFee = surchargeFee,
                surchargeName = surchargeName
            )
            result.onSuccess { orderId ->
                cancelEditingOrder()
                _checkoutEvent.emit(CheckoutEvent.Success(orderId, isUnpaid))
            }.onFailure { exception ->
                _checkoutEvent.emit(CheckoutEvent.Error(exception.message ?: "Checkout failed"))
            }
        }
    }

    class Factory(private val repository: PosRepository, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PosViewModel(repository, context) as T
        }
    }
}
