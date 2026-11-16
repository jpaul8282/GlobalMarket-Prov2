package com.example.util

import com.example.data.PayoutWireEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyPayoutTrendPoint(
    val dayKey: String,
    val shortDayLabel: String,
    val fullDateLabel: String,
    val processedNetUsd: Double,
    val simulatedNetUsd: Double,
    val totalNetUsd: Double,
    val totalGrossUsd: Double,
    val transactionCount: Int
)

data class PayoutTrendSummary(
    val points: List<DailyPayoutTrendPoint>,
    val maxDailyNetUsd: Double,
    val averageDailyNetUsd: Double,
    val peakPoint: DailyPayoutTrendPoint?,
    val activeDaysCount: Int
)

/**
 * Aggregates Room [PayoutWireEntity] records into continuous daily buckets
 * for bar and line trend visualizations.
 */
object PayoutTrendAggregator {

    fun buildDailyTrends(
        records: List<PayoutWireEntity>,
        windowDays: Int = 7,
        nowMs: Long = System.currentTimeMillis()
    ): PayoutTrendSummary {
        val safeDays = windowDays.coerceIn(2, 60)
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val fullFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // Anchor end day to max(nowMs, newest record timestamp) so future-dated test records still chart cleanly
        val newestTimestamp = records.maxOfOrNull { it.timestamp }?.coerceAtLeast(nowMs) ?: nowMs
        val endCal = Calendar.getInstance().apply {
            timeInMillis = newestTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Group records by yyyy-MM-dd in local calendar
        val grouped = records.groupBy { rec ->
            keyFormat.format(Date(rec.timestamp))
        }

        val points = mutableListOf<DailyPayoutTrendPoint>()
        for (offset in (safeDays - 1) downTo 0) {
            val dayCal = (endCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val date = dayCal.time
            val dayKey = keyFormat.format(date)
            val dayRecords = grouped[dayKey].orEmpty()

            val processedNet = dayRecords
                .filter { !it.isSimulatedWire && it.transactionType == "PROCESSED_PAYOUT" }
                .sumOf { it.netAmountUsd }
            val simulatedNet = dayRecords
                .filter { it.isSimulatedWire || it.transactionType == "SIMULATED_BANK_WIRE" }
                .sumOf { it.netAmountUsd }
            val totalNet = processedNet + simulatedNet
            val totalGross = dayRecords.sumOf { it.grossAmountUsd }

            points.add(
                DailyPayoutTrendPoint(
                    dayKey = dayKey,
                    shortDayLabel = shortFormat.format(date),
                    fullDateLabel = fullFormat.format(date),
                    processedNetUsd = processedNet,
                    simulatedNetUsd = simulatedNet,
                    totalNetUsd = totalNet,
                    totalGrossUsd = totalGross,
                    transactionCount = dayRecords.size
                )
            )
        }

        val maxVal = points.maxOfOrNull { it.totalNetUsd } ?: 0.0
        val activeDays = points.count { it.transactionCount > 0 }
        val totalWindowNet = points.sumOf { it.totalNetUsd }
        val avg = if (points.isNotEmpty()) totalWindowNet / points.size else 0.0
        val peak = points.filter { it.transactionCount > 0 }.maxByOrNull { it.totalNetUsd }

        return PayoutTrendSummary(
            points = points,
            maxDailyNetUsd = maxVal,
            averageDailyNetUsd = avg,
            peakPoint = peak,
            activeDaysCount = activeDays
        )
    }
}
