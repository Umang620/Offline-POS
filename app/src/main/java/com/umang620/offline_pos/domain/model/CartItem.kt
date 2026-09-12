package com.umang620.offline_pos.domain.model

import com.umang620.offline_pos.data.local.AddOnEntity
import com.umang620.offline_pos.data.local.ProductEntity
import java.util.Locale

data class CartItem(
    val product: ProductEntity,
    val selectedAddOns: List<AddOnEntity> = emptyList(),
    val itemNote: String = "",
    val quantity: Int = 1
) {
    val unitPriceWithAddOns: Double
        get() = product.price + selectedAddOns.sumOf { it.price }

    val subtotal: Double
        get() = unitPriceWithAddOns * quantity

    val addOnsSummary: String
        get() {
            val parts = mutableListOf<String>()
            if (selectedAddOns.isNotEmpty()) {
                val addOnsStr = selectedAddOns.joinToString(", ") { addon ->
                    if (addon.price > 0) {
                        "${addon.name} (+₱${String.format(Locale.US, "%.2f", addon.price)})"
                    } else {
                        addon.name
                    }
                }
                parts.add(addOnsStr)
            }
            if (itemNote.isNotBlank()) {
                parts.add("Note: $itemNote")
            }
            return parts.joinToString("; ")
        }
}
