package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity representing a persisted history of processed merchant payouts
 * and simulated bank wire / SEPA / iDEAL transactions.
 */
@Entity(
    tableName = "payout_wire_history",
    indices = [
        Index(value = ["referenceCode"], unique = true),
        Index(value = ["transactionType"]),
        Index(value = ["method"]),
        Index(value = ["status"]),
        Index(value = ["currencyCode"]),
        Index(value = ["timestamp"]),
        Index(value = ["isSimulatedWire"])
    ]
)
data class PayoutWireEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "referenceCode")
    val referenceCode: String,

    @ColumnInfo(name = "transactionType")
    val transactionType: String = "PROCESSED_PAYOUT", // "PROCESSED_PAYOUT" or "SIMULATED_BANK_WIRE"

    @ColumnInfo(name = "method")
    val method: String = "BANK_WIRE", // "BANK_WIRE", "PAYPAL", "SEPA_INSTANT", "IDEAL"

    @ColumnInfo(name = "grossAmountUsd")
    val grossAmountUsd: Double = 0.0,

    @ColumnInfo(name = "feeUsd")
    val feeUsd: Double = 0.0,

    @ColumnInfo(name = "netAmountUsd")
    val netAmountUsd: Double = 0.0,

    @ColumnInfo(name = "convertedAmount")
    val convertedAmount: Double = 0.0,

    @ColumnInfo(name = "currencyCode")
    val currencyCode: String = "USD",

    @ColumnInfo(name = "exchangeRate")
    val exchangeRate: Double = 1.0,

    @ColumnInfo(name = "recipientName")
    val recipientName: String = "",

    @ColumnInfo(name = "bankName")
    val bankName: String = "",

    @ColumnInfo(name = "ibanOrAccount")
    val ibanOrAccount: String = "",

    @ColumnInfo(name = "swiftBic")
    val swiftBic: String = "",

    @ColumnInfo(name = "routingNumber")
    val routingNumber: String = "",

    @ColumnInfo(name = "intermediaryBank")
    val intermediaryBank: String = "",

    @ColumnInfo(name = "country")
    val country: String = "",

    @ColumnInfo(name = "clearingRail")
    val clearingRail: String = "SEPA Target2", // "SEPA Target2", "SWIFT", "iDEAL", "PayPal Instant"

    @ColumnInfo(name = "status")
    val status: String = "COMPLETED", // "COMPLETED", "SETTLED", "PROCESSING", "PENDING", "FAILED"

    @ColumnInfo(name = "isSimulatedWire")
    val isSimulatedWire: Boolean = false,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
