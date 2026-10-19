package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) to persist and query transaction logs,
 * allowing users to view a comprehensive history of scanned items and payments.
 */
@Dao
interface TransactionLogDao {

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE logType = :logType ORDER BY timestamp DESC")
    fun getLogsByType(logType: String): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE logType = 'SCANNED_ITEM' ORDER BY timestamp DESC")
    fun getScannedItemLogs(): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE logType != 'SCANNED_ITEM' ORDER BY timestamp DESC")
    fun getPaymentLogs(): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE id = :id LIMIT 1")
    fun getLogById(id: Long): Flow<TransactionLogEntity?>

    @Query("SELECT * FROM transaction_logs WHERE currencyCode = :currencyCode ORDER BY timestamp DESC")
    fun getLogsByCurrency(currencyCode: String): Flow<List<TransactionLogEntity>>

    @Query("""
        SELECT * FROM transaction_logs 
        WHERE title LIKE '%' || :query || '%' 
           OR customerOrRecipient LIKE '%' || :query || '%' 
           OR barcodeOrRawValue LIKE '%' || :query || '%' 
           OR referenceCode LIKE '%' || :query || '%' 
           OR country LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchLogs(query: String): Flow<List<TransactionLogEntity>>

    @Query("SELECT COUNT(*) FROM transaction_logs")
    fun getTotalLogCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transaction_logs WHERE logType = 'SCANNED_ITEM'")
    fun getScannedItemCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transaction_logs WHERE logType != 'SCANNED_ITEM'")
    fun getPaymentCount(): Flow<Int>

    @Query("SELECT SUM(amountInUsd) FROM transaction_logs WHERE logType != 'PAYOUT'")
    fun getTotalVolumeUsd(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransactionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<TransactionLogEntity>)

    @Update
    suspend fun updateLog(log: TransactionLogEntity)

    @Delete
    suspend fun deleteLog(log: TransactionLogEntity)

    @Query("DELETE FROM transaction_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM transaction_logs WHERE logType = :logType")
    suspend fun deleteLogsByType(logType: String)

    @Query("DELETE FROM transaction_logs")
    suspend fun clearAllLogs()
}
