package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the sales transaction history Room schema.
 *
 * Provides reactive Flow-based queries and coroutine suspend functions to
 * store, retrieve, filter, search, update, and analyze historical sales transactions.
 */
@Dao
interface SalesTransactionDao {

    // ==========================================
    // STORING SALES TRANSACTIONS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesTransaction(transaction: SalesTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesTransactions(transactions: List<SalesTransactionEntity>): List<Long>

    @Update
    suspend fun updateSalesTransaction(transaction: SalesTransactionEntity)

    @Query("UPDATE sales_transaction_history SET paymentStatus = :newStatus WHERE id = :id")
    suspend fun updateTransactionStatus(id: Long, newStatus: String)

    @Delete
    suspend fun deleteSalesTransaction(transaction: SalesTransactionEntity)

    @Query("DELETE FROM sales_transaction_history WHERE id = :id")
    suspend fun deleteSalesTransactionById(id: Long)

    @Query("DELETE FROM sales_transaction_history")
    suspend fun clearAllSalesTransactions()

    // Convenient aliases
    suspend fun insert(transaction: SalesTransactionEntity): Long = insertSalesTransaction(transaction)
    suspend fun insertAll(transactions: List<SalesTransactionEntity>): List<Long> = insertSalesTransactions(transactions)
    suspend fun update(transaction: SalesTransactionEntity) = updateSalesTransaction(transaction)
    suspend fun delete(transaction: SalesTransactionEntity) = deleteSalesTransaction(transaction)
    suspend fun deleteById(id: Long) = deleteSalesTransactionById(id)
    suspend fun clearAll() = clearAllSalesTransactions()


    // ==========================================
    // RETRIEVING SALES TRANSACTIONS
    // ==========================================

    @Query("SELECT * FROM sales_transaction_history ORDER BY timestamp DESC")
    fun getAllSalesTransactions(): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transaction_history WHERE id = :id LIMIT 1")
    fun getSalesTransactionById(id: Long): Flow<SalesTransactionEntity?>

    @Query("SELECT * FROM sales_transaction_history WHERE transactionReference = :reference LIMIT 1")
    fun getSalesTransactionByReference(reference: String): Flow<SalesTransactionEntity?>

    @Query("SELECT * FROM sales_transaction_history WHERE receiptNumber = :receiptNumber LIMIT 1")
    fun getSalesTransactionByReceiptNumber(receiptNumber: String): Flow<SalesTransactionEntity?>

    @Query("SELECT * FROM sales_transaction_history WHERE currency = :currency ORDER BY timestamp DESC")
    fun getSalesTransactionsByCurrency(currency: String): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transaction_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getSalesTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transaction_history WHERE paymentStatus = :status ORDER BY timestamp DESC")
    fun getSalesTransactionsByStatus(status: String): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transaction_history WHERE paymentMethod = :method ORDER BY timestamp DESC")
    fun getSalesTransactionsByPaymentMethod(method: String): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transaction_history WHERE terminalId = :terminalId ORDER BY timestamp DESC")
    fun getSalesTransactionsByTerminal(terminalId: String): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transaction_history WHERE customerName LIKE '%' || :customerName || '%' ORDER BY timestamp DESC")
    fun getSalesTransactionsByCustomer(customerName: String): Flow<List<SalesTransactionEntity>>

    @Query("""
        SELECT * FROM sales_transaction_history
        WHERE customerName LIKE '%' || :query || '%'
           OR transactionReference LIKE '%' || :query || '%'
           OR receiptNumber LIKE '%' || :query || '%'
           OR itemsSummary LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
           OR customerEmail LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchSalesTransactions(query: String): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transaction_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSalesTransactions(limit: Int = 10): Flow<List<SalesTransactionEntity>>

    // Convenient aliases
    fun getAll(): Flow<List<SalesTransactionEntity>> = getAllSalesTransactions()
    fun getById(id: Long): Flow<SalesTransactionEntity?> = getSalesTransactionById(id)
    fun getByReference(reference: String): Flow<SalesTransactionEntity?> = getSalesTransactionByReference(reference)
    fun getByCurrency(currency: String): Flow<List<SalesTransactionEntity>> = getSalesTransactionsByCurrency(currency)
    fun getByStatus(status: String): Flow<List<SalesTransactionEntity>> = getSalesTransactionsByStatus(status)
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<SalesTransactionEntity>> = getSalesTransactionsByDateRange(startTime, endTime)
    fun search(query: String): Flow<List<SalesTransactionEntity>> = searchSalesTransactions(query)


    // ==========================================
    // AGGREGATIONS & METRICS
    // ==========================================

    @Query("SELECT COUNT(*) FROM sales_transaction_history")
    fun getTotalSalesCount(): Flow<Int>

    @Query("SELECT SUM(totalAmount) FROM sales_transaction_history WHERE paymentStatus = 'COMPLETED'")
    fun getTotalGrossSales(): Flow<Double?>

    @Query("SELECT SUM(amountInUsd) FROM sales_transaction_history WHERE paymentStatus = 'COMPLETED'")
    fun getTotalSalesInUsd(): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM sales_transaction_history WHERE currency = :currency AND paymentStatus = 'COMPLETED'")
    fun getTotalSalesByCurrency(currency: String): Flow<Double?>

    @Query("SELECT AVG(amountInUsd) FROM sales_transaction_history WHERE paymentStatus = 'COMPLETED'")
    fun getAverageOrderValueUsd(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM sales_transaction_history WHERE paymentStatus = :status")
    fun getSalesCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sales_transaction_history WHERE paymentMethod = :method")
    fun getSalesCountByPaymentMethod(method: String): Flow<Int>
}
