package com.umang620.offline_pos.data.repository

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
import kotlinx.coroutines.flow.Flow
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
}
