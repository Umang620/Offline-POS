package com.umang620.offline_pos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity): Int

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    fun getTotalExpenses(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE paymentMethod = 'Cash'")
    fun getCashExpensesTotal(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE paymentMethod = 'GCash'")
    fun getGCashExpensesTotal(): Flow<Double>
}
