package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database schema representing sales transaction history.
 *
 * Stores comprehensive point-of-sale and digital sales records,
 * including transaction reference, timestamps, customer details, item quantities,
 * item breakdown summary, pricing, tax, discount, total amounts, currency,
 * payment rail / method, and transaction status.
 */
@Entity(
    tableName = "sales_transaction_history",
    indices = [
        Index(value = ["transactionReference"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["customerName"]),
        Index(value = ["currency"]),
        Index(value = ["paymentStatus"]),
        Index(value = ["paymentMethod"]),
        Index(value = ["terminalId"])
    ]
)
data class SalesTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "transactionReference")
    val transactionReference: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "customerName")
    val customerName: String = "Walk-in Customer",

    @ColumnInfo(name = "customerEmail")
    val customerEmail: String? = null,

    @ColumnInfo(name = "subtotalAmount")
    val subtotalAmount: Double = 0.0,

    @ColumnInfo(name = "taxAmount")
    val taxAmount: Double = 0.0,

    @ColumnInfo(name = "discountAmount")
    val discountAmount: Double = 0.0,

    @ColumnInfo(name = "totalAmount")
    val totalAmount: Double = 0.0,

    @ColumnInfo(name = "currency")
    val currency: String = "USD",

    @ColumnInfo(name = "amountInUsd")
    val amountInUsd: Double = 0.0,

    @ColumnInfo(name = "exchangeRate")
    val exchangeRate: Double = 1.0,

    @ColumnInfo(name = "paymentMethod")
    val paymentMethod: String = "CARD", // "CARD", "CASH", "QR_CODE", "NFC", "ONLINE"

    @ColumnInfo(name = "paymentStatus")
    val paymentStatus: String = "COMPLETED", // "COMPLETED", "PENDING", "REFUNDED", "VOIDED", "FAILED"

    @ColumnInfo(name = "terminalId")
    val terminalId: String = "POS-TERM-01",

    @ColumnInfo(name = "cashierName")
    val cashierName: String = "Cashier",

    @ColumnInfo(name = "itemCount")
    val itemCount: Int = 1,

    @ColumnInfo(name = "itemsSummary")
    val itemsSummary: String = "",

    @ColumnInfo(name = "receiptNumber")
    val receiptNumber: String = "",

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
