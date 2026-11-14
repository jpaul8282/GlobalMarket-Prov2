package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for storing and retrieving the history of
 * processed payouts and simulated bank wire transactions.
 */
@Dao
interface PayoutWireDao {

    // ==========================================
    // STORING & MUTATING PAYOUT / WIRE RECORDS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayoutWire(record: PayoutWireEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayoutWires(records: List<PayoutWireEntity>): List<Long>

    @Update
    suspend fun updatePayoutWire(record: PayoutWireEntity)

    @Query("UPDATE payout_wire_history SET status = :newStatus WHERE id = :id")
    suspend fun updatePayoutWireStatus(id: Long, newStatus: String)

    @Delete
    suspend fun deletePayoutWire(record: PayoutWireEntity)

    @Query("DELETE FROM payout_wire_history WHERE id = :id")
    suspend fun deletePayoutWireById(id: Long)

    @Query("DELETE FROM payout_wire_history")
    suspend fun clearAllPayoutWires()

    // ==========================================
    // RETRIEVING PAYOUT & BANK WIRE HISTORY
    // ==========================================

    @Query("SELECT * FROM payout_wire_history ORDER BY timestamp DESC")
    fun getAllPayoutWireHistory(): Flow<List<PayoutWireEntity>>

    @Query("SELECT * FROM payout_wire_history WHERE id = :id LIMIT 1")
    fun getPayoutWireById(id: Long): Flow<PayoutWireEntity?>

    @Query("SELECT * FROM payout_wire_history WHERE referenceCode = :referenceCode LIMIT 1")
    fun getPayoutWireByReference(referenceCode: String): Flow<PayoutWireEntity?>

    @Query("SELECT * FROM payout_wire_history WHERE transactionType = :transactionType ORDER BY timestamp DESC")
    fun getPayoutWiresByType(transactionType: String): Flow<List<PayoutWireEntity>>

    @Query("SELECT * FROM payout_wire_history WHERE transactionType = 'PROCESSED_PAYOUT' AND isSimulatedWire = 0 ORDER BY timestamp DESC")
    fun getProcessedPayouts(): Flow<List<PayoutWireEntity>>

    @Query("SELECT * FROM payout_wire_history WHERE transactionType = 'SIMULATED_BANK_WIRE' OR isSimulatedWire = 1 ORDER BY timestamp DESC")
    fun getSimulatedBankWires(): Flow<List<PayoutWireEntity>>

    @Query("SELECT * FROM payout_wire_history WHERE method = :method ORDER BY timestamp DESC")
    fun getPayoutWiresByMethod(method: String): Flow<List<PayoutWireEntity>>

    @Query("SELECT * FROM payout_wire_history WHERE status = :status ORDER BY timestamp DESC")
    fun getPayoutWiresByStatus(status: String): Flow<List<PayoutWireEntity>>

    @Query("SELECT * FROM payout_wire_history WHERE currencyCode = :currencyCode ORDER BY timestamp DESC")
    fun getPayoutWiresByCurrency(currencyCode: String): Flow<List<PayoutWireEntity>>

    @Query("SELECT * FROM payout_wire_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPayoutWiresByDateRange(startTime: Long, endTime: Long): Flow<List<PayoutWireEntity>>

    @Query("""
        SELECT * FROM payout_wire_history
        WHERE referenceCode LIKE '%' || :query || '%'
           OR recipientName LIKE '%' || :query || '%'
           OR bankName LIKE '%' || :query || '%'
           OR ibanOrAccount LIKE '%' || :query || '%'
           OR swiftBic LIKE '%' || :query || '%'
           OR clearingRail LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchPayoutWireHistory(query: String): Flow<List<PayoutWireEntity>>

    // ==========================================
    // AGGREGATIONS & METRICS
    // ==========================================

    @Query("SELECT COUNT(*) FROM payout_wire_history")
    fun getTotalPayoutWireCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM payout_wire_history WHERE transactionType = 'PROCESSED_PAYOUT' AND isSimulatedWire = 0")
    fun getProcessedPayoutCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM payout_wire_history WHERE transactionType = 'SIMULATED_BANK_WIRE' OR isSimulatedWire = 1")
    fun getSimulatedBankWireCount(): Flow<Int>

    @Query("SELECT SUM(netAmountUsd) FROM payout_wire_history WHERE status IN ('COMPLETED', 'SETTLED')")
    fun getTotalNetDisbursedUsd(): Flow<Double?>

    @Query("SELECT SUM(grossAmountUsd) FROM payout_wire_history WHERE status IN ('COMPLETED', 'SETTLED')")
    fun getTotalGrossDisbursedUsd(): Flow<Double?>
}
