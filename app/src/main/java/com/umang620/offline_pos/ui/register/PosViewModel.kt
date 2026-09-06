package com.umang620.offline_pos.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.local.ProductEntity
import com.umang620.offline_pos.data.repository.PosRepository
import com.umang620.offline_pos.domain.model.CartItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class CheckoutEvent {
    data class Success(val orderId: Long, val isUnpaid: Boolean) : CheckoutEvent()
    data class Error(val message: String) : CheckoutEvent()
}

class PosViewModel(private val repository: PosRepository) : ViewModel() {

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

    private val _categoriesFlow = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _categoriesFlow.asStateFlow()

    val filteredProducts = combine(
        repository.activeProducts,
        _searchQuery,
        _selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true) ||
                    product.sku.contains(query, ignoreCase = true)
            val matchesCategory = category == null || product.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    init {
        viewModelScope.launch {
            repository.categories.collect { cats ->
                _categoriesFlow.value = cats
            }
        }
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
            val cartList = orderWithItems.items.map { orderItem ->
                val existingProduct = currentProducts.find { it.id == orderItem.productId }
                    ?: ProductEntity(
                        id = orderItem.productId,
                        name = orderItem.productName,
                        price = orderItem.unitPrice,
                        category = "General",
                        stockQuantity = orderItem.quantity
                    )
                CartItem(product = existingProduct, quantity = orderItem.quantity)
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

    fun addToCart(product: ProductEntity) {
        val currentCart = _cartItems.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
        val maxStock = product.stockQuantity + (initialQuantities[product.id] ?: 0)

        if (existingIndex >= 0) {
            val existingItem = currentCart[existingIndex]
            if (existingItem.quantity < maxStock) {
                currentCart[existingIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
            }
        } else {
            if (maxStock > 0) {
                currentCart.add(CartItem(product = product, quantity = 1))
            }
        }
        _cartItems.value = currentCart
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        val currentCart = _cartItems.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }

        if (index >= 0) {
            val item = currentCart[index]
            val maxStock = item.product.stockQuantity + (initialQuantities[productId] ?: 0)
            val newQuantity = item.quantity + delta
            if (newQuantity <= 0) {
                currentCart.removeAt(index)
            } else if (newQuantity <= maxStock) {
                currentCart[index] = item.copy(quantity = newQuantity)
            }
            _cartItems.value = currentCart
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun processCheckout(
        paymentMethod: String,
        isUnpaid: Boolean = false,
        cashReceived: Double? = null,
        changeAmount: Double? = null,
        gcashRefNumber: String? = null,
        customerName: String = ""
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
                editingOrderId = editingId
            )
            result.onSuccess { orderId ->
                cancelEditingOrder()
                _checkoutEvent.emit(CheckoutEvent.Success(orderId, isUnpaid))
            }.onFailure { exception ->
                _checkoutEvent.emit(CheckoutEvent.Error(exception.message ?: "Checkout failed"))
            }
        }
    }

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PosViewModel(repository) as T
        }
    }
}
