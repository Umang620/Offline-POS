package com.umang620.offline_pos.utils

import android.content.Context
import android.content.SharedPreferences
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
    val ordersWithItems: List<OrderWithItems>,
    val expenses: List<ExpenseEntity>,
    val settings: Map<String, Any>
)

object JsonBackupHelper {

    fun createJsonBackup(
        products: List<ProductEntity>,
        ordersWithItems: List<OrderWithItems>,
        expenses: List<ExpenseEntity>,
        prefs: SharedPreferences
    ): String {
        val root = JSONObject()
        root.put("app", "Offline POS")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Products
        val productsArray = JSONArray()
        products.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("name", p.name)
            pObj.put("price", p.price)
            pObj.put("category", p.category)
            pObj.put("stockQuantity", p.stockQuantity)
            pObj.put("sku", p.sku)
            pObj.put("isActive", p.isActive)
            pObj.put("itemType", p.itemType)
            pObj.put("unit", p.unit)
            productsArray.put(pObj)
        }
        root.put("products", productsArray)

        // 2. Orders & Order Items
        val ordersArray = JSONArray()
        ordersWithItems.forEach { owi ->
            val o = owi.order
            val oObj = JSONObject()
            oObj.put("id", o.id)
            oObj.put("orderNumber", o.orderNumber)
            oObj.put("timestamp", o.timestamp)
            oObj.put("totalAmount", o.totalAmount)
            oObj.put("paymentMethod", o.paymentMethod)
            oObj.put("totalItems", o.totalItems)
            oObj.put("status", o.status)
            if (o.cashReceived != null) oObj.put("cashReceived", o.cashReceived) else oObj.put("cashReceived", JSONObject.NULL)
            if (o.changeAmount != null) oObj.put("changeAmount", o.changeAmount) else oObj.put("changeAmount", JSONObject.NULL)
            if (o.gcashRefNumber != null) oObj.put("gcashRefNumber", o.gcashRefNumber) else oObj.put("gcashRefNumber", JSONObject.NULL)
            if (o.voidedTimestamp != null) oObj.put("voidedTimestamp", o.voidedTimestamp) else oObj.put("voidedTimestamp", JSONObject.NULL)

            val itemsArray = JSONArray()
            owi.items.forEach { item ->
                val iObj = JSONObject()
                iObj.put("id", item.id)
                iObj.put("orderId", item.orderId)
                iObj.put("productId", item.productId)
                iObj.put("productName", item.productName)
                iObj.put("unitPrice", item.unitPrice)
                iObj.put("quantity", item.quantity)
                iObj.put("subtotal", item.subtotal)
                itemsArray.put(iObj)
            }
            oObj.put("items", itemsArray)
            ordersArray.put(oObj)
        }
        root.put("orders", ordersArray)

