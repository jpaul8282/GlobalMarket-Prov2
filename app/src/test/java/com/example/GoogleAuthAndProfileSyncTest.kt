package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AuthenticatedUserSummary
import com.example.data.FirestoreCloudTransactionLog
import com.example.data.FirestoreOwnerPayoutRecord
import com.example.data.FirestorePayoutSetting
import com.example.data.FirestoreRepository
import com.example.data.FirestoreUserProfile
import com.example.data.TransactionRepository
import com.example.ui.AuthScreen
import com.example.ui.FirestorePayoutStreamState
import com.example.ui.GlobalMarketViewModel
import com.example.ui.findActivity
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GoogleAuthAndProfileSyncTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var fakeFirestoreRepo: FakeAuthFirestoreRepository

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
        val firestore = FirebaseFirestore.getInstance(app, "test-auth-db")
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
        fakeFirestoreRepo = FakeAuthFirestoreRepository(firestore, auth)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun authScreen_displaysGoogleSignInCardAndButton() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_screen_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun findActivity_unwrapsNestedContextWrappers() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        val wrapped = ContextWrapper(ContextWrapper(activity))
        assertEquals(activity, wrapped.findActivity())
        assertNull(context.findActivity())
    }

    @Test
    fun sanitizeFirestoreId_enforcesRulesRegex() {
        val sanitized = FirestoreRepository.sanitizeFirestoreId("PO#123/456:abc", "log")
        assertTrue(sanitized.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
        val blankFallback = FirestoreRepository.sanitizeFirestoreId("   ///  ", "payout")
        assertTrue(blankFallback.startsWith("payout_"))
        assertTrue(blankFallback.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
    }

    @Test
    fun viewModel_bootstrapsProfileFromGoogleUserAndPersistsProfileUpdates() = runBlocking {
        val viewModel = GlobalMarketViewModel(
            repository = repository,
            firestoreRepository = fakeFirestoreRepo
        )

        val deadline = System.currentTimeMillis() + 3000L
        while (fakeFirestoreRepo.savedProfiles.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }

        assertEquals("google_uid_42", fakeFirestoreRepo.savedProfiles.first().userId)
        assertEquals("Ada Lovelace", viewModel.userProfile.value.name)
        assertEquals("ada@globalmarket.pro", viewModel.userProfile.value.email)

        viewModel.updateProfile("Ada L. Merchant", "ada.merchant@globalmarket.pro", "Analytical Engine Co")

        val updateDeadline = System.currentTimeMillis() + 3000L
        while (fakeFirestoreRepo.savedProfiles.size < 2 && System.currentTimeMillis() < updateDeadline) {
            Thread.sleep(20)
        }

        val latest = fakeFirestoreRepo.savedProfiles.lastOrNull()
        assertNotNull(latest)
        assertEquals("Ada L. Merchant", latest?.fullName)
        assertEquals("Analytical Engine Co", latest?.organization)
    }

    @Test
    fun viewModel_prepareForSignOutDetachesLivePayoutStream() = runBlocking {
        val viewModel = GlobalMarketViewModel(
            repository = repository,
            firestoreRepository = fakeFirestoreRepo
        )

        // Ensure stream can switch to Unavailable cleanly on sign-out prep
        viewModel.prepareForSignOut()
        // Re-verify state starts Unavailable/Connecting or transitions to Unavailable cleanly without exception
        assertTrue(
            viewModel.firestorePayoutStreamState.value is FirestorePayoutStreamState.Unavailable ||
                viewModel.firestorePayoutStreamState.value is FirestorePayoutStreamState.Connecting
        )
    }

    private class FakeAuthFirestoreRepository(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ) : FirestoreRepository(db, auth) {
        val savedProfiles = mutableListOf<FirestoreUserProfile>()
        private val payoutsStream = MutableSharedFlow<List<FirestoreOwnerPayoutRecord>>(replay = 1)

        override fun currentUserSummary(): AuthenticatedUserSummary = AuthenticatedUserSummary(
            uid = "google_uid_42",
            displayName = "Ada Lovelace",
            email = "ada@globalmarket.pro"
        )

        override fun requireUserId(): String = "google_uid_42"

        override suspend fun getUserProfile(): Result<FirestoreUserProfile?> = Result.success(null)

        override suspend fun saveUserProfile(profile: FirestoreUserProfile): Result<Unit> {
            savedProfiles += profile
            return Result.success(Unit)
        }

        override suspend fun getPayoutSetting(): Result<FirestorePayoutSetting?> = Result.success(null)

        override suspend fun savePayoutSetting(setting: FirestorePayoutSetting): Result<Unit> = Result.success(Unit)

        override suspend fun recordOwnerPayout(payout: FirestoreOwnerPayoutRecord): Result<Unit> = Result.success(Unit)

        override suspend fun updateOwnerPayoutStatus(payoutId: String, newStatus: String): Result<Unit> = Result.success(Unit)

        override fun observeOwnerPayouts(): Flow<List<FirestoreOwnerPayoutRecord>> = payoutsStream

        override suspend fun recordTransactionLog(log: FirestoreCloudTransactionLog): Result<Unit> = Result.success(Unit)

        override fun observeTransactionLogs(): Flow<List<FirestoreCloudTransactionLog>> = flowOf(emptyList())
    }
}
