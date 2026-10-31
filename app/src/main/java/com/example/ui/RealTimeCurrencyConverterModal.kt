package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive Real-Time Currency Conversion & Payment Simulator Modal.
 *
 * Utilizes Retrofit live exchange rate API (api.exchangerate-api.com) with offline fallback.
 * Allows users to convert between any global currencies, view live interbank rates,
 * select regional payment rails, and simulate live transactions that record directly
 * to SQLite Room database and Firebase cloud ledger.
 */
@Composable
fun RealTimeCurrencyConverterModal(
    viewModel: GlobalMarketViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val currencies = viewModel.currencies
    val liveRateMap by viewModel.liveRateMap.collectAsStateWithLifecycle()
    val rateChangePercents by viewModel.rateChangePercents.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRateRefreshing.collectAsStateWithLifecycle()
    val lastRefreshTime by viewModel.lastRateRefreshTimestamp.collectAsStateWithLifecycle()

    var sourceCurrency by remember { mutableStateOf(currencies.first { it.code == "USD" }) }
    var targetCurrency by remember { mutableStateOf(currencies.first { it.code == "EUR" }) }
    var inputAmountStr by remember { mutableStateOf("100.00") }
    var customerNameInput by remember { mutableStateOf("J.P. Westerveld") }
    var isSimulatingPayment by remember { mutableStateOf(false) }
    var completedReceipt by remember { mutableStateOf<SimulatedReceiptData?>(null) }

    val parsedAmount = inputAmountStr.toDoubleOrNull() ?: 100.0

    // Compute live rate: rate relative to USD
    val sourceToUsdRate = liveRateMap[sourceCurrency.code] ?: sourceCurrency.exchangeRate
    val targetToUsdRate = liveRateMap[targetCurrency.code] ?: targetCurrency.exchangeRate
    val effectiveRate = if (sourceToUsdRate > 0.0) targetToUsdRate / sourceToUsdRate else 1.0
    val convertedAmount = parsedAmount * effectiveRate
    val inverseRate = if (effectiveRate > 0.0) 1.0 / effectiveRate else 1.0

    val availableRails = remember(targetCurrency.code) {
        viewModel.getPaymentRailsForCurrency(targetCurrency.code)
    }
    var selectedRail by remember(targetCurrency.code) {
        mutableStateOf(availableRails.firstOrNull())
    }

    val currencyFormatter = remember { CurrencyFormatter.DEFAULT }

    // Pulsing animation for live API status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live_api")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Rotation animation for refresh icon
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    Dialog(
        onDismissRequest = { if (!isSimulatingPayment) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(26.dp))
                .testTag("currency_converter_modal"),
            color = CleanSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, CleanBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. MODAL HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Real-Time Currency Converter",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(CleanSuccess.copy(alpha = pulseAlpha))
                                )
                            }
                            Text(
                                text = "Live Interbank API Rates & Payment Simulation",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CleanSurfaceVariant)
                            .border(BorderStroke(1.dp, CleanBorderSubtle), CircleShape)
                            .testTag("close_converter_modal")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CleanTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // 2. LIVE API STATUS TICKER & REFRESH ACTION
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanSurfaceVariant,
                    border = BorderStroke(1.dp, CleanBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CleanSuccess)
                            )
                            Column {
                                Text(
                                    text = "LIVE API FEED (exchangerate-api.com)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                val timeText = remember(lastRefreshTime) {
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastRefreshTime))
                                }
                                Text(
                                    text = "Updated: $timeText • Latency < 25ms",
                                    fontSize = 10.sp,
                                    color = CleanTextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.refreshExchangeRates(sourceCurrency.code)
                                Toast.makeText(context, "Fetching latest live rates for ${sourceCurrency.code}...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CleanSurface)
                                .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                                .testTag("refresh_api_rates_btn")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CleanPrimary)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = CleanPrimary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .rotate(rotationAngle)
                                )
                            }
                        }
                    }
                }

                // 3. INPUT AMOUNT & SOURCE CURRENCY
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurfaceVariant),
                    border = BorderStroke(1.dp, CleanBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SOURCE CURRENCY & AMOUNT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 0.6.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputAmountStr,
                                onValueChange = { inputAmountStr = it },
                                leadingIcon = {
                                    Text(
                                        text = sourceCurrency.symbol,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = CleanPrimary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("converter_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CleanPrimary,
                                    unfocusedBorderColor = CleanBorder,
                                    focusedContainerColor = CleanSurface,
                                    unfocusedContainerColor = CleanSurface
                                )
                            )

                            // Source currency badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CleanPrimaryContainer,
                                border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = sourceCurrency.flagEmoji, fontSize = 18.sp)
                                    Text(text = sourceCurrency.code, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CleanOnPrimaryContainer)
                                }
                            }
                        }

                        // Preset Amount Chips (25, 50, 100, 250, 500)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("25", "50", "100", "250", "500").forEach { preset ->
                                Surface(
                                    onClick = { inputAmountStr = "$preset.00" },
                                    shape = RoundedCornerShape(8.dp),
                                    color = CleanSurface,
                                    border = BorderStroke(1.dp, CleanBorderSubtle),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(text = "$preset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanTextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. SWAP CURRENCIES BUTTON
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalDivider(color = CleanBorderSubtle, modifier = Modifier.fillMaxWidth())

                    Surface(
                        onClick = {
                            val temp = sourceCurrency
                            sourceCurrency = targetCurrency
                            targetCurrency = temp
                            viewModel.selectCurrency(targetCurrency)
                            viewModel.refreshExchangeRates(targetCurrency.code)
                            Toast.makeText(context, "Swapped: ${sourceCurrency.code} ⇄ ${targetCurrency.code}", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        color = CleanPrimary,
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("swap_currencies_action_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Swap", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // 5. TARGET CURRENCY SELECTOR STRIP
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT TARGET CURRENCY (12 GLOBAL RAILS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.6.sp
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(currencies) { curr ->
                            val isSelected = curr.code == targetCurrency.code
                            Surface(
                                onClick = {
                                    targetCurrency = curr
                                    viewModel.selectCurrency(curr)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CleanPrimary else CleanSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorderSubtle),
                                modifier = Modifier.testTag("target_curr_${curr.code.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = curr.flagEmoji, fontSize = 16.sp)
                                    Column {
                                        Text(
                                            text = curr.code,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else CleanTextPrimary
                                        )
                                        Text(
                                            text = curr.symbol,
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else CleanTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. REAL-TIME CONVERTED TOTAL & EXCHANGE RATE QUOTE
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurfaceSecondary),
                    border = BorderStroke(1.dp, CleanBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CONVERTED PAYMENT AMOUNT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = currencyFormatter.format(convertedAmount, targetCurrency.code),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CleanPrimary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CleanSuccessContainer,
                                border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = CleanSuccess, modifier = Modifier.size(12.dp))
                                    Text(text = "ZERO FX MARKUP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CleanSuccess)
                                }
                            }
                        }

                        HorizontalDivider(color = CleanBorderSubtle)

                        // Real-Time Rate details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1 ${sourceCurrency.code} = ${String.format(Locale.US, "%.4f", effectiveRate)} ${targetCurrency.code}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CleanSecondary
                            )
                            Text(
                                text = "1 ${targetCurrency.code} = ${String.format(Locale.US, "%.4f", inverseRate)} ${sourceCurrency.code}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CleanTextMuted
                            )
                        }
                    }
                }

                // 7. LOCAL PAYMENT RAIL SELECTION FOR TARGET CURRENCY
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LOCAL PAYMENT RAIL (${targetCurrency.code})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.6.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableRails.forEach { rail ->
                            val isRailSelected = selectedRail?.id == rail.id
                            Surface(
                                onClick = { selectedRail = rail },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isRailSelected) CleanSecondaryContainer else CleanSurfaceVariant,
                                border = BorderStroke(1.dp, if (isRailSelected) CleanSecondary else CleanBorderSubtle),
                                modifier = Modifier.testTag("rail_select_${rail.id}")
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
                                        Text(text = rail.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                                        Text(text = "${rail.feeText} • ${rail.settlementTime}", fontSize = 10.sp, color = CleanTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. SIMULATION ACTION BUTTON
                Button(
                    onClick = {
                        val activeRail = selectedRail ?: availableRails.first()
                        isSimulatingPayment = true
                        scope.launch {
                            delay(600) // Realistic UI animation
                            val refCode = "SIM-${targetCurrency.code}-${(100000..999999).random()}"
                            val usdEquivalent = if (sourceToUsdRate > 0.0) parsedAmount / sourceToUsdRate else parsedAmount

                            viewModel.simulatePaymentWithDetails(
                                amountUsd = usdEquivalent,
                                targetCurrency = targetCurrency,
                                paymentRail = activeRail,
                                customerName = customerNameInput,
                                country = targetCurrency.defaultCountry,
                                onSuccess = {
                                    isSimulatingPayment = false
                                    completedReceipt = SimulatedReceiptData(
                                        referenceCode = refCode,
                                        timestamp = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date()),
                                        customerName = customerNameInput,
                                        country = targetCurrency.defaultCountry,
                                        sourceCurrencyCode = sourceCurrency.code,
                                        sourceAmount = parsedAmount,
                                        targetCurrencyCode = targetCurrency.code,
                                        targetAmount = convertedAmount,
                                        exchangeRateUsed = effectiveRate,
                                        paymentRailName = activeRail.name,
                                        status = "SETTLED"
                                    )
                                    Toast.makeText(context, "Payment of ${currencyFormatter.format(convertedAmount, targetCurrency.code)} simulated & recorded!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("execute_simulated_payment_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CleanPrimary,
                        contentColor = Color.White
                    ),
                    enabled = !isSimulatingPayment && parsedAmount > 0.0
                ) {
                    if (isSimulatingPayment) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Simulating Real-Time Payment...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simulate Payment: ${currencyFormatter.format(convertedAmount, targetCurrency.code)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // SUCCESS SIMULATED PAYMENT RECEIPT MODAL
    if (completedReceipt != null) {
        SimulatedPaymentSuccessModal(
            receipt = completedReceipt!!,
            currencyFormatter = currencyFormatter,
            onDismiss = {
                completedReceipt = null
                onDismiss()
            }
        )
    }
}

data class SimulatedReceiptData(
    val referenceCode: String,
    val timestamp: String,
    val customerName: String,
    val country: String,
    val sourceCurrencyCode: String,
    val sourceAmount: Double,
    val targetCurrencyCode: String,
    val targetAmount: Double,
    val exchangeRateUsed: Double,
    val paymentRailName: String,
    val status: String
)

@Composable
fun SimulatedPaymentSuccessModal(
    receipt: SimulatedReceiptData,
    currencyFormatter: CurrencyFormatter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val receiptText = remember(receipt) {
        """
            GLOBALMARKET PRO - SIMULATED PAYMENT CONFIRMATION
            =================================================
            Reference       : ${receipt.referenceCode}
            Timestamp       : ${receipt.timestamp}
            Customer        : ${receipt.customerName} (${receipt.country})
            Payment Rail    : ${receipt.paymentRailName}
            Status          : ${receipt.status} (Room SQLite + Firestore)
            -------------------------------------------------
            Paid in Target  : ${currencyFormatter.format(receipt.targetAmount, receipt.targetCurrencyCode)}
            Source Base     : ${currencyFormatter.format(receipt.sourceAmount, receipt.sourceCurrencyCode)}
            API Rate Used   : 1 ${receipt.sourceCurrencyCode} = ${String.format(Locale.US, "%.4f", receipt.exchangeRateUsed)} ${receipt.targetCurrencyCode}
            =================================================
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("simulation_success_modal"),
            color = CleanSurface,
            border = BorderStroke(1.dp, CleanBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(CleanSuccessContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CleanSuccess, modifier = Modifier.size(32.dp))
                }

                Text(
                    text = "Payment Simulated Successfully!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextPrimary
                )
                Text(
                    text = "Converted via real-time API and logged to Room database",
                    fontSize = 11.sp,
                    color = CleanTextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CleanSurfaceSecondary,
                    border = BorderStroke(1.dp, CleanBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReceiptRow("Reference", receipt.referenceCode, isMonospace = true)
                        ReceiptRow("Timestamp", receipt.timestamp)
                        ReceiptRow("Customer", receipt.customerName)
                        ReceiptRow("Payment Rail", receipt.paymentRailName)
                        ReceiptRow(
                            "Live Rate",
                            "1 ${receipt.sourceCurrencyCode} = ${String.format(Locale.US, "%.4f", receipt.exchangeRateUsed)} ${receipt.targetCurrencyCode}",
                            isMonospace = true
                        )

                        HorizontalDivider(color = CleanBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Converted:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                            Text(
                                text = currencyFormatter.format(receipt.targetAmount, receipt.targetCurrencyCode),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(receiptText))
                            Toast.makeText(context, "Receipt copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Simulated Payment Receipt - ${receipt.referenceCode}")
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Payment Confirmation"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CleanPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = CleanTextMuted)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = CleanTextPrimary
        )
    }
}
