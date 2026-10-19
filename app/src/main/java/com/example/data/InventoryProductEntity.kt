package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room database entity representing a product in the local sales terminal inventory.
 *
 * Tracks product details, pricing in multiple global currencies, current stock levels,
 * low-stock alert thresholds, and identification codes (SKU, barcode).
 */
@Entity(
    tableName = "inventory_products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["barcode"]),
        Index(value = ["category"]),
        Index(value = ["isActive"]),
        Index(value = ["stockQuantity"])
    ]
)
data class InventoryProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sku")
    val sku: String, // e.g. "SKU-POS-1001"

    @ColumnInfo(name = "barcode")
    val barcode: String = "", // e.g. UPC/EAN barcode

    @ColumnInfo(name = "name")
    val name: String, // e.g. "Glaba POS Terminal Stand"

    @ColumnInfo(name = "category")
    val category: String = "Retail Item", // "Hardware POS", "Retail", "Accessories", "Digital"

    @ColumnInfo(name = "price")
    val price: Double, // Unit price in default base currency

    @ColumnInfo(name = "currency")
    val currency: String = "USD", // Base currency code (USD, EUR, GBP, JPY, CNY)

    @ColumnInfo(name = "costPrice")
    val costPrice: Double = 0.0, // Wholesale/acquisition cost for margin calculations

    @ColumnInfo(name = "stockQuantity")
    val stockQuantity: Int = 0, // Current inventory count available on terminal

    @ColumnInfo(name = "minStockThreshold")
    val minStockThreshold: Int = 5, // Alert trigger for replenishment

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "imageUrl")
    val imageUrl: String? = null,

    @ColumnInfo(name = "isActive")
    val isActive: Boolean = true,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)
