package com.umang620.offline_pos.data.local

data class ProductSalesSummary(
    val productId: Long,
    val productName: String,
    val totalQuantity: Int,
    val totalAmount: Double
)
