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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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

    val totalDisbursedUsd = remember(payoutLogs) {
        payoutLogs.sumOf { it.amountInUsd }
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
    var lastSuccessRef by remember { mutableStateOf<String?>(null) }
    var showEditCredentialsDialog by remember { mutableStateOf(false) }

    val parsedAmount = payoutAmountInput.toDoubleOrNull() ?: 0.0
    val calculatedFee = viewModel.calculatePayoutFee(activeMethod, parsedAmount)
    val netDisbursement = (parsedAmount - calculatedFee).coerceAtLeast(0.0)

    LazyColumn(
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CleanSurfaceVariant)
                                    .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                                    .testTag("owner_payout_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
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

                    // Direct Full Modal Trigger Button
                    IconButton(
                        onClick = { viewModel.openPayoutModal() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CleanPrimaryContainer)
                            .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                            .testTag("owner_open_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Open Full Modal",
                            tint = CleanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
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
                                text = "${payoutLogs.size} records",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimary
                            )
                        }
                    }
                }
            }
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

                        IconButton(
                            onClick = { lastSuccessRef = null },
                            modifier = Modifier.size(28.dp)
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

                    // A. Payout Method Selector (Bank Wire vs PayPal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                modifier = Modifier.padding(14.dp),
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isWire) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = CleanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Bank Wire",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWire) CleanOnPrimaryContainer else CleanTextPrimary
                                )
                                Text(
                                    text = "SWIFT / IBAN / ACH",
                                    fontSize = 10.sp,
                                    color = if (isWire) CleanPrimary else CleanTextSecondary
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
                                modifier = Modifier.padding(14.dp),
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isPayPal) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = CleanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "PayPal Wallet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayPal) CleanOnPrimaryContainer else CleanTextPrimary
                                )
                                Text(
                                    text = "Instant Digital Wallet",
                                    fontSize = 10.sp,
                                    color = if (isPayPal) CleanPrimary else CleanTextSecondary
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
                                    imageVector = if (activeMethod == PayoutMethodType.BANK_WIRE) Icons.Default.AccountBalance else Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (activeMethod == PayoutMethodType.BANK_WIRE) {
                                        "${payoutConfig.bankWire.bankName} • ${payoutConfig.bankWire.accountHolderName}"
                                    } else {
                                        "PayPal • ${payoutConfig.payPal.paypalEmailOrId}"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanTextPrimary
                                )
                                Text(
                                    text = if (activeMethod == PayoutMethodType.BANK_WIRE) {
                                        "IBAN: ••••${payoutConfig.bankWire.accountNumberOrIban.takeLast(6)} • SWIFT: ${payoutConfig.bankWire.swiftBic}"
                                    } else {
                                        "Recipient: ${payoutConfig.payPal.recipientName} • ${payoutConfig.payPal.phoneOrPaypalMe}"
                                    },
                                    fontSize = 11.sp,
                                    color = CleanTextSecondary
                                )
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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
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
                                    text = if (activeMethod == PayoutMethodType.BANK_WIRE) "Interbank Wire Fee:" else "PayPal Processing Fee:",
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
                                    onSuccess = { created ->
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
                        colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                    ) {
                        if (isDisbursing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CleanOnPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Settling Payout...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Icon(
                                imageVector = if (activeMethod == PayoutMethodType.BANK_WIRE) Icons.Default.AccountBalance else Icons.Default.Payments,
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
                }
            }
        }

        // 5. OWNER SETTLEMENT AUDIT HISTORY (ROOM DATABASE)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OWNER SETTLEMENT AUDIT LEDGER (${payoutLogs.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.8.sp
                )

                if (payoutLogs.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val summary = payoutLogs.joinToString("\n") { log ->
                                "${log.referenceCode},${log.timestamp},${log.paymentMethod},${log.amountInUsd},${log.currencyCode},${log.status}"
                            }
                            clipboardManager.setText(AnnotatedString(summary))
                            Toast.makeText(context, "Settlement audit records copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = CleanPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Copy Audit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CleanPrimary)
                    }
                }
            }
        }

        if (payoutLogs.isEmpty()) {
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
                            text = "No Owner Payouts Disbursed Yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Text(
                            text = "Use the disbursement form above to trigger your first Bank Wire or PayPal settlement.",
                            fontSize = 12.sp,
                            color = CleanTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
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
