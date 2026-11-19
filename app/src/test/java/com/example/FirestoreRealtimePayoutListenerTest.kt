package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FirestoreCloudTransactionLog
import com.example.data.FirestoreOwnerPayoutRecord
import com.example.data.FirestorePayoutSetting
import com.example.data.FirestoreRepository
import com.example.data.FirestoreUserProfile
import com.example.data.TransactionRepository
import com.example.ui.FirestoreLivePayoutStatusPanel
import com.example.ui.FirestoreLiveSyncBadgeStrip
import com.example.ui.FirestorePayoutStreamState
import com.example.ui.GlobalMarketViewModel
import com.example.ui.OwnerPayoutSectionView
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CurrencyFormatter
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FirestoreRealtimePayoutListenerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var fakeFirestoreRepo: InMemoryFakeFirestoreRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.globalmarketpro.mfxvqz")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-test-api-key")
                    .build()
            )
        } else {
            FirebaseApp.getInstance()
        }
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        val dbId = if (resId != 0) context.getString(resId) else "test-db"
        val firestore = FirebaseFirestore.getInstance(app, dbId)
        val auth = FirebaseAuth.getInstance(app)

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
        fakeFirestoreRepo = InMemoryFakeFirestoreRepository(firestore, auth)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun firestoreListener_syncsStatusTransitionsIntoUiAndRoom() {
        runBlocking {
            val viewModel = GlobalMarketViewModel(
                repository = repository,
                firestoreRepository = fakeFirestoreRepo
            )

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutSectionView(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("firestore_live_payout_panel").assertIsDisplayed()

        // Emit initial snapshot (PROCESSING)
        val initialPayout = FirestoreOwnerPayoutRecord(
            docId = "po_live_001",
            userId = "owner_uid_1",
            payoutId = "po_live_001",
            method = "STRIPE_PAYOUT",
            amount = 420.0,
            fee = 0.0,
            netAmount = 420.0,
            currency = "USD",
            recipientName = "J.P. Westerveld",
            destinationSummary = "acct_live_1234",
            reference = "po_live_001",
            status = "PROCESSING",
            createdAt = Timestamp(Date()),
            updatedAt = Timestamp(Date())
        )
        fakeFirestoreRepo.emitPayouts(listOf(initialPayout))

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("LIVE").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("PROCESSING").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Room synced the externally emitted Firestore payout record
        val storedBefore = db.payoutWireDao().getPayoutWireByReference("po_live_001").first()
        assertEquals("PROCESSING", storedBefore?.status)

        // Emit status transition in Firestore: PROCESSING -> SETTLED
        fakeFirestoreRepo.emitPayouts(
            listOf(initialPayout.copy(status = "SETTLED", updatedAt = Timestamp(Date())))
        )

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("SETTLED").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("firestore_latest_transition_banner")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Live status update: po_live_001: PROCESSING → SETTLED")
            .assertIsDisplayed()

        val storedAfter = db.payoutWireDao().getPayoutWireByReference("po_live_001").first()
        assertEquals("SETTLED", storedAfter?.status)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun firestoreListener_unwrapsTwoTierErrorAndRecoversOnRetry() {
        runBlocking {
            fakeFirestoreRepo.failNextSubscription = CancellationException(
            "Snapshot listener terminated",
            FirebaseFirestoreException(
                "PERMISSION_DENIED: Missing or insufficient permissions",
                FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        )

        val viewModel = GlobalMarketViewModel(
            repository = repository,
            firestoreRepository = fakeFirestoreRepo
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutSectionView(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            viewModel.firestorePayoutStreamState.value is FirestorePayoutStreamState.Error
        }

        composeTestRule.onNodeWithTag("firestore_payout_retry_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        fakeFirestoreRepo.emitPayouts(
            listOf(
                FirestoreOwnerPayoutRecord(
                    docId = "po_recovered_002",
                    userId = "owner_uid_1",
                    payoutId = "po_recovered_002",
                    method = "BANK_WIRE",
                    amount = 150.0,
                    fee = 15.0,
                    netAmount = 135.0,
                    currency = "USD",
                    recipientName = "Merchant Owner",
                    destinationSummary = "NL43INGB0006372146",
                    reference = "po_recovered_002",
                    status = "IN_TRANSIT",
                    createdAt = Timestamp(Date()),
                    updatedAt = Timestamp(Date())
                )
            )
        )

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("IN_TRANSIT").fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(2, fakeFirestoreRepo.observeCallCount)
        assertTrue(viewModel.firestorePayoutStreamState.value is FirestorePayoutStreamState.Live)
        composeTestRule.onNodeWithTag("firestore_live_payout_row_po_recovered_002")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("IN_TRANSIT").assertIsDisplayed()
        }
    }

    @Test
    fun updatePayoutWireStatus_propagatesToFirestoreRepository() {
        runBlocking {
            val viewModel = GlobalMarketViewModel(
                repository = repository,
                firestoreRepository = fakeFirestoreRepo
            )

            viewModel.saveDisbursePayout(
                method = com.example.ui.PayoutMethodType.BANK_WIRE,
                amountInUsd = 500.0,
                currencyCode = "USD",
                notes = "Sync back test"
            )

            val inserted = db.payoutWireDao().getAllPayoutWireHistory().first().first()
            viewModel.updatePayoutWireStatus(inserted.id, "SETTLED")

            val deadline = System.currentTimeMillis() + 3000L
            while (fakeFirestoreRepo.statusUpdates.isEmpty() && System.currentTimeMillis() < deadline) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(20)
            }
            assertEquals(inserted.referenceCode to "SETTLED", fakeFirestoreRepo.statusUpdates.firstOrNull())
        }
    }

    @Test
    fun liveStatusPanelAndSyncStrip_renderAllStatesProperly() {
        var retried = false
        val samplePayouts = listOf(
            FirestoreOwnerPayoutRecord(
                payoutId = "po_1",
                method = "STRIPE_PAYOUT",
                netAmount = 200.0,
                currency = "USD",
                status = "PAID"
            ),
            FirestoreOwnerPayoutRecord(
                payoutId = "po_2",
                method = "BANK_WIRE",
                netAmount = 300.0,
                currency = "EUR",
                status = "IN_TRANSIT"
            ),
            FirestoreOwnerPayoutRecord(
                payoutId = "po_3",
                method = "PAYPAL",
                netAmount = 50.0,
                currency = "USD",
                status = "FAILED"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column {
                    FirestoreLiveSyncBadgeStrip(
                        streamState = FirestorePayoutStreamState.Live(
                            payouts = samplePayouts,
                            lastEventEpochMs = System.currentTimeMillis(),
                            latestStatusChange = "po_1: PROCESSING → PAID"
                        ),
                        onRetry = {}
                    )
                    FirestoreLivePayoutStatusPanel(
                        streamState = FirestorePayoutStreamState.Live(
                            payouts = samplePayouts,
                            lastEventEpochMs = System.currentTimeMillis(),
                            latestStatusChange = "po_1: PROCESSING → PAID"
                        ),
                        currencyFormatter = CurrencyFormatter.DEFAULT,
                        onRetry = { retried = true }
                    )
                    FirestoreLiveSyncBadgeStrip(
                        streamState = FirestorePayoutStreamState.Error("Offline timeout"),
                        onRetry = { retried = true }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Firestore Live • Updated po_1: PROCESSING → PAID").assertIsDisplayed()
        composeTestRule.onNodeWithTag("firestore_latest_transition_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("firestore_live_payout_row_po_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("firestore_live_payout_row_po_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("firestore_live_payout_row_po_3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("firestore_strip_retry_button")
            .assertTextContains("Retry")
            .performClick()
        assertTrue(retried)
    }

    private class InMemoryFakeFirestoreRepository(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ) : FirestoreRepository(db, auth) {
        private val payoutsStream = MutableSharedFlow<List<FirestoreOwnerPayoutRecord>>(replay = 1)
        var failNextSubscription: Throwable? = null
        val statusUpdates = mutableListOf<Pair<String, String>>()
        var observeCallCount = 0

        suspend fun emitPayouts(records: List<FirestoreOwnerPayoutRecord>) {
            payoutsStream.emit(records)
        }

        override fun requireUserId(): String = "owner_uid_1"

        override suspend fun getUserProfile(): Result<FirestoreUserProfile?> = Result.success(null)

        override suspend fun saveUserProfile(profile: FirestoreUserProfile): Result<Unit> = Result.success(Unit)

        override suspend fun getPayoutSetting(): Result<FirestorePayoutSetting?> = Result.success(null)

        override suspend fun savePayoutSetting(setting: FirestorePayoutSetting): Result<Unit> = Result.success(Unit)

        override suspend fun recordOwnerPayout(payout: FirestoreOwnerPayoutRecord): Result<Unit> = Result.success(Unit)

        override suspend fun updateOwnerPayoutStatus(payoutId: String, newStatus: String): Result<Unit> {
            statusUpdates += payoutId to newStatus
            return Result.success(Unit)
        }

        override fun observeOwnerPayouts(): Flow<List<FirestoreOwnerPayoutRecord>> {
            observeCallCount += 1
            val errorToThrow = failNextSubscription
            if (errorToThrow != null) {
                failNextSubscription = null
                return flow { throw errorToThrow }
            }
            return payoutsStream
        }

        override suspend fun recordTransactionLog(log: FirestoreCloudTransactionLog): Result<Unit> = Result.success(Unit)

        override fun observeTransactionLogs(): Flow<List<FirestoreCloudTransactionLog>> = flowOf(emptyList())
    }
}
