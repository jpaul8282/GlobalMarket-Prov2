package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on scanned products.
 */
@Dao
interface ScannedProductDao {
    @Query("SELECT * FROM scanned_products ORDER BY timestamp DESC")
    fun getAllScannedProducts(): Flow<List<ScannedProductEntity>>

    @Query("SELECT * FROM scanned_products WHERE id = :id LIMIT 1")
    fun getScannedProductById(id: Long): Flow<ScannedProductEntity?>

    @Query("SELECT * FROM scanned_products WHERE barcode = :barcode ORDER BY timestamp DESC")
    fun getScannedProductsByBarcode(barcode: String): Flow<List<ScannedProductEntity>>

    @Query("SELECT COUNT(*) FROM scanned_products")
    fun getScannedProductCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedProduct(product: ScannedProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedProducts(products: List<ScannedProductEntity>)

    @Update
    suspend fun updateScannedProduct(product: ScannedProductEntity)

    @Delete
    suspend fun deleteScannedProduct(product: ScannedProductEntity)

    @Query("DELETE FROM scanned_products WHERE id = :id")
    suspend fun deleteScannedProductById(id: Long)

    @Query("DELETE FROM scanned_products")
    suspend fun clearAllScannedProducts()
}
