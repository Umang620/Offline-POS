package com.umang620.offline_pos.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.umang620.offline_pos.data.local.ExpenseEntity
import com.umang620.offline_pos.data.repository.PosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpensesViewModel(private val repository: PosRepository) : ViewModel() {

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalExpenses: StateFlow<Double> = repository.totalExpensesTotal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun addExpense(amount: Double, description: String, category: String, paymentMethod: String) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                amount = amount,
                description = description,
                category = category,
                paymentMethod = paymentMethod,
                timestamp = System.currentTimeMillis()
            )
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    class Factory(private val repository: PosRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExpensesViewModel(repository) as T
        }
    }
}
