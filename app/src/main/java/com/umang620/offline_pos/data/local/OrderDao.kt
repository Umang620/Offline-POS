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

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getTotalRevenueByDate(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND paymentMethod = 'Cash' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getCashSalesTotalByDate(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'PAID' AND paymentMethod = 'GCash' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getGCashSalesTotalByDate(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM orders WHERE status = 'UNPAID' AND timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getUnpaidSalesTotalByDate(startOfDay: Long, endOfDay: Long): Flow<Double>
}
