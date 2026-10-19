package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val transactionLogDao: TransactionLogDao? = null,
    private val scannedProductDao: ScannedProductDao? = null,
    private val transactionHistoryDao: TransactionHistoryDao? = null,
    private val salesTerminalDao: SalesTerminalDao? = null,
    private val inventoryProductDao: InventoryProductDao? = null,
    private val salesTransactionDao: SalesTransactionDao? = null
) {
    // Legacy / Primary Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val totalRevenueUsd: Flow<Double?> = transactionDao.getTotalRevenueUsd()
    val transactionCount: Flow<Int> = transactionDao.getTransactionCount()

    // Enhanced Transaction Logs for Scanned Items & Payments
    val allLogs: Flow<List<TransactionLogEntity>> =
        transactionLogDao?.getAllLogs() ?: flowOf(emptyList())

    val scannedItemLogs: Flow<List<TransactionLogEntity>> =
        transactionLogDao?.getScannedItemLogs() ?: flowOf(emptyList())

    val paymentLogs: Flow<List<TransactionLogEntity>> =
        transactionLogDao?.getPaymentLogs() ?: flowOf(emptyList())

    val totalLogCount: Flow<Int> =
        transactionLogDao?.getTotalLogCount() ?: flowOf(0)

    val scannedItemCount: Flow<Int> =
        transactionLogDao?.getScannedItemCount() ?: flowOf(0)

    val paymentCount: Flow<Int> =
        transactionLogDao?.getPaymentCount() ?: flowOf(0)

    fun getLogById(id: Long): Flow<TransactionLogEntity?> =
        transactionLogDao?.getLogById(id) ?: flowOf(null)

    fun getLogsByType(type: String): Flow<List<TransactionLogEntity>> =
        transactionLogDao?.getLogsByType(type) ?: flowOf(emptyList())

    fun searchLogs(query: String): Flow<List<TransactionLogEntity>> =
        transactionLogDao?.searchLogs(query) ?: flowOf(emptyList())

    suspend fun insertLog(log: TransactionLogEntity): Long {
        return transactionLogDao?.insertLog(log) ?: -1L
    }

    suspend fun insertLogs(logs: List<TransactionLogEntity>) {
        transactionLogDao?.insertLogs(logs)
    }

    suspend fun deleteLog(log: TransactionLogEntity) {
        transactionLogDao?.deleteLog(log)
    }

    suspend fun deleteLogById(id: Long) {
        transactionLogDao?.deleteLogById(id)
    }

    suspend fun clearAllLogs() {
        transactionLogDao?.clearAllLogs()
    }

    // Standard transactions
    fun getTransactionById(id: Int): Flow<TransactionEntity?> =
        transactionDao.getTransactionById(id)

    fun getTransactionsByCurrency(currency: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCurrency(currency)

    suspend fun insert(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertAll(transactions: List<TransactionEntity>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun clearAll() {
        transactionDao.clearAllTransactions()
    }

    // Scanned Products in Room Database
    val allScannedProducts: Flow<List<ScannedProductEntity>> =
        scannedProductDao?.getAllScannedProducts() ?: flowOf(emptyList())

    val scannedProductCount: Flow<Int> =
        scannedProductDao?.getScannedProductCount() ?: flowOf(0)

    fun getScannedProductById(id: Long): Flow<ScannedProductEntity?> =
        scannedProductDao?.getScannedProductById(id) ?: flowOf(null)

    fun getScannedProductsByBarcode(barcode: String): Flow<List<ScannedProductEntity>> =
        scannedProductDao?.getScannedProductsByBarcode(barcode) ?: flowOf(emptyList())

    suspend fun insertScannedProduct(product: ScannedProductEntity): Long {
        return scannedProductDao?.insertScannedProduct(product) ?: -1L
    }

    suspend fun insertScannedProducts(products: List<ScannedProductEntity>) {
        scannedProductDao?.insertScannedProducts(products)
    }

    suspend fun updateScannedProduct(product: ScannedProductEntity) {
        scannedProductDao?.updateScannedProduct(product)
    }

    suspend fun deleteScannedProduct(product: ScannedProductEntity) {
        scannedProductDao?.deleteScannedProduct(product)
    }

    suspend fun deleteScannedProductById(id: Long) {
        scannedProductDao?.deleteScannedProductById(id)
    }

    suspend fun clearAllScannedProducts() {
        scannedProductDao?.clearAllScannedProducts()
    }

    // Transaction History Records
    val allHistoryRecords: Flow<List<TransactionHistoryEntity>> =
        transactionHistoryDao?.getAllTransactionHistory() ?: flowOf(emptyList())

    val historyCount: Flow<Int> =
        transactionHistoryDao?.getTransactionHistoryCount() ?: flowOf(0)

    fun getHistoryById(id: Long): Flow<TransactionHistoryEntity?> =
        transactionHistoryDao?.getTransactionHistoryById(id) ?: flowOf(null)

    fun getHistoryByCurrency(currency: String): Flow<List<TransactionHistoryEntity>> =
        transactionHistoryDao?.getTransactionHistoryByCurrency(currency) ?: flowOf(emptyList())

    fun getHistoryByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionHistoryEntity>> =
        transactionHistoryDao?.getTransactionHistoryByDateRange(startDate, endDate) ?: flowOf(emptyList())

    fun getHistoryByType(type: String): Flow<List<TransactionHistoryEntity>> =
        transactionHistoryDao?.getTransactionHistoryByType(type) ?: flowOf(emptyList())

    fun getTotalHistoryAmountByCurrency(currency: String): Flow<Double?> =
        transactionHistoryDao?.getTotalAmountByCurrency(currency) ?: flowOf(null)

    suspend fun insertHistoryRecord(record: TransactionHistoryEntity): Long {
        return transactionHistoryDao?.insertTransactionHistory(record) ?: -1L
    }

    suspend fun insertHistoryRecords(records: List<TransactionHistoryEntity>) {
        transactionHistoryDao?.insertTransactionHistoryList(records)
    }

    suspend fun updateHistoryRecord(record: TransactionHistoryEntity) {
        transactionHistoryDao?.updateTransactionHistory(record)
    }

    suspend fun deleteHistoryRecord(record: TransactionHistoryEntity) {
        transactionHistoryDao?.deleteTransactionHistory(record)
    }

    suspend fun deleteHistoryRecordById(id: Long) {
        transactionHistoryDao?.deleteTransactionHistoryById(id)
    }

    suspend fun clearAllHistoryRecords() {
        transactionHistoryDao?.clearAllTransactionHistory()
    }

    // ==========================================
    // Sales Terminal - Local Inventory of Products
    // ==========================================
    val allInventoryProducts: Flow<List<InventoryProductEntity>> =
        salesTerminalDao?.getAllInventoryProducts()
            ?: inventoryProductDao?.getAllProducts()
            ?: flowOf(emptyList())

    val inventoryProductCount: Flow<Int> =
        salesTerminalDao?.getTotalProductCount()
            ?: inventoryProductDao?.getProductCount()
            ?: flowOf(0)

    val lowStockProducts: Flow<List<InventoryProductEntity>> =
        salesTerminalDao?.getLowStockProducts()
            ?: inventoryProductDao?.getLowStockProducts()
            ?: flowOf(emptyList())

    val totalStockQuantity: Flow<Int?> =
        salesTerminalDao?.getTotalStockQuantity()
            ?: inventoryProductDao?.getTotalStockQuantity()
            ?: flowOf(0)

    val totalInventoryValue: Flow<Double?> =
        salesTerminalDao?.getTotalInventoryValue() ?: flowOf(0.0)

    fun getInventoryProductById(id: Long): Flow<InventoryProductEntity?> =
        salesTerminalDao?.getInventoryProductById(id)
            ?: inventoryProductDao?.getProductById(id)
            ?: flowOf(null)

    fun getInventoryProductByBarcode(barcode: String): Flow<InventoryProductEntity?> =
        salesTerminalDao?.getInventoryProductByBarcode(barcode)
            ?: inventoryProductDao?.getProductByBarcode(barcode)
            ?: flowOf(null)

    fun getInventoryProductBySku(sku: String): Flow<InventoryProductEntity?> =
        salesTerminalDao?.getInventoryProductBySku(sku)
            ?: inventoryProductDao?.getProductBySku(sku)
            ?: flowOf(null)

    fun searchInventoryProducts(query: String): Flow<List<InventoryProductEntity>> =
        salesTerminalDao?.searchInventoryProducts(query)
            ?: inventoryProductDao?.searchProducts(query)
            ?: flowOf(emptyList())

    fun getInventoryProductsByCategory(category: String): Flow<List<InventoryProductEntity>> =
        salesTerminalDao?.getInventoryProductsByCategory(category)
            ?: inventoryProductDao?.getProductsByCategory(category)
            ?: flowOf(emptyList())

    suspend fun insertInventoryProduct(product: InventoryProductEntity): Long {
        return salesTerminalDao?.insertProduct(product)
            ?: inventoryProductDao?.insertProduct(product)
            ?: -1L
    }

    suspend fun insertInventoryProducts(products: List<InventoryProductEntity>) {
        salesTerminalDao?.insertProducts(products)
            ?: inventoryProductDao?.insertProducts(products)
    }

    suspend fun updateInventoryProduct(product: InventoryProductEntity) {
        salesTerminalDao?.updateProduct(product)
            ?: inventoryProductDao?.updateProduct(product)
    }

    suspend fun updateStock(id: Long, newStock: Int) {
        salesTerminalDao?.updateStock(id, newStock)
            ?: inventoryProductDao?.updateStock(id, newStock)
    }

    suspend fun decrementStock(id: Long, quantity: Int): Int {
        return salesTerminalDao?.decrementStock(id, quantity)
            ?: inventoryProductDao?.decrementStock(id, quantity)
            ?: 0
    }

    suspend fun incrementStock(id: Long, quantity: Int): Int {
        return salesTerminalDao?.incrementStock(id, quantity)
            ?: inventoryProductDao?.incrementStock(id, quantity)
            ?: 0
    }

    suspend fun deleteInventoryProduct(product: InventoryProductEntity) {
        salesTerminalDao?.deleteProduct(product)
            ?: inventoryProductDao?.deleteProduct(product)
    }

    suspend fun deleteInventoryProductById(id: Long) {
        salesTerminalDao?.deleteProductById(id)
            ?: inventoryProductDao?.deleteProductById(id)
    }

    suspend fun clearAllInventoryProducts() {
        salesTerminalDao?.clearAllProducts()
            ?: inventoryProductDao?.clearAllProducts()
    }

    // ==========================================
    // Sales Terminal - Transaction Logs
    // ==========================================
    val allTerminalLogs: Flow<List<TerminalTransactionLogEntity>> =
        salesTerminalDao?.getAllTerminalLogs() ?: flowOf(emptyList())

    val terminalLogCount: Flow<Int> =
        salesTerminalDao?.getTerminalLogCount() ?: flowOf(0)

    val totalTerminalSalesVolumeUsd: Flow<Double?> =
        salesTerminalDao?.getTotalTerminalSalesVolumeUsd() ?: flowOf(0.0)

    fun getTerminalLogById(id: Long): Flow<TerminalTransactionLogEntity?> =
        salesTerminalDao?.getTerminalLogById(id) ?: flowOf(null)

    fun getTerminalLogsByTerminal(terminalId: String): Flow<List<TerminalTransactionLogEntity>> =
        salesTerminalDao?.getTerminalLogsByTerminal(terminalId) ?: flowOf(emptyList())

    fun getTerminalLogsByType(type: String): Flow<List<TerminalTransactionLogEntity>> =
        salesTerminalDao?.getTerminalLogsByType(type) ?: flowOf(emptyList())

    fun getTerminalLogsByStatus(status: String): Flow<List<TerminalTransactionLogEntity>> =
        salesTerminalDao?.getTerminalLogsByStatus(status) ?: flowOf(emptyList())

    fun searchTerminalLogs(query: String): Flow<List<TerminalTransactionLogEntity>> =
        salesTerminalDao?.searchTerminalLogs(query) ?: flowOf(emptyList())

    suspend fun insertTerminalLog(log: TerminalTransactionLogEntity): Long {
        return salesTerminalDao?.insertTerminalLog(log) ?: -1L
    }

    suspend fun insertTerminalLogs(logs: List<TerminalTransactionLogEntity>) {
        salesTerminalDao?.insertTerminalLogs(logs)
    }

    suspend fun processTerminalSale(
        log: TerminalTransactionLogEntity,
        productId: Long? = log.productId,
        quantitySold: Int = log.quantity
    ): Long {
        return salesTerminalDao?.processTerminalSale(log, productId, quantitySold)
            ?: -1L
    }

    suspend fun updateTerminalLog(log: TerminalTransactionLogEntity) {
        salesTerminalDao?.updateTerminalLog(log)
    }

    suspend fun deleteTerminalLog(log: TerminalTransactionLogEntity) {
        salesTerminalDao?.deleteTerminalLog(log)
    }

    suspend fun deleteTerminalLogById(id: Long) {
        salesTerminalDao?.deleteTerminalLogById(id)
    }

    suspend fun clearAllTerminalLogs() {
        salesTerminalDao?.clearAllTerminalLogs()
    }

    // ==========================================
    // Sales Transaction History
    // ==========================================
    val allSalesTransactions: Flow<List<SalesTransactionEntity>> =
        salesTransactionDao?.getAllSalesTransactions() ?: flowOf(emptyList())

    val totalSalesCount: Flow<Int> =
        salesTransactionDao?.getTotalSalesCount() ?: flowOf(0)

    val totalGrossSales: Flow<Double?> =
        salesTransactionDao?.getTotalGrossSales() ?: flowOf(0.0)

    val totalSalesInUsd: Flow<Double?> =
        salesTransactionDao?.getTotalSalesInUsd() ?: flowOf(0.0)

    val averageOrderValueUsd: Flow<Double?> =
        salesTransactionDao?.getAverageOrderValueUsd() ?: flowOf(0.0)

    fun getSalesTransactionById(id: Long): Flow<SalesTransactionEntity?> =
        salesTransactionDao?.getSalesTransactionById(id) ?: flowOf(null)

    fun getSalesTransactionByReference(reference: String): Flow<SalesTransactionEntity?> =
        salesTransactionDao?.getSalesTransactionByReference(reference) ?: flowOf(null)

    fun getSalesTransactionsByCurrency(currency: String): Flow<List<SalesTransactionEntity>> =
        salesTransactionDao?.getSalesTransactionsByCurrency(currency) ?: flowOf(emptyList())

    fun getSalesTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<SalesTransactionEntity>> =
        salesTransactionDao?.getSalesTransactionsByDateRange(startTime, endTime) ?: flowOf(emptyList())

    fun getSalesTransactionsByStatus(status: String): Flow<List<SalesTransactionEntity>> =
        salesTransactionDao?.getSalesTransactionsByStatus(status) ?: flowOf(emptyList())

    fun getSalesTransactionsByPaymentMethod(method: String): Flow<List<SalesTransactionEntity>> =
        salesTransactionDao?.getSalesTransactionsByPaymentMethod(method) ?: flowOf(emptyList())

    fun searchSalesTransactions(query: String): Flow<List<SalesTransactionEntity>> =
        salesTransactionDao?.searchSalesTransactions(query) ?: flowOf(emptyList())

    fun getRecentSalesTransactions(limit: Int = 10): Flow<List<SalesTransactionEntity>> =
        salesTransactionDao?.getRecentSalesTransactions(limit) ?: flowOf(emptyList())

    suspend fun insertSalesTransaction(transaction: SalesTransactionEntity): Long {
        return salesTransactionDao?.insertSalesTransaction(transaction) ?: -1L
    }

    suspend fun insertSalesTransactions(transactions: List<SalesTransactionEntity>): List<Long> {
        return salesTransactionDao?.insertSalesTransactions(transactions) ?: emptyList()
    }

    suspend fun updateSalesTransaction(transaction: SalesTransactionEntity) {
        salesTransactionDao?.updateSalesTransaction(transaction)
    }

    suspend fun updateTransactionStatus(id: Long, newStatus: String) {
        salesTransactionDao?.updateTransactionStatus(id, newStatus)
    }

    suspend fun deleteSalesTransaction(transaction: SalesTransactionEntity) {
        salesTransactionDao?.deleteSalesTransaction(transaction)
    }

    suspend fun deleteSalesTransactionById(id: Long) {
        salesTransactionDao?.deleteSalesTransactionById(id)
    }

    suspend fun clearAllSalesTransactions() {
        salesTransactionDao?.clearAllSalesTransactions()
    }
}

