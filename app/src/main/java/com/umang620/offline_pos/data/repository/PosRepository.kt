package com.umang620.offline_pos.data.repository

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.umang620.offline_pos.data.local.ExpenseDao
import com.umang620.offline_pos.data.local.ExpenseEntity
import com.umang620.offline_pos.data.local.OrderDao
import com.umang620.offline_pos.data.local.OrderEntity
import com.umang620.offline_pos.data.local.OrderItemEntity
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.local.ProductDao
import com.umang620.offline_pos.data.local.ProductEntity
import com.umang620.offline_pos.data.local.ProductSalesSummary
import com.umang620.offline_pos.domain.model.CartItem
import com.umang620.offline_pos.utils.JsonBackupHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val expenseDao: ExpenseDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val activeProducts: Flow<List<ProductEntity>> = productDao.getActiveProducts()
    val categories: Flow<List<String>> = productDao.getCategories()

    fun getProductsByType(itemType: String): Flow<List<ProductEntity>> = productDao.getProductsByType(itemType)

    val allOrdersWithItems: Flow<List<OrderWithItems>> = orderDao.getAllOrdersWithItems()
    val paidOrdersWithItems: Flow<List<OrderWithItems>> = orderDao.getPaidOrdersWithItems()
    val unpaidOrdersWithItems: Flow<List<OrderWithItems>> = orderDao.getUnpaidOrdersWithItems()

    suspend fun getOrderWithItemsById(orderId: Long): OrderWithItems? = orderDao.getOrderWithItemsById(orderId)

    val totalRevenue: Flow<Double> = orderDao.getTotalRevenue()
    val cashSalesTotal: Flow<Double> = orderDao.getCashSalesTotal()
    val gcashSalesTotal: Flow<Double> = orderDao.getGCashSalesTotal()
    val unpaidSalesTotal: Flow<Double> = orderDao.getUnpaidSalesTotal()
    val totalOrderCount: Flow<Int> = orderDao.getTotalOrderCount()
    val allTimeProductsSold: Flow<Int> = orderDao.getAllTimeProductsSold()

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val cashExpensesTotal: Flow<Double> = expenseDao.getCashExpensesTotal()
    val gcashExpensesTotal: Flow<Double> = expenseDao.getGCashExpensesTotal()
    val totalExpensesTotal: Flow<Double> = expenseDao.getTotalExpenses()

    fun getTotalRevenueByDate(startOfDay: Long, endOfDay: Long) = orderDao.getTotalRevenueByDate(startOfDay, endOfDay)
    fun getCashSalesTotalByDate(startOfDay: Long, endOfDay: Long) = orderDao.getCashSalesTotalByDate(startOfDay, endOfDay)
    fun getGCashSalesTotalByDate(startOfDay: Long, endOfDay: Long) = orderDao.getGCashSalesTotalByDate(startOfDay, endOfDay)
    fun getUnpaidSalesTotalByDate(startOfDay: Long, endOfDay: Long) = orderDao.getUnpaidSalesTotalByDate(startOfDay, endOfDay)
    fun getProductsSoldByDate(startOfDay: Long, endOfDay: Long) = orderDao.getProductsSoldByDate(startOfDay, endOfDay)
    fun getProductSalesSummaryByDate(startOfDay: Long, endOfDay: Long) = orderDao.getProductSalesSummaryByDate(startOfDay, endOfDay)

    fun getTotalExpensesByDate(startOfDay: Long, endOfDay: Long) = expenseDao.getTotalExpensesByDate(startOfDay, endOfDay)
    fun getCashExpensesTotalByDate(startOfDay: Long, endOfDay: Long) = expenseDao.getCashExpensesTotalByDate(startOfDay, endOfDay)
    fun getGCashExpensesTotalByDate(startOfDay: Long, endOfDay: Long) = expenseDao.getGCashExpensesTotalByDate(startOfDay, endOfDay)

    suspend fun insertProduct(product: ProductEntity) {
        insertOrMergeProduct(product)
    }

    suspend fun insertOrMergeProduct(product: ProductEntity) {
        val existing = productDao.findProductByNameAndCategory(
            name = product.name,
            category = product.category,
            itemType = product.itemType
        )
        if (existing != null && existing.id != product.id) {
            // Merge stock into existing product/raw material
            val mergedProduct = existing.copy(
                stockQuantity = existing.stockQuantity + product.stockQuantity,
                price = if (product.price > 0) product.price else existing.price,
                sku = if (product.sku.isNotBlank()) product.sku else existing.sku,
                unit = if (product.unit.isNotBlank()) product.unit else existing.unit,
                isActive = product.isActive
            )
            productDao.updateProduct(mergedProduct)
            if (product.id > 0) {
                productDao.deleteProduct(product)
            }
        } else {
            if (product.id > 0) {
                productDao.updateProduct(product)
            } else {
                productDao.insertProduct(product)
            }
        }
    }

    suspend fun updateProduct(product: ProductEntity) {
        insertOrMergeProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun setProductStatus(productId: Long, isActive: Boolean) {
        productDao.setProductStatus(productId, isActive)
    }

    suspend fun processCheckout(
        cartItems: List<CartItem>,
        paymentMethod: String,
        isUnpaid: Boolean = false,
        cashReceived: Double? = null,
        changeAmount: Double? = null,
        gcashRefNumber: String? = null,
        orderName: String = "",
        editingOrderId: Long? = null
    ): Result<Long> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty"))
        }

        val totalAmount = cartItems.sumOf { it.subtotal }
        val totalItems = cartItems.sumOf { it.quantity }
        val status = if (isUnpaid) "UNPAID" else "PAID"
        val timestamp = System.currentTimeMillis()

        if (editingOrderId != null) {
            val oldItems = orderDao.getOrderItemsByOrderId(editingOrderId)
            oldItems.forEach { oldItem ->
                productDao.increaseStock(oldItem.productId, oldItem.quantity)
            }
            orderDao.deleteOrderItemsByOrderId(editingOrderId)

            val baseOrderNumber = "ORD-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))}-${String.format(Locale.US, "%04d", editingOrderId)}"
            val formattedOrderNumber = if (orderName.isNotBlank()) "$orderName - $baseOrderNumber" else baseOrderNumber

            orderDao.updateOrder(
                orderId = editingOrderId,
                orderNumber = formattedOrderNumber,
                totalAmount = totalAmount,
                totalItems = totalItems,
                status = status,
                paymentMethod = if (isUnpaid) "Unpaid" else paymentMethod,
                cashReceived = if (!isUnpaid && paymentMethod == "Cash") cashReceived else null,
                changeAmount = if (!isUnpaid && paymentMethod == "Cash") changeAmount else null,
                gcashRefNumber = if (!isUnpaid && paymentMethod == "GCash") gcashRefNumber else null,
                timestamp = timestamp
            )

            val newOrderItems = cartItems.map { cartItem ->
                OrderItemEntity(
                    orderId = editingOrderId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    unitPrice = cartItem.product.price,
                    quantity = cartItem.quantity,
                    subtotal = cartItem.subtotal
                )
            }
            orderDao.insertOrderItems(newOrderItems)

            cartItems.forEach { cartItem ->
                productDao.reduceStock(cartItem.product.id, cartItem.quantity)
            }

            return Result.success(editingOrderId)
        } else {
            val order = OrderEntity(
                orderNumber = "",
                timestamp = timestamp,
                totalAmount = totalAmount,
                paymentMethod = if (isUnpaid) "Unpaid" else paymentMethod,
                totalItems = totalItems,
                status = status,
                cashReceived = if (!isUnpaid && paymentMethod == "Cash") cashReceived else null,
                changeAmount = if (!isUnpaid && paymentMethod == "Cash") changeAmount else null,
                gcashRefNumber = if (!isUnpaid && paymentMethod == "GCash") gcashRefNumber else null
            )

            val orderId = orderDao.insertOrder(order)
            val baseOrderNumber = "ORD-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))}-${String.format(Locale.US, "%04d", orderId)}"
            val formattedOrderNumber = if (orderName.isNotBlank()) "$orderName - $baseOrderNumber" else baseOrderNumber
            orderDao.updateOrderNumber(orderId, formattedOrderNumber)

            val orderItems = cartItems.map { cartItem ->
                OrderItemEntity(
                    orderId = orderId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    unitPrice = cartItem.product.price,
                    quantity = cartItem.quantity,
                    subtotal = cartItem.subtotal
                )
            }

            orderDao.insertOrderItems(orderItems)

            cartItems.forEach { cartItem ->
                productDao.reduceStock(cartItem.product.id, cartItem.quantity)
            }

            return Result.success(orderId)
        }
    }

    suspend fun markOrderAsPaid(
        orderId: Long,
        paymentMethod: String,
        cashReceived: Double? = null,
        changeAmount: Double? = null,
        gcashRefNumber: String? = null
    ): Result<Unit> {
        val count = orderDao.markOrderAsPaid(
            orderId = orderId,
            paymentMethod = paymentMethod,
            cashReceived = if (paymentMethod == "Cash") cashReceived else null,
            changeAmount = if (paymentMethod == "Cash") changeAmount else null,
            gcashRefNumber = if (paymentMethod == "GCash") gcashRefNumber else null
        )
        return if (count > 0) Result.success(Unit) else Result.failure(Exception("Order not found"))
    }

    suspend fun voidOrder(orderId: Long): Result<Unit> {
        val count = orderDao.voidOrder(orderId)
        return if (count > 0) Result.success(Unit) else Result.failure(Exception("Order not found"))
    }

    suspend fun deleteOrder(orderId: Long): Result<Unit> {
        val count = orderDao.deleteOrderById(orderId)
        return if (count > 0) Result.success(Unit) else Result.failure(Exception("Order not found"))
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun exportJsonBackup(context: Context): Result<String> {
        return try {
            val products = productDao.getAllProducts().first()
            val orders = orderDao.getAllOrdersWithItems().first()
            val expenses = expenseDao.getAllExpenses().first()
            val prefs = context.getSharedPreferences("pos_settings", Context.MODE_PRIVATE)

            val jsonString = JsonBackupHelper.createJsonBackup(products, orders, expenses, prefs)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "POS_Full_Backup_$timeStamp.json"

            // Save to Downloads directory
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val downloadFile = File(downloadsDir, fileName)
                    downloadFile.writeText(jsonString, Charsets.UTF_8)
                }
            } catch (_: Exception) {}

            // Save to Cache directory for sharing
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(jsonString, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Offline POS Full Data Backup ($fileName)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share/Save POS Backup JSON")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Result.success("Exported to Downloads: $fileName")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to export JSON backup: ${e.message}"))
        }
    }

    suspend fun importJsonBackup(context: Context, jsonString: String, replaceExisting: Boolean): Result<String> {
        return try {
            val payload = JsonBackupHelper.parseJsonBackup(jsonString)

            if (replaceExisting) {
                expenseDao.deleteAllExpenses()
                orderDao.deleteAllOrderItems()
                orderDao.deleteAllOrders()
                productDao.deleteAllProducts()
            }

            // Product ID Remapping
            val productIdMap = mutableMapOf<Long, Long>()
            payload.products.forEach { product ->
                if (replaceExisting) {
                    val id = productDao.insertProduct(product)
                    productIdMap[product.id] = id
                } else {
                    insertOrMergeProduct(product)
                }
            }

            // Orders & Order Items
            payload.ordersWithItems.forEach { owi ->
                val order = owi.order
                val newOrderId = orderDao.insertOrder(order.copy(id = if (replaceExisting) order.id else 0))

                val newItems = owi.items.map { item ->
                    val remappedProductId = productIdMap[item.productId] ?: item.productId
                    item.copy(id = 0, orderId = newOrderId, productId = remappedProductId)
                }
                orderDao.insertOrderItems(newItems)
            }

            // Expenses
            payload.expenses.forEach { expense ->
                expenseDao.insertExpense(expense.copy(id = if (replaceExisting) expense.id else 0))
            }

            // Settings
            if (payload.settings.isNotEmpty()) {
                JsonBackupHelper.applySettings(context, payload.settings)
            }

            val summary = "Imported ${payload.products.size} products, ${payload.ordersWithItems.size} orders, and ${payload.expenses.size} expenses!"
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to import JSON backup: ${e.message}"))
        }
    }
}
