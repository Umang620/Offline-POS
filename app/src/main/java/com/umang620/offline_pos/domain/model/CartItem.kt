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
                val grouped = selectedAddOns.groupBy { it.id }
                val addOnsStr = grouped.values.joinToString(", ") { list ->
                    val addon = list.first()
                    val count = list.size
                    val totalAddOnPrice = addon.price * count
                    val countSuffix = if (count > 1) " x$count" else ""
                    if (totalAddOnPrice > 0) {
                        "${addon.name}$countSuffix (+₱${String.format(Locale.US, "%.2f", totalAddOnPrice)})"
                    } else {
                        "${addon.name}$countSuffix"
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
