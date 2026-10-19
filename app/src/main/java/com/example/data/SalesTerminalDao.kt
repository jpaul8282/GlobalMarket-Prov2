package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the Sales Terminal.
 *
 * Manages both the local inventory of products and the terminal's transaction logs,
 * enabling real-time stock tracking, atomic checkout deductions, auditing,
 * and multi-currency point-of-sale operations.
 */
@Dao
interface SalesTerminalDao {

    // ==========================================
    // LOCAL INVENTORY OF PRODUCTS
    // ==========================================

    @Query("SELECT * FROM inventory_products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllInventoryProducts(): Flow<List<InventoryProductEntity>>

    @Query("SELECT * FROM inventory_products WHERE id = :id LIMIT 1")
    fun getInventoryProductById(id: Long): Flow<InventoryProductEntity?>

    @Query("SELECT * FROM inventory_products WHERE barcode = :barcode AND isActive = 1 LIMIT 1")
    fun getInventoryProductByBarcode(barcode: String): Flow<InventoryProductEntity?>

    @Query("SELECT * FROM inventory_products WHERE sku = :sku AND isActive = 1 LIMIT 1")
    fun getInventoryProductBySku(sku: String): Flow<InventoryProductEntity?>

    @Query("SELECT * FROM inventory_products WHERE category = :category AND isActive = 1 ORDER BY name ASC")
    fun getInventoryProductsByCategory(category: String): Flow<List<InventoryProductEntity>>

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
    fun searchInventoryProducts(query: String): Flow<List<InventoryProductEntity>>

    @Query("SELECT COUNT(*) FROM inventory_products WHERE isActive = 1")
    fun getTotalProductCount(): Flow<Int>

    @Query("SELECT SUM(stockQuantity) FROM inventory_products WHERE isActive = 1")
    fun getTotalStockQuantity(): Flow<Int?>

    @Query("SELECT SUM(price * stockQuantity) FROM inventory_products WHERE isActive = 1")
    fun getTotalInventoryValue(): Flow<Double?>

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


    // ==========================================
    // SALES TERMINAL TRANSACTION LOGS
    // ==========================================

    @Query("SELECT * FROM terminal_transaction_logs ORDER BY timestamp DESC")
    fun getAllTerminalLogs(): Flow<List<TerminalTransactionLogEntity>>

    @Query("SELECT * FROM terminal_transaction_logs WHERE id = :id LIMIT 1")
    fun getTerminalLogById(id: Long): Flow<TerminalTransactionLogEntity?>

    @Query("SELECT * FROM terminal_transaction_logs WHERE terminalId = :terminalId ORDER BY timestamp DESC")
    fun getTerminalLogsByTerminal(terminalId: String): Flow<List<TerminalTransactionLogEntity>>

    @Query("SELECT * FROM terminal_transaction_logs WHERE transactionType = :type ORDER BY timestamp DESC")
    fun getTerminalLogsByType(type: String): Flow<List<TerminalTransactionLogEntity>>

    @Query("SELECT * FROM terminal_transaction_logs WHERE status = :status ORDER BY timestamp DESC")
    fun getTerminalLogsByStatus(status: String): Flow<List<TerminalTransactionLogEntity>>

    @Query("SELECT * FROM terminal_transaction_logs WHERE paymentMethod = :method ORDER BY timestamp DESC")
    fun getTerminalLogsByPaymentMethod(method: String): Flow<List<TerminalTransactionLogEntity>>

    @Query("""
        SELECT * FROM terminal_transaction_logs 
        WHERE productName LIKE '%' || :query || '%' 
           OR referenceCode LIKE '%' || :query || '%' 
           OR customerName LIKE '%' || :query || '%'
           OR terminalId LIKE '%' || :query || '%'
           OR productSku LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTerminalLogs(query: String): Flow<List<TerminalTransactionLogEntity>>

    @Query("SELECT COUNT(*) FROM terminal_transaction_logs")
    fun getTerminalLogCount(): Flow<Int>

    @Query("SELECT SUM(amountInUsd) FROM terminal_transaction_logs WHERE status = 'COMPLETED'")
    fun getTotalTerminalSalesVolumeUsd(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalLog(log: TerminalTransactionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerminalLogs(logs: List<TerminalTransactionLogEntity>)

    @Update
    suspend fun updateTerminalLog(log: TerminalTransactionLogEntity)

    @Delete
    suspend fun deleteTerminalLog(log: TerminalTransactionLogEntity)

    @Query("DELETE FROM terminal_transaction_logs WHERE id = :id")
    suspend fun deleteTerminalLogById(id: Long)

    @Query("DELETE FROM terminal_transaction_logs WHERE terminalId = :terminalId")
    suspend fun deleteTerminalLogsByTerminal(terminalId: String)

    @Query("DELETE FROM terminal_transaction_logs")
    suspend fun clearAllTerminalLogs()


    // ==========================================
    // ATOMIC POINT-OF-SALE TRANSACTION
    // ==========================================

    @Transaction
    suspend fun processTerminalSale(
        log: TerminalTransactionLogEntity,
        productId: Long? = log.productId,
        quantitySold: Int = log.quantity
    ): Long {
        if (productId != null && quantitySold > 0) {
            decrementStock(productId, quantitySold)
        }
        return insertTerminalLog(log)
    }
}
