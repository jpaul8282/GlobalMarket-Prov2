package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database schema representing persisted transaction logs for both
 * scanned items and payments.
 *
 * Allows querying and auditing the complete history of barcode scans,
 * POS retail terminal transactions, web checkouts, and disbursements.
 */
@Entity(
    tableName = "transaction_logs",
    indices = [
        Index(value = ["logType"]),
        Index(value = ["timestamp"]),
        Index(value = ["currencyCode"]),
        Index(value = ["referenceCode"])
    ]
)
data class TransactionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "logType")
    val logType: String, // "SCANNED_ITEM", "PAYMENT", "PAYOUT", "WEB_CHECKOUT"

    @ColumnInfo(name = "title")
    val title: String, // e.g. "Glaba Pro Storefront", "UPC-A Retail Item", "Terminal POS Card"

    @ColumnInfo(name = "barcodeOrRawValue")
    val barcodeOrRawValue: String? = null, // Barcode value, QR URL, or product code

    @ColumnInfo(name = "barcodeFormat")
    val barcodeFormat: String? = null, // "QR_CODE", "UPC_A", "EAN_13", "CODE_128", "MANUAL"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currencyCode")
    val currencyCode: String, // "USD", "EUR", "GBP", "JPY", "CNY", etc.

    @ColumnInfo(name = "amountInUsd")
    val amountInUsd: Double,

    @ColumnInfo(name = "customerOrRecipient")
    val customerOrRecipient: String,

    @ColumnInfo(name = "country")
    val country: String,

    @ColumnInfo(name = "paymentMethod")
    val paymentMethod: String = "CARD", // "SCANNER", "CARD", "APPLE_GOOGLE_PAY", "SEPA", "CASH"

    @ColumnInfo(name = "status")
    val status: String = "COMPLETED", // "COMPLETED", "PENDING", "REFUNDED"

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "referenceCode")
    val referenceCode: String = "",

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
