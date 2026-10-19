package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity to store scanned product details including name,
 * price, original currency, target currency, converted price, and barcode details.
 */
@Entity(
    tableName = "scanned_products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["currency"]),
        Index(value = ["timestamp"])
    ]
)
data class ScannedProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "price")
    val price: Double,

    @ColumnInfo(name = "currency")
    val currency: String, // e.g. "USD", "EUR", "GBP", "JPY", "CNY"

    @ColumnInfo(name = "barcode")
    val barcode: String = "",

    @ColumnInfo(name = "barcodeFormat")
    val barcodeFormat: String = "BARCODE",

    @ColumnInfo(name = "targetCurrency")
    val targetCurrency: String? = null,

    @ColumnInfo(name = "convertedPrice")
    val convertedPrice: Double? = null,

    @ColumnInfo(name = "exchangeRateUsed")
    val exchangeRateUsed: Double? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "category")
    val category: String = "Retail Item",

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
