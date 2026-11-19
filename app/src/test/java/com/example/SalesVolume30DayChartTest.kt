package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.SalesTransactionEntity
import com.example.data.TransactionEntity
import com.example.ui.SalesVolume30DayChart
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CurrencyFormatter
import com.example.util.SalesVolumeAggregator
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class SalesVolume30DayChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aggregator_builds30DailyBucketsAndExcludesPayoutMirrorsAndOldSales() {
        val now = 1_760_000_000_000L
        val oneDayMs = 86_400_000L

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                amountInUsd = 200.0,
                targetCurrency = "USD",
                convertedAmount = 200.0,
                customerName = "Alice Checkout",
                country = "United States",
                timestamp = now - 2 * oneDayMs,
                stripeSubscriptionId = "sub_001"
            ),
            TransactionEntity(
                id = 2,
                amountInUsd = 100.0,
                targetCurrency = "EUR",
                convertedAmount = 92.0,
                customerName = "Bob Older",
                country = "Germany",
                timestamp = now - 20 * oneDayMs,
                stripeSubscriptionId = "sub_002"
            ),
            // Outside 30-day window -> must be excluded
            TransactionEntity(
                id = 3,
                amountInUsd = 999.0,
                targetCurrency = "USD",
                convertedAmount = 999.0,
                customerName = "Old Archive",
                country = "United States",
                timestamp = now - 35 * oneDayMs,
                stripeSubscriptionId = "sub_old"
            ),
            // Payout mirror -> must be excluded from sales volume
            TransactionEntity(
                id = 4,
                amountInUsd = 500.0,
                targetCurrency = "USD",
                convertedAmount = 500.0,
                customerName = "Payout (Bank Wire Transfer): Owner",
                country = "Netherlands",
                timestamp = now - 1 * oneDayMs,
                stripeSubscriptionId = "po_wire_9911"
            )
        )

        val posSales = listOf(
            SalesTransactionEntity(
                id = 1,
                transactionReference = "POS-101",
                timestamp = now - 2 * oneDayMs,
                customerName = "Walk-in",
                amountInUsd = 150.0,
                totalAmount = 150.0,
                paymentStatus = "COMPLETED",
                terminalId = "POS-TERM-01"
            ),
            // Refunded POS sale -> excluded
            SalesTransactionEntity(
                id = 2,
                transactionReference = "POS-102",
                timestamp = now - 1 * oneDayMs,
                customerName = "Refunded User",
                amountInUsd = 80.0,
                totalAmount = 80.0,
                paymentStatus = "REFUNDED",
                terminalId = "POS-TERM-01"
            ),
            // Payout mirror POS entry -> excluded
            SalesTransactionEntity(
                id = 3,
                transactionReference = "po_live_123",
                timestamp = now - 1 * oneDayMs,
                customerName = "Stripe Payout: Owner",
                amountInUsd = 300.0,
                totalAmount = 300.0,
                paymentStatus = "COMPLETED",
                terminalId = "STRIPE-PAYOUT-API"
            )
        )

        val summary = SalesVolumeAggregator.build30DayVolume(
            transactions = transactions,
            salesTransactions = posSales,
            windowDays = 30,
            nowMs = now
        )

        assertEquals(30, summary.points.size)
        assertEquals(450.0, summary.totalVolume30dUsd, 0.01)
        assertEquals(3, summary.totalOrders30d)
        assertEquals(2, summary.activeDaysCount)
        assertNotNull(summary.peakDay)
        assertEquals(350.0, summary.peakDay?.totalVolumeUsd ?: 0.0, 0.01)
        // Recent 15 days = 350, prior 15 days = 100 -> +250%
        assertEquals(250.0, summary.periodDeltaPercent ?: 0.0, 0.01)
    }

    @Test
    fun salesVolume30DayChart_displaysVolumeAndTogglesAreaBarModes() {
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        val sampleTx = listOf(
            TransactionEntity(
                id = 1,
                amountInUsd = 425.0,
                targetCurrency = "USD",
                convertedAmount = 425.0,
                customerName = "Clara Jensen",
                country = "Denmark",
                timestamp = now - 3 * oneDayMs,
                stripeSubscriptionId = "sub_dk_1"
            ),
            TransactionEntity(
                id = 2,
                amountInUsd = 275.0,
                targetCurrency = "EUR",
                convertedAmount = 253.0,
                customerName = "Marco Rossi",
                country = "Italy",
                timestamp = now,
                stripeSubscriptionId = "sub_it_2"
            )
        )

        var analyticsOpened = false
        composeTestRule.setContent {
            MyApplicationTheme {
                SalesVolume30DayChart(
                    transactions = sampleTx,
                    salesTransactions = emptyList(),
                    currencyFormatter = CurrencyFormatter.DEFAULT,
                    onOpenFullAnalytics = { analyticsOpened = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sales_volume_30d_chart_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sales_volume_30d_total").assertTextContains("$700.00")
        composeTestRule.onNodeWithTag("sales_volume_30d_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sales_volume_selected_tooltip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sales_volume_mode_bar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sales_volume_mode_area").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sales_volume_full_analytics_btn").performClick()
        assertTrue(analyticsOpened)
    }

    @Test
    fun salesVolume30DayChart_emptyStateTriggersSeedCallback() {
        var seeded = false
        composeTestRule.setContent {
            MyApplicationTheme {
                SalesVolume30DayChart(
                    transactions = emptyList(),
                    salesTransactions = emptyList(),
                    currencyFormatter = CurrencyFormatter.DEFAULT,
                    onSeedSampleSales = { seeded = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sales_volume_30d_empty_hint").assertIsDisplayed()
        composeTestRule.onNodeWithText("No sales in the last 30 days yet. Seed sample sales or process a checkout.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("sales_volume_seed_button").performClick()
        assertTrue(seeded)
    }
}
