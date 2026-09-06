package com.umang620.offline_pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE status = 'PAID' ORDER BY timestamp DESC")
    fun getPaidOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE status = 'UNPAID' ORDER BY timestamp DESC")
    fun getUnpaidOrdersWithItems(): Flow<List<OrderWithItems>>

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long>

    @Query("UPDATE orders SET orderNumber = :orderNumber WHERE id = :orderId")
    suspend fun updateOrderNumber(orderId: Long, orderNumber: String): Int

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsByOrderId(orderId: Long): List<OrderItemEntity>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItemsByOrderId(orderId: Long): Int

    @Query("UPDATE orders SET orderNumber = :orderNumber, totalAmount = :totalAmount, totalItems = :totalItems, status = :status, paymentMethod = :paymentMethod, cashReceived = :cashReceived, changeAmount = :changeAmount, gcashRefNumber = :gcashRefNumber, timestamp = :timestamp WHERE id = :orderId")
    suspend fun updateOrder(
        orderId: Long,
        orderNumber: String,
        totalAmount: Double,
        totalItems: Int,
        status: String,
        paymentMethod: String,
        cashReceived: Double?,
        changeAmount: Double?,
        gcashRefNumber: String?,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE orders SET status = 'PAID', paymentMethod = :paymentMethod, cashReceived = :cashReceived, changeAmount = :changeAmount, gcashRefNumber = :gcashRefNumber, timestamp = :timestamp WHERE id = :orderId")
    suspend fun markOrderAsPaid(
        orderId: Long,
        paymentMethod: String,
        cashReceived: Double?,
        changeAmount: Double?,
        gcashRefNumber: String?,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE orders SET status = 'VOIDED', voidedTimestamp = :voidedTimestamp WHERE id = :orderId")
    suspend fun voidOrder(orderId: Long, voidedTimestamp: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Long): Int

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID'")
    fun getTotalRevenue(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND paymentMethod = 'Cash'")
    fun getCashSalesTotal(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND paymentMethod = 'GCash'")
    fun getGCashSalesTotal(): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'UNPAID'")
    fun getUnpaidSalesTotal(): Flow<Double>

    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PAID'")
    fun getTotalOrderCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalItems), 0) FROM orders WHERE status = 'PAID'")
    fun getAllTimeProductsSold(): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getTotalRevenueByDate(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND paymentMethod = 'Cash' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getCashSalesTotalByDate(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND paymentMethod = 'GCash' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getGCashSalesTotalByDate(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'UNPAID' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getUnpaidSalesTotalByDate(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalItems), 0) FROM orders WHERE status = 'PAID' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getProductsSoldByDate(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT oi.productId AS productId, oi.productName AS productName, SUM(oi.quantity) AS totalQuantity, SUM(oi.subtotal) AS totalAmount FROM order_items oi INNER JOIN orders o ON oi.orderId = o.id WHERE o.status = 'PAID' AND o.timestamp BETWEEN :startOfDay AND :endOfDay GROUP BY oi.productId, oi.productName ORDER BY totalQuantity DESC")
    fun getProductSalesSummaryByDate(startOfDay: Long, endOfDay: Long): Flow<List<ProductSalesSummary>>
}
