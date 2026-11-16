package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PayoutWireEntity
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanOrange
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSecondary
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceSecondary
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.DailyPayoutTrendPoint
import com.example.util.PayoutTrendAggregator
import kotlin.math.roundToInt

enum class PayoutChartMode {
    BAR,
    LINE
}

/**
 * Interactive Native Jetpack Compose Bar & Line Chart visualizing daily payout
 * and simulated bank wire trends directly from the Room database.
 */
@Composable
fun PayoutDailyTrendsChart(
    records: List<PayoutWireEntity>,
    currencyFormatter: CurrencyFormatter,
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf(PayoutChartMode.BAR) }
    var windowDays by remember { mutableIntStateOf(7) }

    val trendSummary = remember(records, windowDays) {
        PayoutTrendAggregator.buildDailyTrends(
            records = records,
            windowDays = windowDays
        )
    }

    val points = trendSummary.points
    val defaultIndex = remember(points) {
        val lastActive = points.indexOfLast { it.transactionCount > 0 }
        if (lastActive >= 0) lastActive else (points.lastIndex.coerceAtLeast(0))
    }
    var selectedDayIndex by remember(windowDays, points.size) { mutableIntStateOf(defaultIndex) }

    LaunchedEffect(points) {
        if (selectedDayIndex !in points.indices) {
            selectedDayIndex = defaultIndex
        } else if (points.getOrNull(selectedDayIndex)?.transactionCount == 0) {
            val active = points.indexOfLast { it.transactionCount > 0 }
            if (active >= 0) selectedDayIndex = active
        }
    }

    val selectedPoint: DailyPayoutTrendPoint? = points.getOrNull(selectedDayIndex)
    val chartMaxUsd = remember(trendSummary.maxDailyNetUsd) {
        if (trendSummary.maxDailyNetUsd <= 0.0) 100.0 else trendSummary.maxDailyNetUsd * 1.15
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payout_daily_trends_chart_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(1.dp, CleanBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header & Bar/Line Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DAILY PAYOUT TRENDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Room Ledger Volume (${windowDays}D)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bar vs Line Segmented Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CleanSurfaceVariant)
                        .border(BorderStroke(1.dp, CleanBorderSubtle), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val isBar = chartMode == PayoutChartMode.BAR
                    Surface(
                        onClick = { chartMode = PayoutChartMode.BAR },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isBar) CleanPrimary else Color.Transparent,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("payout_chart_mode_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Bar Chart",
                                tint = if (isBar) Color.White else CleanTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Bar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBar) Color.White else CleanTextSecondary
                            )
                        }
                    }

                    val isLine = chartMode == PayoutChartMode.LINE
                    Surface(
                        onClick = { chartMode = PayoutChartMode.LINE },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isLine) CleanSecondary else Color.Transparent,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("payout_chart_mode_line")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Line Chart",
                                tint = if (isLine) Color.White else CleanTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Line",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLine) Color.White else CleanTextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Timeframe Pills (7D, 14D, 30D) + Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        val active = windowDays == days
                        Surface(
                            onClick = { windowDays = days },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) CleanPrimaryContainer else CleanSurfaceVariant,
                            border = BorderStroke(1.dp, if (active) CleanPrimary else CleanBorderSubtle),
                            modifier = Modifier.testTag("payout_chart_range_$days")
                        ) {
                            Text(
                                text = "${days}D",
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) CleanPrimary else CleanTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDotLabel(color = CleanPrimary, label = "Payouts")
                    LegendDotLabel(color = CleanOrange, label = "Sim Wires")
                }
            }

            // 3. Selected Day Tooltip Banner
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanSurfaceSecondary,
                    border = BorderStroke(1.dp, CleanBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payout_chart_selected_tooltip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedPoint.fullDateLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "${selectedPoint.transactionCount} tx • Processed ${currencyFormatter.formatUsd(selectedPoint.processedNetUsd)} • Wire ${currencyFormatter.formatUsd(selectedPoint.simulatedNetUsd)}",
                                fontSize = 10.sp,
                                color = CleanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = currencyFormatter.formatUsd(selectedPoint.totalNetUsd),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanPrimary
                        )
                    }
                }
            }

            // 4. Chart Canvas with Y-Axis Scale
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-axis ticks
                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCompactUsd(chartMaxUsd),
                        fontSize = 9.sp,
                        color = CleanTextMuted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = formatCompactUsd(chartMaxUsd * 0.5),
                        fontSize = 9.sp,
                        color = CleanTextMuted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "$0",
                        fontSize = 9.sp,
                        color = CleanTextMuted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // Canvas Area (uses DrawScope size.width & size.height directly)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val primaryColor = CleanPrimary
                    val secondaryColor = CleanSecondary
                    val orangeColor = CleanOrange
                    val gridColor = CleanBorderSubtle
                    val highlightBg = CleanPrimary.copy(alpha = 0.08f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    if (points.isNotEmpty() && size.width > 0) {
                                        val slotWidth = size.width.toFloat() / points.size
                                        val tapped = (offset.x / slotWidth)
                                            .toInt()
                                            .coerceIn(0, points.lastIndex)
                                        selectedDayIndex = tapped
                                    }
                                }
                            }
                            .testTag("payout_chart_canvas")
                    ) {
                        val w = size.width
                        val h = size.height
                        if (w <= 0f || h <= 0f || points.isEmpty()) return@Canvas

                        val topPad = 10.dp.toPx()
                        val bottomPad = 10.dp.toPx()
                        val plotHeight = (h - topPad - bottomPad).coerceAtLeast(1f)
                        val baselineY = h - bottomPad

                        // Dashed grid lines at top, mid, baseline
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0f, 0.5f, 1f).forEach { fraction ->
                            val y = topPad + plotHeight * fraction
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (fraction < 1f) dashEffect else null
                            )
                        }

                        val count = points.size
                        val slotWidth = w / count.toFloat()

                        if (chartMode == PayoutChartMode.BAR) {
                            val barWidth = (slotWidth * 0.58f).coerceIn(6.dp.toPx(), 26.dp.toPx())
                            val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                            points.forEachIndexed { idx, pt ->
                                val centerX = slotWidth * idx + slotWidth / 2f
                                val leftX = centerX - barWidth / 2f

                                if (idx == selectedDayIndex) {
                                    drawRoundRect(
                                        color = highlightBg,
                                        topLeft = Offset(slotWidth * idx + 2f, topPad),
                                        size = Size((slotWidth - 4f).coerceAtLeast(2f), plotHeight),
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }

                                val totalRatio = (pt.totalNetUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)
                                val processedRatio = (pt.processedNetUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)
                                val simulatedRatio = (pt.simulatedNetUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)

                                val totalBarHeight = if (pt.totalNetUsd > 0.0) {
                                    (totalRatio * plotHeight).coerceAtLeast(4.dp.toPx())
                                } else {
                                    2.dp.toPx()
                                }
                                val barTopY = baselineY - totalBarHeight

                                if (pt.totalNetUsd <= 0.0) {
                                    drawRoundRect(
                                        color = gridColor,
                                        topLeft = Offset(leftX, baselineY - 2.dp.toPx()),
                                        size = Size(barWidth, 2.dp.toPx()),
                                        cornerRadius = corner
                                    )
                                } else {
                                    val processedH = (processedRatio * plotHeight)
                                    val simulatedH = (simulatedRatio * plotHeight)

                                    // Bottom Processed Payouts bar segment
                                    if (processedH > 0f) {
                                        val procTop = (baselineY - processedH).coerceAtLeast(topPad)
                                        drawRoundRect(
                                            color = if (idx == selectedDayIndex) primaryColor else primaryColor.copy(alpha = 0.85f),
                                            topLeft = Offset(leftX, procTop),
                                            size = Size(barWidth, baselineY - procTop),
                                            cornerRadius = corner
                                        )
                                    }

                                    // Top Simulated Wire bar segment stacked cleanly
                                    if (simulatedH > 0f) {
                                        val stackBottom = (baselineY - processedH).coerceAtMost(baselineY)
                                        val stackTop = (barTopY).coerceAtMost(stackBottom - 3.dp.toPx())
                                        drawRoundRect(
                                            color = if (idx == selectedDayIndex) orangeColor else orangeColor.copy(alpha = 0.88f),
                                            topLeft = Offset(leftX, stackTop),
                                            size = Size(barWidth, (stackBottom - stackTop).coerceAtLeast(2.dp.toPx())),
                                            cornerRadius = corner
                                        )
                                    }
                                }
                            }
                        } else {
                            // LINE CHART MODE
                            val totalCoords = points.mapIndexed { idx, pt ->
                                val x = slotWidth * idx + slotWidth / 2f
                                val norm = (pt.totalNetUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)
                                val y = baselineY - (norm * plotHeight)
                                Offset(x, y)
                            }
                            val simCoords = points.mapIndexed { idx, pt ->
                                val x = slotWidth * idx + slotWidth / 2f
                                val norm = (pt.simulatedNetUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)
                                val y = baselineY - (norm * plotHeight)
                                Offset(x, y)
                            }

                            if (totalCoords.isNotEmpty()) {
                                val linePath = Path().apply {
                                    moveTo(totalCoords.first().x, totalCoords.first().y)
                                    for (i in 1 until totalCoords.size) {
                                        val prev = totalCoords[i - 1]
                                        val curr = totalCoords[i]
                                        val midX = (prev.x + curr.x) / 2f
                                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                    }
                                }

                                val fillPath = Path().apply {
                                    addPath(linePath)
                                    lineTo(totalCoords.last().x, baselineY)
                                    lineTo(totalCoords.first().x, baselineY)
                                    close()
                                }

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            secondaryColor.copy(alpha = 0.28f),
                                            secondaryColor.copy(alpha = 0.02f)
                                        ),
                                        startY = topPad,
                                        endY = baselineY
                                    )
                                )

                                drawPath(
                                    path = linePath,
                                    color = secondaryColor,
                                    style = Stroke(
                                        width = 2.6.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                // Simulated Wire overlay line if any simulated wires exist in window
                                if (points.any { it.simulatedNetUsd > 0.0 }) {
                                    val simPath = Path().apply {
                                        moveTo(simCoords.first().x, simCoords.first().y)
                                        for (i in 1 until simCoords.size) {
                                            lineTo(simCoords[i].x, simCoords[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = simPath,
                                        color = orangeColor,
                                        style = Stroke(
                                            width = 1.8.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                        )
                                    )
                                }

                                // Vertical guide & nodes
                                val selectedCoord = totalCoords.getOrNull(selectedDayIndex)
                                if (selectedCoord != null) {
                                    drawLine(
                                        color = secondaryColor.copy(alpha = 0.35f),
                                        start = Offset(selectedCoord.x, topPad),
                                        end = Offset(selectedCoord.x, baselineY),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }

                                totalCoords.forEachIndexed { idx, coord ->
                                    val isSel = idx == selectedDayIndex
                                    if (isSel) {
                                        drawCircle(
                                            color = secondaryColor.copy(alpha = 0.22f),
                                            radius = 9.dp.toPx(),
                                            center = coord
                                        )
                                    }
                                    drawCircle(
                                        color = Color.White,
                                        radius = if (isSel) 5.dp.toPx() else 3.5.dp.toPx(),
                                        center = coord
                                    )
                                    drawCircle(
                                        color = if (isSel) primaryColor else secondaryColor,
                                        radius = if (isSel) 3.5.dp.toPx() else 2.2.dp.toPx(),
                                        center = coord
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. X-Axis Daily Labels (spaced evenly to avoid crowding on 14D/30D)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labelIndices = when {
                    points.size <= 7 -> points.indices.toList()
                    points.size <= 14 -> listOf(0, 3, 6, 9, points.lastIndex).distinct()
                    else -> listOf(0, 7, 14, 21, points.lastIndex).distinct()
                }
                labelIndices.forEach { idx ->
                    val pt = points[idx]
                    Text(
                        text = pt.shortDayLabel,
                        fontSize = 9.sp,
                        fontWeight = if (idx == selectedDayIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (idx == selectedDayIndex) CleanPrimary else CleanTextMuted
                    )
                }
            }

            HorizontalDivider(color = CleanBorderSubtle)

            // 6. Peak & Daily Average Metrics Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Daily Average", fontSize = 10.sp, color = CleanTextMuted)
                    Text(
                        text = currencyFormatter.formatUsd(trendSummary.averageDailyNetUsd),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary,
                        modifier = Modifier.testTag("payout_chart_daily_average")
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Active Days", fontSize = 10.sp, color = CleanTextMuted)
                    Text(
                        text = "${trendSummary.activeDaysCount} / ${points.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Peak Day", fontSize = 10.sp, color = CleanTextMuted)
                    Text(
                        text = trendSummary.peakPoint?.let {
                            "${it.shortDayLabel} • ${currencyFormatter.formatUsd(it.totalNetUsd)}"
                        } ?: "$0.00",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanPrimary,
                        modifier = Modifier.testTag("payout_chart_peak_day")
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDotLabel(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = CleanTextSecondary
        )
    }
}

private fun formatCompactUsd(amount: Double): String {
    return when {
        amount >= 1000.0 -> "$${(amount / 100.0).roundToInt() / 10.0}k"
        else -> "$${amount.roundToInt()}"
    }
}
