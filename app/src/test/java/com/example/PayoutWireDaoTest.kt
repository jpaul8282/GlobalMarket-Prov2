package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PayoutWireDao
import com.example.data.PayoutWireEntity
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.PayoutMethodType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PayoutWireDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var payoutWireDao: PayoutWireDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        payoutWireDao = db.payoutWireDao()
        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            transactionLogDao = db.transactionLogDao(),
            scannedProductDao = db.scannedProductDao(),
            transactionHistoryDao = db.transactionHistoryDao(),
            salesTerminalDao = db.salesTerminalDao(),
            inventoryProductDao = db.inventoryProductDao(),
            salesTransactionDao = db.salesTransactionDao(),
            payoutWireDao = payoutWireDao
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `store and retrieve processed payout and simulated bank wire records`() = runBlocking {
        val processed = PayoutWireEntity(
            referenceCode = "PO-WIRE-100001",
            transactionType = "PROCESSED_PAYOUT",
            method = "BANK_WIRE",
            grossAmountUsd = 1000.0,
            feeUsd = 15.0,
            netAmountUsd = 985.0,
            convertedAmount = 906.20,
            currencyCode = "EUR",
            exchangeRate = 0.92,
            recipientName = "J.P. Westerveld",
            bankName = "ING Bank N.V.",
            ibanOrAccount = "NL43INGB0006372146",
            swiftBic = "INGBL2A",
            country = "Netherlands",
            clearingRail = "SEPA Target2 / SWIFT",
            status = "COMPLETED",
            isSimulatedWire = false,
            timestamp = 2000L,
            notes = "Live owner settlement"
        )

        val simulatedWire = PayoutWireEntity(
            referenceCode = "SIM-IDL-200002",
            transactionType = "SIMULATED_BANK_WIRE",
            method = "IDEAL",
            grossAmountUsd = 250.0,
            feeUsd = 0.35,
            netAmountUsd = 249.65,
            convertedAmount = 230.0,
            currencyCode = "EUR",
            exchangeRate = 0.92,
            recipientName = "J.P. Westerveld",
            bankName = "ING Bank N.V.",
            ibanOrAccount = "NL43INGB0006372146",
            swiftBic = "INGBL2A",
            country = "Netherlands",
            clearingRail = "iDEAL / SEPA Instant",
            status = "SETTLED",
            isSimulatedWire = true,
            timestamp = 5000L,
            notes = "Simulated iDEAL wire test"
        )

        val id1 = payoutWireDao.insertPayoutWire(processed)
        val id2 = payoutWireDao.insertPayoutWire(simulatedWire)
        assertTrue(id1 > 0 && id2 > 0)

        // All records ordered newest first
        val all = payoutWireDao.getAllPayoutWireHistory().first()
        assertEquals(2, all.size)
        assertEquals("SIM-IDL-200002", all[0].referenceCode)
        assertEquals("PO-WIRE-100001", all[1].referenceCode)

        // Filtered queries
        val processedOnly = payoutWireDao.getProcessedPayouts().first()
        assertEquals(1, processedOnly.size)
        assertFalse(processedOnly[0].isSimulatedWire)
        assertEquals("PO-WIRE-100001", processedOnly[0].referenceCode)

        val simulatedOnly = payoutWireDao.getSimulatedBankWires().first()
        assertEquals(1, simulatedOnly.size)
        assertTrue(simulatedOnly[0].isSimulatedWire)
        assertEquals("SIM-IDL-200002", simulatedOnly[0].referenceCode)

        // By reference lookup
        val byRef = payoutWireDao.getPayoutWireByReference("PO-WIRE-100001").first()
        assertNotNull(byRef)
        assertEquals("NL43INGB0006372146", byRef?.ibanOrAccount)
    }

    @Test
    fun `search filter update and delete payout wire history`() = runBlocking {
        val wire1 = PayoutWireEntity(
            referenceCode = "SIM-SEPA-300001",
            transactionType = "SIMULATED_BANK_WIRE",
            method = "SEPA_INSTANT",
            grossAmountUsd = 400.0,
            feeUsd = 0.35,
            netAmountUsd = 399.65,
            convertedAmount = 368.0,
            currencyCode = "EUR",
            recipientName = "Elena Rostova",
            bankName = "Deutsche Bank AG",
            ibanOrAccount = "DE89370400440532013000",
            swiftBic = "DEUTDEDDFXX",
            clearingRail = "SEPA Instant",
            status = "PROCESSING",
            isSimulatedWire = true,
            timestamp = 10000L
        )
        val wire2 = PayoutWireEntity(
            referenceCode = "PO-PP-400001",
            transactionType = "PROCESSED_PAYOUT",
            method = "PAYPAL",
            grossAmountUsd = 150.0,
            feeUsd = 4.65,
            netAmountUsd = 145.35,
            convertedAmount = 150.0,
            currencyCode = "USD",
            recipientName = "Liam O'Connor",
            bankName = "PayPal Digital Wallet",
            ibanOrAccount = "liam@example.com",
            swiftBic = "PAYPAL",
            clearingRail = "PayPal Instant",
            status = "COMPLETED",
            isSimulatedWire = false,
            timestamp = 20000L
        )

        val insertedIds = payoutWireDao.insertPayoutWires(listOf(wire1, wire2))
        assertEquals(2, insertedIds.size)

        // Search by BIC or bank name
        val searchByBank = payoutWireDao.searchPayoutWireHistory("Deutsche Bank").first()
        assertEquals(1, searchByBank.size)
        assertEquals("SIM-SEPA-300001", searchByBank[0].referenceCode)

        // Filter by currency and date range
        assertEquals(1, payoutWireDao.getPayoutWiresByCurrency("USD").first().size)
        assertEquals(2, payoutWireDao.getPayoutWiresByDateRange(5000L, 25000L).first().size)

        // Status mutation
        payoutWireDao.updatePayoutWireStatus(insertedIds[0], "SETTLED")
        val updated = payoutWireDao.getPayoutWireById(insertedIds[0]).first()
        assertEquals("SETTLED", updated?.status)

        // Aggregations
        assertEquals(2, payoutWireDao.getTotalPayoutWireCount().first())
        assertEquals(1, payoutWireDao.getProcessedPayoutCount().first())
        assertEquals(1, payoutWireDao.getSimulatedBankWireCount().first())
        assertEquals(545.0, payoutWireDao.getTotalNetDisbursedUsd().first() ?: 0.0, 0.01)

        // Delete
        payoutWireDao.deletePayoutWireById(insertedIds[1])
        assertNull(payoutWireDao.getPayoutWireById(insertedIds[1]).first())
        assertEquals(1, payoutWireDao.getTotalPayoutWireCount().first())
    }

    @Test
    fun `viewModel records simulated bank wire and processed payout into repository`() = runBlocking {
        val viewModel = GlobalMarketViewModel(repository)

        val simWire = viewModel.saveSimulatedBankWire(
            amountInUsd = 500.0,
            currencyCode = "EUR",
            method = "IDEAL",
            clearingRail = "iDEAL / SEPA Instant",
            notes = "End-to-end simulation check"
        )
        assertTrue(simWire.id > 0)
        assertTrue(simWire.isSimulatedWire)
        assertEquals("SIMULATED_BANK_WIRE", simWire.transactionType)

        viewModel.saveDisbursePayout(
            method = PayoutMethodType.BANK_WIRE,
            amountInUsd = 300.0,
            currencyCode = "EUR",
            notes = "Live payout check"
        )

        val history = repository.allPayoutWireHistory.first()
        assertEquals(2, history.size)
        assertEquals(1, repository.simulatedBankWires.first().size)
        assertEquals(1, repository.processedPayouts.first().size)
    }
}
