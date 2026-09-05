package com.umang620.offline_pos.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SalesViewModel(private val repository: PosRepository) : ViewModel() {

    val ordersWithItems: StateFlow<List<OrderWithItems>> = repository.allOrdersWithItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val paidOrdersWithItems: StateFlow<List<OrderWithItems>> = repository.paidOrdersWithItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalRevenue: StateFlow<Double> = repository.totalRevenue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val totalOrderCount: StateFlow<Int> = repository.totalOrderCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun voidOrder(orderId: Long) {
        viewModelScope.launch {
            repository.voidOrder(orderId)
        }
    }

    fun deleteOrder(orderId: Long) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
        }
    }

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SalesViewModel(repository) as T
        }
    }
}
