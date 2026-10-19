package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderLight
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanDanger
import com.example.ui.theme.CleanOnPrimary
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSecondary
import com.example.ui.theme.CleanSecondaryContainer
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanSuccessContainer
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceSecondary
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Localized Payment Simulator Component
 *
 * Provides real-time currency conversion simulation with live exchange rates,
 * regional payment rails (SEPA, Faster Payments, iDEAL, Konbini, Pix, UPI, Alipay),
 * transparent FX fee comparisons, rate-lock countdowns, and automated Room ledger recording.
 */
@Composable
fun LocalizedPaymentSimulator(
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier,
    onOpenTerminalCheckout: (() -> Unit)? = null
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val amountUsd by viewModel.amountUsd.collectAsStateWithLifecycle()
    val isRateRefreshing by viewModel.isRateRefreshing.collectAsStateWithLifecycle()
    val lastRefreshTime by viewModel.lastRateRefreshTimestamp.collectAsStateWithLifecycle()
    val liveRates by viewModel.liveRateMap.collectAsStateWithLifecycle()
    val rateChangePercents by viewModel.rateChangePercents.collectAsStateWithLifecycle()
    val selectedPaymentRail by viewModel.selectedPaymentRail.collectAsStateWithLifecycle()
    val isSimulatorModalOpen by viewModel.isSimulatorModalOpen.collectAsStateWithLifecycle()

    var selectedRegionFilter by remember { mutableStateOf("All") }
    var isFeeBreakdownExpanded by remember { mutableStateOf(false) }

    // Pulse animation for live FX market ticker
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val currentAmountUsd = amountUsd.toDoubleOrNull() ?: 49.99
    val currentRate = liveRates[selectedCurrency.code] ?: selectedCurrency.exchangeRate
    val convertedAmount = currentAmountUsd * currentRate
    val inverseRate = if (currentRate > 0.0) 1.0 / currentRate else 1.0
    val estimatedSavingsUsd = viewModel.calculateFxSavingsUsd(currentAmountUsd)

    val formatter = remember(selectedCurrency.code) {
        CurrencyFormatter.DEFAULT
    }
    val formattedConvertedTotal = formatter.format(convertedAmount, selectedCurrency.code)

    // Filter currencies based on region chip
    val filteredCurrencies = remember(selectedRegionFilter, viewModel.currencies) {
        when (selectedRegionFilter) {
            "G10" -> viewModel.currencies.filter { it.code in listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF") }
            "Europe" -> viewModel.currencies.filter { it.region == "Europe" }
            "Asia-Pacific" -> viewModel.currencies.filter { it.region == "Asia-Pacific" }
            "Americas" -> viewModel.currencies.filter { it.region == "Americas" }
            else -> viewModel.currencies
        }
    }

    val availableRails = remember(selectedCurrency.code) {
        viewModel.getPaymentRailsForCurrency(selectedCurrency.code)
    }

    val activeRail = selectedPaymentRail ?: availableRails.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("localized_payment_simulator_card"),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, CleanBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER & LIVE RATE STATUS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanPrimaryContainer
                        ) {
                            Text(
                                text = "LIVE FX ENGINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Pulsing Live Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CleanSuccess.copy(alpha = pulseAlpha))
                        )

                        Text(
                            text = "Mid-Market",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Localized Payment Simulator",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )
                }

                // Live Rates Refresh Button
                IconButton(
                    onClick = { viewModel.refreshExchangeRates() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CleanSurfaceVariant)
                        .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                        .testTag("simulator_refresh_rates_button")
                ) {
                    if (isRateRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CleanPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Live Exchange Rates",
                            tint = CleanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick live timestamp & volatility hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedTime = remember(lastRefreshTime) {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastRefreshTime))
                }
                Text(
                    text = "Rates synced at $formattedTime • 0.0% spread markup",
                    fontSize = 11.sp,
                    color = CleanTextMuted
                )

                Text(
                    text = "12 Currencies Active",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanSecondary
                )
            }

            // 2. INTERACTIVE AMOUNT INPUT WITH PRESET CHIPS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Base Amount (USD)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanTextSecondary
                    )

                    // Quick presets row
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("25", "50", "100", "250").forEach { preset ->
                            Surface(
                                onClick = { viewModel.updateAmountUsd(preset) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (amountUsd == preset) CleanPrimaryContainer else CleanSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (amountUsd == preset) CleanPrimary else CleanBorderLight
                                ),
                                modifier = Modifier.testTag("simulator_preset_$preset")
                            ) {
                                Text(
                                    text = "$$preset",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (amountUsd == preset) CleanPrimary else CleanTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountUsd,
                    onValueChange = { viewModel.updateAmountUsd(it) },
                    prefix = { Text("$ ", fontWeight = FontWeight.Bold, color = CleanPrimary, fontSize = 16.sp) },
                    suffix = { Text("USD", fontWeight = FontWeight.SemiBold, color = CleanTextMuted, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulator_amount_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CleanPrimary,
                        unfocusedBorderColor = CleanBorder,
                        focusedContainerColor = CleanSurfaceVariant,
                        unfocusedContainerColor = CleanSurfaceVariant,
                        focusedTextColor = CleanTextPrimary,
                        unfocusedTextColor = CleanTextPrimary
                    )
                )
            }

            // 3. REGION FILTER TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "G10", "Europe", "Asia-Pacific", "Americas").forEach { region ->
                    val isSelected = selectedRegionFilter == region
                    Surface(
                        onClick = { selectedRegionFilter = region },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CleanPrimary else CleanSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorderLight),
                        modifier = Modifier.testTag("simulator_region_chip_$region")
                    ) {
                        Text(
                            text = region,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CleanOnPrimary else CleanTextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // 4. GLOBAL CURRENCY SELECTOR STRIP
            Text(
                text = "Target Settlement Currency",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CleanTextSecondary
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(filteredCurrencies) { currency ->
                    val isSelected = selectedCurrency.code == currency.code
                    val rate = liveRates[currency.code] ?: currency.exchangeRate
                    val changePercent = rateChangePercents[currency.code] ?: 0.0

                    Card(
                        modifier = Modifier
                            .width(88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.selectCurrency(currency) }
                            .testTag("currency_toggle_${currency.code.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CleanPrimaryContainer else CleanSurfaceVariant
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) CleanPrimary else CleanBorderLight
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(text = currency.flagEmoji, fontSize = 20.sp)
                            Text(
                                text = currency.code,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CleanPrimary else CleanTextPrimary
                            )
                            Text(
                                text = if (rate >= 10.0) String.format(Locale.US, "%.1f", rate) else String.format(Locale.US, "%.3f", rate),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextSecondary
                            )

                            // Trend Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (changePercent >= 0.0) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = CleanSuccess,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "+${String.format(Locale.US, "%.1f", changePercent)}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanSuccess
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = CleanDanger,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", changePercent)}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanDanger
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. LIVE CONVERSION BREAKDOWN CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CleanSurfaceVariant, CleanSurface)
                        )
                    )
                    .border(BorderStroke(1.dp, CleanBorder), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "LOCALIZED PAYOUT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanTextMuted,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedConvertedTotal,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanTextPrimary,
                                modifier = Modifier.testTag("simulator_converted_total_text")
                            )
                        }

                        // Currency Swap / Inversion Button
                        Surface(
                            onClick = {
                                // Refresh or re-calculate inverse
                                viewModel.refreshExchangeRates()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = CleanPrimaryContainer,
                            modifier = Modifier.testTag("simulator_swap_currencies_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Invert Exchange Rate",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Live FX",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CleanBorderLight)

                    // Rate Formula details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1 USD = ${String.format(Locale.US, "%.4f", currentRate)} ${selectedCurrency.code}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanSecondary
                        )

                        Text(
                            text = "1 ${selectedCurrency.code} = $${String.format(Locale.US, "%.4f", inverseRate)} USD",
                            fontSize = 11.sp,
                            color = CleanTextMuted
                        )
                    }

                    // Zero-Spread FX Savings Callout
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CleanSuccessContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = CleanSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Zero FX markup: Saves ~$${String.format(Locale.US, "%.2f", estimatedSavingsUsd)} vs traditional bank 3.5% foreign transaction fees.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanSuccess
                            )
                        }
                    }
                }
            }

            // 6. REGIONAL LOCAL PAYMENT RAILS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Regional Payment Rails (${selectedCurrency.code})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanTextSecondary
                    )

                    TextButton(
                        onClick = { isFeeBreakdownExpanded = !isFeeBreakdownExpanded },
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isFeeBreakdownExpanded) "Hide Details" else "Fee Breakdown",
                            fontSize = 11.sp,
                            color = CleanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableRails.forEach { rail ->
                        val isRailSelected = activeRail?.id == rail.id

                        Card(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectPaymentRail(rail) }
                                .testTag("simulator_rail_${rail.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRailSelected) CleanSecondaryContainer else CleanSurfaceVariant
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isRailSelected) CleanSecondary else CleanBorderLight
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (rail.isInstant) Icons.Default.FlashOn else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (isRailSelected) CleanSecondary else CleanTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = rail.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary
                                    )
                                    Text(
                                        text = "${rail.feeText} • ${rail.settlementTime}",
                                        fontSize = 10.sp,
                                        color = CleanTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable fee details
                AnimatedVisibility(
                    visible = isFeeBreakdownExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CleanSurfaceSecondary,
                        border = BorderStroke(1.dp, CleanBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Active Rail", fontSize = 11.sp, color = CleanTextSecondary)
                                Text(text = activeRail?.name ?: "SEPA Instant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Settlement Speed", fontSize = 11.sp, color = CleanTextSecondary)
                                Text(text = activeRail?.settlementTime ?: "Instant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanSuccess)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Merchant Processing Fee", fontSize = 11.sp, color = CleanTextSecondary)
                                Text(text = activeRail?.feeText ?: "0.20%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Buyer Pays", fontSize = 11.sp, color = CleanTextSecondary)
                                Text(text = formattedConvertedTotal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. ACTION BUTTONS: SIMULATE LOCALIZED PAYMENT + LOAD TERMINAL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.openSimulatorModal() },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .testTag("simulator_simulate_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CleanPrimary,
                        contentColor = CleanOnPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulate Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        // Open terminal checkout directly with selected currency and amount
                        if (onOpenTerminalCheckout != null) {
                            onOpenTerminalCheckout()
                        } else {
                            viewModel.openCheckout()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("simulator_load_terminal_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CleanPrimary
                    ),
                    border = BorderStroke(1.dp, CleanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Checkout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // SIMULATION CONFIRMATION & SETTLEMENT MODAL
    if (isSimulatorModalOpen) {
        PaymentSimulatorDialog(
            viewModel = viewModel,
            amountUsd = currentAmountUsd,
            targetCurrency = selectedCurrency,
            paymentRail = activeRail ?: availableRails.first(),
            onDismiss = { viewModel.closeSimulatorModal() }
        )
    }
}

/**
 * Interactive Simulation Dialog showing rate-lock, authorization flow, and direct Room ledger recording.
 */
@Composable
fun PaymentSimulatorDialog(
    viewModel: GlobalMarketViewModel,
    amountUsd: Double,
    targetCurrency: CurrencyInfo,
    paymentRail: LocalPaymentRail,
    onDismiss: () -> Unit
) {
    val customerName by viewModel.simulatorCustomerName.collectAsStateWithLifecycle()
    val billingCountry by viewModel.simulatorBillingCountry.collectAsStateWithLifecycle()
    var isSimulating by remember { mutableStateOf(false) }
    var isSettledSuccess by remember { mutableStateOf(false) }
    var referenceNumber by remember { mutableStateOf("") }

    val currentRate = viewModel.liveRateMap.collectAsStateWithLifecycle().value[targetCurrency.code] ?: targetCurrency.exchangeRate
    val convertedTotal = amountUsd * currentRate
    val formattedConverted = CurrencyFormatter.DEFAULT.format(convertedTotal, targetCurrency.code)

    Dialog(
        onDismissRequest = {
            if (!isSimulating) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("payment_simulator_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            border = BorderStroke(1.dp, CleanBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Localized Payment Simulation",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Live Interbank Settlement Engine",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("simulator_dialog_close")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isSettledSuccess) {
                    // SUCCESS STATE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CleanSuccess,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Payment Settled Successfully!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Text(
                            text = "Recorded in Room SQLite ledger under reference $referenceNumber",
                            fontSize = 12.sp,
                            color = CleanTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Amount Paid", fontSize = 12.sp, color = CleanTextMuted)
                                    Text(text = "$formattedConverted (${targetCurrency.code})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Base Value", fontSize = 12.sp, color = CleanTextMuted)
                                    Text(text = "$${String.format(Locale.US, "%.2f", amountUsd)} USD", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Payment Rail", fontSize = 12.sp, color = CleanTextMuted)
                                    Text(text = paymentRail.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Status", fontSize = 12.sp, color = CleanTextMuted)
                                    Text(text = "SETTLED (Instant)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanSuccess)
                                }
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("simulator_dialog_done_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "Close & View Ledger", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // PRE-SETTLEMENT SIMULATION FORM
                    // 15-Min Guaranteed Rate Lock Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CleanPrimaryContainer,
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "15-Min Rate Lock Guaranteed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary
                                )
                            }
                            Text(
                                text = "14:59",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanPrimary
                            )
                        }
                    }

                    // Customer Name Input
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.updateSimulatorCustomerName(it) },
                        label = { Text("Simulated Customer Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulator_customer_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanPrimary,
                            unfocusedBorderColor = CleanBorder
                        )
                    )

                    // Billing Country Input
                    OutlinedTextField(
                        value = billingCountry,
                        onValueChange = { viewModel.updateSimulatorBillingCountry(it) },
                        label = { Text("Settlement Market / Country") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulator_billing_country_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanPrimary,
                            unfocusedBorderColor = CleanBorder
                        )
                    )

                    // Summary Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CleanSurfaceSecondary,
                        border = BorderStroke(1.dp, CleanBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Buyer Pays Locally", fontSize = 12.sp, color = CleanTextSecondary)
                                Text(
                                    text = formattedConverted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CleanTextPrimary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Net Merchant Settlement", fontSize = 12.sp, color = CleanTextSecondary)
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", amountUsd)} USD",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanSecondary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Payment Rail & Fee", fontSize = 11.sp, color = CleanTextMuted)
                                Text(
                                    text = "${paymentRail.name} (${paymentRail.feeText})",
                                    fontSize = 11.sp,
                                    color = CleanTextMuted
                                )
                            }
                        }
                    }

                    // Confirm & Record Button
                    Button(
                        onClick = {
                            isSimulating = true
                            viewModel.simulatePaymentWithDetails(
                                amountUsd = amountUsd,
                                targetCurrency = targetCurrency,
                                paymentRail = paymentRail,
                                customerName = customerName,
                                country = billingCountry,
                                onSuccess = { tx ->
                                    referenceNumber = tx.stripeSubscriptionId
                                    isSimulating = false
                                    isSettledSuccess = true
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("simulator_confirm_payment_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanPrimary,
                            contentColor = CleanOnPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isSimulating
                    ) {
                        if (isSimulating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = CleanOnPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Authorizing Local Rail...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirm & Settle $formattedConverted",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
