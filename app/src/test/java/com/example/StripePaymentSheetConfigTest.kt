package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SimulatedPaymentSheetOutcome
import com.example.data.StripeAccountDto
import com.example.data.StripeBalanceResponseDto
import com.example.data.StripePaymentIntentDto
import com.example.data.StripePaymentSheetConfigFactory
import com.example.data.StripePaymentSheetPreparedConfig
import com.example.data.StripePaymentSheetSetupRequest
import com.example.data.StripePayoutApiService
import com.example.data.StripePayoutDto
import com.example.data.StripePayoutGateway
import com.example.data.StripePayoutListResponseDto
import com.example.data.StripeTerminalPaymentProcessor
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.StripeTerminalCheckoutModal
import com.example.ui.StripeTerminalCheckoutState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StripePaymentSheetConfigTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var processor: StripeTerminalPaymentProcessor
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
        processor = StripeTerminalPaymentProcessor(
            publishableKeyProvider = { "pk_test_paymentsheet_config_key" }
        )
        viewModel = GlobalMarketViewModel(
            repository = repository,
            firestoreRepository = null,
            stripeTerminalProcessor = processor
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun paymentSheetConfigFactory_buildsOfficialConfigurationIntentAndBillingDetails() {
        val elena = StripePaymentSheetConfigFactory.defaultCustomers.first { it.id == "cust_eu_elena" }
        val request = StripePaymentSheetSetupRequest(
            merchantDisplayName = "GlobalMarket Pro",
            amountUsd = 100.0,
            currencyCode = "EUR",
            exchangeRate = 0.92,
            localizedAmountMajor = 92.0,
            customerProfile = elena,
            allowsDelayedPaymentMethods = true,
            enableGooglePay = true,
            collectFullAddress = true,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal
        )

        val prepared = StripePaymentSheetConfigFactory.buildPreparedConfig(
            context = context,
            processor = processor,
            request = request
        )

        assertEquals("pk_test_paymentsheet_config_key", PaymentConfiguration.getInstance(context).publishableKey)
        assertEquals("GlobalMarket Pro", prepared.merchantDisplayName)
        assertEquals("cus_sim_eu_elena92", prepared.customerId)
        assertEquals("DE", prepared.countryIso2)
        assertEquals(9200L, prepared.minorUnits)
        assertTrue(prepared.paymentMethodOrder.contains("sepa_debit"))
        assertTrue(prepared.paymentMethodOrder.contains("ideal"))

        // Verify public PaymentSheet.BillingDetails and IntentConfiguration fields
        assertEquals("Elena Rostova", prepared.defaultBillingDetails.name)
        assertEquals("elena.rostova@glaba-sim.eu", prepared.defaultBillingDetails.email)
        assertEquals("DE", prepared.defaultBillingDetails.address?.country)
        assertEquals("Berlin", prepared.defaultBillingDetails.address?.city)

        val intentMode = prepared.intentConfiguration.mode as PaymentSheet.IntentConfiguration.Mode.Payment
        assertEquals(9200L, intentMode.amount)
        assertEquals("eur", intentMode.currency)
        assertTrue(prepared.intentConfiguration.paymentMethodTypes.contains("ideal"))

        // Guest customer + disabled delayed payment methods filters out delayed rails
        val guest = StripePaymentSheetConfigFactory.defaultCustomers.first { it.isGuest }
        val guestPrepared = StripePaymentSheetConfigFactory.buildPreparedConfig(
            context = context,
            processor = processor,
            request = request.copy(
                currencyCode = "EUR",
                customerProfile = guest,
                allowsDelayedPaymentMethods = false,
                enableGooglePay = false
            )
        )
        assertNull(guestPrepared.customerId)
        assertFalse(guestPrepared.googlePayEnabled)
        assertFalse(guestPrepared.paymentMethodOrder.contains("sepa_debit"))
    }

    private suspend fun awaitCheckoutState(
        predicate: (StripeTerminalCheckoutState) -> Boolean
    ): StripeTerminalCheckoutState {
        repeat(100) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            val current = viewModel.stripeTerminalCheckoutState.value
            if (predicate(current)) return current
            kotlinx.coroutines.delay(20)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        return viewModel.stripeTerminalCheckoutState.value
    }

    @Test
    fun nativePaymentSheetResult_handlesCompletedCanceledAndFailedResults() {
        runBlocking {
            viewModel.preparePaymentSheetConfiguration(context)

            // 1. Canceled -> Failed state with no Room writes
            val canceledResult = PaymentSheetResult.Canceled::class.java
                .getDeclaredConstructor()
                .newInstance()
            viewModel.onNativePaymentSheetResult(canceledResult, simulateNetworkDelayMs = 0L)
            val canceledState = awaitCheckoutState { it is StripeTerminalCheckoutState.Failed }
            assertTrue("Expected Failed but was $canceledState", canceledState is StripeTerminalCheckoutState.Failed)
            assertEquals("paymentsheet_canceled", (canceledState as StripeTerminalCheckoutState.Failed).errorCode)
            assertEquals(0, db.salesTransactionDao().getAllSalesTransactions().first().size)

            // 2. Failed -> Failed state with error details and no Room writes
            val failedResult = PaymentSheetResult.Failed::class.java
                .getDeclaredConstructor(Throwable::class.java)
                .newInstance(IllegalStateException("Network timeout"))
            viewModel.resetStripeTerminalCheckout()
            viewModel.onNativePaymentSheetResult(failedResult, simulateNetworkDelayMs = 0L)
            val failedState = awaitCheckoutState { it is StripeTerminalCheckoutState.Failed }
            assertTrue("Expected Failed but was $failedState", failedState is StripeTerminalCheckoutState.Failed)
            assertEquals("paymentsheet_failed", (failedState as StripeTerminalCheckoutState.Failed).errorCode)
            assertEquals(0, db.salesTransactionDao().getAllSalesTransactions().first().size)

            // 3. Completed -> Settled receipt persisted to all Room tables
            val completedResult = PaymentSheetResult.Completed::class.java
                .getDeclaredConstructor()
                .newInstance()
            viewModel.onNativePaymentSheetResult(completedResult, simulateNetworkDelayMs = 0L)
            val completedState = awaitCheckoutState { it is StripeTerminalCheckoutState.Succeeded }
            assertTrue("Expected Succeeded but was $completedState", completedState is StripeTerminalCheckoutState.Succeeded)
            val receipt = (completedState as StripeTerminalCheckoutState.Succeeded).receipt
            assertEquals("PS-SHEET-SIM", receipt.terminalId)
            assertEquals(1, db.transactionDao().getAllTransactions().first().size)
            assertEquals(1, db.salesTransactionDao().getAllSalesTransactions().first().size)
            assertEquals(1, db.salesTerminalDao().getAllTerminalLogs().first().size)
        }
    }

    @Test
    fun simulatedPaymentSheetScaFlow_triggersChallengeAndSettlesOnApproval() = runBlocking {
        viewModel.setPaymentSheetSimulatedOutcome(SimulatedPaymentSheetOutcome.REQUIRES_SCA)
        viewModel.runSimulatedPaymentSheetCheckout(context = context, simulateNetworkDelayMs = 0L)

        repeat(40) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (viewModel.stripeTerminalCheckoutState.value is StripeTerminalCheckoutState.Requires3dsAction) {
                return@repeat
            }
            kotlinx.coroutines.delay(15)
        }
        val scaState = viewModel.stripeTerminalCheckoutState.value
        assertTrue("Expected Requires3dsAction but was $scaState", scaState is StripeTerminalCheckoutState.Requires3dsAction)

        viewModel.completeSimulated3dsChallenge(simulateNetworkDelayMs = 0L)
        repeat(40) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (viewModel.stripeTerminalCheckoutState.value is StripeTerminalCheckoutState.Succeeded) {
                return@repeat
            }
            kotlinx.coroutines.delay(15)
        }
        val settled = viewModel.stripeTerminalCheckoutState.value
        assertTrue("Expected Succeeded but was $settled", settled is StripeTerminalCheckoutState.Succeeded)
        assertEquals(1, db.salesTransactionDao().getAllSalesTransactions().first().size)
    }

    @Test
    fun paymentSheetTabUi_configuresCustomerAndRunsSimulatedCheckout() {
        composeTestRule.setContent {
            MyApplicationTheme {
                StripeTerminalCheckoutModal(
                    viewModel = viewModel,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Switch to PaymentSheet tab
        composeTestRule.onNodeWithTag("stripe_terminal_mode_payment_sheet").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("paymentsheet_config_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("paymentsheet_config_summary_card").performScrollTo().assertIsDisplayed()

        // Select Japanese customer preset (Kenji Takahashi) -> currency switches to JPY
        composeTestRule.onNodeWithTag("paymentsheet_customer_chip_cust_jp_kenji")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("JPY", viewModel.selectedCurrency.value.code)
        val prepared = viewModel.preparedPaymentSheetConfig.value
        assertNotNull(prepared)
        assertEquals("JP", prepared!!.countryIso2)
        assertEquals("cus_sim_jp_kenji88", prepared.customerId)
        assertTrue(prepared.paymentMethodOrder.contains("konbini"))

        // Run simulated PaymentSheet checkout
        composeTestRule.onNodeWithTag("paymentsheet_simulate_checkout_button")
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            val s = viewModel.stripeTerminalCheckoutState.value
            s is StripeTerminalCheckoutState.Succeeded || s is StripeTerminalCheckoutState.Failed
        }
        assertTrue(
            "Expected Succeeded but was ${viewModel.stripeTerminalCheckoutState.value}",
            viewModel.stripeTerminalCheckoutState.value is StripeTerminalCheckoutState.Succeeded
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stripe_terminal_success_panel").assertIsDisplayed()
    }

    @Test
    fun livePaymentSheet_createsPaymentIntentAndTriggersLaunchCallback() {
        runBlocking {
            val fakeApi = object : StripePayoutApiService {
                override suspend fun getAccount(authorization: String, stripeAccount: String?) = StripeAccountDto()
                override suspend fun getBalance(authorization: String, stripeAccount: String?) = StripeBalanceResponseDto()
                override suspend fun listPayouts(authorization: String, limit: Int, stripeAccount: String?) = StripePayoutListResponseDto()
                override suspend fun createPayout(
                    authorization: String,
                    amountMinor: Long,
                    currencyLowercase: String,
                    method: String,
                    destination: String?,
                    description: String?,
                    statementDescriptor: String?,
                    metadataSource: String,
                    idempotencyKey: String?,
                    stripeAccount: String?
                ) = StripePayoutDto()

                override suspend fun cancelPayout(authorization: String, payoutId: String, stripeAccount: String?) = StripePayoutDto()

                override suspend fun createPaymentIntent(
                    authorization: String,
                    amountMinor: Long,
                    currencyLowercase: String,
                    automaticPaymentMethodsEnabled: Boolean,
                    description: String?,
                    receiptEmail: String?,
                    metadataSource: String,
                    idempotencyKey: String?,
                    stripeAccount: String?
                ): StripePaymentIntentDto {
                    return StripePaymentIntentDto(
                        id = "pi_live_pos_001",
                        amountMinor = amountMinor,
                        currency = currencyLowercase,
                        clientSecret = "pi_live_pos_001_secret_xyz999",
                        status = "requires_payment_method",
                        livemode = false
                    )
                }
            }

            val liveProcessor = StripeTerminalPaymentProcessor(
                publishableKeyProvider = { "pk_live_51TestReadyKey9999" }
            )
            val liveGateway = StripePayoutGateway(
                apiProvider = { fakeApi },
                publishableKeyProvider = { "pk_live_51TestReadyKey9999" },
                secretKeyProvider = { "sk_test_51SecretForIntent9999" }
            )
            val liveVm = GlobalMarketViewModel(
                repository = repository,
                firestoreRepository = null,
                stripePayoutGateway = liveGateway,
                stripeTerminalProcessor = liveProcessor
            )

            assertTrue(liveVm.isStripePublishableKeyConfigured())
            assertTrue(liveVm.isStripeLivePaymentIntentReady())
            assertEquals("STRIPE SDK • LIVE KEY ACTIVE", liveVm.getStripeSdkEnvironmentBadge())

            var launchedConfig: StripePaymentSheetPreparedConfig? = null
            var launchedSecret: String? = null

            liveVm.createAndPresentLivePaymentSheet(context) { cfg, secret ->
                launchedConfig = cfg
                launchedSecret = secret
            }

            repeat(50) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                if (launchedSecret != null) return@repeat
                kotlinx.coroutines.delay(15)
            }

            assertEquals("pi_live_pos_001_secret_xyz999", launchedSecret)
            assertNotNull(launchedConfig)
            assertTrue(
                liveVm.stripeTerminalCheckoutState.value is StripeTerminalCheckoutState.ConfirmingPaymentIntent
            )
        }
    }

    @Test
    fun paymentSheetTabUi_showsPublishableKeyStatusAndLiveLaunchButtonWhenConfigured() {
        val fakeApi = object : StripePayoutApiService {
            override suspend fun getAccount(authorization: String, stripeAccount: String?) = StripeAccountDto()
            override suspend fun getBalance(authorization: String, stripeAccount: String?) = StripeBalanceResponseDto()
            override suspend fun listPayouts(authorization: String, limit: Int, stripeAccount: String?) = StripePayoutListResponseDto()
            override suspend fun createPayout(
                authorization: String,
                amountMinor: Long,
                currencyLowercase: String,
                method: String,
                destination: String?,
                description: String?,
                statementDescriptor: String?,
                metadataSource: String,
                idempotencyKey: String?,
                stripeAccount: String?
            ) = StripePayoutDto()
            override suspend fun cancelPayout(authorization: String, payoutId: String, stripeAccount: String?) = StripePayoutDto()
            override suspend fun createPaymentIntent(
                authorization: String,
                amountMinor: Long,
                currencyLowercase: String,
                automaticPaymentMethodsEnabled: Boolean,
                description: String?,
                receiptEmail: String?,
                metadataSource: String,
                idempotencyKey: String?,
                stripeAccount: String?
            ) = StripePaymentIntentDto(
                id = "pi_ui_launch_01",
                amountMinor = amountMinor,
                currency = currencyLowercase,
                clientSecret = "pi_ui_launch_01_secret_ui999",
                status = "requires_payment_method"
            )
        }
        val liveProcessor = StripeTerminalPaymentProcessor(
            publishableKeyProvider = { "pk_live_51TestReadyKey9999" }
        )
        val liveGateway = StripePayoutGateway(
            apiProvider = { fakeApi },
            publishableKeyProvider = { "pk_live_51TestReadyKey9999" },
            secretKeyProvider = { "sk_test_51SecretForIntent9999" }
        )
        val liveVm = GlobalMarketViewModel(
            repository = repository,
            firestoreRepository = null,
            stripePayoutGateway = liveGateway,
            stripeTerminalProcessor = liveProcessor
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                StripeTerminalCheckoutModal(
                    viewModel = liveVm,
                    onDismiss = {},
                    onPresentLivePaymentSheet = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stripe_terminal_mode_payment_sheet").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("paymentsheet_publishable_key_status").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("paymentsheet_launch_native_button").performScrollTo().assertIsDisplayed()
    }
}
