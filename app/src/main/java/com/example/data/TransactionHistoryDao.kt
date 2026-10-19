package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Room database operations on transaction history records.
 * Provides queries to store, retrieve, filter, and manage transaction history.
 */
@Dao
interface TransactionHistoryDao {
    @Query("SELECT * FROM transaction_history ORDER BY date DESC")
    fun getAllTransactionHistory(): Flow<List<TransactionHistoryEntity>>

    @Query("SELECT * FROM transaction_history WHERE id = :id LIMIT 1")
    fun getTransactionHistoryById(id: Long): Flow<TransactionHistoryEntity?>

    @Query("SELECT * FROM transaction_history WHERE currency = :currency ORDER BY date DESC")
    fun getTransactionHistoryByCurrency(currency: String): Flow<List<TransactionHistoryEntity>>

    @Query("SELECT * FROM transaction_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionHistoryByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionHistoryEntity>>

    @Query("SELECT * FROM transaction_history WHERE type = :type ORDER BY date DESC")
    fun getTransactionHistoryByType(type: String): Flow<List<TransactionHistoryEntity>>

    @Query("SELECT SUM(amount) FROM transaction_history WHERE currency = :currency")
    fun getTotalAmountByCurrency(currency: String): Flow<Double?>

    @Query("SELECT COUNT(*) FROM transaction_history")
    fun getTransactionHistoryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionHistory(record: TransactionHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionHistoryList(records: List<TransactionHistoryEntity>)

    @Update
    suspend fun updateTransactionHistory(record: TransactionHistoryEntity)

    @Delete
    suspend fun deleteTransactionHistory(record: TransactionHistoryEntity)

    @Query("DELETE FROM transaction_history WHERE id = :id")
    suspend fun deleteTransactionHistoryById(id: Long)

    @Query("DELETE FROM transaction_history")
    suspend fun clearAllTransactionHistory()
}
