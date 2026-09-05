package com.umang620.offline_pos.domain.model

import com.umang620.offline_pos.data.local.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = product.price * quantity
}
