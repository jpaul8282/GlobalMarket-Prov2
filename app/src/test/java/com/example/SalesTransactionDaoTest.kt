package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SalesTransactionDao
import com.example.data.SalesTransactionEntity
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SalesTransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var salesDao: SalesTransactionDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        salesDao = db.salesTransactionDao()
        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            transactionLogDao = db.transactionLogDao(),
            scannedProductDao = db.scannedProductDao(),
            transactionHistoryDao = db.transactionHistoryDao(),
            salesTerminalDao = db.salesTerminalDao(),
            inventoryProductDao = db.inventoryProductDao(),
            salesTransactionDao = salesDao
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `store and retrieve single sales transaction history record`() = runBlocking {
        val timestamp = 1716000000000L
        val tx = SalesTransactionEntity(
            transactionReference = "TXN-2026-001",
            timestamp = timestamp,
            customerName = "Elena Rostova",
            customerEmail = "elena.rostova@example.com",
            subtotalAmount = 100.0,
            taxAmount = 19.0,
            discountAmount = 10.0,
            totalAmount = 109.0,
            currency = "EUR",
            amountInUsd = 118.81,
            exchangeRate = 1.09,
            paymentMethod = "CARD",
            paymentStatus = "COMPLETED",
            terminalId = "POS-BERLIN-01",
            cashierName = "Hans Gruber",
            itemCount = 2,
            itemsSummary = "Glaba Pro POS Terminal-X (x1), Thermal Receipt Paper (x1)",
            receiptNumber = "REC-94821",
            notes = "Customer used promotional discount code"
        )

        val insertedId = salesDao.insertSalesTransaction(tx)
        assertTrue("Inserted row ID must be greater than 0", insertedId > 0)

        val retrievedById = salesDao.getSalesTransactionById(insertedId).first()
        assertNotNull(retrievedById)
        assertEquals("TXN-2026-001", retrievedById?.transactionReference)
        assertEquals("Elena Rostova", retrievedById?.customerName)
        assertEquals("elena.rostova@example.com", retrievedById?.customerEmail)
        assertEquals(109.0, retrievedById?.totalAmount ?: 0.0, 0.001)
        assertEquals("EUR", retrievedById?.currency)
        assertEquals(118.81, retrievedById?.amountInUsd ?: 0.0, 0.001)
        assertEquals("COMPLETED", retrievedById?.paymentStatus)
        assertEquals("REC-94821", retrievedById?.receiptNumber)

        val retrievedByRef = salesDao.getSalesTransactionByReference("TXN-2026-001").first()
        assertNotNull(retrievedByRef)
        assertEquals(insertedId, retrievedByRef?.id)
    }

    @Test
    fun `store multiple sales transactions and retrieve ordered by timestamp descending`() = runBlocking {
        val tx1 = SalesTransactionEntity(
            transactionReference = "TXN-001",
            timestamp = 1000L,
            customerName = "Customer 1",
            totalAmount = 50.0,
            currency = "USD",
            amountInUsd = 50.0
        )
        val tx2 = SalesTransactionEntity(
            transactionReference = "TXN-002",
            timestamp = 3000L,
            customerName = "Customer 2",
            totalAmount = 120.0,
            currency = "GBP",
            amountInUsd = 150.0
        )
        val tx3 = SalesTransactionEntity(
            transactionReference = "TXN-003",
            timestamp = 2000L,
            customerName = "Customer 3",
            totalAmount = 80.0,
            currency = "EUR",
            amountInUsd = 88.0
        )

        salesDao.insertSalesTransactions(listOf(tx1, tx2, tx3))

        val allTransactions = salesDao.getAllSalesTransactions().first()
        assertEquals(3, allTransactions.size)
        // Ordered by timestamp DESC: tx2 (3000L), tx3 (2000L), tx1 (1000L)
        assertEquals("TXN-002", allTransactions[0].transactionReference)
        assertEquals("TXN-003", allTransactions[1].transactionReference)
        assertEquals("TXN-001", allTransactions[2].transactionReference)
    }

    @Test
    fun `filter sales transactions by currency, date range, and status`() = runBlocking {
        val txEur1 = SalesTransactionEntity(
            transactionReference = "TXN-EUR-1",
            timestamp = 10000L,
            totalAmount = 45.0,
            currency = "EUR",
            paymentStatus = "COMPLETED"
        )
        val txEur2 = SalesTransactionEntity(
            transactionReference = "TXN-EUR-2",
            timestamp = 20000L,
            totalAmount = 90.0,
            currency = "EUR",
            paymentStatus = "REFUNDED"
        )
        val txGbp = SalesTransactionEntity(
            transactionReference = "TXN-GBP-1",
            timestamp = 15000L,
            totalAmount = 75.0,
            currency = "GBP",
            paymentStatus = "COMPLETED"
        )

        salesDao.insertSalesTransactions(listOf(txEur1, txEur2, txGbp))

        // Currency filter
        val eurList = salesDao.getSalesTransactionsByCurrency("EUR").first()
        assertEquals(2, eurList.size)
        assertTrue(eurList.all { it.currency == "EUR" })

        // Date range filter (12000L to 25000L should include txGbp (15000) and txEur2 (20000))
        val rangeList = salesDao.getSalesTransactionsByDateRange(12000L, 25000L).first()
        assertEquals(2, rangeList.size)
        assertEquals("TXN-EUR-2", rangeList[0].transactionReference)
        assertEquals("TXN-GBP-1", rangeList[1].transactionReference)

        // Status filter
        val completedList = salesDao.getSalesTransactionsByStatus("COMPLETED").first()
        assertEquals(2, completedList.size)
        val refundedList = salesDao.getSalesTransactionsByStatus("REFUNDED").first()
        assertEquals(1, refundedList.size)
        assertEquals("TXN-EUR-2", refundedList[0].transactionReference)
    }

    @Test
    fun `search sales transactions across customer name, reference, and items summary`() = runBlocking {
        val tx1 = SalesTransactionEntity(
            transactionReference = "TXN-SEARCH-101",
            customerName = "Sophie Dubois",
            itemsSummary = "Espresso Roast Beans, Filter Papers",
            receiptNumber = "REC-PARIS-01"
        )
        val tx2 = SalesTransactionEntity(
            transactionReference = "TXN-SEARCH-102",
            customerName = "Liam O'Connor",
            itemsSummary = "Enterprise POS Barcode Scanner",
            receiptNumber = "REC-LONDON-02"
        )

        salesDao.insertSalesTransactions(listOf(tx1, tx2))

        // Search by customer name
        val byName = salesDao.searchSalesTransactions("Sophie").first()
        assertEquals(1, byName.size)
        assertEquals("Sophie Dubois", byName[0].customerName)

        // Search by item summary
        val byItem = salesDao.searchSalesTransactions("Barcode Scanner").first()
        assertEquals(1, byItem.size)
        assertEquals("Liam O'Connor", byItem[0].customerName)

        // Search by receipt number
        val byReceipt = salesDao.searchSalesTransactions("REC-PARIS").first()
        assertEquals(1, byReceipt.size)
        assertEquals("TXN-SEARCH-101", byReceipt[0].transactionReference)
    }

    @Test
    fun `aggregate metrics calculate gross sales, USD sales volume, and order counts`() = runBlocking {
        val tx1 = SalesTransactionEntity(
            transactionReference = "TXN-AGG-1",
            totalAmount = 100.0,
            amountInUsd = 100.0,
            currency = "USD",
            paymentStatus = "COMPLETED",
            paymentMethod = "CARD"
        )
        val tx2 = SalesTransactionEntity(
            transactionReference = "TXN-AGG-2",
            totalAmount = 200.0,
            amountInUsd = 220.0,
            currency = "EUR",
            paymentStatus = "COMPLETED",
            paymentMethod = "CARD"
        )
        val tx3 = SalesTransactionEntity(
            transactionReference = "TXN-AGG-3",
            totalAmount = 50.0,
            amountInUsd = 50.0,
            currency = "USD",
            paymentStatus = "REFUNDED", // Should not count in completed totals
            paymentMethod = "CASH"
        )

        salesDao.insertSalesTransactions(listOf(tx1, tx2, tx3))

        val totalCount = salesDao.getTotalSalesCount().first()
        assertEquals(3, totalCount)

        val totalUsd = salesDao.getTotalSalesInUsd().first()
        assertEquals(320.0, totalUsd ?: 0.0, 0.001)

        val completedCount = salesDao.getSalesCountByStatus("COMPLETED").first()
        assertEquals(2, completedCount)

        val refundedCount = salesDao.getSalesCountByStatus("REFUNDED").first()
        assertEquals(1, refundedCount)

        val cardCount = salesDao.getSalesCountByPaymentMethod("CARD").first()
        assertEquals(2, cardCount)
    }

    @Test
    fun `update and delete sales transactions`() = runBlocking {
        val tx = SalesTransactionEntity(
            transactionReference = "TXN-MUTATE-1",
            totalAmount = 150.0,
            paymentStatus = "COMPLETED"
        )

        val id = salesDao.insertSalesTransaction(tx)

        // Update status to REFUNDED
        salesDao.updateTransactionStatus(id, "REFUNDED")
        val updated = salesDao.getSalesTransactionById(id).first()
        assertEquals("REFUNDED", updated?.paymentStatus)

        // Delete by ID
        salesDao.deleteSalesTransactionById(id)
        val deleted = salesDao.getSalesTransactionById(id).first()
        assertNull(deleted)
    }

    @Test
    fun `verify repository and viewModel sales transaction history integration`() = runBlocking {
        val vm = GlobalMarketViewModel(repository)

        // Record a sale via ViewModel
        vm.recordSalesTransaction(
            transactionReference = "TXN-VM-99",
            customerName = "Alexander Hamilton",
            totalAmount = 250.0,
            currency = "USD",
            amountInUsd = 250.0,
            paymentMethod = "NFC",
            paymentStatus = "COMPLETED",
            itemsSummary = "Global Retail Terminal Bundle"
        )

        // Verify stored in repository
        val stored = repository.getSalesTransactionByReference("TXN-VM-99").first()
        assertNotNull(stored)
        assertEquals("Alexander Hamilton", stored?.customerName)
        assertEquals(250.0, stored?.totalAmount ?: 0.0, 0.001)
        assertEquals("NFC", stored?.paymentMethod)
    }
}
