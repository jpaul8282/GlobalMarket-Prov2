package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.data.SalesTransactionEntity
import com.example.data.TransactionEntity
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanDanger
import com.example.ui.theme.CleanDangerContainer
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSecondary
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanSuccessContainer
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceSecondary
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.DailySalesVolumePoint
import com.example.util.SalesVolumeAggregator
import java.util.Locale
import kotlin.math.roundToInt

enum class SalesVolumeChartMode {
    AREA,
    BAR
}

/**
 * Interactive 30-day Total Sales Volume chart for the main dashboard built natively
 * with Jetpack Compose Canvas, featuring Area & Bar modes, Cartesian dashed gridlines,
 * and tap/drag crosshair inspection.
 */
@Composable
fun SalesVolume30DayChart(
    transactions: List<TransactionEntity>,
    salesTransactions: List<SalesTransactionEntity>,
    currencyFormatter: CurrencyFormatter,
    modifier: Modifier = Modifier,
    onSeedSampleSales: (() -> Unit)? = null,
    onOpenFullAnalytics: (() -> Unit)? = null
) {
    var chartMode by remember { mutableStateOf(SalesVolumeChartMode.AREA) }

    val summary = remember(transactions, salesTransactions) {
        SalesVolumeAggregator.build30DayVolume(
            transactions = transactions,
            salesTransactions = salesTransactions,
            windowDays = 30
        )
    }

    val points = summary.points
    val defaultSelectedIndex = remember(points) {
        val lastActive = points.indexOfLast { it.orderCount > 0 }
        if (lastActive >= 0) lastActive else (points.lastIndex.coerceAtLeast(0))
    }
    var selectedDayIndex by remember(points.size) { mutableIntStateOf(defaultSelectedIndex) }

    LaunchedEffect(points) {
        if (selectedDayIndex !in points.indices) {
            selectedDayIndex = defaultSelectedIndex
        } else if (points.getOrNull(selectedDayIndex)?.orderCount == 0) {
            val active = points.indexOfLast { it.orderCount > 0 }
            if (active >= 0) selectedDayIndex = active
        }
    }

    val selectedPoint: DailySalesVolumePoint? = points.getOrNull(selectedDayIndex)
    val chartMaxUsd = remember(summary.maxDailyVolumeUsd) {
        if (summary.maxDailyVolumeUsd <= 0.0) 100.0 else summary.maxDailyVolumeUsd * 1.18
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sales_volume_30d_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(1.dp, CleanBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header & Area / Bar Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL SALES VOLUME • LAST 30 DAYS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currencyFormatter.formatUsd(summary.totalVolume30dUsd),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanTextPrimary,
                            modifier = Modifier.testTag("sales_volume_30d_total")
                        )

                        val delta = summary.periodDeltaPercent
                        if (delta != null) {
                            val positive = delta >= 0.0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (positive) CleanSuccessContainer else CleanDangerContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "15-day volume trend",
                                        tint = if (positive) CleanSuccess else CleanDanger,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%s%.1f%%",
                                            if (positive) "+" else "",
                                            delta
                                        ),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (positive) CleanSuccess else CleanDanger
                                    )
                                }
                            }
                        }
                    }
                }

                // Segmented Area vs Bar Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CleanSurfaceVariant)
                        .border(BorderStroke(1.dp, CleanBorderSubtle), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val isArea = chartMode == SalesVolumeChartMode.AREA
                    Surface(
                        onClick = { chartMode = SalesVolumeChartMode.AREA },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isArea) CleanPrimary else Color.Transparent,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .height(36.dp)
                            .testTag("sales_volume_mode_area")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Area Chart Mode",
                                tint = if (isArea) Color.White else CleanTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Area",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isArea) Color.White else CleanTextSecondary
                            )
                        }
                    }

                    val isBar = chartMode == SalesVolumeChartMode.BAR
                    Surface(
                        onClick = { chartMode = SalesVolumeChartMode.BAR },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isBar) CleanSecondary else Color.Transparent,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .height(36.dp)
                            .testTag("sales_volume_mode_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Bar Chart Mode",
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
                }
            }

            // 2. Legend + Scrub Tip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SalesLegendDot(color = CleanPrimary, label = "Total / Checkout")
                    SalesLegendDot(color = CleanSecondary, label = "POS Sales")
                }
                Text(
                    text = "Tap or drag chart",
                    fontSize = 10.sp,
                    color = CleanTextMuted
                )
            }

            // 3. Selected Day Crosshair Tooltip Banner
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanSurfaceSecondary,
                    border = BorderStroke(1.dp, CleanBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_volume_selected_tooltip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedPoint.fullDateLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "${selectedPoint.orderCount} orders • Checkout ${currencyFormatter.formatUsd(selectedPoint.checkoutVolumeUsd)} • POS ${currencyFormatter.formatUsd(selectedPoint.posVolumeUsd)}",
                                fontSize = 10.sp,
                                color = CleanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currencyFormatter.formatUsd(selectedPoint.totalVolumeUsd),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanPrimary
                        )
                    }
                }
            }

            // 4. Interactive Cartesian Chart Canvas with Y-Axis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCompactSalesUsd(chartMaxUsd),
                        fontSize = 9.sp,
                        color = CleanTextMuted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = formatCompactSalesUsd(chartMaxUsd * 0.5),
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val primaryColor = CleanPrimary
                    val secondaryColor = CleanSecondary
                    val gridColor = CleanBorderSubtle
                    val crosshairColor = CleanPrimary.copy(alpha = 0.45f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .testTag("sales_volume_30d_canvas")
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    if (points.isNotEmpty() && size.width > 0) {
                                        val idx = ((offset.x / size.width.toFloat()) * points.size)
                                            .toInt()
                                            .coerceIn(0, points.lastIndex)
                                        selectedDayIndex = idx
                                    }
                                }
                            }
                            .pointerInput(points) {
                                detectHorizontalDragGestures { change, _ ->
                                    if (points.isNotEmpty() && size.width > 0) {
                                        val idx = ((change.position.x / size.width.toFloat()) * points.size)
                                            .toInt()
                                            .coerceIn(0, points.lastIndex)
                                        selectedDayIndex = idx
                                    }
                                }
                            }
                    ) {
                        val chartWidth = size.width
                        val chartHeight = size.height
                        if (chartWidth <= 0f || chartHeight <= 0f || points.isEmpty()) return@Canvas

                        val topPad = 10.dp.toPx()
                        val bottomPad = 6.dp.toPx()
                        val usableHeight = (chartHeight - topPad - bottomPad).coerceAtLeast(1f)

                        // Cartesian dashed horizontal gridlines
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        listOf(0f, 0.5f, 1f).forEach { ratio ->
                            val y = topPad + usableHeight * ratio
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(chartWidth, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (ratio < 1f) dashEffect else null
                            )
                        }

                        if (chartMode == SalesVolumeChartMode.BAR) {
                            val slotWidth = chartWidth / points.size.toFloat()
                            val barWidth = (slotWidth * 0.62f).coerceIn(3.dp.toPx(), 18.dp.toPx())

                            points.forEachIndexed { index, pt ->
                                val slotLeft = index * slotWidth
                                val barLeft = slotLeft + (slotWidth - barWidth) / 2f
                                val isSelected = index == selectedDayIndex

                                val totalHeight = ((pt.totalVolumeUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)) * usableHeight
                                val posHeight = ((pt.posVolumeUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)) * usableHeight
                                val checkoutHeight = (totalHeight - posHeight).coerceAtLeast(0f)

                                if (isSelected) {
                                    drawRoundRect(
                                        color = primaryColor.copy(alpha = 0.08f),
                                        topLeft = Offset(slotLeft, 0f),
                                        size = Size(slotWidth, chartHeight),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }

                                val baselineY = topPad + usableHeight
                                val dimAlpha = if (isSelected) 1f else 0.72f

                                if (checkoutHeight > 0f) {
                                    drawRoundRect(
                                        color = primaryColor.copy(alpha = dimAlpha),
                                        topLeft = Offset(barLeft, baselineY - checkoutHeight),
                                        size = Size(barWidth, checkoutHeight),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                }

                                if (posHeight > 0f) {
                                    drawRoundRect(
                                        color = secondaryColor.copy(alpha = dimAlpha),
                                        topLeft = Offset(barLeft, baselineY - totalHeight),
                                        size = Size(barWidth, posHeight),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                }

                                if (totalHeight == 0f) {
                                    drawRoundRect(
                                        color = gridColor.copy(alpha = 0.55f),
                                        topLeft = Offset(barLeft, baselineY - 2.dp.toPx()),
                                        size = Size(barWidth, 2.dp.toPx()),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                }
                            }
                        } else {
                            // Smooth cubic Bezier AreaChart (Recharts monotone style)
                            val stepX = if (points.size > 1) chartWidth / (points.size - 1).toFloat() else chartWidth
                            val coords = points.mapIndexed { index, pt ->
                                val x = if (points.size == 1) chartWidth / 2f else index * stepX
                                val ratio = (pt.totalVolumeUsd / chartMaxUsd).toFloat().coerceIn(0f, 1f)
                                val y = topPad + usableHeight - (ratio * usableHeight)
                                Offset(x, y)
                            }

                            val linePath = Path().apply {
                                coords.forEachIndexed { idx, pt ->
                                    if (idx == 0) {
                                        moveTo(pt.x, pt.y)
                                    } else {
                                        val prev = coords[idx - 1]
                                        val midX = (prev.x + pt.x) / 2f
                                        cubicTo(midX, prev.y, midX, pt.y, pt.x, pt.y)
                                    }
                                }
                            }

                            val areaPath = Path().apply {
                                addPath(linePath)
                                val last = coords.last()
                                val first = coords.first()
                                val baselineY = topPad + usableHeight
                                lineTo(last.x, baselineY)
                                lineTo(first.x, baselineY)
                                close()
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.28f),
                                        primaryColor.copy(alpha = 0.03f)
                                    ),
                                    startY = topPad,
                                    endY = topPad + usableHeight
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(
                                    width = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Selected crosshair vertical line & active dot
                            coords.getOrNull(selectedDayIndex)?.let { active ->
                                drawLine(
                                    color = crosshairColor,
                                    start = Offset(active.x, topPad),
                                    end = Offset(active.x, topPad + usableHeight),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.2f),
                                    radius = 8.dp.toPx(),
                                    center = active
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.5.dp.toPx(),
                                    center = active
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = active
                                )
                            }
                        }
                    }
                }
            }

            // 5. X-Axis Date Markers (5 evenly distributed markers across 30 days)
            if (points.isNotEmpty()) {
                val labelIndices = remember(points.size) {
                    if (points.size <= 5) {
                        points.indices.toList()
                    } else {
                        listOf(0, 7, 14, 21, points.lastIndex).distinct()
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labelIndices.forEach { idx ->
                        val point = points[idx]
                        val isSelected = idx == selectedDayIndex
                        Text(
                            text = point.shortDayLabel,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CleanPrimary else CleanTextMuted
                        )
                    }
                }
            }

            // 6. Empty State Prompt when no sales exist in last 30 days
            if (summary.totalOrders30d == 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanPrimaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_volume_30d_empty_hint")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "No sales in the last 30 days yet. Seed sample sales or process a checkout.",
                                fontSize = 11.sp,
                                color = CleanTextSecondary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                        if (onSeedSampleSales != null) {
                            Button(
                                onClick = onSeedSampleSales,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .height(40.dp)
                                    .testTag("sales_volume_seed_button")
                            ) {
                                Text("Seed Sales", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = CleanBorderSubtle)

            // 7. 30-Day Summary KPI Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SalesMetricColumn(
                    label = "30D AVG / DAY",
                    value = currencyFormatter.formatUsd(summary.averageDailyVolumeUsd)
                )
                SalesMetricColumn(
                    label = "PEAK DAY",
                    value = summary.peakDay?.let {
                        "${it.shortDayLabel} (${formatCompactSalesUsd(it.totalVolumeUsd)})"
                    } ?: "No sales"
                )
                SalesMetricColumn(
                    label = "ORDERS",
                    value = "${summary.totalOrders30d} (${summary.activeDaysCount}d)"
                )
                if (onOpenFullAnalytics != null) {
                    TextButton(
                        onClick = onOpenFullAnalytics,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("sales_volume_full_analytics_btn")
                    ) {
                        Text(
                            text = "Analytics →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
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

@Composable
private fun SalesMetricColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = CleanTextMuted,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CleanTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatCompactSalesUsd(amount: Double): String {
    return when {
        amount >= 1_000_000.0 -> "$${((amount / 100_000.0).roundToInt() / 10.0)}M"
        amount >= 1_000.0 -> "$${((amount / 100.0).roundToInt() / 10.0)}k"
        else -> "$${amount.roundToInt()}"
    }
}
