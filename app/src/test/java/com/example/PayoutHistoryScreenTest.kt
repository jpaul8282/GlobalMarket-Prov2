package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.PayoutHistoryScreen
import com.example.ui.PayoutMethodType
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PayoutHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
            salesTransactionDao = db.salesTransactionDao(),
            payoutWireDao = db.payoutWireDao()
        )
        viewModel = GlobalMarketViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun payoutHistoryScreen_displaysSummaryAndRoomRecords() {
        runBlocking {
            viewModel.saveDisbursePayout(
                method = PayoutMethodType.BANK_WIRE,
                amountInUsd = 1000.0,
                currencyCode = "EUR",
                notes = "Quarterly owner bank settlement"
            )
            viewModel.saveSimulatedBankWire(
                amountInUsd = 250.0,
                currencyCode = "EUR",
                method = "IDEAL",
                clearingRail = "iDEAL / SEPA Instant",
                notes = "Instant iDEAL clearing test"
            )
        }

        composeTestRule.setContent {
            MyApplicationTheme {
                PayoutHistoryScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        // Wait for Room Flow to emit into Compose StateFlow
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("STORED TRANSACTIONS (2)").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Summary Hero Card & Total Processed Transactions
        composeTestRule.onNodeWithTag("payout_history_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payout_summary_total_count")
            .assertIsDisplayed()
            .assertTextContains("2 Transactions")
        composeTestRule.onNodeWithTag("payout_summary_net_usd").assertIsDisplayed()

        // Verify both items are displayed
        composeTestRule.onNodeWithText("STORED TRANSACTIONS (2)").assertIsDisplayed()

        // Filter by Simulated Wires
        composeTestRule.onNodeWithTag("payout_history_chip_SIMULATED").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("STORED TRANSACTIONS (1)").assertIsDisplayed()

        // Filter by Processed Payouts
        composeTestRule.onNodeWithTag("payout_history_chip_PROCESSED").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("STORED TRANSACTIONS (1)").assertIsDisplayed()

        // Return to All and test Search
        composeTestRule.onNodeWithTag("payout_history_chip_ALL").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("payout_history_search_input").performTextInput("iDEAL")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("STORED TRANSACTIONS (1)").assertIsDisplayed()
    }

    @Test
    fun payoutHistoryScreen_emptyStateCanSimulateWire() {
        composeTestRule.setContent {
            MyApplicationTheme {
                PayoutHistoryScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("payout_summary_total_count")
            .assertTextContains("0 Transactions")
        composeTestRule.onNodeWithTag("payout_history_empty_state")
            .performScrollTo()
            .assertIsDisplayed()

        // Click simulate from empty state
        composeTestRule.onNodeWithTag("payout_history_empty_simulate_button")
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("1 Transactions").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
