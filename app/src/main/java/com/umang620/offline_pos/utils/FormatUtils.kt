package com.umang620.offline_pos.utils

import java.util.Locale
import kotlin.math.abs

fun formatMoney(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = abs(amount)
    val formattedStr = when {
        absAmount >= 1_000_000_000 -> String.format(Locale.US, "₱%.2fB", absAmount / 1_000_000_000)
        absAmount >= 1_000_000 -> String.format(Locale.US, "₱%.2fM", absAmount / 1_000_000)
        else -> String.format(Locale.US, "₱%,.2f", absAmount)
    }
    return if (isNegative) "-$formattedStr" else formattedStr
}
