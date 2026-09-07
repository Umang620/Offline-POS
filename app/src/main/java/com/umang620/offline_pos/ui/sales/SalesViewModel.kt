package com.umang620.offline_pos.ui.sales

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

enum class SalesFilter(val label: String) {
    ALL("All"),
    CASH("Cash"),
    GCASH("GCash"),
    VOIDED("Voided")
}

class SalesViewModel(private val repository: PosRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SalesFilter.ALL)
    val selectedFilter: StateFlow<SalesFilter> = _selectedFilter.asStateFlow()

    fun updateSelectedDate(timestamp: Long) {
        _selectedDate.value = getStartOfDay(timestamp)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: SalesFilter) {
        _selectedFilter.value = filter
    }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val ordersWithItems: StateFlow<List<OrderWithItems>> = _selectedDate.flatMapLatest { date ->
        val start = getStartOfDay(date)
        val end = getEndOfDay(date)
        repository.allOrdersWithItems.map { list ->
            list.filter { it.order.timestamp in start..end }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredOrdersWithItems: StateFlow<List<OrderWithItems>> = combine(
        ordersWithItems,
        searchQuery,
        selectedFilter
    ) { orders, query, filter ->
        orders.filter { orderWithItems ->
            val order = orderWithItems.order
            val matchesFilter = when (filter) {
                SalesFilter.ALL -> true
                SalesFilter.CASH -> order.paymentMethod.equals("CASH", ignoreCase = true) && order.status != "VOIDED"
                SalesFilter.GCASH -> order.paymentMethod.equals("GCASH", ignoreCase = true) && order.status != "VOIDED"
                SalesFilter.VOIDED -> order.status == "VOIDED"
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                order.id.toString().contains(q) ||
                        order.orderNumber.lowercase().contains(q) ||
                        order.paymentMethod.lowercase().contains(q) ||
                        (order.gcashRefNumber?.lowercase()?.contains(q) == true) ||
                        orderWithItems.items.any { it.productName.lowercase().contains(q) }
            }

            matchesFilter && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val paidOrdersWithItems: StateFlow<List<OrderWithItems>> = _selectedDate.flatMapLatest { date ->
        val start = getStartOfDay(date)
        val end = getEndOfDay(date)
        repository.paidOrdersWithItems.map { list ->
            list.filter { it.order.timestamp in start..end }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayRevenue: StateFlow<Double> = repository.getTotalRevenueByDate(
        getStartOfDay(System.currentTimeMillis()),
        getEndOfDay(System.currentTimeMillis())
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalRevenue: StateFlow<Double> = _selectedDate.flatMapLatest { date ->
        repository.getTotalRevenueByDate(getStartOfDay(date), getEndOfDay(date))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalOrderCount: StateFlow<Int> = paidOrdersWithItems.map { list ->
        list.size
    }.stateIn(
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
