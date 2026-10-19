package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        TransactionLogEntity::class,
        ScannedProductEntity::class,
        TransactionHistoryEntity::class,
        InventoryProductEntity::class,
        TerminalTransactionLogEntity::class,
        SalesTransactionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionLogDao(): TransactionLogDao
    abstract fun scannedProductDao(): ScannedProductDao
    abstract fun transactionHistoryDao(): TransactionHistoryDao
    abstract fun salesTerminalDao(): SalesTerminalDao
    abstract fun inventoryProductDao(): InventoryProductDao
    abstract fun salesTransactionDao(): SalesTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "globalmarket_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
