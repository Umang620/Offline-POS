package com.umang620.offline_pos.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DailySummaryViewModel(repository: PosRepository) : ViewModel() {

    val cashSales: StateFlow<Double> = repository.cashSalesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val gcashSales: StateFlow<Double> = repository.gcashSalesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val totalSales: StateFlow<Double> = repository.totalRevenue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val cashExpenses: StateFlow<Double> = repository.cashExpensesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val gcashExpenses: StateFlow<Double> = repository.gcashExpensesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val totalExpenses: StateFlow<Double> = repository.totalExpensesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val unpaidTotal: StateFlow<Double> = repository.unpaidSalesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val cashOnHand: StateFlow<Double> = combine(cashSales, cashExpenses) { sales, expenses ->
        sales - expenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val gcashRecorded: StateFlow<Double> = combine(gcashSales, gcashExpenses) { sales, expenses ->
        sales - expenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DailySummaryViewModel(repository) as T
        }
    }
}
