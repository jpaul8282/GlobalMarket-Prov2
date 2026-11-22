package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.StripeConnectionHealth
import com.example.data.StripeCurrencyCatalog
import com.example.data.StripeCurrencyRegion
import com.example.data.StripeExternalAccount
import com.example.data.StripePayoutGateway
import com.example.data.TransactionRepository
import com.example.ui.ExternalAccountsCurrencyCoinsSection
import com.example.ui.GlobalMarketViewModel
import com.example.ui.StripeLivePayoutCard
import com.example.ui.StripePayoutUiState
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CurrencyFormatter
import com.example.util.CurrencyUtils
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StripeExternalAccountCurrenciesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository

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
            salesTransactionDao = db.salesTransactionDao(),
            payoutWireDao = db.payoutWireDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `stripe currency catalog includes all 135+ unique ISO currencies across all regions and precisions`() {
        val allCoins = StripeCurrencyCatalog.allStripeCurrencies
        assertTrue("Expected at least 135 Stripe currencies, got ${allCoins.size}", allCoins.size >= 135)

        val uniqueCodes = allCoins.map { it.code.uppercase() }.toSet()
        assertEquals("Currency codes must be unique", allCoins.size, uniqueCodes.size)

        allCoins.forEach { coin ->
            assertEquals(3, coin.code.length)
            assertTrue(coin.name.isNotBlank())
            assertTrue(coin.symbol.isNotBlank())
            assertTrue(coin.flagEmoji.isNotBlank())
            assertTrue(coin.defaultCountry.isNotBlank())
            assertTrue(coin.approximateUsdRate > 0.0)
            assertTrue(coin.decimalPlaces in setOf(0, 2, 3))
        }

        // Verify all 5 regions populated
        StripeCurrencyRegion.entries.forEach { region ->
            assertTrue(
                "Missing coins for region $region",
                allCoins.any { it.region == region }
            )
        }

        // Verify all 16 zero-decimal and 5 three-decimal Stripe currencies
        StripeCurrencyCatalog.ZERO_DECIMAL_CODES.forEach { zeroCode ->
            val found = StripeCurrencyCatalog.findByCode(zeroCode)
            assertNotNull("Missing zero-decimal currency $zeroCode", found)
            assertEquals(0, found?.decimalPlaces)
        }
        StripeCurrencyCatalog.THREE_DECIMAL_CODES.forEach { threeCode ->
            val found = StripeCurrencyCatalog.findByCode(threeCode)
            assertNotNull("Missing three-decimal currency $threeCode", found)
            assertEquals(3, found?.decimalPlaces)
        }
    }

    @Test
    fun `stripe minor unit math and currency utils support zero, two, and three decimal coins`() {
        // 0-decimal
        assertEquals(15520L, StripePayoutGateway.toMinorUnits(15520.0, "JPY"))
        assertEquals(15520.0, StripePayoutGateway.toMajorAmount(15520L, "JPY"), 0.001)
        assertEquals(0, CurrencyUtils.getFractionDigits("KRW"))
        assertEquals(0, CurrencyUtils.getFractionDigits("VND"))

        // 2-decimal
        assertEquals(25050L, StripePayoutGateway.toMinorUnits(250.50, "USD"))
        assertEquals(250.50, StripePayoutGateway.toMajorAmount(25050L, "USD"), 0.001)
        assertEquals(2, CurrencyUtils.getFractionDigits("NGN"))

        // 3-decimal (BHD, JOD, KWD, OMR, TND)
        assertEquals(12345L, StripePayoutGateway.toMinorUnits(12.345, "KWD"))
        assertEquals(12.345, StripePayoutGateway.toMajorAmount(12345L, "KWD"), 0.0001)
        assertEquals(1000L, StripePayoutGateway.toMinorUnits(1.000, "BHD"))
        assertEquals(3, CurrencyUtils.getFractionDigits("OMR"))
    }

    @Test
    fun `filterCoins supports query by code or country and category filters`() {
        val kwdMatches = StripeCurrencyCatalog.filterCoins(query = "Kuwait")
        assertTrue(kwdMatches.any { it.code == "KWD" })

        val threeDecimal = StripeCurrencyCatalog.filterCoins(categoryFilter = "THREE_DECIMAL")
        assertEquals(5, threeDecimal.size)
        assertTrue(threeDecimal.map { it.code }.containsAll(listOf("BHD", "JOD", "KWD", "OMR", "TND")))

        val zeroDecimal = StripeCurrencyCatalog.filterCoins(categoryFilter = "ZERO_DECIMAL")
        assertEquals(16, zeroDecimal.size)

        val africa = StripeCurrencyCatalog.filterCoins(categoryFilter = "AFRICA")
        assertTrue(africa.any { it.code == "NGN" })
        assertTrue(africa.any { it.code == "ZAR" })
        assertTrue(africa.any { it.code == "KES" })
    }

    @Test
    fun `viewModel selects any of the 136 stripe currency coins and syncs currency state`() {
        val viewModel = GlobalMarketViewModel(repository)

        assertTrue(viewModel.allStripeCurrencyCoins.size >= 135)

        viewModel.selectStripeExternalAccountCurrency("KWD")
        assertEquals("KWD", viewModel.stripePayoutState.value.selectedExternalAccountCurrencyCode)
        assertEquals("KWD", viewModel.selectedCurrency.value.code)
        assertEquals("Kuwait", viewModel.selectedCurrency.value.defaultCountry)

        val ngn = viewModel.findAnyCurrencyInfo("ngn")
        assertEquals("NGN", ngn.code)
        assertEquals("₦", ngn.symbol)
        assertEquals("Nigeria", ngn.defaultCountry)
    }

    @Test
    fun `externalAccountsCurrencyCoinsSection displays coins, searches KWD, and invokes callback`() {
        var selectedCurrency = "EUR"
        var selectedDestination: String? = "ba_test_eur_01"

        composeTestRule.setContent {
            MyApplicationTheme {
                ExternalAccountsCurrencyCoinsSection(
                    externalAccounts = listOf(
                        StripeExternalAccount(
                            id = "ba_test_eur_01",
                            type = "bank_account",
                            label = "ING Bank N.V.",
                            last4 = "4821",
                            currencyCode = "EUR",
                            country = "NL",
                            status = "verified",
                            isDefaultForCurrency = true
                        )
                    ),
                    selectedDestinationId = selectedDestination,
                    selectedCurrencyCode = selectedCurrency,
                    onSelectDestination = { selectedDestination = it },
                    onSelectCurrencyCoin = { selectedCurrency = it },
                    scopeTag = "test_ext",
                    destinationTagPrefix = "test_dest_"
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_ext_external_accounts_coins_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_ext_coin_count_badge")
            .assertTextContains("${StripeCurrencyCatalog.allStripeCurrencies.size} Coins")
        composeTestRule.onNodeWithTag("test_dest_ba_test_eur_01").assertIsDisplayed()

        // Filter by 3-decimal category and verify KWD coin chip appears and is clickable
        composeTestRule.onNodeWithTag("test_ext_coin_filter_THREE_DECIMAL").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_ext_coin_list").performScrollToNode(hasTestTag("test_ext_coin_KWD"))
        composeTestRule.onNodeWithTag("test_ext_coin_KWD").assertIsDisplayed().performClick()
        assertEquals("KWD", selectedCurrency)

        // Reset filter to ALL, search by name "Nigerian", verify NGN appears & click
        composeTestRule.onNodeWithTag("test_ext_coin_filter_ALL").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("test_ext_coin_search").performTextInput("Nigerian")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_ext_coin_NGN").assertIsDisplayed().performClick()
        assertEquals("NGN", selectedCurrency)
    }

    @Test
    fun `stripeLivePayoutCard embeds all stripe currency coins in external accounts block`() {
        var chosenCoin = "EUR"
        composeTestRule.setContent {
            MyApplicationTheme {
                StripeLivePayoutCard(
                    stripeState = StripePayoutUiState(
                        isKeyConfigured = true,
                        environmentLabel = "TEST MODE",
                        connectionHealth = StripeConnectionHealth.CONNECTED,
                        maskedKeyPreview = "sk_test_••••9999",
                        externalAccounts = listOf(
                            StripeExternalAccount(
                                id = "ba_1N8vING001",
                                type = "bank_account",
                                label = "ING Bank N.V.",
                                last4 = "4821",
                                currencyCode = "EUR",
                                country = "NL",
                                status = "verified",
                                isDefaultForCurrency = true
                            )
                        )
                    ),
                    amountInUsd = 250.0,
                    currencyCode = "EUR",
                    currencyFormatter = CurrencyFormatter.DEFAULT,
                    onSelectSpeed = {},
                    onSyncStripe = {},
                    onExecuteStripePayout = {},
                    onCancelStripePayout = {},
                    onSelectCurrencyCoin = { chosenCoin = it }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stripe_card_external_accounts_coins_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_card_coin_search").performTextInput("JPY")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Japanese Yen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_card_coin_JPY").performClick()
        assertEquals("JPY", chosenCoin)
    }
}
