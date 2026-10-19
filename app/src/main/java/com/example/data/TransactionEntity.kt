package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountInUsd: Double,
    val targetCurrency: String,
    val convertedAmount: Double,
    val customerName: String,
    val country: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stripeSubscriptionId: String
)
