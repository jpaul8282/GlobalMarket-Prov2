package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.StripeAccountDto
import com.example.data.StripeBalanceResponseDto
import com.example.data.StripeBusinessProfileDto
import com.example.data.StripeConnectionHealth
import com.example.data.StripeCurrencyBalanceDto
import com.example.data.StripeExternalAccount
import com.example.data.StripeExternalAccountDto
import com.example.data.StripeExternalAccountListDto
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
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
import retrofit2.HttpException
import retrofit2.Response

private class FakeStripePayoutApiService(
    var simulateAccountForbiddenForRestrictedKey: Boolean = false
) : StripePayoutApiService {
    var lastAuthHeader: String? = null
    var lastStripeAccountHeader: String? = null
    var lastCreatedAmountMinor: Long? = null
    var lastCreatedCurrency: String? = null
    var lastCreatedMethod: String? = null
    var lastCreatedDestination: String? = null

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

    override suspend fun getAccount(
        authorization: String,
        stripeAccount: String?
    ): StripeAccountDto {
        lastAuthHeader = authorization
        lastStripeAccountHeader = stripeAccount
        if (simulateAccountForbiddenForRestrictedKey) {
            throw HttpException(
                Response.error<StripeAccountDto>(
                    403,
                    """{"error":{"code":"permission_denied","message":"Restricted key lacks account read"}}"""
                        .toResponseBody("application/json".toMediaType())
                )
            )
        }
        return StripeAccountDto(
            id = stripeAccount ?: "acct_1GlobalMarketTest01",
            country = "NL",
            defaultCurrency = "eur",
            payoutsEnabled = true,
            chargesEnabled = true,
            detailsSubmitted = true,
            type = "standard",
            businessProfile = StripeBusinessProfileDto(name = "GlobalMarket Pro B.V."),
            externalAccounts = StripeExternalAccountListDto(
                data = listOf(
                    StripeExternalAccountDto(
                        id = "ba_1N8vING001",
                        objectType = "bank_account",
                        bankName = "ING Bank N.V.",
                        country = "NL",
                        currency = "eur",
                        last4 = "4821",
                        status = "verified",
                        defaultForCurrency = true
                    ),
                    StripeExternalAccountDto(
                        id = "card_1DebitInstant02",
                        objectType = "card",
                        brand = "Visa Debit",
                        country = "NL",
                        currency = "eur",
                        last4 = "9012",
                        status = "verified",
                        defaultForCurrency = false
                    )
                )
            )
        )
    }

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
        destination: String?,
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
        lastCreatedDestination = destination
        val created = StripePayoutDto(
            id = "po_test_live_${storedRemotePayouts.size + 1}",
            amountMinor = amountMinor,
            currency = currencyLowercase,
            arrivalDateEpochSec = 1763337600L,
            createdEpochSec = 1763251200L,
            description = description,
            destination = destination ?: "ba_1N8vING001",
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
            publishableKeyProvider = { "MY_STRIPE_PUBLISHABLE_KEY" },
            secretKeyProvider = { "MY_STRIPE_SECRET_KEY" },
            connectedAccountProvider = { "MY_STRIPE_CONNECTED_ACCOUNT_ID" }
        )
        assertFalse(gateway.isConfigured())
        assertFalse(gateway.isPublishableKeyConfigured())
        assertEquals("API KEY REQUIRED", gateway.environmentLabel())

        val result = gateway.createPayout(100.0, "USD")
        assertTrue(result is StripePayoutApiResult.MissingApiKey)
    }

    @Test
    fun `publishable key configured separately succeeds and pk key in secret slot triggers clear guard`() = runBlocking {
        val gateway = StripePayoutGateway(
            publishableKeyProvider = { "pk_test_51PublishableExampleKey9999" },
            secretKeyProvider = { "pk_test_51WrongSlotKey1234" }
        )
        assertTrue(gateway.isPublishableKeyConfigured())
        assertTrue(gateway.isPublishableKeyMistakenForSecret())
        assertFalse(gateway.isConfigured())
        assertEquals("SECRET KEY INVALID", gateway.environmentLabel())
        assertEquals("pk_test_••••9999", gateway.maskedPublishableKeyHint())

        val check = gateway.verifyConnection()
        assertTrue(check is StripePayoutApiResult.ApiError)
        assertEquals("publishable_key_used_as_secret", (check as StripePayoutApiResult.ApiError).stripeErrorCode)
    }

    @Test
    fun `minor unit conversion supports standard and zero decimal currencies`() {
        assertEquals(25050L, StripePayoutGateway.toMinorUnits(250.50, "USD"))
        assertEquals(250.50, StripePayoutGateway.toMajorAmount(25050L, "USD"), 0.001)
        assertEquals(15520L, StripePayoutGateway.toMinorUnits(15520.0, "JPY"))
        assertEquals(15520.0, StripePayoutGateway.toMajorAmount(15520L, "JPY"), 0.001)
    }

    @Test
    fun `viewModel verifies stripe connection, discovers destinations, executes payout to selected bank, cancels pending, and persists to Room`() = runBlocking {
        val fakeApi = FakeStripePayoutApiService()
        val gateway = StripePayoutGateway(
            apiProvider = { fakeApi },
            secretKeyProvider = { "sk_test_51ExampleKeyForTest9999" },
            connectedAccountProvider = { "acct_1ConnectTest99" },
            lastRequestIdProvider = { "req_test_verify_01" }
        )
        val viewModel = GlobalMarketViewModel(
            repository = repository,
            stripePayoutGateway = gateway
        )

        assertTrue(viewModel.stripePayoutState.value.isKeyConfigured)
        assertEquals("TEST MODE", viewModel.stripePayoutState.value.environmentLabel)
        assertEquals("acct_1ConnectTest99", viewModel.stripePayoutState.value.connectedAccountId)

        // 1. Verify Stripe Connection & Balance + External Accounts
        val synced = viewModel.verifyStripeConnection()
        assertTrue(synced)
        assertEquals(StripeConnectionHealth.CONNECTED, viewModel.stripePayoutState.value.connectionHealth)
        assertEquals("GlobalMarket Pro B.V.", viewModel.stripePayoutState.value.accountBusinessName)
        assertEquals("NL", viewModel.stripePayoutState.value.accountCountry)
        assertTrue(viewModel.stripePayoutState.value.payoutsEnabled)
        assertEquals(2, viewModel.stripePayoutState.value.externalAccounts.size)
        assertEquals("ba_1N8vING001", viewModel.stripePayoutState.value.selectedDestinationId)
        assertEquals("req_test_verify_01", viewModel.stripePayoutState.value.lastRequestId)
        assertNotNull(viewModel.stripePayoutState.value.lastLatencyMs)
        assertEquals("Bearer sk_test_51ExampleKeyForTest9999", fakeApi.lastAuthHeader)
        assertEquals("acct_1ConnectTest99", fakeApi.lastStripeAccountHeader)
        assertEquals(4500.00, viewModel.stripePayoutState.value.availableBalances.first().majorAmount, 0.01)

        // Verify synced payout stored in Room
        val syncedRoom = repository.getPayoutWireByReference("po_remote_existing_01").first()
        assertEquals("SETTLED", syncedRoom?.status)
        assertEquals("STRIPE_PAYOUT", syncedRoom?.method)

        // 2. Select debit card destination & execute live instant Stripe payout in EUR
        viewModel.selectStripeDestination("card_1DebitInstant02")
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
        assertEquals("card_1DebitInstant02", fakeApi.lastCreatedDestination)
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
    fun `restricted key falling back on account endpoint still marks connection RESTRICTED_CONNECTED`() = runBlocking {
        val fakeApi = FakeStripePayoutApiService(simulateAccountForbiddenForRestrictedKey = true)
        val gateway = StripePayoutGateway(
            apiProvider = { fakeApi },
            secretKeyProvider = { "rk_test_51RestrictedKey9999" },
            connectedAccountProvider = { "" },
            lastRequestIdProvider = { "req_rk_02" }
        )
        val viewModel = GlobalMarketViewModel(
            repository = repository,
            stripePayoutGateway = gateway
        )

        val verified = viewModel.verifyStripeConnection()
        assertTrue(verified)
        assertEquals(StripeConnectionHealth.RESTRICTED_CONNECTED, viewModel.stripePayoutState.value.connectionHealth)
        assertTrue(viewModel.stripePayoutState.value.isRestrictedKey)
    }

    @Test
    fun `stripeLivePayoutCard displays missing secret guidance cleanly`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                StripeLivePayoutCard(
                    stripeState = StripePayoutUiState(
                        isKeyConfigured = false,
                        environmentLabel = "API KEY REQUIRED",
                        connectionHealth = StripeConnectionHealth.KEY_REQUIRED,
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
        composeTestRule.onNodeWithTag("stripe_connection_diagnostics_strip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_missing_key_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_verify_connection_button").performClick()
        composeTestRule.onNodeWithTag("stripe_sync_button").performClick()
    }

    @Test
    fun `stripeLivePayoutCard displays verified connection account details and destination pills`() {
        var selectedDest: String? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                StripeLivePayoutCard(
                    stripeState = StripePayoutUiState(
                        isKeyConfigured = true,
                        isLiveMode = false,
                        environmentLabel = "TEST MODE",
                        connectionHealth = StripeConnectionHealth.CONNECTED,
                        maskedKeyPreview = "sk_test_••••9999",
                        accountId = "acct_1GlobalMarketTest01",
                        accountBusinessName = "GlobalMarket Pro B.V.",
                        accountCountry = "NL",
                        accountDefaultCurrency = "EUR",
                        payoutsEnabled = true,
                        lastLatencyMs = 42L,
                        lastRequestId = "req_123456",
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
                    onSelectDestination = { selectedDest = it },
                    onExecuteStripePayout = {},
                    onCancelStripePayout = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("stripe_connection_health_label").assertTextContains("CONNECTED")
        composeTestRule.onNodeWithText("GlobalMarket Pro B.V.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stripe_dest_ba_1N8vING001").performClick()
        assertEquals("ba_1N8vING001", selectedDest)
    }
}
