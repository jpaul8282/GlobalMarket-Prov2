package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local inventory products.
 */
@Dao
interface InventoryProductDao {

    @Query("SELECT * FROM inventory_products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<InventoryProductEntity>>

    @Query("SELECT * FROM inventory_products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<InventoryProductEntity?>

    @Query("SELECT * FROM inventory_products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    fun getProductByBarcode(barcode: String): Flow<InventoryProductEntity?>

    @Query("SELECT * FROM inventory_products WHERE sku = :sku AND isActive = 1 LIMIT 1")
    fun getProductBySku(sku: String): Flow<InventoryProductEntity?>

    @Query("SELECT * FROM inventory_products WHERE category = :category AND isActive = 1 ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<InventoryProductEntity>>

    @Query("SELECT * FROM inventory_products WHERE stockQuantity <= minStockThreshold AND isActive = 1 ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<InventoryProductEntity>>

    @Query("""
        SELECT * FROM inventory_products 
        WHERE isActive = 1 AND (
            name LIKE '%' || :query || '%' 
            OR sku LIKE '%' || :query || '%' 
            OR barcode LIKE '%' || :query || '%' 
            OR category LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
    """)
    fun searchProducts(query: String): Flow<List<InventoryProductEntity>>

    @Query("SELECT COUNT(*) FROM inventory_products WHERE isActive = 1")
    fun getProductCount(): Flow<Int>

    @Query("SELECT SUM(stockQuantity) FROM inventory_products WHERE isActive = 1")
    fun getTotalStockQuantity(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: InventoryProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<InventoryProductEntity>)

    @Update
    suspend fun updateProduct(product: InventoryProductEntity)

    @Query("UPDATE inventory_products SET stockQuantity = :newStock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_products SET stockQuantity = stockQuantity - :quantity, updatedAt = :updatedAt WHERE id = :id AND stockQuantity >= :quantity")
    suspend fun decrementStock(id: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE inventory_products SET stockQuantity = stockQuantity + :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun incrementStock(id: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis()): Int

    @Delete
    suspend fun deleteProduct(product: InventoryProductEntity)

    @Query("DELETE FROM inventory_products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM inventory_products")
    suspend fun clearAllProducts()
}
