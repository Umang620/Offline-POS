package com.umang620.offline_pos.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun addToCart(product: ProductEntity) {
        val currentCart = _cartItems.value.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }

        if (existingIndex >= 0) {
            val existingItem = currentCart[existingIndex]
            if (existingItem.quantity < product.stockQuantity) {
                currentCart[existingIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
            }
        } else {
            if (product.stockQuantity > 0) {
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
            val newQuantity = item.quantity + delta
            if (newQuantity <= 0) {
                currentCart.removeAt(index)
            } else if (newQuantity <= item.product.stockQuantity) {
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
        gcashRefNumber: String? = null
    ) {
        viewModelScope.launch {
            val result = repository.processCheckout(
                cartItems = _cartItems.value,
                paymentMethod = paymentMethod,
                isUnpaid = isUnpaid,
                cashReceived = cashReceived,
                changeAmount = changeAmount,
                gcashRefNumber = gcashRefNumber
            )
            result.onSuccess { orderId ->
                clearCart()
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
