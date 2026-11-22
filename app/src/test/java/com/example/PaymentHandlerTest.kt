package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PaymentHandler
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.StripeTerminalCheckoutModal
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.stripe.android.PaymentConfiguration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PaymentHandlerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: GlobalMarketViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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
        viewModel = GlobalMarketViewModel(
            repository = repository,
            firestoreRepository = null
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun paymentHandler_initializesStripeSdkWithEnvironmentPublishableKey() {
        val handler = PaymentHandler(
            publishableKeyProvider = { "pk_live_51ExampleHandlerKey9999" }
        )

        assertTrue(handler.isEnvironmentKeyConfigured())
        assertTrue(handler.isLiveEnvironmentKey())
        assertEquals("pk_live_••••9999", handler.maskedPublishableKey())

        val initializedKey = handler.initializeStripeSdk(context)
        assertEquals("pk_live_51ExampleHandlerKey9999", initializedKey)
        assertTrue(handler.isInitialized)
        assertEquals(
            "pk_live_51ExampleHandlerKey9999",
            PaymentConfiguration.getInstance(context).publishableKey
        )

        val config = handler.buildPaymentSheetConfiguration(merchantDisplayName = "GlobalMarket Pro")
        assertNotNull(config)
    }

    @Test
    fun paymentHandler_fallsBackGracefullyWhenKeyIsPlaceholder() {
        val handler = PaymentHandler(
            publishableKeyProvider = { "MY_STRIPE_PUBLISHABLE_KEY" }
        )

        assertFalse(handler.isEnvironmentKeyConfigured())
        assertFalse(handler.isLiveEnvironmentKey())

        val key = handler.initializeStripeSdk(context)
        assertTrue(key.startsWith("pk_test_"))
        assertTrue(handler.isInitialized)
        assertNotNull(PaymentConfiguration.getInstance(context).publishableKey)
    }

    @Test
    fun mainUiCheckoutButton_triggersPaymentHandlerAndOpensPaymentSheetTab() {
        val testKey = "pk_live_51DashboardCheckoutKey8888"
        val handler = PaymentHandler(
            publishableKeyProvider = { testKey }
        )
        val liveVm = GlobalMarketViewModel(
            repository = repository,
            firestoreRepository = null,
            stripeTerminalProcessor = com.example.data.StripeTerminalPaymentProcessor(
                publishableKeyProvider = { testKey }
            )
        )

        composeTestRule.setContent {
            val isCheckoutVisible = liveVm.isCheckoutVisible.collectAsStateWithLifecycle().value
            MyApplicationTheme {
                GlobalMarketDashboard(
                    viewModel = liveVm,
                    onCheckoutClick = {
                        handler.launchCheckout(
                            context = context,
                            viewModel = liveVm,
                            paymentSheet = null
                        )
                    }
                )
                if (isCheckoutVisible) {
                    StripeTerminalCheckoutModal(
                        viewModel = liveVm,
                        onDismiss = { liveVm.closeCheckout() }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("main_checkout_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Checkout").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main_checkout_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(handler.isInitialized)
        assertEquals(testKey, handler.activePublishableKey)
        assertEquals(testKey, PaymentConfiguration.getInstance(context).publishableKey)
        assertTrue(liveVm.isCheckoutVisible.value)
        assertTrue(liveVm.openCheckoutToPaymentSheet.value)
        composeTestRule.onNodeWithTag("paymentsheet_config_section").assertIsDisplayed()
    }
}
