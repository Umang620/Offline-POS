package com.umang620.offline_pos.utils

import android.content.Context
import android.content.SharedPreferences
import com.umang620.offline_pos.data.local.AddOnEntity
import com.umang620.offline_pos.data.local.ExpenseEntity
import com.umang620.offline_pos.data.local.OrderEntity
import com.umang620.offline_pos.data.local.OrderItemEntity
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.local.ProductEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val products: List<ProductEntity>,
    val addOns: List<AddOnEntity> = emptyList(),
    val ordersWithItems: List<OrderWithItems>,
    val expenses: List<ExpenseEntity>,
    val settings: Map<String, Any>
)

object JsonBackupHelper {

    fun createJsonBackup(
        products: List<ProductEntity>,
        addOns: List<AddOnEntity> = emptyList(),
        ordersWithItems: List<OrderWithItems>,
        expenses: List<ExpenseEntity>,
        prefs: SharedPreferences
    ): String {
        val root = JSONObject()
        root.put("app", "Offline POS")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val productsArray = JSONArray()
        for (product in products) {
            val pObj = JSONObject()
            pObj.put("id", product.id)
            pObj.put("name", product.name)
            pObj.put("price", product.price)
            pObj.put("category", product.category)
            pObj.put("stockQuantity", product.stockQuantity)
            pObj.put("sku", product.sku)
            pObj.put("isActive", product.isActive)
            pObj.put("itemType", product.itemType)
            pObj.put("unit", product.unit)
            productsArray.put(pObj)
        }
        root.put("products", productsArray)

        val addOnsArray = JSONArray()
        for (addOn in addOns) {
            val aObj = JSONObject()
            aObj.put("id", addOn.id)
            aObj.put("name", addOn.name)
            aObj.put("price", addOn.price)
            aObj.put("category", addOn.category)
            aObj.put("isActive", addOn.isActive)
            addOnsArray.put(aObj)
        }
        root.put("addOns", addOnsArray)

        val ordersArray = JSONArray()
        for (orderWithItems in ordersWithItems) {
            val order = orderWithItems.order
            val oObj = JSONObject()
            oObj.put("id", order.id)
            oObj.put("orderNumber", order.orderNumber)
            oObj.put("timestamp", order.timestamp)
            oObj.put("totalAmount", order.totalAmount)
            oObj.put("paymentMethod", order.paymentMethod)
            oObj.put("totalItems", order.totalItems)
            oObj.put("status", order.status)
            oObj.put("cashReceived", order.cashReceived ?: JSONObject.NULL)
            oObj.put("changeAmount", order.changeAmount ?: JSONObject.NULL)
            oObj.put("gcashRefNumber", order.gcashRefNumber ?: JSONObject.NULL)
            oObj.put("voidedTimestamp", order.voidedTimestamp ?: JSONObject.NULL)
            oObj.put("orderType", order.orderType ?: JSONObject.NULL)
            oObj.put("surchargeFee", order.surchargeFee)
            oObj.put("surchargeName", order.surchargeName)

            val itemsArray = JSONArray()
            for (item in orderWithItems.items) {
                val iObj = JSONObject()
                iObj.put("id", item.id)
                iObj.put("orderId", item.orderId)
                iObj.put("productId", item.productId)
                iObj.put("productName", item.productName)
                iObj.put("unitPrice", item.unitPrice)
                iObj.put("quantity", item.quantity)
                iObj.put("subtotal", item.subtotal)
                iObj.put("addOnsSummary", item.addOnsSummary)
                itemsArray.put(iObj)
            }
            oObj.put("items", itemsArray)
            ordersArray.put(oObj)
        }
        root.put("orders", ordersArray)

        val expensesArray = JSONArray()
        for (expense in expenses) {
            val eObj = JSONObject()
            eObj.put("id", expense.id)
            eObj.put("amount", expense.amount)
            eObj.put("description", expense.description)
            eObj.put("category", expense.category)
            eObj.put("paymentMethod", expense.paymentMethod)
            eObj.put("timestamp", expense.timestamp)
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        val settingsObj = JSONObject()
        settingsObj.put("manager_pin", prefs.getString("manager_pin", "") ?: "")
        settingsObj.put("is_pin_required", prefs.getBoolean("is_pin_required", true))
        settingsObj.put("is_void_pin_required", prefs.getBoolean("is_void_pin_required", false))
        settingsObj.put("is_auto_print_enabled", prefs.getBoolean("is_auto_print_enabled", true))
        settingsObj.put("enable_item_add_ons", prefs.getBoolean("enable_item_add_ons", true))
        settingsObj.put("enable_order_types", prefs.getBoolean("enable_order_types", true))
        settingsObj.put("enable_surcharge_fee", prefs.getBoolean("enable_surcharge_fee", true))
        settingsObj.put("surcharge_name", prefs.getString("surcharge_name", "Delivery Fee") ?: "Delivery Fee")
        settingsObj.put("default_cash_float", prefs.getFloat("default_cash_float", 1000.0f))
        settingsObj.put("selected_printer_address", prefs.getString("selected_printer_address", "") ?: "")
        settingsObj.put("selected_printer_name", prefs.getString("selected_printer_name", "No Printer Selected") ?: "No Printer Selected")
        settingsObj.put("selected_printer_type", prefs.getString("selected_printer_type", "BLUETOOTH") ?: "BLUETOOTH")
        settingsObj.put("is_dark_mode", prefs.getBoolean("is_dark_mode", false))
        root.put("settings", settingsObj)

        return root.toString(2)
    }

    fun parseJsonBackup(jsonString: String): BackupPayload {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

        val productsList = mutableListOf<ProductEntity>()
        if (root.has("products")) {
            val productsArray = root.getJSONArray("products")
            for (i in 0 until productsArray.length()) {
                val pObj = productsArray.getJSONObject(i)
                productsList.add(
                    ProductEntity(
                        id = pObj.optLong("id", 0L),
                        name = pObj.optString("name", ""),
                        price = pObj.optDouble("price", 0.0),
                        category = pObj.optString("category", "General"),
                        stockQuantity = pObj.optInt("stockQuantity", 0),
                        sku = pObj.optString("sku", ""),
                        isActive = pObj.optBoolean("isActive", true),
                        itemType = pObj.optString("itemType", "PRODUCT"),
                        unit = pObj.optString("unit", "pcs")
                    )
                )
            }
        }

        val addOnsList = mutableListOf<AddOnEntity>()
        if (root.has("addOns")) {
            val addOnsArray = root.getJSONArray("addOns")
            for (i in 0 until addOnsArray.length()) {
                val aObj = addOnsArray.getJSONObject(i)
                addOnsList.add(
                    AddOnEntity(
                        id = aObj.optLong("id", 0L),
                        name = aObj.optString("name", ""),
                        price = aObj.optDouble("price", 0.0),
                        category = aObj.optString("category", "General"),
                        isActive = aObj.optBoolean("isActive", true)
                    )
                )
            }
        }

        val ordersList = mutableListOf<OrderWithItems>()
        if (root.has("orders")) {
            val ordersArray = root.getJSONArray("orders")
            for (i in 0 until ordersArray.length()) {
                val oObj = ordersArray.getJSONObject(i)
                val cashReceived = if (oObj.isNull("cashReceived")) null else oObj.optDouble("cashReceived")
                val changeAmount = if (oObj.isNull("changeAmount")) null else oObj.optDouble("changeAmount")
                val gcashRefNumber = if (oObj.isNull("gcashRefNumber")) null else oObj.optString("gcashRefNumber")
                val voidedTimestamp = if (oObj.isNull("voidedTimestamp")) null else oObj.optLong("voidedTimestamp")
                val orderType = if (oObj.isNull("orderType")) null else oObj.optString("orderType")
                val surchargeFee = oObj.optDouble("surchargeFee", 0.0)
                val surchargeName = oObj.optString("surchargeName", "Delivery Fee")

                val order = OrderEntity(
                    id = oObj.optLong("id", 0L),
                    orderNumber = oObj.optString("orderNumber", ""),
                    timestamp = oObj.optLong("timestamp", System.currentTimeMillis()),
                    totalAmount = oObj.optDouble("totalAmount", 0.0),
                    paymentMethod = oObj.optString("paymentMethod", "Cash"),
                    totalItems = oObj.optInt("totalItems", 0),
                    status = oObj.optString("status", "PAID"),
                    cashReceived = cashReceived,
                    changeAmount = changeAmount,
                    gcashRefNumber = gcashRefNumber,
                    voidedTimestamp = voidedTimestamp,
                    orderType = orderType,
                    surchargeFee = surchargeFee,
                    surchargeName = surchargeName
                )

                val itemsList = mutableListOf<OrderItemEntity>()
                if (oObj.has("items")) {
                    val itemsArray = oObj.getJSONArray("items")
                    for (j in 0 until itemsArray.length()) {
                        val iObj = itemsArray.getJSONObject(j)
                        itemsList.add(
                            OrderItemEntity(
                                id = iObj.optLong("id", 0L),
                                orderId = iObj.optLong("orderId", order.id),
                                productId = iObj.optLong("productId", 0L),
                                productName = iObj.optString("productName", ""),
                                unitPrice = iObj.optDouble("unitPrice", 0.0),
                                quantity = iObj.optInt("quantity", 1),
                                subtotal = iObj.optDouble("subtotal", 0.0),
                                addOnsSummary = iObj.optString("addOnsSummary", "")
                            )
                        )
                    }
                }
                ordersList.add(OrderWithItems(order, itemsList))
            }
        }

        val expensesList = mutableListOf<ExpenseEntity>()
        if (root.has("expenses")) {
            val expensesArray = root.getJSONArray("expenses")
            for (i in 0 until expensesArray.length()) {
                val eObj = expensesArray.getJSONObject(i)
                expensesList.add(
                    ExpenseEntity(
                        id = eObj.optLong("id", 0L),
                        amount = eObj.optDouble("amount", 0.0),
                        description = eObj.optString("description", ""),
                        category = eObj.optString("category", "General"),
                        paymentMethod = eObj.optString("paymentMethod", "Cash"),
                        timestamp = eObj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        val settingsMap = mutableMapOf<String, Any>()
        if (root.has("settings")) {
            val settingsObj = root.getJSONObject("settings")
            if (settingsObj.has("manager_pin")) settingsMap["manager_pin"] = settingsObj.optString("manager_pin")
            if (settingsObj.has("is_pin_required")) settingsMap["is_pin_required"] = settingsObj.optBoolean("is_pin_required")
            if (settingsObj.has("is_void_pin_required")) settingsMap["is_void_pin_required"] = settingsObj.optBoolean("is_void_pin_required")
            if (settingsObj.has("is_auto_print_enabled")) settingsMap["is_auto_print_enabled"] = settingsObj.optBoolean("is_auto_print_enabled")
            if (settingsObj.has("enable_item_add_ons")) settingsMap["enable_item_add_ons"] = settingsObj.optBoolean("enable_item_add_ons")
            if (settingsObj.has("enable_order_types")) settingsMap["enable_order_types"] = settingsObj.optBoolean("enable_order_types")
            if (settingsObj.has("enable_surcharge_fee")) settingsMap["enable_surcharge_fee"] = settingsObj.optBoolean("enable_surcharge_fee")
            if (settingsObj.has("surcharge_name")) settingsMap["surcharge_name"] = settingsObj.optString("surcharge_name")
            if (settingsObj.has("default_cash_float")) settingsMap["default_cash_float"] = settingsObj.optDouble("default_cash_float")
            if (settingsObj.has("selected_printer_address")) settingsMap["selected_printer_address"] = settingsObj.optString("selected_printer_address")
            if (settingsObj.has("selected_printer_name")) settingsMap["selected_printer_name"] = settingsObj.optString("selected_printer_name")
            if (settingsObj.has("selected_printer_type")) settingsMap["selected_printer_type"] = settingsObj.optString("selected_printer_type")
            if (settingsObj.has("is_dark_mode")) settingsMap["is_dark_mode"] = settingsObj.optBoolean("is_dark_mode")
        }

        return BackupPayload(
            version = version,
            exportedAt = exportedAt,
            products = productsList,
            addOns = addOnsList,
            ordersWithItems = ordersList,
            expenses = expensesList,
            settings = settingsMap
        )
    }

    fun applySettings(context: Context, settingsMap: Map<String, Any>) {
        val editor = context.getSharedPreferences("pos_settings", Context.MODE_PRIVATE).edit()
        for ((key, value) in settingsMap) {
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        editor.apply()
    }
}
