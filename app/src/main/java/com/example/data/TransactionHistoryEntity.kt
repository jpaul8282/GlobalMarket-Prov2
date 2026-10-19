package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity representing a transaction history record.
 * Stores date, amount, currency, and relevant transaction details.
 */
@Entity(
    tableName = "transaction_history",
    indices = [
        Index(value = ["date"]),
        Index(value = ["currency"]),
        Index(value = ["type"]),
        Index(value = ["status"])
    ]
)
data class TransactionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currency")
    val currency: String, // e.g. "USD", "EUR", "GBP", "JPY", "CAD"

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "type")
    val type: String = "PAYMENT", // e.g. "PAYMENT", "REFUND", "PAYOUT", "TRANSFER"

    @ColumnInfo(name = "status")
    val status: String = "COMPLETED", // e.g. "COMPLETED", "PENDING", "FAILED"

    @ColumnInfo(name = "category")
    val category: String = "General",

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
