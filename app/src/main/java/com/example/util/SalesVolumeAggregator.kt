package com.example.util

import com.example.data.SalesTransactionEntity
import com.example.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailySalesVolumePoint(
    val dayKey: String,
    val shortDayLabel: String,
    val fullDateLabel: String,
    val checkoutVolumeUsd: Double,
    val posVolumeUsd: Double,
    val totalVolumeUsd: Double,
    val orderCount: Int
)

data class SalesVolume30DaySummary(
    val points: List<DailySalesVolumePoint>,
    val totalVolume30dUsd: Double,
    val maxDailyVolumeUsd: Double,
    val averageDailyVolumeUsd: Double,
    val peakDay: DailySalesVolumePoint?,
    val totalOrders30d: Int,
    val activeDaysCount: Int,
    val periodDeltaPercent: Double?
)

/**
 * Aggregates checkout [TransactionEntity] and POS [SalesTransactionEntity] records
 * into a continuous 30-day daily sales volume time series (in USD), excluding payout mirrors
 * and voided/refunded records.
 */
object SalesVolumeAggregator {

    private val NON_REALIZED_STATUSES = setOf("REFUNDED", "VOIDED", "FAILED", "CANCELED")

    fun build30DayVolume(
        transactions: List<TransactionEntity>,
        salesTransactions: List<SalesTransactionEntity> = emptyList(),
        windowDays: Int = 30,
        nowMs: Long = System.currentTimeMillis()
    ): SalesVolume30DaySummary {
        val safeDays = windowDays.coerceIn(7, 90)
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shortFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val fullFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val validCheckoutTx = transactions.filterNot { tx ->
            tx.customerName.startsWith("Payout (") ||
                tx.customerName.startsWith("Stripe Payout:") ||
                tx.stripeSubscriptionId.startsWith("po_") ||
                tx.amountInUsd <= 0.0
        }

        val validPosSales = salesTransactions.filter { sale ->
            sale.terminalId != "PAYOUT-TERMINAL" &&
                sale.terminalId != "STRIPE-PAYOUT-API" &&
                sale.paymentStatus.uppercase(Locale.US) !in NON_REALIZED_STATUSES &&
                sale.amountInUsd > 0.0
        }

        val newestTimestamp = maxOf(
            nowMs,
            validCheckoutTx.maxOfOrNull { it.timestamp } ?: 0L,
            validPosSales.maxOfOrNull { it.timestamp } ?: 0L
        )

        val endCal = Calendar.getInstance().apply {
            timeInMillis = newestTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val checkoutByDay = validCheckoutTx.groupBy { keyFormat.format(Date(it.timestamp)) }
        val posByDay = validPosSales.groupBy { keyFormat.format(Date(it.timestamp)) }

        val points = mutableListOf<DailySalesVolumePoint>()
        for (offset in (safeDays - 1) downTo 0) {
            val dayCal = (endCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val date = dayCal.time
            val dayKey = keyFormat.format(date)

            val dayCheckouts = checkoutByDay[dayKey].orEmpty()
            val dayPos = posByDay[dayKey].orEmpty()

            val checkoutUsd = dayCheckouts.sumOf { it.amountInUsd }
            val posUsd = dayPos.sumOf { it.amountInUsd }
            val totalUsd = checkoutUsd + posUsd

            points.add(
                DailySalesVolumePoint(
                    dayKey = dayKey,
                    shortDayLabel = shortFormat.format(date),
                    fullDateLabel = fullFormat.format(date),
                    checkoutVolumeUsd = checkoutUsd,
                    posVolumeUsd = posUsd,
                    totalVolumeUsd = totalUsd,
                    orderCount = dayCheckouts.size + dayPos.size
                )
            )
        }

        val totalVolume = points.sumOf { it.totalVolumeUsd }
        val maxDaily = points.maxOfOrNull { it.totalVolumeUsd } ?: 0.0
        val totalOrders = points.sumOf { it.orderCount }
        val activeDays = points.count { it.orderCount > 0 }
        val avgDaily = if (points.isNotEmpty()) totalVolume / points.size else 0.0
        val peakDay = points.filter { it.orderCount > 0 }.maxByOrNull { it.totalVolumeUsd }

        val halfSize = points.size / 2
        val priorHalfTotal = points.take(halfSize).sumOf { it.totalVolumeUsd }
        val recentHalfTotal = points.drop(halfSize).sumOf { it.totalVolumeUsd }
        val periodDelta = if (priorHalfTotal > 0.0) {
            ((recentHalfTotal - priorHalfTotal) / priorHalfTotal) * 100.0
        } else {
            null
        }

        return SalesVolume30DaySummary(
            points = points,
            totalVolume30dUsd = totalVolume,
            maxDailyVolumeUsd = maxDaily,
            averageDailyVolumeUsd = avgDaily,
            peakDay = peakDay,
            totalOrders30d = totalOrders,
            activeDaysCount = activeDays,
            periodDeltaPercent = periodDelta
        )
    }
}
