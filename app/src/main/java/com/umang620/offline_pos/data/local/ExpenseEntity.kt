package com.umang620.offline_pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val category: String, // Beans, Ice, Transportation, Electricity, Cleaning supplies, Ingredients, Other
    val paymentMethod: String, // "Cash", "GCash"
    val timestamp: Long = System.currentTimeMillis()
)
