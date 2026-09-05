package com.umang620.offline_pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val paymentMethod: String,
    val totalItems: Int,
    val status: String = "PAID", // "PAID", "UNPAID", "VOIDED"
    val cashReceived: Double? = null,
    val changeAmount: Double? = null,
    val gcashRefNumber: String? = null,
    val voidedTimestamp: Long? = null
)
