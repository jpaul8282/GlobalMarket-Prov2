package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PayoutWireEntity
import com.example.data.TransactionLogEntity
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dedicated Owner Payout Section view providing comprehensive merchant settlement
 * controls, live wallet/vault balance, instant Bank Wire / PayPal disbursement,
 * routing credentials management, and dedicated settlement audit history.
 */
@Composable
fun OwnerPayoutSectionView(
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val payoutConfig by viewModel.payoutConfig.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val transactionLogs by viewModel.transactionLogs.collectAsStateWithLifecycle()
    val payoutWireHistory by viewModel.payoutWireHistory.collectAsStateWithLifecycle()
    val stripePayoutState by viewModel.stripePayoutState.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val currencyFormatter = remember { CurrencyFormatter.DEFAULT }

    // Owner financial calculations
    val totalGrossRevenueUsd = remember(transactions) {
        transactions.sumOf { it.amountInUsd }
    }

    // Payout transactions history from Room logs
    val payoutLogs = remember(transactionLogs) {
        transactionLogs.filter { it.logType == "PAYOUT" || it.paymentMethod.contains("Wire", ignoreCase = true) || it.paymentMethod.contains("PayPal", ignoreCase = true) }
            .sortedByDescending { it.timestamp }
    }

    var historyFilter by remember { mutableStateOf("ALL") }
    val filteredPayoutWires = remember(payoutWireHistory, historyFilter) {
        when (historyFilter) {
            "STRIPE" -> payoutWireHistory.filter { it.method.equals("STRIPE_PAYOUT", ignoreCase = true) }
            "PAYOUTS" -> payoutWireHistory.filter { !it.isSimulatedWire && it.transactionType == "PROCESSED_PAYOUT" }
            "SIM_WIRES" -> payoutWireHistory.filter { it.isSimulatedWire || it.transactionType == "SIMULATED_BANK_WIRE" }
            "IDEAL" -> payoutWireHistory.filter {
                it.method.equals("IDEAL", ignoreCase = true) ||
                    it.clearingRail.contains("iDEAL", ignoreCase = true) ||
                    it.clearingRail.contains("SEPA", ignoreCase = true)
            }
            else -> payoutWireHistory
        }
    }

    val totalDisbursedUsd = remember(payoutWireHistory, payoutLogs) {
        if (payoutWireHistory.isNotEmpty()) {
            payoutWireHistory.filter { !it.isSimulatedWire }.sumOf { it.grossAmountUsd }
        } else {
            payoutLogs.sumOf { it.amountInUsd }
        }
    }

    val availablePayoutBalanceUsd = remember(totalGrossRevenueUsd, totalDisbursedUsd) {
        (totalGrossRevenueUsd - totalDisbursedUsd).coerceAtLeast(0.0)
    }

    // Interactive payout input states
    var activeMethod by remember { mutableStateOf(payoutConfig.selectedMethod) }
    var payoutAmountInput by remember { mutableStateOf("250.00") }
    var selectedPayoutCurrency by remember { mutableStateOf(selectedCurrency) }
    var isCurrencyMenuOpen by remember { mutableStateOf(false) }
    var isDisbursing by remember { mutableStateOf(false) }
    var isSimulatingWire by remember { mutableStateOf(false) }
    var lastSuccessRef by remember { mutableStateOf<String?>(null) }
    var showEditCredentialsDialog by remember { mutableStateOf(false) }
    var showPayoutExportDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val parsedAmount = payoutAmountInput.toDoubleOrNull() ?: 0.0

    LaunchedEffect(payoutConfig.selectedMethod) {
        activeMethod = payoutConfig.selectedMethod
    }

    val scrollToSectionKey: (String) -> Unit = { targetKey ->
        scope.launch {
            val targetIdx = listState.layoutInfo.totalItemsCount.let {
                // Find by matching item keys if visible, or use stable layout ordering
                when (targetKey) {
                    "stripe_section" -> 5
                    "disburse_section" -> if (lastSuccessRef != null) 7 else 6
                    "trends_section" -> if (lastSuccessRef != null) 8 else 7
                    else -> 0
                }
            }
            listState.animateScrollToItem(targetIdx)
        }
    }

    LaunchedEffect(stripePayoutState.isKeyConfigured) {
        if (stripePayoutState.isKeyConfigured &&
            stripePayoutState.lastSyncedEpochMs == null &&
            !stripePayoutState.isLoadingBalance
        ) {
            viewModel.triggerVerifyStripeConnection()
        }
    }
    val calculatedFee = viewModel.calculatePayoutFee(activeMethod, parsedAmount)
    val netDisbursement = (parsedAmount - calculatedFee).coerceAtLeast(0.0)

    val triggerQuickSimulatedWire: () -> Unit = {
        val simAmount = if (parsedAmount > 0.0) parsedAmount else 250.0
        isSimulatingWire = true
        scope.launch {
            delay(350)
            viewModel.simulateBankWireTransfer(
                amountInUsd = simAmount,
                currencyCode = selectedPayoutCurrency.code,
                method = "IDEAL",
                clearingRail = "iDEAL / SEPA Instant",
                recipientName = payoutConfig.bankWire.accountHolderName,
                bankName = payoutConfig.bankWire.bankName,
                ibanOrAccount = payoutConfig.bankWire.accountNumberOrIban,
                swiftBic = payoutConfig.bankWire.swiftBic,
                country = payoutConfig.bankWire.country,
                notes = "Simulated iDEAL & ING Bank SEPA wire",
                onSuccess = { saved ->
                    lastSuccessRef = saved.referenceCode
                    isSimulatingWire = false
                    Toast.makeText(
                        context,
                        "Simulated Bank Wire ${saved.referenceCode} saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("owner_payout_section_view")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // 1. TOP HEADER & NAVIGATION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CleanSurfaceVariant)
                                    .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                                    .testTag("owner_payout_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Owner Payout & Settlement",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanTextPrimary,
                                    letterSpacing = (-0.4).sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CleanTertiaryContainer,
                                    border = BorderStroke(1.dp, CleanTertiary.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "VERIFIED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CleanOnTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Disburse revenue to ${profile.name} (${profile.organization})",
                                fontSize = 12.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.openStripeUi() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StripeIndigoContainer)
                                .border(BorderStroke(1.dp, StripeIndigo.copy(alpha = 0.45f)), CircleShape)
                                .testTag("owner_open_stripe_ui_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Open Full Stripe UI",
                                tint = StripeIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.openPayoutHistoryScreen() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CleanSurfaceVariant)
                                .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                                .testTag("owner_open_payout_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "View Payout History",
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Direct Full Modal Trigger Button
                        IconButton(
                            onClick = { viewModel.openPayoutModal() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer)
                                .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                                .testTag("owner_open_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = "Open Full Modal",
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. OWNER VAULT & AVAILABLE BALANCE HERO CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = CleanPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = CleanOnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "AVAILABLE FOR DISBURSEMENT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimary.copy(alpha = 0.85f),
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = profile.orgId.take(12) + "...",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Big Amount Display
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = currencyFormatter.formatUsd(availablePayoutBalanceUsd),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanOnPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        val convertedVault = availablePayoutBalanceUsd * selectedPayoutCurrency.exchangeRate
                        Text(
                            text = "≈ ${currencyFormatter.format(convertedVault, selectedPayoutCurrency.code)} (${selectedPayoutCurrency.code})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanOnPrimary.copy(alpha = 0.8f)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    // Three Mini Statistics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Gross Revenue",
                                fontSize = 11.sp,
                                color = CleanOnPrimary.copy(alpha = 0.75f)
                            )
                            Text(
                                text = currencyFormatter.formatUsd(totalGrossRevenueUsd),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Total Disbursed",
                                fontSize = 11.sp,
                                color = CleanOnPrimary.copy(alpha = 0.75f)
                            )
                            Text(
                                text = currencyFormatter.formatUsd(totalDisbursedUsd),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Settled Payouts",
                                fontSize = 11.sp,
                                color = CleanOnPrimary.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${payoutWireHistory.size.coerceAtLeast(payoutLogs.size)} records",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimary
                            )
                        }
                    }

                    // Stripe Live Integration Status Strip inside Vault
                    val primaryStripeAvail = stripePayoutState.availableBalances.firstOrNull()
                    Surface(
                        onClick = {
                            activeMethod = PayoutMethodType.STRIPE_PAYOUT
                            viewModel.selectPayoutMethod(PayoutMethodType.STRIPE_PAYOUT)
                            viewModel.refreshStripeBalanceAndPayouts()
                            scrollToSectionKey("stripe_section")
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_stripe_status_pill")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Stripe Live Status",
                                    tint = CleanOnPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Stripe Live • ${stripePayoutState.connectionHealth.label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanOnPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (primaryStripeAvail != null) {
                                            "Stripe Available: ${currencyFormatter.format(primaryStripeAvail.majorAmount, primaryStripeAvail.currencyCode)}"
                                        } else {
                                            "${stripePayoutState.environmentLabel} • Tap to sync & pay out"
                                        },
                                        fontSize = 10.sp,
                                        color = CleanOnPrimary.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Stripe Section",
                                tint = CleanOnPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Vault Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (availablePayoutBalanceUsd > 0.0) {
                                    payoutAmountInput = String.format(Locale.US, "%.2f", availablePayoutBalanceUsd)
                                }
                                scrollToSectionKey("disburse_section")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("vault_disburse_max_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = CleanPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (availablePayoutBalanceUsd > 0.0) "Disburse Max" else "New Payout",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.openPayoutHistoryScreen() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("vault_view_history_btn"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanOnPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ledger (${payoutWireHistory.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2A. OWNER PAYOUT ACTION BUTTONS HUB
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_payout_action_buttons_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK PAYOUT CONTROLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Active: ${activeMethod.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanPrimary
                        )
                    }

                    // Primary Rail Switch Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeMethod = PayoutMethodType.BANK_WIRE
                                viewModel.selectPayoutMethod(PayoutMethodType.BANK_WIRE)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("payout_hub_btn_wire"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeMethod == PayoutMethodType.BANK_WIRE) CleanPrimary else CleanSurfaceVariant,
                                contentColor = if (activeMethod == PayoutMethodType.BANK_WIRE) CleanOnPrimary else CleanTextPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bank Wire", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = {
                                activeMethod = PayoutMethodType.PAYPAL
                                viewModel.selectPayoutMethod(PayoutMethodType.PAYPAL)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("payout_hub_btn_paypal"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeMethod == PayoutMethodType.PAYPAL) CleanPrimary else CleanSurfaceVariant,
                                contentColor = if (activeMethod == PayoutMethodType.PAYPAL) CleanOnPrimary else CleanTextPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PayPal", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = {
                                activeMethod = PayoutMethodType.STRIPE_PAYOUT
                                viewModel.selectPayoutMethod(PayoutMethodType.STRIPE_PAYOUT)
                                viewModel.refreshStripeBalanceAndPayouts()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("payout_hub_btn_stripe"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) CleanSecondary else CleanSurfaceVariant,
                                contentColor = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) CleanOnPrimary else CleanTextPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stripe", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Instant Action Row: Simulate iDEAL Wire + Sync Stripe API
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { triggerQuickSimulatedWire() },
                            enabled = !isSimulatingWire && !isDisbursing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("payout_hub_btn_simulate_ideal"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CleanOrange.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanOrangeText),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SyncAlt, contentDescription = null, tint = CleanOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSimulatingWire) "Simulating..." else "Simulate iDEAL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.refreshStripeBalanceAndPayouts()
                                scrollToSectionKey("stripe_section")
                            },
                            enabled = !stripePayoutState.isLoadingBalance,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("payout_hub_btn_sync_stripe"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CleanSecondary.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanSecondary),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (stripePayoutState.isLoadingBalance) "Syncing..." else "Sync Stripe API",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Utility Shortcuts Bar
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.openPayoutHistoryScreen() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("payout_hub_btn_history"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Full History", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { showPayoutExportDialog = true },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("payout_hub_btn_export"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Export CSV/JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = {
                                    if (payoutWireHistory.isNotEmpty()) {
                                        scrollToSectionKey("trends_section")
                                    } else {
                                        viewModel.openPayoutHistoryScreen()
                                    }
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("payout_hub_btn_trends"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Daily Trends", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { showEditCredentialsDialog = true },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("payout_hub_btn_edit_accounts"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Edit Accounts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.openStripeUi() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("payout_hub_btn_stripe_ui"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = StripeIndigoContainer,
                                    contentColor = StripeIndigo
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Stripe UI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { viewModel.openPayoutModal() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("payout_hub_btn_modal"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Rail Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2B. OWNER LIVE BANK ACCOUNT (ING BANK NETHERLANDS)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_ing_bank_live_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.5.dp, CleanOrange.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row with ING branding & Live status badge
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
                                    .background(CleanOrangeContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = "ING Bank",
                                    tint = CleanOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = payoutConfig.bankWire.bankName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary
                                    )
                                    Text(text = "🇳🇱", fontSize = 14.sp)
                                }
                                Text(
                                    text = "Owner Primary Bank Account",
                                    fontSize = 11.sp,
                                    color = CleanTextSecondary
                                )
                            }
                        }

                        // Live connected badge
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
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CleanSuccess)
                                )
                                Text(
                                    text = "LIVE CONNECTED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CleanSuccess,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CleanBorderSubtle)

                    // Account Details Grid (Holder, IBAN, BIC)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Account Holder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Account Holder", fontSize = 11.sp, color = CleanTextMuted)
                            Text(
                                text = payoutConfig.bankWire.accountHolderName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextPrimary
                            )
                        }

                        // IBAN with Copy
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CleanSurfaceVariant,
                            border = BorderStroke(1.dp, CleanBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "IBAN (NETHERLANDS)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = payoutConfig.bankWire.accountNumberOrIban,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(payoutConfig.bankWire.accountNumberOrIban))
                                        Toast.makeText(context, "IBAN copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy IBAN",
                                        tint = CleanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // BIC / SWIFT with Copy & Clearing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CleanSurfaceVariant,
                                border = BorderStroke(1.dp, CleanBorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "BIC / SWIFT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextMuted,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = payoutConfig.bankWire.swiftBic,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CleanTextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(payoutConfig.bankWire.swiftBic))
                                            Toast.makeText(context, "BIC copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy BIC",
                                            tint = CleanPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CleanSurfaceVariant,
                                border = BorderStroke(1.dp, CleanBorderSubtle),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "CLEARING / SEPA",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanTextMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "SEPA / iDEAL Wire",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CleanTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Quick Action: Pre-fill & switch to Wire or Disburse
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeMethod = PayoutMethodType.BANK_WIRE
                                viewModel.selectPayoutMethod(PayoutMethodType.BANK_WIRE)
                                if (availablePayoutBalanceUsd > 0.0) {
                                    payoutAmountInput = String.format(Locale.US, "%.2f", availablePayoutBalanceUsd)
                                }
                                Toast.makeText(context, "Selected ING Bank (${payoutConfig.bankWire.accountNumberOrIban})", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CleanOrange,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Disburse to ING Live",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showEditCredentialsDialog = true },
                            modifier = Modifier.height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CleanBorder)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = CleanTextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2C. LIVE VIEW & ACTIONS TELEMETRY PANEL
        item {
            OwnerPayoutLiveTelemetryView(
                viewModel = viewModel,
                availableBalanceUsd = availablePayoutBalanceUsd,
                payoutConfig = payoutConfig,
                currencyFormatter = currencyFormatter,
                onQuickAmountSelected = { amountStr ->
                    payoutAmountInput = amountStr
                    activeMethod = PayoutMethodType.BANK_WIRE
                    viewModel.selectPayoutMethod(PayoutMethodType.BANK_WIRE)
                },
                onInstantDisburseRequested = { fullAmount ->
                    activeMethod = PayoutMethodType.BANK_WIRE
                    viewModel.selectPayoutMethod(PayoutMethodType.BANK_WIRE)
                    payoutAmountInput = String.format(Locale.US, "%.2f", fullAmount)
                    isDisbursing = true
                    scope.launch {
                        delay(500)
                        viewModel.disbursePayout(
                            method = PayoutMethodType.BANK_WIRE,
                            amountInUsd = fullAmount,
                            currencyCode = selectedPayoutCurrency.code,
                            recipientName = payoutConfig.bankWire.accountHolderName,
                            destinationIdentifier = payoutConfig.bankWire.accountNumberOrIban,
                            notes = "Instant live vault settlement to ${payoutConfig.bankWire.bankName} (${payoutConfig.bankWire.swiftBic})",
                            onSuccess = {
                                lastSuccessRef = viewModel.lastDisbursedPayoutReference.value ?: "PO-LIVE-SETTLED"
                                isDisbursing = false
                                Toast.makeText(
                                    context,
                                    "Disbursed $${String.format(Locale.US, "%.2f", fullAmount)} to ING Bank Live!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            )
        }

        // 3B. LIVE STRIPE PAYOUTS API CONSOLE (v1/balance, v1/payouts)
        item {
            StripeLivePayoutCard(
                stripeState = stripePayoutState,
                amountInUsd = parsedAmount,
                currencyCode = selectedPayoutCurrency.code,
                currencyFormatter = currencyFormatter,
                onSelectSpeed = { speed -> viewModel.setStripePayoutSpeed(speed) },
                onSyncStripe = { viewModel.refreshStripeBalanceAndPayouts() },
                onVerifyConnection = { viewModel.triggerVerifyStripeConnection() },
                onSelectDestination = { destId -> viewModel.selectStripeDestination(destId) },
                onExecuteStripePayout = {
                    viewModel.executeLiveStripePayout(
                        amountInUsd = parsedAmount,
                        currencyCode = selectedPayoutCurrency.code,
                        speedMethod = stripePayoutState.selectedSpeedMethod,
                        onResult = { ok, msg ->
                            if (ok) {
                                lastSuccessRef = viewModel.lastDisbursedPayoutReference.value
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onCancelStripePayout = { payoutId ->
                    viewModel.cancelPendingStripePayout(payoutId) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 3. SUCCESS BANNER (IF DISBURSED)
        if (lastSuccessRef != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CleanTertiaryContainer,
                    border = BorderStroke(1.dp, CleanTertiary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CleanTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Payout Successfully Dispatched!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanOnTertiaryContainer
                                )
                                Text(
                                    text = "Ref: $lastSuccessRef • Saved to SQLite Ledger",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CleanOnTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    lastSuccessRef?.let {
                                        clipboardManager.setText(AnnotatedString(it))
                                        Toast.makeText(context, "Copied ref $it", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("payout_success_copy_ref_button")
                            ) {
                                Text("Copy Ref", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanTertiary)
                            }
                            IconButton(
                                onClick = { lastSuccessRef = null },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = CleanOnTertiaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. INSTANT DISBURSEMENT ACTION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanSurface),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Disburse Revenue Payout",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Direct interbank wire or instant PayPal transfer",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }

                        TextButton(
                            onClick = { showEditCredentialsDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = CleanPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Accounts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CleanPrimary)
                        }
                    }

                    // A. Payout Method Selector (Bank Wire vs PayPal vs Stripe Live)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bank Wire Pill
                        val isWire = activeMethod == PayoutMethodType.BANK_WIRE
                        Surface(
                            onClick = {
                                activeMethod = PayoutMethodType.BANK_WIRE
                                viewModel.selectPayoutMethod(PayoutMethodType.BANK_WIRE)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isWire) CleanPrimaryContainer else CleanSurfaceVariant,
                            border = BorderStroke(1.5.dp, if (isWire) CleanPrimary else CleanBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_payout_select_wire")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = if (isWire) CleanPrimary else CleanTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (isWire) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = CleanPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Bank Wire",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWire) CleanOnPrimaryContainer else CleanTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "SWIFT / IBAN",
                                    fontSize = 9.sp,
                                    color = if (isWire) CleanPrimary else CleanTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        // PayPal Pill
                        val isPayPal = activeMethod == PayoutMethodType.PAYPAL
                        Surface(
                            onClick = {
                                activeMethod = PayoutMethodType.PAYPAL
                                viewModel.selectPayoutMethod(PayoutMethodType.PAYPAL)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPayPal) CleanPrimaryContainer else CleanSurfaceVariant,
                            border = BorderStroke(1.5.dp, if (isPayPal) CleanPrimary else CleanBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_payout_select_paypal")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = if (isPayPal) CleanPrimary else CleanTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (isPayPal) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = CleanPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "PayPal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayPal) CleanOnPrimaryContainer else CleanTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Digital Wallet",
                                    fontSize = 9.sp,
                                    color = if (isPayPal) CleanPrimary else CleanTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        // Stripe Live Payout Pill
                        val isStripe = activeMethod == PayoutMethodType.STRIPE_PAYOUT
                        Surface(
                            onClick = {
                                activeMethod = PayoutMethodType.STRIPE_PAYOUT
                                viewModel.selectPayoutMethod(PayoutMethodType.STRIPE_PAYOUT)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isStripe) CleanSecondaryContainer else CleanSurfaceVariant,
                            border = BorderStroke(1.5.dp, if (isStripe) CleanSecondary else CleanBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_payout_select_stripe")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (isStripe) CleanSecondary else CleanTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (isStripe) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = CleanSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Stripe Live",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStripe) CleanSecondary else CleanTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "v1/payouts API",
                                    fontSize = 9.sp,
                                    color = if (isStripe) CleanSecondary else CleanTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // B. Active Destination Credential Preview
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CleanSurfaceSecondary,
                        border = BorderStroke(1.dp, CleanBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CleanSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (activeMethod) {
                                        PayoutMethodType.BANK_WIRE -> Icons.Default.AccountBalance
                                        PayoutMethodType.PAYPAL -> Icons.Default.Payments
                                        PayoutMethodType.STRIPE_PAYOUT -> Icons.Default.Bolt
                                    },
                                    contentDescription = null,
                                    tint = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) CleanSecondary else CleanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (activeMethod) {
                                        PayoutMethodType.BANK_WIRE -> "${payoutConfig.bankWire.bankName} • ${payoutConfig.bankWire.accountHolderName}"
                                        PayoutMethodType.PAYPAL -> "PayPal • ${payoutConfig.payPal.paypalEmailOrId}"
                                        PayoutMethodType.STRIPE_PAYOUT -> stripePayoutState.accountBusinessName
                                            ?: "Stripe Live (${stripePayoutState.environmentLabel})"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = when (activeMethod) {
                                        PayoutMethodType.BANK_WIRE -> "IBAN: ${payoutConfig.bankWire.accountNumberOrIban} • BIC: ${payoutConfig.bankWire.swiftBic}"
                                        PayoutMethodType.PAYPAL -> "Recipient: ${payoutConfig.payPal.recipientName} • ${payoutConfig.payPal.phoneOrPaypalMe}"
                                        PayoutMethodType.STRIPE_PAYOUT -> "Health: ${stripePayoutState.connectionHealth.label} • Rail: ${stripePayoutState.selectedSpeedMethod.uppercase()} • ${stripePayoutState.maskedKeyPreview}"
                                    },
                                    fontSize = 11.sp,
                                    color = CleanTextSecondary
                                )
                            }
                            if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) {
                                TextButton(
                                    onClick = { viewModel.triggerVerifyStripeConnection() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("disburse_card_verify_stripe_btn")
                                ) {
                                    Text(
                                        text = if (stripePayoutState.isCheckingConnection) "..." else "Verify",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Inline Stripe Live Speed & Destination Controls when Stripe Live is active
                    AnimatedVisibility(visible = activeMethod == PayoutMethodType.STRIPE_PAYOUT) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("disburse_card_stripe_inline_controls"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isStd = stripePayoutState.selectedSpeedMethod == "standard"
                                OutlinedButton(
                                    onClick = { viewModel.setStripePayoutSpeed("standard") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("disburse_stripe_speed_standard"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isStd) CleanSecondaryContainer else Color.Transparent,
                                        contentColor = if (isStd) CleanSecondary else CleanTextSecondary
                                    ),
                                    border = BorderStroke(1.dp, if (isStd) CleanSecondary else CleanBorderSubtle)
                                ) {
                                    Text("Standard ($0 Fee)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                val isInst = stripePayoutState.selectedSpeedMethod == "instant"
                                OutlinedButton(
                                    onClick = { viewModel.setStripePayoutSpeed("instant") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("disburse_stripe_speed_instant"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isInst) CleanOrangeContainer else Color.Transparent,
                                        contentColor = if (isInst) CleanOrangeText else CleanTextSecondary
                                    ),
                                    border = BorderStroke(1.dp, if (isInst) CleanOrange else CleanBorderSubtle)
                                ) {
                                    Text("Instant (1% Fee)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (stripePayoutState.externalAccounts.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(stripePayoutState.externalAccounts, key = { it.id }) { ext ->
                                        val isDestSelected = stripePayoutState.selectedDestinationId == ext.id
                                        FilterChip(
                                            selected = isDestSelected,
                                            onClick = { viewModel.selectStripeDestination(ext.id) },
                                            label = {
                                                Text(
                                                    text = "${ext.label} ••••${ext.last4} (${ext.currencyCode})",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            modifier = Modifier.testTag("disburse_stripe_dest_${ext.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // C. Amount Input & Preset Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DISBURSEMENT AMOUNT",
                            fontSize = 11.sp,
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
                                value = payoutAmountInput,
                                onValueChange = { payoutAmountInput = it },
                                leadingIcon = {
                                    Text(
                                        text = "$",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = CleanPrimary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("owner_payout_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CleanPrimary,
                                    unfocusedBorderColor = CleanBorder,
                                    focusedContainerColor = CleanSurface,
                                    unfocusedContainerColor = CleanSurface
                                )
                            )

                            // Currency selector trigger
                            Box {
                                Surface(
                                    onClick = { isCurrencyMenuOpen = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = CleanSurfaceVariant,
                                    border = BorderStroke(1.dp, CleanBorder),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = selectedPayoutCurrency.code, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = isCurrencyMenuOpen,
                                    onDismissRequest = { isCurrencyMenuOpen = false }
                                ) {
                                    viewModel.currencies.forEach { curr ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${curr.code} (${curr.symbol}) - ${curr.defaultCountry}")
                                            },
                                            onClick = {
                                                selectedPayoutCurrency = curr
                                                isCurrencyMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Preset Amount Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("100.00", "250.00", "500.00", "1000.00").forEach { preset ->
                                val isSelected = payoutAmountInput == preset
                                Surface(
                                    onClick = { payoutAmountInput = preset },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) CleanPrimaryContainer else CleanSurfaceVariant,
                                    border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$${preset.substringBefore(".")}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) CleanPrimary else CleanTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Balance Percentage Split & Clear Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "25%" to 0.25,
                                "50%" to 0.50,
                                "75%" to 0.75,
                                "MAX" to 1.00
                            ).forEach { (label, factor) ->
                                OutlinedButton(
                                    onClick = {
                                        val computed = (availablePayoutBalanceUsd * factor).coerceAtLeast(0.0)
                                        if (computed > 0.0) {
                                            payoutAmountInput = String.format(Locale.US, "%.2f", computed)
                                        } else {
                                            Toast.makeText(context, "Available balance is $0.00 — enter custom amount", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("payout_split_${label.lowercase().replace("%", "")}"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    border = BorderStroke(1.dp, CleanBorderSubtle)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanPrimary
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { payoutAmountInput = "" },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("payout_split_clear"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = BorderStroke(1.dp, CleanBorderSubtle)
                            ) {
                                Text(
                                    text = "Clear",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanTextSecondary
                                )
                            }
                        }
                    }

                    // D. Fee & Net Conversion Breakdown
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CleanSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Gross Disbursed:", fontSize = 12.sp, color = CleanTextSecondary)
                                Text(text = currencyFormatter.formatUsd(parsedAmount), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CleanTextPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (activeMethod) {
                                        PayoutMethodType.BANK_WIRE -> "Interbank Wire Fee:"
                                        PayoutMethodType.PAYPAL -> "PayPal Processing Fee:"
                                        PayoutMethodType.STRIPE_PAYOUT -> "Stripe Payout Fee (${stripePayoutState.selectedSpeedMethod.uppercase()}):"
                                    },
                                    fontSize = 12.sp,
                                    color = CleanTextSecondary
                                )
                                Text(
                                    text = "-${currencyFormatter.formatUsd(calculatedFee)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanDanger
                                )
                            }

                            HorizontalDivider(color = CleanBorderSubtle)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Net Received by Owner:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CleanTextPrimary)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = currencyFormatter.formatUsd(netDisbursement),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CleanTertiary
                                    )
                                    val netConverted = netDisbursement * selectedPayoutCurrency.exchangeRate
                                    Text(
                                        text = "≈ ${currencyFormatter.format(netConverted, selectedPayoutCurrency.code)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CleanTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // E. Action Button
                    Button(
                        onClick = {
                            if (parsedAmount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid payout amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) {
                                isDisbursing = true
                                viewModel.executeLiveStripePayout(
                                    amountInUsd = parsedAmount,
                                    currencyCode = selectedPayoutCurrency.code,
                                    speedMethod = stripePayoutState.selectedSpeedMethod,
                                    onResult = { ok, msg ->
                                        isDisbursing = false
                                        if (ok) {
                                            lastSuccessRef = viewModel.lastDisbursedPayoutReference.value ?: "PO-STRIPE"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                                return@Button
                            }
                            isDisbursing = true
                            scope.launch {
                                delay(600) // Quick smooth UI transition
                                val recipient = if (activeMethod == PayoutMethodType.BANK_WIRE) payoutConfig.bankWire.accountHolderName else payoutConfig.payPal.recipientName
                                val destination = if (activeMethod == PayoutMethodType.BANK_WIRE) payoutConfig.bankWire.accountNumberOrIban else payoutConfig.payPal.paypalEmailOrId
                                val notes = if (activeMethod == PayoutMethodType.BANK_WIRE) {
                                    "Owner settlement to ${payoutConfig.bankWire.bankName} (${payoutConfig.bankWire.swiftBic})"
                                } else {
                                    "Owner instant digital wallet payout to ${payoutConfig.payPal.phoneOrPaypalMe}"
                                }

                                viewModel.disbursePayout(
                                    method = activeMethod,
                                    amountInUsd = parsedAmount,
                                    currencyCode = selectedPayoutCurrency.code,
                                    recipientName = recipient,
                                    destinationIdentifier = destination,
                                    notes = notes,
                                    onSuccess = { _ ->
                                        lastSuccessRef = viewModel.lastDisbursedPayoutReference.value ?: "PO-SETTLED"
                                        isDisbursing = false
                                        Toast.makeText(context, "${activeMethod.displayName} of $${String.format(Locale.US, "%.2f", parsedAmount)} settled!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        enabled = !isDisbursing && parsedAmount > 0.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("owner_disburse_action_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) CleanSecondary else CleanPrimary
                        )
                    ) {
                        if (isDisbursing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CleanOnPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (activeMethod == PayoutMethodType.STRIPE_PAYOUT) "Calling Stripe Payout API..." else "Settling Payout...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = when (activeMethod) {
                                    PayoutMethodType.BANK_WIRE -> Icons.Default.AccountBalance
                                    PayoutMethodType.PAYPAL -> Icons.Default.Payments
                                    PayoutMethodType.STRIPE_PAYOUT -> Icons.Default.Bolt
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Disburse ${currencyFormatter.formatUsd(parsedAmount)} via ${activeMethod.displayName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // F. Simulate iDEAL / SEPA Bank Wire Action Button
                    OutlinedButton(
                        onClick = { triggerQuickSimulatedWire() },
                        enabled = !isSimulatingWire && !isDisbursing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("simulate_ideal_bank_wire_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CleanOrange.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanOrangeText)
                    ) {
                        if (isSimulatingWire) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CleanOrange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Simulating Bank Wire...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = CleanOrange,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulate Bank Wire (iDEAL / SEPA)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4B. DAILY PAYOUT TRENDS CHART (ROOM DATABASE)
        if (payoutWireHistory.isNotEmpty()) {
            item {
                PayoutDailyTrendsChart(
                    records = payoutWireHistory,
                    currencyFormatter = currencyFormatter
                )
            }
        }

        // 5. OWNER SETTLEMENT AUDIT HISTORY (ROOM DATABASE: payout_wire_history)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAYOUT & BANK WIRE HISTORY (${filteredPayoutWires.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.8.sp
                    )

                    if (payoutWireHistory.isNotEmpty()) {
                        TextButton(
                            onClick = { showPayoutExportDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("owner_payout_export_button")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = CleanPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Export CSV / JSON", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanPrimary)
                        }
                    }
                }

                // Filter Chips for Payouts vs Stripe vs Simulated Wires vs iDEAL
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        listOf(
                            "ALL" to "All (${payoutWireHistory.size})",
                            "STRIPE" to "Stripe",
                            "PAYOUTS" to "Payouts",
                            "SIM_WIRES" to "Sim Wires",
                            "IDEAL" to "iDEAL / SEPA"
                        ),
                        key = { it.first }
                    ) { (key, label) ->
                        val isSelected = historyFilter == key
                        Surface(
                            onClick = { historyFilter = key },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CleanPrimaryContainer else CleanSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorderSubtle),
                            modifier = Modifier.testTag("payout_history_filter_$key")
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CleanPrimary else CleanTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (filteredPayoutWires.isEmpty() && payoutLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurface),
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "No Payout or Wire History Yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Text(
                            text = "Disburse a payout or run a simulated Bank Wire / iDEAL transfer above to store it in Room.",
                            fontSize = 12.sp,
                            color = CleanTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { triggerQuickSimulatedWire() },
                                enabled = !isSimulatingWire && !isDisbursing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("owner_empty_simulate_wire_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CleanOrange)
                            ) {
                                Text("Simulate Wire", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.refreshStripeBalanceAndPayouts() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("owner_empty_sync_stripe_btn"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CleanSecondary)
                            ) {
                                Text("Sync Stripe", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanSecondary)
                            }
                        }
                    }
                }
            }
        } else if (filteredPayoutWires.isNotEmpty()) {
            items(
                items = filteredPayoutWires,
                key = { "wire_${it.id}" }
            ) { wireRecord ->
                PayoutWireHistoryCard(
                    record = wireRecord,
                    currencyFormatter = currencyFormatter,
                    onToggleStatus = {
                        val nextStatus = if (wireRecord.status == "SETTLED") "COMPLETED" else "SETTLED"
                        viewModel.updatePayoutWireStatus(wireRecord.id, nextStatus)
                    }
                )
            }
        } else {
            items(
                items = payoutLogs,
                key = { it.id }
            ) { log ->
                OwnerPayoutHistoryCard(
                    log = log,
                    currencyFormatter = currencyFormatter,
                    onClick = { viewModel.selectLog(log) }
                )
            }
        }
    }

    // 6. EDIT CREDENTIALS MODAL
    if (showEditCredentialsDialog) {
        OwnerEditCredentialsDialog(
            viewModel = viewModel,
            currentConfig = payoutConfig,
            onDismiss = { showEditCredentialsDialog = false }
        )
    }

    // 7. PAYOUT HISTORY CSV / JSON CLIPBOARD EXPORT MODAL
    if (showPayoutExportDialog) {
        PayoutHistoryExportDialog(
            allRecords = payoutWireHistory,
            currencyFormatter = currencyFormatter,
            onDismiss = { showPayoutExportDialog = false }
        )
    }
}

/**
 * Dedicated Room-backed Payout & Simulated Bank Wire Card
 */
@Composable
fun PayoutWireHistoryCard(
    record: PayoutWireEntity,
    currencyFormatter: CurrencyFormatter,
    onToggleStatus: () -> Unit
) {
    val dateString = remember(record.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }
    val isStripe = record.method.equals("STRIPE_PAYOUT", ignoreCase = true)
    val isBankOrIdeal = record.method != "PAYPAL" && !isStripe

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onToggleStatus() }
            .testTag("payout_wire_history_card_${record.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(
            1.dp,
            when {
                record.isSimulatedWire -> CleanOrange.copy(alpha = 0.45f)
                isStripe -> CleanSecondary.copy(alpha = 0.45f)
                else -> CleanBorder
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    record.isSimulatedWire -> CleanOrangeContainer
                                    isStripe -> CleanSecondaryContainer
                                    isBankOrIdeal -> CleanPrimaryContainer
                                    else -> CleanSecondaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isStripe -> Icons.Default.Bolt
                                isBankOrIdeal -> Icons.Default.AccountBalance
                                else -> Icons.Default.Payments
                            },
                            contentDescription = null,
                            tint = when {
                                record.isSimulatedWire -> CleanOrange
                                isStripe -> CleanSecondary
                                isBankOrIdeal -> CleanPrimary
                                else -> CleanSecondary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = record.clearingRail,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    record.isSimulatedWire -> CleanOrangeContainer
                                    isStripe -> CleanSecondaryContainer
                                    else -> CleanPrimaryContainer
                                }
                            ) {
                                Text(
                                    text = when {
                                        record.isSimulatedWire -> "SIM WIRE"
                                        isStripe -> "STRIPE"
                                        else -> "PAYOUT"
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        record.isSimulatedWire -> CleanOrangeText
                                        isStripe -> CleanSecondary
                                        else -> CleanPrimary
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$dateString • ${record.referenceCode}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CleanTextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.formatUsd(record.netAmountUsd),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            record.isSimulatedWire -> CleanOrange
                            isStripe -> CleanSecondary
                            else -> CleanPrimary
                        }
                    )
                    Text(
                        text = "≈ ${currencyFormatter.format(record.convertedAmount, record.currencyCode)}",
                        fontSize = 10.sp,
                        color = CleanTextSecondary
                    )
                }
            }

            HorizontalDivider(color = CleanBorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${record.bankName} • ${record.ibanOrAccount}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CleanTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val itemClipboard = LocalClipboardManager.current
                    val itemContext = LocalContext.current
                    TextButton(
                        onClick = {
                            itemClipboard.setText(AnnotatedString(record.referenceCode))
                            Toast.makeText(itemContext, "Copied ${record.referenceCode}", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("payout_wire_copy_ref_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy reference code",
                            tint = CleanPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Copy Ref", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CleanPrimary)
                    }

                    Surface(
                        onClick = onToggleStatus,
                        shape = RoundedCornerShape(8.dp),
                        color = CleanSuccessContainer,
                        border = BorderStroke(1.dp, CleanSuccess.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("payout_wire_toggle_status_${record.id}")
                    ) {
                        Text(
                            text = record.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanSuccess,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Settlement Card for Owner Payout History
 */
@Composable
fun OwnerPayoutHistoryCard(
    log: TransactionLogEntity,
    currencyFormatter: CurrencyFormatter,
    onClick: () -> Unit
) {
    val dateString = remember(log.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(log.timestamp))
    }

    val isWire = log.paymentMethod.contains("Wire", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("owner_payout_history_${log.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isWire) CleanPrimaryContainer else CleanSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWire) Icons.Default.AccountBalance else Icons.Default.Payments,
                        contentDescription = null,
                        tint = if (isWire) CleanPrimary else CleanSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = log.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateString • ${log.referenceCode}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CleanTextSecondary
                    )
                    if (!log.barcodeOrRawValue.isNullOrBlank()) {
                        Text(
                            text = "Destination: ${log.barcodeOrRawValue}",
                            fontSize = 10.sp,
                            color = CleanTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormatter.formatUsd(log.amountInUsd),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CleanPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CleanTertiaryContainer
                ) {
                    Text(
                        text = log.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanOnTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Edit Owner Settlement Accounts Dialog
 */
@Composable
fun OwnerEditCredentialsDialog(
    viewModel: GlobalMarketViewModel,
    currentConfig: MerchantPayoutConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bankName by remember { mutableStateOf(currentConfig.bankWire.bankName) }
    var accountHolder by remember { mutableStateOf(currentConfig.bankWire.accountHolderName) }
    var ibanOrAccount by remember { mutableStateOf(currentConfig.bankWire.accountNumberOrIban) }
    var swiftBic by remember { mutableStateOf(currentConfig.bankWire.swiftBic) }
    var routingNumber by remember { mutableStateOf(currentConfig.bankWire.routingNumber) }
    var bankCountry by remember { mutableStateOf(currentConfig.bankWire.country) }

    var paypalEmail by remember { mutableStateOf(currentConfig.payPal.paypalEmailOrId) }
    var paypalRecipient by remember { mutableStateOf(currentConfig.payPal.recipientName) }
    var paypalMe by remember { mutableStateOf(currentConfig.payPal.phoneOrPaypalMe) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Owner Payout Credentials",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CleanTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "BANK WIRE (SWIFT / IBAN / ACH)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanPrimary
                )
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it },
                    label = { Text("Account Holder") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ibanOrAccount,
                    onValueChange = { ibanOrAccount = it },
                    label = { Text("IBAN or Account Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = swiftBic,
                        onValueChange = { swiftBic = it },
                        label = { Text("SWIFT / BIC") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = routingNumber,
                        onValueChange = { routingNumber = it },
                        label = { Text("Routing #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = CleanBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "PAYPAL DIGITAL WALLET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanPrimary
                )
                OutlinedTextField(
                    value = paypalEmail,
                    onValueChange = { paypalEmail = it },
                    label = { Text("PayPal Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paypalRecipient,
                    onValueChange = { paypalRecipient = it },
                    label = { Text("Recipient Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paypalMe,
                    onValueChange = { paypalMe = it },
                    label = { Text("PayPal.Me handle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateBankWireDetails(
                        BankWireDetails(
                            bankName = bankName,
                            accountHolderName = accountHolder,
                            accountNumberOrIban = ibanOrAccount,
                            swiftBic = swiftBic,
                            routingNumber = routingNumber,
                            country = bankCountry
                        )
                    )
                    viewModel.updatePayPalDetails(
                        PayPalDetails(
                            paypalEmailOrId = paypalEmail,
                            recipientName = paypalRecipient,
                            phoneOrPaypalMe = paypalMe
                        )
                    )
                    Toast.makeText(context, "Owner payout credentials saved!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
            ) {
                Text("Save Credentials", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CleanTextSecondary)
            }
        }
    )
}
