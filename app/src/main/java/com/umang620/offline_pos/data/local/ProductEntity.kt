package com.umang620.offline_pos.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double = 0.0,
    val category: String = "General",
    val stockQuantity: Int = 0,
    val sku: String = "",
    val isActive: Boolean = true,
    @ColumnInfo(defaultValue = "PRODUCT")
    val itemType: String = "PRODUCT", // "PRODUCT" vs "RAW_MATERIAL"
    @ColumnInfo(defaultValue = "pcs")
    val unit: String = "pcs" // "pcs", "kg", "g", "L", "mL", "box"
)
