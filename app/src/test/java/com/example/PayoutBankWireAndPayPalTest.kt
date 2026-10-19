package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.BankWireDetails
import com.example.ui.GlobalMarketViewModel
import com.example.ui.PayPalDetails
import com.example.ui.PayoutMethodType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PayoutBankWireAndPayPalTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: GlobalMarketViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransactionRepository(
            transactionDao = db.transactionDao(),
            transactionLogDao = db.transactionLogDao(),
            scannedProductDao = db.scannedProductDao(),
            transactionHistoryDao = db.transactionHistoryDao(),
            salesTerminalDao = db.salesTerminalDao(),
            inventoryProductDao = db.inventoryProductDao(),
            salesTransactionDao = db.salesTransactionDao()
        )
        viewModel = GlobalMarketViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `verify Bank Wire and PayPal payment rails exist for USD and EUR`() {
        val usdRails = viewModel.getPaymentRailsForCurrency("USD")
        assertTrue("USD rails should include Bank Wire", usdRails.any { it.id == "bank_wire" && it.name.contains("Wire") })
        assertTrue("USD rails should include PayPal", usdRails.any { it.id == "paypal" && it.name.contains("PayPal") })

        val eurRails = viewModel.getPaymentRailsForCurrency("EUR")
        assertTrue("EUR rails should include Bank Wire", eurRails.any { it.id == "bank_wire_eur" })
        assertTrue("EUR rails should include PayPal", eurRails.any { it.id == "paypal_eur" })
    }

    @Test
    fun `verify payout fee calculation for Bank Wire and PayPal`() {
        val wireFee = viewModel.calculatePayoutFee(PayoutMethodType.BANK_WIRE, 500.0)
        assertEquals(15.00, wireFee, 0.001)

        val payPalFee = viewModel.calculatePayoutFee(PayoutMethodType.PAYPAL, 100.0)
        // 100 * 0.029 + 0.30 = 3.20
        assertEquals(3.20, payPalFee, 0.01)
    }

    @Test
    fun `disburse Bank Wire payout and verify database persistence across all tables`() = runBlocking {
        val customBank = BankWireDetails(
            bankName = "Deutsche Bank AG",
            accountHolderName = "Alexander von Humboldt",
            accountNumberOrIban = "DE89370400440532013000",
            swiftBic = "DEUTDEDDFXX",
            routingNumber = "37040044",
            country = "Germany"
        )
        viewModel.updateBankWireDetails(customBank)
        viewModel.selectPayoutMethod(PayoutMethodType.BANK_WIRE)

        viewModel.saveDisbursePayout(
            method = PayoutMethodType.BANK_WIRE,
            amountInUsd = 750.0,
            currencyCode = "EUR",
            recipientName = "Alexander von Humboldt",
            destinationIdentifier = "DE89370400440532013000",
            notes = "Monthly Retail Revenue Settlement"
        )

        // Verify TransactionEntity
        val transactions = repository.allTransactions.first()
        val wireTx = transactions.firstOrNull { it.customerName.contains("Alexander von Humboldt") && it.customerName.contains("Bank Wire") }
        assertNotNull("Wire transaction entity should be persisted", wireTx)
        assertEquals(750.0, wireTx?.amountInUsd ?: 0.0, 0.01)
        assertEquals("EUR", wireTx?.targetCurrency)

        // Verify TransactionLogEntity
        val logs = repository.allLogs.first()
        val wireLog = logs.firstOrNull { it.logType == "PAYOUT" && it.paymentMethod == "Bank Wire Transfer" }
        assertNotNull("Wire transaction log should be persisted", wireLog)
        assertEquals(750.0, wireLog?.amountInUsd ?: 0.0, 0.01)
        assertEquals("COMPLETED", wireLog?.status)
        assertTrue(wireLog?.referenceCode?.startsWith("PO-WIRE-") == true)

        // Verify SalesTransactionEntity
        val sales = repository.allSalesTransactions.first()
        val wireSale = sales.firstOrNull { it.paymentMethod == "Bank Wire Transfer" }
        assertNotNull("Wire sales transaction should be persisted", wireSale)
        assertEquals(750.0, wireSale?.amountInUsd ?: 0.0, 0.01)
        assertEquals("COMPLETED", wireSale?.paymentStatus)
    }

    @Test
    fun `disburse PayPal payout and verify database persistence across all tables`() = runBlocking {
        val customPayPal = PayPalDetails(
            paypalEmailOrId = "merchant.finance@globalmarket.com",
            recipientName = "Sophie Dubois",
            phoneOrPaypalMe = "paypal.me/sophiedubois"
        )
        viewModel.updatePayPalDetails(customPayPal)
        viewModel.selectPayoutMethod(PayoutMethodType.PAYPAL)

        viewModel.saveDisbursePayout(
            method = PayoutMethodType.PAYPAL,
            amountInUsd = 320.0,
            currencyCode = "USD",
            recipientName = "Sophie Dubois",
            destinationIdentifier = "merchant.finance@globalmarket.com",
            notes = "Affiliate marketing referral commission"
        )

        // Verify TransactionEntity
        val transactions = repository.allTransactions.first()
        val payPalTx = transactions.firstOrNull { it.customerName.contains("Sophie Dubois") && it.customerName.contains("PayPal") }
        assertNotNull("PayPal transaction entity should be persisted", payPalTx)
        assertEquals(320.0, payPalTx?.amountInUsd ?: 0.0, 0.01)

        // Verify TransactionLogEntity
        val logs = repository.allLogs.first()
        val payPalLog = logs.firstOrNull { it.logType == "PAYOUT" && it.paymentMethod == "PayPal" }
        assertNotNull("PayPal transaction log should be persisted", payPalLog)
        assertEquals(320.0, payPalLog?.amountInUsd ?: 0.0, 0.01)
        assertEquals("merchant.finance@globalmarket.com", payPalLog?.barcodeOrRawValue)
        assertTrue(payPalLog?.referenceCode?.startsWith("PO-PP-") == true)

        // Verify SalesTransactionEntity
        val sales = repository.allSalesTransactions.first()
        val payPalSale = sales.firstOrNull { it.paymentMethod == "PayPal" }
        assertNotNull("PayPal sales record should be persisted", payPalSale)
        assertEquals("merchant.finance@globalmarket.com", payPalSale?.customerEmail)
    }

    @Test
    fun `verify payout configuration state switching and modal open close`() {
        assertEquals(PayoutMethodType.BANK_WIRE, viewModel.payoutConfig.value.selectedMethod)
        viewModel.selectPayoutMethod(PayoutMethodType.PAYPAL)
        assertEquals(PayoutMethodType.PAYPAL, viewModel.payoutConfig.value.selectedMethod)

        viewModel.selectPayoutMethod(PayoutMethodType.BANK_WIRE)
        assertEquals(PayoutMethodType.BANK_WIRE, viewModel.payoutConfig.value.selectedMethod)

        assertEquals(false, viewModel.isPayoutModalOpen.value)
        viewModel.openPayoutModal()
        assertEquals(true, viewModel.isPayoutModalOpen.value)
        viewModel.closePayoutModal()
        assertEquals(false, viewModel.isPayoutModalOpen.value)
    }
}
