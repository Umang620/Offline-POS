package com.umang620.offline_pos.ui.unpaid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UnpaidOrdersViewModel(private val repository: PosRepository) : ViewModel() {

    val unpaidOrders: StateFlow<List<OrderWithItems>> = repository.unpaidOrdersWithItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unpaidTotal: StateFlow<Double> = repository.unpaidSalesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun payUnpaidOrder(
        orderId: Long,
        paymentMethod: String,
        cashReceived: Double? = null,
        changeAmount: Double? = null,
        gcashRefNumber: String? = null
    ) {
        viewModelScope.launch {
            repository.markOrderAsPaid(
                orderId = orderId,
                paymentMethod = paymentMethod,
                cashReceived = cashReceived,
                changeAmount = changeAmount,
                gcashRefNumber = gcashRefNumber
            )
        }
    }

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UnpaidOrdersViewModel(repository) as T
        }
    }
}