        // 3. Expenses
        val expensesArray = JSONArray()
        expenses.forEach { e ->
            val eObj = JSONObject()
            eObj.put("id", e.id)
            eObj.put("amount", e.amount)
            eObj.put("description", e.description)
            eObj.put("category", e.category)
            eObj.put("paymentMethod", e.paymentMethod)
            eObj.put("timestamp", e.timestamp)
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        // 4. Settings
        val settingsObj = JSONObject()
        settingsObj.put("manager_pin", prefs.getString("manager_pin", "") ?: "")
        settingsObj.put("is_pin_required", prefs.getBoolean("is_pin_required", true))
        settingsObj.put("is_void_pin_required", prefs.getBoolean("is_void_pin_required", false))
        settingsObj.put("is_auto_print_enabled", prefs.getBoolean("is_auto_print_enabled", true))
        settingsObj.put("default_cash_float", prefs.getFloat("default_cash_float", 1000f).toDouble())
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

        // 1. Products
        val products = mutableListOf<ProductEntity>()
        if (root.has("products")) {
            val pArray = root.getJSONArray("products")
            for (i in 0 until pArray.length()) {
                val pObj = pArray.getJSONObject(i)
                products.add(
                    ProductEntity(
                        id = pObj.optLong("id", 0),
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

        // 2. Orders & Order Items
        val ordersWithItems = mutableListOf<OrderWithItems>()
        if (root.has("orders")) {
            val oArray = root.getJSONArray("orders")
            for (i in 0 until oArray.length()) {
                val oObj = oArray.getJSONObject(i)

                val cashReceived = if (oObj.isNull("cashReceived")) null else oObj.optDouble("cashReceived")
                val changeAmount = if (oObj.isNull("changeAmount")) null else oObj.optDouble("changeAmount")
                val gcashRefNumber = if (oObj.isNull("gcashRefNumber")) null else oObj.optString("gcashRefNumber")
                val voidedTimestamp = if (oObj.isNull("voidedTimestamp")) null else oObj.optLong("voidedTimestamp")

                val order = OrderEntity(
                    id = oObj.optLong("id", 0),
                    orderNumber = oObj.optString("orderNumber", ""),
                    timestamp = oObj.optLong("timestamp", System.currentTimeMillis()),
                    totalAmount = oObj.optDouble("totalAmount", 0.0),
                    paymentMethod = oObj.optString("paymentMethod", "Cash"),
                    totalItems = oObj.optInt("totalItems", 0),
                    status = oObj.optString("status", "PAID"),
                    cashReceived = cashReceived,
                    changeAmount = changeAmount,
                    gcashRefNumber = gcashRefNumber,
                    voidedTimestamp = voidedTimestamp
                )

                val items = mutableListOf<OrderItemEntity>()
                if (oObj.has("items")) {
                    val iArray = oObj.getJSONArray("items")
                    for (j in 0 until iArray.length()) {
                        val iObj = iArray.getJSONObject(j)
                        items.add(
                            OrderItemEntity(
                                id = iObj.optLong("id", 0),
                                orderId = iObj.optLong("orderId", order.id),
                                productId = iObj.optLong("productId", 0),
                                productName = iObj.optString("productName", ""),
                                unitPrice = iObj.optDouble("unitPrice", 0.0),
                                quantity = iObj.optInt("quantity", 1),
                                subtotal = iObj.optDouble("subtotal", 0.0)
                            )
                        )
                    }
                }

                ordersWithItems.add(OrderWithItems(order = order, items = items))
            }
        }

        // 3. Expenses
        val expenses = mutableListOf<ExpenseEntity>()
        if (root.has("expenses")) {
            val eArray = root.getJSONArray("expenses")
            for (i in 0 until eArray.length()) {
                val eObj = eArray.getJSONObject(i)
                expenses.add(
                    ExpenseEntity(
                        id = eObj.optLong("id", 0),
                        amount = eObj.optDouble("amount", 0.0),
                        description = eObj.optString("description", ""),
                        category = eObj.optString("category", "General"),
                        paymentMethod = eObj.optString("paymentMethod", "Cash"),
                        timestamp = eObj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // 4. Settings
        val settingsMap = mutableMapOf<String, Any>()
        if (root.has("settings")) {
            val sObj = root.getJSONObject("settings")
            if (sObj.has("manager_pin")) settingsMap["manager_pin"] = sObj.optString("manager_pin")
            if (sObj.has("is_pin_required")) settingsMap["is_pin_required"] = sObj.optBoolean("is_pin_required")
            if (sObj.has("is_void_pin_required")) settingsMap["is_void_pin_required"] = sObj.optBoolean("is_void_pin_required")
            if (sObj.has("is_auto_print_enabled")) settingsMap["is_auto_print_enabled"] = sObj.optBoolean("is_auto_print_enabled")
            if (sObj.has("default_cash_float")) settingsMap["default_cash_float"] = sObj.optDouble("default_cash_float")
            if (sObj.has("selected_printer_address")) settingsMap["selected_printer_address"] = sObj.optString("selected_printer_address")
            if (sObj.has("selected_printer_name")) settingsMap["selected_printer_name"] = sObj.optString("selected_printer_name")
            if (sObj.has("selected_printer_type")) settingsMap["selected_printer_type"] = sObj.optString("selected_printer_type")
            if (sObj.has("is_dark_mode")) settingsMap["is_dark_mode"] = sObj.optBoolean("is_dark_mode")
        }

        return BackupPayload(
            version = version,
            exportedAt = exportedAt,
            products = products,
            ordersWithItems = ordersWithItems,
            expenses = expenses,
            settings = settingsMap
        )
    }

    fun applySettings(context: Context, settingsMap: Map<String, Any>) {
        val prefs = context.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        settingsMap.forEach { (key, value) ->
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
