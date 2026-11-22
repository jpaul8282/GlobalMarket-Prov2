package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Currency
import com.example.data.CurrencyRepository
import com.example.data.StripeCurrencyRepository
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.LocalizedPaymentSimulator
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class CurrencyRepositoryAndSimulatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var txRepository: TransactionRepository
    private lateinit var currencyRepository: CurrencyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        txRepository = TransactionRepository(
            transactionDao = db.transactionDao(),
            transactionLogDao = db.transactionLogDao(),
            scannedProductDao = db.scannedProductDao(),
            transactionHistoryDao = db.transactionHistoryDao(),
            salesTerminalDao = db.salesTerminalDao(),
            inventoryProductDao = db.inventoryProductDao(),
            salesTransactionDao = db.salesTransactionDao(),
            payoutWireDao = db.payoutWireDao()
        )
        currencyRepository = StripeCurrencyRepository()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `currency data class holds code and symbol cleanly`() {
        val usd = Currency(code = "USD", symbol = "$")
        assertEquals("USD", usd.code)
        assertEquals("$", usd.symbol)

        val eur = Currency(code = "EUR", symbol = "€", name = "Euro")
        assertEquals("EUR", eur.code)
        assertEquals("€", eur.symbol)
        assertEquals("Euro", eur.name)
    }

    @Test
    fun `currencyRepository returns all supported Stripe currencies with codes and symbols`() {
        val supported = currencyRepository.getSupportedStripeCurrencies()
        assertTrue(supported.size >= 135)

        val usd = currencyRepository.getCurrencyByCode("usd")
        assertNotNull(usd)
        assertEquals("USD", usd?.code)
        assertEquals("$", usd?.symbol)

        val eur = currencyRepository.getCurrencyByCode("EUR")
        assertEquals("€", eur?.symbol)

        val gbp = currencyRepository.getCurrencyByCode("GBP")
        assertEquals("£", gbp?.symbol)

        val jpy = currencyRepository.getCurrencyByCode("JPY")
        assertEquals("¥", jpy?.symbol)
        assertEquals(0, jpy?.decimalPlaces)

        val kwd = currencyRepository.getCurrencyByCode("KWD")
        assertEquals("د.ك", kwd?.symbol)
        assertEquals(3, kwd?.decimalPlaces)

        assertNull(currencyRepository.getCurrencyByCode("INVALID"))
    }

    @Test
    fun `currencyRepository filters by G10, region, symbol, and search query`() {
        val g10 = currencyRepository.filterCurrencies(regionFilter = "G10")
        assertEquals(7, g10.size)

        val nairaSearch = currencyRepository.filterCurrencies(query = "₦")
        assertTrue(nairaSearch.any { it.code == "NGN" && it.symbol == "₦" })

        val middleEast = currencyRepository.filterCurrencies(regionFilter = "Middle East")
        assertTrue(middleEast.any { it.code == "AED" })
        assertTrue(middleEast.any { it.code == "SAR" })
        assertTrue(middleEast.any { it.code == "KWD" })
    }

    @Test
    fun `localizedPaymentSimulator displays repository Stripe currencies and selects currency`() {
        val viewModel = GlobalMarketViewModel(
            repository = txRepository,
            currencyRepository = currencyRepository
        )

        assertEquals(currencyRepository.getSupportedStripeCurrencies().size, viewModel.supportedStripeCurrencies.size)

        composeTestRule.setContent {
            MyApplicationTheme {
                LocalizedPaymentSimulator(viewModel = viewModel)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("simulator_currency_count")
            .assertIsDisplayed()
            .assertTextContains("${viewModel.supportedStripeCurrencies.size} Stripe Currencies")

        composeTestRule.onNodeWithTag("simulator_currency_search").performTextInput("NGN")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("currency_toggle_ngn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertEquals("NGN", viewModel.selectedCurrency.value.code)
        assertEquals("₦", viewModel.selectedCurrency.value.symbol)
    }
}
