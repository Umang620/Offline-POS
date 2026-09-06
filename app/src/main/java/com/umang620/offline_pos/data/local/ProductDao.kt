package com.umang620.offline_pos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND (itemType = 'PRODUCT' OR itemType IS NULL) ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE (itemType = :itemType OR (:itemType = 'PRODUCT' AND itemType IS NULL)) ORDER BY name ASC")
    fun getProductsByType(itemType: String): Flow<List<ProductEntity>>

    @Query("SELECT DISTINCT category FROM products WHERE (itemType = 'PRODUCT' OR itemType IS NULL) ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProducts(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun updateProduct(product: ProductEntity): Int

    @Delete
    suspend fun deleteProduct(product: ProductEntity): Int

    @Query("UPDATE products SET isActive = :isActive WHERE id = :productId")
    suspend fun setProductStatus(productId: Long, isActive: Boolean): Int

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :productId AND stockQuantity >= :quantity")
    suspend fun reduceStock(productId: Long, quantity: Int): Int
}
