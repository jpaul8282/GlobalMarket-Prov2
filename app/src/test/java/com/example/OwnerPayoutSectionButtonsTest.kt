package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.OwnerPayoutSectionView
import com.example.ui.PayoutMethodType
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class OwnerPayoutSectionButtonsTest {

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
        runBlocking {
            db.transactionDao().insertTransaction(
                TransactionEntity(
                    customerName = "Order Revenue Seed",
                    amountInUsd = 1000.0,
                    targetCurrency = "USD",
                    convertedAmount = 1000.0,
                    country = "United States",
                    stripeSubscriptionId = "sub_seed_001"
                )
            )
        }
        viewModel = GlobalMarketViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun ownerPayoutSection_displaysControlButtonsAndTriggersActions() {
        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutSectionView(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Wait for Room transaction seed to emit into Vault balance
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("$1,000.00").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Hero Vault buttons & Action Hub card
        composeTestRule.onNodeWithTag("vault_disburse_max_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vault_view_history_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("owner_payout_action_buttons_card").assertIsDisplayed()

        // Switch rails via Hub buttons
        composeTestRule.onNodeWithTag("payout_hub_btn_paypal").performClick()
        composeTestRule.waitForIdle()
        assertEquals(PayoutMethodType.PAYPAL, viewModel.payoutConfig.value.selectedMethod)

        composeTestRule.onNodeWithTag("payout_hub_btn_stripe").performClick()
        composeTestRule.waitForIdle()
        assertEquals(PayoutMethodType.STRIPE_PAYOUT, viewModel.payoutConfig.value.selectedMethod)

        composeTestRule.onNodeWithTag("payout_hub_btn_wire").performClick()
        composeTestRule.waitForIdle()
        assertEquals(PayoutMethodType.BANK_WIRE, viewModel.payoutConfig.value.selectedMethod)

        // Scroll to split percentage buttons in Disburse card & verify 50% populates 500.00
        composeTestRule.onNodeWithTag("owner_payout_section_view")
            .performScrollToNode(hasTestTag("payout_split_50"))
        composeTestRule.onNodeWithTag("payout_split_50").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owner_payout_amount_input")
            .assertTextContains("500.00")

        // Scroll back up to Action Hub & test Export CSV/JSON button opens dialog
        composeTestRule.onNodeWithTag("owner_payout_section_view")
            .performScrollToNode(hasTestTag("payout_hub_btn_export"))
        composeTestRule.onNodeWithTag("payout_hub_btn_export").performClick()
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("payout_export_dialog").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("payout_export_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payout_export_close_button").performClick()
        composeTestRule.waitForIdle()

        // Test History Ledger button opens Payout History Screen
        composeTestRule.onNodeWithTag("payout_hub_btn_history").performClick()
        composeTestRule.waitForIdle()
        assertTrue(viewModel.isPayoutHistoryScreenOpen.value)

        // Scroll to Vault Stripe status pill & verify inline Stripe controls in Disburse card
        composeTestRule.onNodeWithTag("owner_payout_section_view")
            .performScrollToNode(hasTestTag("vault_stripe_status_pill"))
        composeTestRule.onNodeWithTag("vault_stripe_status_pill").assertIsDisplayed()

        composeTestRule.onNodeWithTag("owner_payout_section_view")
            .performScrollToNode(hasTestTag("owner_payout_select_stripe"))
        composeTestRule.onNodeWithTag("owner_payout_select_stripe").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owner_payout_section_view")
            .performScrollToNode(hasTestTag("disburse_stripe_speed_instant"))
        composeTestRule.onNodeWithTag("disburse_stripe_speed_instant").performClick()
        composeTestRule.waitForIdle()
        assertEquals("instant", viewModel.stripePayoutState.value.selectedSpeedMethod)
    }

    @Test
    fun payoutDisbursementModal_supportsStripeLiveRailAndSpeedSelection() {
        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.PayoutDisbursementModal(
                    viewModel = viewModel,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("payout_method_stripe").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("modal_stripe_details_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("modal_stripe_speed_instant").performClick()
        composeTestRule.waitForIdle()
        assertEquals("instant", viewModel.stripePayoutState.value.selectedSpeedMethod)
    }
}
