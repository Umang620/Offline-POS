package com.umang620.offline_pos.data.repository

import com.umang620.offline_pos.data.local.ExpenseDao
import com.umang620.offline_pos.data.local.ExpenseEntity
import com.umang620.offline_pos.data.local.OrderDao
import com.umang620.offline_pos.data.local.OrderEntity
import com.umang620.offline_pos.data.local.OrderItemEntity
import com.umang620.offline_pos.data.local.OrderWithItems
import com.umang620.offline_pos.data.local.ProductDao
import com.umang620.offline_pos.data.local.ProductEntity
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

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val cashExpensesTotal: Flow<Double> = expenseDao.getCashExpensesTotal()
    val gcashExpensesTotal: Flow<Double> = expenseDao.getGCashExpensesTotal()
    val totalExpensesTotal: Flow<Double> = expenseDao.getTotalExpenses()

    fun getTotalRevenueByDate(startOfDay: Long, endOfDay: Long) = orderDao.getTotalRevenueByDate(startOfDay, endOfDay)
    fun getCashSalesTotalByDate(startOfDay: Long, endOfDay: Long) = orderDao.getCashSalesTotalByDate(startOfDay, endOfDay)
    fun getGCashSalesTotalByDate(startOfDay: Long, endOfDay: Long) = orderDao.getGCashSalesTotalByDate(startOfDay, endOfDay)
    fun getUnpaidSalesTotalByDate(startOfDay: Long, endOfDay: Long) = orderDao.getUnpaidSalesTotalByDate(startOfDay, endOfDay)

    fun getTotalExpensesByDate(startOfDay: Long, endOfDay: Long) = expenseDao.getTotalExpensesByDate(startOfDay, endOfDay)
    fun getCashExpensesTotalByDate(startOfDay: Long, endOfDay: Long) = expenseDao.getCashExpensesTotalByDate(startOfDay, endOfDay)
    fun getGCashExpensesTotalByDate(startOfDay: Long, endOfDay: Long) = expenseDao.getGCashExpensesTotalByDate(startOfDay, endOfDay)

    suspend fun insertProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
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
        gcashRefNumber: String? = null
    ): Result<Long> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty"))
        }

        val totalAmount = cartItems.sumOf { it.subtotal }
        val totalItems = cartItems.sumOf { it.quantity }
        val status = if (isUnpaid) "UNPAID" else "PAID"
        val timestamp = System.currentTimeMillis()

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
        val formattedOrderNumber = "ORD-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))}-${String.format(Locale.US, "%04d", orderId)}"
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

        // Deduct stock
        cartItems.forEach { cartItem ->
            productDao.reduceStock(cartItem.product.id, cartItem.quantity)
        }

        return Result.success(orderId)
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
