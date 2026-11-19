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
import com.example.data.StripeCardBrand
import com.example.data.StripeCardValidator
import com.example.data.StripeSdkPreparationOutcome
import com.example.data.StripeTerminalExecutionStep
import com.example.data.StripeTerminalPaymentMode
import com.example.data.StripeTerminalPaymentProcessor
import com.example.data.StripeTerminalPaymentRequest
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.StripeTerminalCheckoutModal
import com.example.ui.StripeTerminalCheckoutState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StripeTerminalPaymentFlowTest {

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
            firestoreRepository = null,
            stripeTerminalProcessor = StripeTerminalPaymentProcessor(
                publishableKeyProvider = { "pk_test_robolectric_terminal_key" }
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun cardValidator_enforcesLuhnBrandExpiryAndCvcRules() {
        assertTrue(StripeCardValidator.isValidLuhn("4242424242424242"))
        assertFalse(StripeCardValidator.isValidLuhn("4242424242424243"))
        assertEquals(StripeCardBrand.VISA, StripeCardValidator.detectBrand("4242424242424242"))
        assertEquals(StripeCardBrand.AMEX, StripeCardValidator.detectBrand("378282246310005"))

        val amexWithShortCvc = StripeCardValidator.validateRequest(
            StripeTerminalPaymentRequest(
                amountUsd = 50.0,
                targetCurrencyCode = "USD",
                exchangeRate = 1.0,
                localizedAmountMajor = 50.0,
                paymentMode = StripeTerminalPaymentMode.CARD,
                railId = "cards",
                railName = "Stripe SDK Card",
                customerName = "Alice Tester",
                countryName = "United States",
                cardNumber = "378282246310005",
                expiryMmYy = "12/29",
                cvc = "123" // Amex requires 4 digits
            )
        )
        assertFalse(amexWithShortCvc.isValid)
        assertNotNull(amexWithShortCvc.cvcError)

        val expiredCard = StripeCardValidator.validateRequest(
            StripeTerminalPaymentRequest(
                amountUsd = 50.0,
                targetCurrencyCode = "EUR",
                exchangeRate = 0.92,
                localizedAmountMajor = 46.0,
                paymentMode = StripeTerminalPaymentMode.CARD,
                railId = "cards",
                railName = "Stripe SDK Card",
                customerName = "Alice Tester",
                countryName = "Germany",
                cardNumber = "4242424242424242",
                expiryMmYy = "01/20",
                cvc = "424"
            )
        )
        assertFalse(expiredCard.isValid)
        assertNotNull(expiredCard.expiryError)
    }

    @Test
    fun stripeSdkProcessor_initializesPaymentConfigurationAndSupportsZeroDecimalJpyAndIdeal() {
        val processor = StripeTerminalPaymentProcessor(
            publishableKeyProvider = { "pk_test_51GlobalMarketProTestKey" }
        )
        val initKey = processor.ensurePaymentConfigurationInitialized(context)
        assertEquals("pk_test_51GlobalMarketProTestKey", initKey)
        assertEquals("pk_test_51GlobalMarketProTestKey", PaymentConfiguration.getInstance(context).publishableKey)

        // Zero-decimal JPY localized card simulation
        val jpyRequest = StripeTerminalPaymentRequest(
            amountUsd = 50.0,
            targetCurrencyCode = "JPY",
            exchangeRate = 152.35,
            localizedAmountMajor = 7617.5,
            paymentMode = StripeTerminalPaymentMode.CARD,
            railId = "cards",
            railName = "Stripe SDK Card",
            customerName = "Kenji Takahashi",
            countryName = "Japan",
            cardNumber = "4242424242424242",
            expiryMmYy = "12/29",
            cvc = "424"
        )
        val jpyPrep = processor.prepareStripeSdkParams(context, jpyRequest)
        assertTrue(jpyPrep is StripeSdkPreparationOutcome.Ready)
        val jpyReady = jpyPrep as StripeSdkPreparationOutcome.Ready
        assertEquals(PaymentMethod.Type.Card.code, jpyReady.paymentMethodCreateParams.typeCode)

        val jpyExec = processor.executeLocalizedSimulation(jpyRequest, jpyReady)
        assertTrue(jpyExec is StripeTerminalExecutionStep.Settled)
        val jpyReceipt = (jpyExec as StripeTerminalExecutionStep.Settled).receipt
        assertEquals("JPY", jpyReceipt.currencyCode)
        assertEquals(7618L, jpyReceipt.minorUnits)
        assertEquals("JP", jpyReceipt.countryIso2)

        // Localized iDEAL rail in EUR (triggers SCA redirect challenge)
        val idealRequest = StripeTerminalPaymentRequest(
            amountUsd = 100.0,
            targetCurrencyCode = "EUR",
            exchangeRate = 0.92,
            localizedAmountMajor = 92.0,
            paymentMode = StripeTerminalPaymentMode.LOCAL_RAIL,
            railId = "ideal",
            railName = "iDEAL",
            customerName = "J.P. Westerveld",
            countryName = "Netherlands",
            idealBankCode = "ing"
        )
        val idealPrep = processor.prepareStripeSdkParams(context, idealRequest)
        assertTrue(idealPrep is StripeSdkPreparationOutcome.Ready)
        val idealReady = idealPrep as StripeSdkPreparationOutcome.Ready
        assertEquals(PaymentMethod.Type.Ideal.code, idealReady.paymentMethodCreateParams.typeCode)
        assertEquals("ideal", idealReady.stripeMethodCode)

        val idealExec = processor.executeLocalizedSimulation(idealRequest, idealReady)
        assertTrue(idealExec is StripeTerminalExecutionStep.Requires3dsAuthentication)
    }

    private suspend fun awaitCheckoutTerminalState(
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
    fun frictionlessSale_settlesAndPersistsPciSafeReceiptToAllRoomTables() {
        runBlocking {
            val rawPan = "4242424242424242"
            val rawCvc = "424"
            viewModel.cardNumber.value = rawPan
            viewModel.expiryDate.value = "12/29"
            viewModel.cvv.value = rawCvc
            viewModel.cardHolderName.value = "Elena Rostova"

            viewModel.startStripeTerminalPayment(
                context = context,
                request = StripeTerminalPaymentRequest(
                    amountUsd = 100.0,
                    targetCurrencyCode = "EUR",
                    exchangeRate = 0.92,
                    localizedAmountMajor = 92.0,
                    paymentMode = StripeTerminalPaymentMode.CARD,
                    railId = "sepa_instant",
                    railName = "Stripe SDK Card",
                    customerName = "Elena Rostova",
                    countryName = "Germany",
                    cardNumber = rawPan,
                    expiryMmYy = "12/29",
                    cvc = rawCvc
                ),
                simulateNetworkDelayMs = 0L
            )
            val state = awaitCheckoutTerminalState {
                it is StripeTerminalCheckoutState.Succeeded || it is StripeTerminalCheckoutState.Failed
            }
            assertTrue("Expected Succeeded but was $state", state is StripeTerminalCheckoutState.Succeeded)
            val receipt = (state as StripeTerminalCheckoutState.Succeeded).receipt
            assertEquals("EUR", receipt.currencyCode)
            assertEquals(9200L, receipt.minorUnits)
            assertEquals("4242", receipt.last4)

            // Transient raw card inputs must be wiped from ViewModel
            assertEquals("", viewModel.cardNumber.value)
            assertEquals("", viewModel.cvv.value)

            // Verify Room ledger persistence & zero PAN/CVC leakage
            val txList = db.transactionDao().getAllTransactions().first()
            val logs = db.transactionLogDao().getAllLogs().first()
            val sales = db.salesTransactionDao().getAllSalesTransactions().first()
            val terminalLogs = db.salesTerminalDao().getAllTerminalLogs().first()

            assertEquals(1, txList.size)
            assertEquals(1, logs.size)
            assertEquals(1, sales.size)
            assertEquals(1, terminalLogs.size)

            assertEquals(receipt.paymentIntentId, txList.first().stripeSubscriptionId)
            assertEquals(receipt.paymentIntentId, sales.first().transactionReference)
            assertEquals("POS-TERM-STRIPE", terminalLogs.first().terminalId)

            val combinedPersistedText = listOf(
                txList.first().toString(),
                logs.first().toString(),
                sales.first().toString(),
                terminalLogs.first().toString()
            ).joinToString(" ")
            assertFalse("Raw PAN must never be persisted", combinedPersistedText.contains(rawPan))
            assertTrue("Masked last4 should be preserved", combinedPersistedText.contains("4242"))
        }
    }

    @Test
    fun threeDsChallenge_handlesApprovalAndCancellationSafely() {
        runBlocking {
            // 1. Trigger 3DS challenge with official Luhn-valid Stripe 3DS test card
            viewModel.startStripeTerminalPayment(
                context = context,
                request = StripeTerminalPaymentRequest(
                    amountUsd = 75.0,
                    targetCurrencyCode = "GBP",
                    exchangeRate = 0.79,
                    localizedAmountMajor = 59.25,
                    paymentMode = StripeTerminalPaymentMode.CARD,
                    railId = "faster_payments",
                    railName = "Stripe SDK Card",
                    customerName = "Arthur Pendelton",
                    countryName = "United Kingdom",
                    cardNumber = "4000000000003220",
                    expiryMmYy = "12/29",
                    cvc = "424"
                ),
                simulateNetworkDelayMs = 0L
            )
            val challengeState = awaitCheckoutTerminalState {
                it is StripeTerminalCheckoutState.Requires3dsAction || it is StripeTerminalCheckoutState.Failed
            }
            assertTrue("Expected Requires3dsAction but was $challengeState", challengeState is StripeTerminalCheckoutState.Requires3dsAction)
            assertEquals(0, db.salesTransactionDao().getAllSalesTransactions().first().size)

            // 2. Cancel first -> must transition to Failed without recording a sale
            viewModel.cancelOrFailSimulated3dsChallenge(cancelledByUser = true)
            val canceledState = viewModel.stripeTerminalCheckoutState.value
            assertTrue(canceledState is StripeTerminalCheckoutState.Failed)
            assertEquals("authentication_canceled", (canceledState as StripeTerminalCheckoutState.Failed).errorCode)
            assertEquals(0, db.salesTransactionDao().getAllSalesTransactions().first().size)

            // 3. Re-trigger & Approve 3DS -> must settle and persist
            viewModel.startStripeTerminalPayment(
                context = context,
                request = StripeTerminalPaymentRequest(
                    amountUsd = 75.0,
                    targetCurrencyCode = "GBP",
                    exchangeRate = 0.79,
                    localizedAmountMajor = 59.25,
                    paymentMode = StripeTerminalPaymentMode.CARD,
                    railId = "faster_payments",
                    railName = "Stripe SDK Card",
                    customerName = "Arthur Pendelton",
                    countryName = "United Kingdom",
                    cardNumber = "4000000000003220",
                    expiryMmYy = "12/29",
                    cvc = "424"
                ),
                simulateNetworkDelayMs = 0L
            )
            val challengeState2 = awaitCheckoutTerminalState {
                it is StripeTerminalCheckoutState.Requires3dsAction || it is StripeTerminalCheckoutState.Failed
            }
            assertTrue("Expected Requires3dsAction but was $challengeState2", challengeState2 is StripeTerminalCheckoutState.Requires3dsAction)

            viewModel.completeSimulated3dsChallenge(simulateNetworkDelayMs = 0L)
            val settled = awaitCheckoutTerminalState {
                it is StripeTerminalCheckoutState.Succeeded || it is StripeTerminalCheckoutState.Failed
            }
            assertTrue("Expected Succeeded but was $settled", settled is StripeTerminalCheckoutState.Succeeded)
            assertEquals("3DS2 / SCA Authenticated", (settled as StripeTerminalCheckoutState.Succeeded).receipt.scaStatus)
            assertEquals(1, db.salesTransactionDao().getAllSalesTransactions().first().size)
        }
    }

    @Test
    fun stripeDeclineTestCard_returnsDeclinedWithoutPersistingSale() {
        runBlocking {
            viewModel.startStripeTerminalPayment(
                context = context,
                request = StripeTerminalPaymentRequest(
                    amountUsd = 49.99,
                    targetCurrencyCode = "EUR",
                    exchangeRate = 0.92,
                    localizedAmountMajor = 45.99,
                    paymentMode = StripeTerminalPaymentMode.CARD,
                    railId = "cards",
                    railName = "Stripe SDK Card",
                    customerName = "Test Buyer",
                    countryName = "France",
                    cardNumber = "4000000000000002",
                    expiryMmYy = "12/29",
                    cvc = "424"
                ),
                simulateNetworkDelayMs = 0L
            )
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            val state = viewModel.stripeTerminalCheckoutState.value
            assertTrue(state is StripeTerminalCheckoutState.Failed)
            assertEquals("card_declined", (state as StripeTerminalCheckoutState.Failed).errorCode)
            assertEquals(0, db.transactionDao().getAllTransactions().first().size)
        }
    }

    @Test
    fun stripeTerminalCheckoutModal_uiFlowSupports3dsPresetAndSettlement() {
        composeTestRule.setContent {
            MyApplicationTheme {
                StripeTerminalCheckoutModal(
                    viewModel = viewModel,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("stripe_terminal_checkout_modal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_terminal_preset_3ds").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("4000000000003220", viewModel.cardNumber.value)

        composeTestRule.onNodeWithTag("checkout_pay_button").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
            val s = viewModel.stripeTerminalCheckoutState.value
            s is StripeTerminalCheckoutState.Requires3dsAction || s is StripeTerminalCheckoutState.Failed
        }
        assertTrue(
            "Expected Requires3dsAction but was ${viewModel.stripeTerminalCheckoutState.value}",
            viewModel.stripeTerminalCheckoutState.value is StripeTerminalCheckoutState.Requires3dsAction
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("stripe_terminal_3ds_challenge_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_terminal_3ds_approve_button").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
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
}
