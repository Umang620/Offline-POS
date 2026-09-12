package com.umang620.offline_pos.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.AddOnEntity
import com.umang620.offline_pos.data.local.ProductEntity
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: PosRepository) : ViewModel() {

    val products: StateFlow<List<ProductEntity>> = repository.getProductsByType("PRODUCT")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val rawMaterials: StateFlow<List<ProductEntity>> = repository.getProductsByType("RAW_MATERIAL")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val addOns: StateFlow<List<AddOnEntity>> = repository.allAddOns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addInventoryItem(
        name: String,
        price: Double = 0.0,
        category: String = "General",
        stockQuantity: Int = 0,
        sku: String = "",
        isActive: Boolean = true,
        itemType: String = "PRODUCT",
        unit: String = "pcs"
    ) {
        viewModelScope.launch {
            repository.insertProduct(
                ProductEntity(
                    name = name,
                    price = price,
                    category = category,
                    stockQuantity = stockQuantity,
                    sku = sku,
                    isActive = isActive,
                    itemType = itemType,
                    unit = unit
                )
            )
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

    fun addAddOn(name: String, price: Double, category: String = "General") {
        viewModelScope.launch {
            repository.insertAddOn(
                AddOnEntity(
                    name = name,
                    price = price,
                    category = category
                )
            )
        }
    }

    fun updateAddOn(addOn: AddOnEntity) {
        viewModelScope.launch {
            repository.updateAddOn(addOn)
        }
    }

    fun deleteAddOn(addOn: AddOnEntity) {
        viewModelScope.launch {
            repository.deleteAddOn(addOn)
        }
    }

    fun toggleAddOnStatus(addOn: AddOnEntity) {
        viewModelScope.launch {
            repository.setAddOnStatus(addOn.id, !addOn.isActive)
        }
    }

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InventoryViewModel(repository) as T
        }
    }
}
