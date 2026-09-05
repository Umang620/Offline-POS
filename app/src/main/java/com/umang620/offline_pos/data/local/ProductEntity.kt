package com.umang620.offline_pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val category: String,
    val stockQuantity: Int,
    val sku: String = "",
    val isActive: Boolean = true
)
