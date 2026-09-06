package com.umang620.offline_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.ProductEntity
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: PosRepository) : ViewModel() {

    val products: StateFlow<List<ProductEntity>> = repository.getProductsByType("PRODUCT").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rawMaterials: StateFlow<List<ProductEntity>> = repository.getProductsByType("RAW_MATERIAL").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addInventoryItem(
        name: String,
        price: Double = 0.0,
        category: String = "General",
        stockQuantity: Int,
        sku: String = "",
        isActive: Boolean = true,
        itemType: String = "PRODUCT",
        unit: String = "pcs"
    ) {
        viewModelScope.launch {
            val newProduct = ProductEntity(
                name = name,
                price = price,
                category = category,
                stockQuantity = stockQuantity,
                sku = sku,
                isActive = isActive,
                itemType = itemType,
                unit = unit
            )
            repository.insertProduct(newProduct)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InventoryViewModel(repository) as T
        }
    }
}
