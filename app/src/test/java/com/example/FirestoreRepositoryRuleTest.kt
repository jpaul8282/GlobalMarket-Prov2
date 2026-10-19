package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.FirestoreOwnerPayoutRecord
import com.example.data.FirestorePayoutSetting
import com.example.data.FirestoreRepository
import com.example.data.FirestoreUserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun testAuthenticatedOwnerCanSaveAndRetrieveProfile() = runBlocking {
        val uid = signInTestUser("merchant_owner@example.com")
        val repository = FirestoreRepository(firestore, auth)

        val profile = FirestoreUserProfile(
            userId = uid,
            fullName = "Elena Rostova",
            email = "merchant_owner@example.com",
            role = "OWNER",
            organization = "Global Trade Hub",
            orgId = "GTH-9901",
            country = "United States",
            currency = "USD"
        )

        val saveResult = repository.saveUserProfile(profile)
        assertTrue("Save profile should succeed", saveResult.isSuccess)

        val fetchResult = repository.getUserProfile()
        assertTrue("Fetch profile should succeed", fetchResult.isSuccess)
        val loaded = fetchResult.getOrNull()
        assertNotNull(loaded)
        assertEquals("Elena Rostova", loaded?.fullName)
        assertEquals("OWNER", loaded?.role)
    }

    @Test
    fun testOwnerPayoutConfigurationAndDisbursementPersistence() = runBlocking {
        val uid = signInTestUser("disbursement_owner@example.com")
        val repository = FirestoreRepository(firestore, auth)

        // Save Payout Setting
        val setting = FirestorePayoutSetting(
            userId = uid,
            selectedMethod = "BANK_WIRE",
            bankName = "Deutsche Bank AG",
            accountHolderName = "Elena Rostova",
            accountNumberOrIban = "DE89370400440532013000",
            swiftBic = "DEUTDEDD",
            routingNumber = "37040044"
        )
        val settingResult = repository.savePayoutSetting(setting)
        assertTrue("Save payout setting should succeed", settingResult.isSuccess)

        val loadedSetting = repository.getPayoutSetting().getOrNull()
        assertNotNull(loadedSetting)
        assertEquals("Deutsche Bank AG", loadedSetting?.bankName)
        assertEquals("BANK_WIRE", loadedSetting?.selectedMethod)

        // Record Owner Payout
        val payoutRecord = FirestoreOwnerPayoutRecord(
            userId = uid,
            payoutId = "payout_rule_test_001",
            method = "BANK_WIRE",
            amount = 3500.0,
            fee = 15.0,
            netAmount = 3485.0,
            currency = "USD",
            recipientName = "Elena Rostova",
            destinationSummary = "DE89370400440532013000",
            reference = "SWIFT-2026-X89",
            status = "SETTLED"
        )
        val payoutResult = repository.recordOwnerPayout(payoutRecord)
        assertTrue("Record owner payout should succeed", payoutResult.isSuccess)

        val payouts = repository.observeOwnerPayouts().first()
        assertTrue("Observed payouts should contain the recorded disbursement", payouts.any { it.payoutId == "payout_rule_test_001" })
    }

    @Test
    fun testUnauthenticatedAccessFailsFast() {
        auth.signOut()
        val repository = FirestoreRepository(firestore, auth)
        var exceptionThrown = false
        try {
            repository.requireUserId()
        } catch (e: IllegalStateException) {
            exceptionThrown = true
        }
        assertTrue("Calling requireUserId without authentication should throw IllegalStateException", exceptionThrown)
    }
}
