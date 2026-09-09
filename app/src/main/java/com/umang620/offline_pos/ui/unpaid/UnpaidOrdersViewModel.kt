package com.umang620.offline_pos.ui.unpaid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class UnpaidOrdersViewModel(private val repository: PosRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSelectedDate(timestamp: Long) {
        _selectedDate.value = getStartOfDay(timestamp)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun getOrderWithItemsById(orderId: Long): OrderWithItems? = repository.getOrderWithItemsById(orderId)

    companion object {
        fun getStartOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun getEndOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return calendar.timeInMillis
        }
    }

    val unpaidOrders: StateFlow<List<OrderWithItems>> = combine(
        _selectedDate,
        _searchQuery,
        repository.unpaidOrdersWithItems
    ) { date, query, allUnpaid ->
        val start = getStartOfDay(date)
        val end = getEndOfDay(date)
        val q = query.trim().lowercase()
        allUnpaid.filter { item ->
            val inDateRange = item.order.timestamp in start..end
            if (!inDateRange) return@filter false
            if (q.isBlank()) return@filter true

            val matchesOrderNumber = item.order.orderNumber.lowercase().contains(q)
            val matchesOrderId = item.order.id.toString().contains(q)
            val matchesItems = item.items.any { it.productName.lowercase().contains(q) }
            matchesOrderNumber || matchesOrderId || matchesItems
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val unpaidTotal: StateFlow<Double> = _selectedDate.flatMapLatest { date ->
        repository.getUnpaidSalesTotalByDate(getStartOfDay(date), getEndOfDay(date))
    }.stateIn(
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
