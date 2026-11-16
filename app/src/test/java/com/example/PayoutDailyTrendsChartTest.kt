package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.PayoutWireEntity
import com.example.ui.PayoutDailyTrendsChart
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CurrencyFormatter
import com.example.util.PayoutTrendAggregator
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PayoutDailyTrendsChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `aggregator groups room payout records into daily buckets and computes peak and average`() {
        val now = 1763294400000L // Fixed UTC anchor
        val dayMs = TimeUnit.DAYS.toMillis(1)

        val records = listOf(
            PayoutWireEntity(
                referenceCode = "PO-1",
                transactionType = "PROCESSED_PAYOUT",
                netAmountUsd = 400.0,
                grossAmountUsd = 415.0,
                isSimulatedWire = false,
                timestamp = now - (2 * dayMs)
            ),
            PayoutWireEntity(
                referenceCode = "SIM-1",
                transactionType = "SIMULATED_BANK_WIRE",
                netAmountUsd = 100.0,
                grossAmountUsd = 100.35,
                isSimulatedWire = true,
                timestamp = now - (2 * dayMs) + 3600_000L
            ),
            PayoutWireEntity(
                referenceCode = "PO-2",
                transactionType = "PROCESSED_PAYOUT",
                netAmountUsd = 200.0,
                grossAmountUsd = 200.0,
                isSimulatedWire = false,
                timestamp = now
            )
        )

        val summary = PayoutTrendAggregator.buildDailyTrends(
            records = records,
            windowDays = 7,
            nowMs = now
        )

        assertEquals(7, summary.points.size)
        assertEquals(2, summary.activeDaysCount)
        assertEquals(500.0, summary.maxDailyNetUsd, 0.01)
        assertNotNull(summary.peakPoint)
        assertEquals(400.0, summary.peakPoint?.processedNetUsd ?: 0.0, 0.01)
        assertEquals(100.0, summary.peakPoint?.simulatedNetUsd ?: 0.0, 0.01)
        assertEquals(2, summary.peakPoint?.transactionCount)
        assertEquals(700.0 / 7.0, summary.averageDailyNetUsd, 0.01)
    }

    @Test
    fun payoutDailyTrendsChart_switchesBarAndLineModesAndTimeframes() {
        val now = System.currentTimeMillis()
        val dayMs = TimeUnit.DAYS.toMillis(1)
        val records = listOf(
            PayoutWireEntity(
                referenceCode = "PO-DAY0",
                transactionType = "PROCESSED_PAYOUT",
                netAmountUsd = 600.0,
                grossAmountUsd = 615.0,
                isSimulatedWire = false,
                timestamp = now
            ),
            PayoutWireEntity(
                referenceCode = "SIM-DAY1",
                transactionType = "SIMULATED_BANK_WIRE",
                netAmountUsd = 250.0,
                grossAmountUsd = 250.35,
                isSimulatedWire = true,
                timestamp = now - dayMs
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                PayoutDailyTrendsChart(
                    records = records,
                    currencyFormatter = CurrencyFormatter.DEFAULT
                )
            }
        }

        composeTestRule.onNodeWithTag("payout_daily_trends_chart_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payout_chart_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payout_chart_selected_tooltip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payout_chart_peak_day").assertTextContains("600.00", substring = true)

        // Switch to Line Chart mode
        composeTestRule.onNodeWithTag("payout_chart_mode_line").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("payout_chart_canvas").assertIsDisplayed()

        // Switch timeframe to 14D and back to Bar mode
        composeTestRule.onNodeWithTag("payout_chart_range_14").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Room Ledger Volume (14D)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("payout_chart_mode_bar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("payout_chart_canvas").assertIsDisplayed()
    }
}
