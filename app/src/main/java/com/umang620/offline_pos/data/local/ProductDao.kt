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
    @Query("SELECT * FROM products ORDER BY category ASC, name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY category ASC, name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE itemType = :itemType AND isActive = 1 ORDER BY category ASC, name ASC")
    fun getProductsByType(itemType: String): Flow<List<ProductEntity>>

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE name = :name AND category = :category AND itemType = :itemType LIMIT 1")
    suspend fun findProductByNameAndCategory(name: String, category: String, itemType: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProducts(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun updateProduct(product: ProductEntity): Int

    @Delete
    suspend fun deleteProduct(product: ProductEntity): Int

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts(): Int

    @Query("UPDATE products SET isActive = :isActive WHERE id = :id")
    suspend fun setProductStatus(id: Long, isActive: Boolean): Int

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity WHERE id = :id")
    suspend fun reduceStock(id: Long, quantity: Int): Int

    @Query("UPDATE products SET stockQuantity = stockQuantity + :quantity WHERE id = :id")
    suspend fun increaseStock(id: Long, quantity: Int): Int
}
