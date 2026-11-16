package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.StripeBalanceResponseDto
import com.example.data.StripeCurrencyBalanceDto
import com.example.data.StripePayoutApiResult
import com.example.data.StripePayoutApiService
import com.example.data.StripePayoutDto
import com.example.data.StripePayoutGateway
import com.example.data.StripePayoutListResponseDto
import com.example.data.TransactionRepository
import com.example.ui.GlobalMarketViewModel
import com.example.ui.StripeLivePayoutCard
import com.example.ui.StripePayoutUiState
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CurrencyFormatter
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeStripePayoutApiService : StripePayoutApiService {
    var lastAuthHeader: String? = null
    var lastStripeAccountHeader: String? = null
    var lastCreatedAmountMinor: Long? = null
    var lastCreatedCurrency: String? = null
    var lastCreatedMethod: String? = null

    val storedRemotePayouts = mutableListOf(
        StripePayoutDto(
            id = "po_remote_existing_01",
            amountMinor = 12500L,
            currency = "eur",
            arrivalDateEpochSec = 1763251200L,
            createdEpochSec = 1763164800L,
            description = "Synced batch payout",
            destination = "ba_1N8vING001",
            livemode = false,
            method = "standard",
            status = "paid",
            type = "bank_account"
        )
    )

    override suspend fun getBalance(
        authorization: String,
        stripeAccount: String?
    ): StripeBalanceResponseDto {
        lastAuthHeader = authorization
        lastStripeAccountHeader = stripeAccount
        return StripeBalanceResponseDto(
            livemode = false,
            available = listOf(StripeCurrencyBalanceDto(amountMinor = 450000L, currency = "usd")),
            pending = listOf(StripeCurrencyBalanceDto(amountMinor = 32000L, currency = "usd"))
        )
    }

    override suspend fun listPayouts(
        authorization: String,
        limit: Int,
        stripeAccount: String?
    ): StripePayoutListResponseDto {
        lastAuthHeader = authorization
        return StripePayoutListResponseDto(data = storedRemotePayouts.toList(), hasMore = false)
    }

    override suspend fun createPayout(
        authorization: String,
        amountMinor: Long,
        currencyLowercase: String,
        method: String,
        description: String?,
        statementDescriptor: String?,
        metadataSource: String,
        idempotencyKey: String?,
        stripeAccount: String?
    ): StripePayoutDto {
        lastAuthHeader = authorization
        lastStripeAccountHeader = stripeAccount
        lastCreatedAmountMinor = amountMinor
        lastCreatedCurrency = currencyLowercase
        lastCreatedMethod = method
        val created = StripePayoutDto(
            id = "po_test_live_${storedRemotePayouts.size + 1}",
            amountMinor = amountMinor,
            currency = currencyLowercase,
            arrivalDateEpochSec = 1763337600L,
            createdEpochSec = 1763251200L,
            description = description,
            destination = "ba_1N8vING999",
            livemode = false,
            method = method,
            status = "pending",
            type = if (method == "instant") "card" else "bank_account"
        )
        storedRemotePayouts.add(0, created)
        return created
    }

    override suspend fun cancelPayout(
        authorization: String,
        payoutId: String,
        stripeAccount: String?
    ): StripePayoutDto {
        val index = storedRemotePayouts.indexOfFirst { it.id == payoutId }
        val target = storedRemotePayouts[index].copy(status = "canceled")
        storedRemotePayouts[index] = target
        return target
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StripeLivePayoutTest {

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
    fun `missing key returns MissingApiKey cleanly without fake data`() = runBlocking {
        val gateway = StripePayoutGateway(
            secretKeyProvider = { "MY_STRIPE_SECRET_KEY" },
            connectedAccountProvider = { "MY_STRIPE_CONNECTED_ACCOUNT_ID" }
        )
        assertFalse(gateway.isConfigured())
        assertEquals("API KEY REQUIRED", gateway.environmentLabel())

        val result = gateway.createPayout(100.0, "USD")
        assertTrue(result is StripePayoutApiResult.MissingApiKey)
    }

    @Test
    fun `minor unit conversion supports standard and zero decimal currencies`() {
        assertEquals(25050L, StripePayoutGateway.toMinorUnits(250.50, "USD"))
        assertEquals(250.50, StripePayoutGateway.toMajorAmount(25050L, "USD"), 0.001)
        assertEquals(15520L, StripePayoutGateway.toMinorUnits(15520.0, "JPY"))
        assertEquals(15520.0, StripePayoutGateway.toMajorAmount(15520L, "JPY"), 0.001)
    }

    @Test
    fun `viewModel executes live stripe payout, syncs balance, cancels pending, and persists to Room`() = runBlocking {
        val fakeApi = FakeStripePayoutApiService()
        val gateway = StripePayoutGateway(
            apiProvider = { fakeApi },
            secretKeyProvider = { "sk_test_51ExampleKeyForTest9999" },
            connectedAccountProvider = { "acct_1ConnectTest99" }
        )
        val viewModel = GlobalMarketViewModel(
            repository = repository,
            stripePayoutGateway = gateway
        )

        assertTrue(viewModel.stripePayoutState.value.isKeyConfigured)
        assertEquals("TEST MODE", viewModel.stripePayoutState.value.environmentLabel)
        assertEquals("acct_1ConnectTest99", viewModel.stripePayoutState.value.connectedAccountId)

        // 1. Sync Stripe Balance & Payouts
        val synced = viewModel.syncStripeBalanceAndPayouts()
        assertTrue(synced)
        assertEquals("Bearer sk_test_51ExampleKeyForTest9999", fakeApi.lastAuthHeader)
        assertEquals("acct_1ConnectTest99", fakeApi.lastStripeAccountHeader)
        assertEquals(4500.00, viewModel.stripePayoutState.value.availableBalances.first().majorAmount, 0.01)

        // Verify synced payout stored in Room
        val syncedRoom = repository.getPayoutWireByReference("po_remote_existing_01").first()
        assertEquals("SETTLED", syncedRoom?.status)
        assertEquals("STRIPE_PAYOUT", syncedRoom?.method)

        // 2. Execute live instant Stripe payout in EUR
        viewModel.setStripePayoutSpeed("instant")
        val createRes = viewModel.submitLiveStripePayout(
            amountInUsd = 200.0,
            currencyCode = "EUR",
            speedMethod = "instant",
            description = "Live owner EUR payout"
        )
        assertTrue(createRes is StripePayoutApiResult.Success)
        val createdItem = (createRes as StripePayoutApiResult.Success).data
        assertEquals("eur", fakeApi.lastCreatedCurrency)
        assertEquals("instant", fakeApi.lastCreatedMethod)
        assertTrue((fakeApi.lastCreatedAmountMinor ?: 0L) > 0L)

        // Verify created Stripe payout saved into Room
        val savedInRoom = repository.getPayoutWireByReference(createdItem.id).first()
        assertEquals("PROCESSING", savedInRoom?.status)
        assertEquals("PROCESSED_PAYOUT", savedInRoom?.transactionType)
        assertEquals("STRIPE_PAYOUT", savedInRoom?.method)

        // 3. Cancel pending payout and verify Room updated to CANCELED
        val cancelRes = viewModel.cancelStripePayoutById(createdItem.id)
        assertTrue(cancelRes is StripePayoutApiResult.Success)
        val afterCancelInRoom = repository.getPayoutWireByReference(createdItem.id).first()
        assertEquals("CANCELED", afterCancelInRoom?.status)
    }

    @Test
    fun `stripeLivePayoutCard displays missing secret guidance cleanly`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                StripeLivePayoutCard(
                    stripeState = StripePayoutUiState(
                        isKeyConfigured = false,
                        environmentLabel = "API KEY REQUIRED",
                        maskedKeyPreview = "Not configured in Secrets"
                    ),
                    amountInUsd = 250.0,
                    currencyCode = "EUR",
                    currencyFormatter = CurrencyFormatter.DEFAULT,
                    onSelectSpeed = {},
                    onSyncStripe = {},
                    onExecuteStripePayout = {},
                    onCancelStripePayout = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("stripe_live_payout_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_env_badge").assertTextContains("API KEY REQUIRED")
        composeTestRule.onNodeWithTag("stripe_missing_key_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_sync_button").performClick()
    }
}
