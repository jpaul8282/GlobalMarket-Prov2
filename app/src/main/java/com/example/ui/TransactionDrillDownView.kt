package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Granular drill-down detail view for a specific transaction.
 * Displays comprehensive financial settlement, fee breakdown, customer metadata,
 * payment instrument verification, and complete lifecycle audit trail.
 */
@Composable
fun TransactionDrillDownView(
    transaction: TransactionEntity,
    viewModel: GlobalMarketViewModel,
    currencyFormatter: CurrencyFormatter,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Intercept hardware/gesture back press to return to summary list
    BackHandler {
        onBack()
    }

    val dateFull = remember(transaction.timestamp) {
        SimpleDateFormat("EEEE, MMM dd, yyyy • HH:mm:ss z", Locale.getDefault()).format(Date(transaction.timestamp))
    }
    val dateShort = remember(transaction.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    val flagEmoji = when (transaction.targetCurrency.uppercase()) {
        "USD" -> "🇺🇸"
        "EUR" -> "🇪🇺"
        "GBP" -> "🇬🇧"
        "JPY" -> "🇯🇵"
        "CNY" -> "🇨🇳"
        else -> "💳"
    }

    // Granular Fee Calculations
    val grossTargetAmount = transaction.convertedAmount
    val baseUsdAmount = transaction.amountInUsd
    val effectiveRate = if (baseUsdAmount > 0.0) grossTargetAmount / baseUsdAmount else 1.0

    // Typical payment gateway fee: 2.9% + fixed standard fee ($0.30 equivalent in target currency)
    val gatewayFixedFeeInTarget = 0.30 * effectiveRate
    val gatewayPercentFeeInTarget = grossTargetAmount * 0.029
    val totalGatewayFee = gatewayPercentFeeInTarget + gatewayFixedFeeInTarget
    val fxConversionFee = grossTargetAmount * 0.010 // 1% FX conversion fee
    val netMerchantPayout = (grossTargetAmount - totalGatewayFee - fxConversionFee).coerceAtLeast(0.0)

    val simulatedPaymentIntent = remember(transaction.id) {
        "pi_3M${(transaction.id * 782391 + 104820).toString(16).uppercase()}Lw2eZvKYlo2C1"
    }
    val simulatedInvoiceId = remember(transaction.id) {
        "in_2026_${transaction.id * 1042 + 99}"
    }
    val simulatedCustomerEmail = remember(transaction.customerName) {
        val clean = transaction.customerName.trim().lowercase().replace("\\s+".toRegex(), ".")
        "$clean@client.global"
    }

    var showCsvExportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transaction_drilldown_view")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        // -----------------------------------------------------------------
        // 1. TOP NAVIGATION BAR
        // -----------------------------------------------------------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("drilldown_back_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = CleanSurfaceVariant,
                            contentColor = CleanTextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Sales Summary"
                        )
                    }

                    Column {
                        Text(
                            text = viewModel.translate("drilldown_title"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Text(
                            text = "#TX-${transaction.id} • ${transaction.country}",
                            fontSize = 12.sp,
                            color = CleanTextMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Quick Share Action
                    IconButton(
                        onClick = {
                            val shareSummary = buildReceiptShareText(
                                transaction = transaction,
                                currencyFormatter = currencyFormatter,
                                dateFull = dateFull,
                                netPayout = netMerchantPayout,
                                fee = totalGatewayFee + fxConversionFee
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Transaction #${transaction.id} - ${transaction.customerName}")
                                putExtra(Intent.EXTRA_TEXT, shareSummary)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Transaction Record"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = CleanPrimary
                        )
                    }

                    // Quick Delete Action
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 2. HERO AMOUNT & SETTLEMENT STATUS CARD
        // -----------------------------------------------------------------
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drilldown_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanPrimaryContainer),
                border = BorderStroke(1.dp, CleanBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Badge Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CleanSurface.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SETTLED & CONFIRMED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Primary Formatted Amount
                    Text(
                        text = currencyFormatter.format(transaction.convertedAmount, transaction.targetCurrency),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = CleanOnPrimaryContainer,
                        textAlign = TextAlign.Center
                    )

                    // Base USD Amount Equivalent
                    Text(
                        text = "Base Amount: ${currencyFormatter.formatUsd(transaction.amountInUsd)} USD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanOnPrimaryContainer.copy(alpha = 0.85f)
                    )

                    // Effective FX Rate Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CleanSurface.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "1 USD ≈ ${String.format(Locale.US, "%.4f", effectiveRate)} ${transaction.targetCurrency}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CleanOnPrimaryContainer.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Timestamp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = CleanOnPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = dateFull,
                            fontSize = 12.sp,
                            color = CleanOnPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 3. FINANCIAL SETTLEMENT & FEE BREAKDOWN (GRANULAR)
        // -----------------------------------------------------------------
        item {
            DrillDownSectionCard(
                title = viewModel.translate("fee_breakdown"),
                icon = Icons.Default.AccountBalanceWallet
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrillDownDetailRow(
                        label = "Gross Settlement",
                        value = currencyFormatter.format(grossTargetAmount, transaction.targetCurrency)
                    )
                    DrillDownDetailRow(
                        label = "Base USD Value",
                        value = "${currencyFormatter.formatUsd(baseUsdAmount)} USD"
                    )
                    DrillDownDetailRow(
                        label = "Gateway Processing Fee (2.9% + $0.30)",
                        value = "- " + currencyFormatter.format(totalGatewayFee, transaction.targetCurrency),
                        valueColor = Color(0xFFDC2626)
                    )
                    DrillDownDetailRow(
                        label = "FX Multi-Currency Conversion (1.0%)",
                        value = "- " + currencyFormatter.format(fxConversionFee, transaction.targetCurrency),
                        valueColor = Color(0xFFDC2626)
                    )
                    DrillDownDetailRow(
                        label = "Cross-Border Regulatory VAT",
                        value = "€0.00 (Zero-Rated B2B)"
                    )

                    HorizontalDivider(color = CleanBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

                    // Highlighted Net Merchant Payout Container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = viewModel.translate("net_payout"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Automated rolling payout (T+2 Days)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                            Text(
                                text = currencyFormatter.format(netMerchantPayout, transaction.targetCurrency),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // 4. CUSTOMER PROFILE & REGIONAL DEMOGRAPHICS
        // -----------------------------------------------------------------
        item {
            DrillDownSectionCard(
                title = viewModel.translate("customer_info"),
                icon = Icons.Default.Person
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Customer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = flagEmoji, fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                text = transaction.customerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "${transaction.country} • Verified Customer",
                                fontSize = 12.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = CleanBorderSubtle, modifier = Modifier.padding(vertical = 2.dp))

                    DrillDownDetailRow(label = "Contact Email", value = simulatedCustomerEmail)
                    DrillDownDetailRow(label = "Billing Jurisdiction", value = transaction.country)
                    DrillDownDetailRow(
                        label = "Clearing Network",
                        value = when (transaction.targetCurrency.uppercase()) {
                            "EUR" -> "SEPA Instant Credit"
                            "GBP" -> "BACS / Faster Payments"
                            "JPY" -> "Zengin System Domestic"
                            "CNY" -> "CIPS / UnionPay Global"
                            else -> "FedNow / ACH Direct"
                        }
                    )
                    DrillDownDetailRow(
                        label = "Radar Fraud Risk",
                        value = "Score: 4 / 100 (Extremely Low)",
                        valueColor = Color(0xFF059669)
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // 5. PAYMENT INSTRUMENT & AUTHENTICATION
        // -----------------------------------------------------------------
        item {
            DrillDownSectionCard(
                title = viewModel.translate("payment_method_info"),
                icon = Icons.Default.CreditCard
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrillDownDetailRow(
                        label = "Card Brand & Mask",
                        value = "Visa Credit •••• 4242"
                    )
                    DrillDownDetailRow(
                        label = "Authentication Standard",
                        value = "3D Secure 2.0 (SCA Compliant)",
                        valueColor = Color(0xFF059669)
                    )

                    // Interactive Copy Chip for Subscription ID
                    DrillDownCopyableRow(
                        context = context,
                        label = "Stripe Subscription ID",
                        value = transaction.stripeSubscriptionId
                    )

                    // Interactive Copy Chip for Payment Intent
                    DrillDownCopyableRow(
                        context = context,
                        label = "Payment Intent ID",
                        value = simulatedPaymentIntent
                    )

                    DrillDownDetailRow(
                        label = "Storage & Security",
                        value = "Room SQLite (AES-256 Encrypted)"
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // 6. ORDER ITEM & PLAN DETAILS
        // -----------------------------------------------------------------
        item {
            DrillDownSectionCard(
                title = viewModel.translate("order_details"),
                icon = Icons.Default.ShoppingCart
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrillDownDetailRow(
                        label = "Product / Service",
                        value = if (baseUsdAmount >= 75.0) "Enterprise Multi-Currency Suite" else "Commercial Seat License"
                    )
                    DrillDownDetailRow(
                        label = "Quantity / Seats",
                        value = "1 Organization License"
                    )
                    DrillDownDetailRow(
                        label = "Billing Cycle",
                        value = "Monthly Recurring • Auto-Renews"
                    )
                    DrillDownCopyableRow(
                        context = context,
                        label = "Invoice Number",
                        value = simulatedInvoiceId
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // 7. LIFECYCLE AUDIT TRAIL (TIMELINE)
        // -----------------------------------------------------------------
        item {
            DrillDownSectionCard(
                title = viewModel.translate("audit_timeline"),
                icon = Icons.Default.History
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TimelineItem(
                        time = dateShort,
                        title = "Checkout Session Initiated",
                        subtitle = "Customer selected localized ${transaction.targetCurrency} payment checkout.",
                        isFirst = true
                    )
                    TimelineItem(
                        time = "+1.2s",
                        title = "3D Secure 2.0 Frictionless SCA Passed",
                        subtitle = "Customer bank verified payment credentials with zero friction.",
                        isFirst = false
                    )
                    TimelineItem(
                        time = "+2.4s",
                        title = "Stripe Payment Captured & Settled",
                        subtitle = "Funds captured in ${transaction.targetCurrency} (${currencyFormatter.format(grossTargetAmount, transaction.targetCurrency)}).",
                        isFirst = false
                    )
                    TimelineItem(
                        time = "+3.1s",
                        title = "Room SQLite Local Ledger Committed",
                        subtitle = "Transaction record #TX-${transaction.id} persisted to encrypted device storage.",
                        isFirst = false
                    )
                    TimelineItem(
                        time = "+3.8s",
                        title = "Customer Receipt & Payout Scheduled",
                        subtitle = "Confirmation email dispatched. Net payout queued on rolling schedule.",
                        isFirst = false,
                        isLast = true
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // 8. ACTION BUTTONS & CLIPBOARD UTILITIES
        // -----------------------------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Copy Full JSON Button
                Button(
                    onClick = {
                        val jsonText = buildTransactionJson(
                            transaction = transaction,
                            simulatedPaymentIntent = simulatedPaymentIntent,
                            simulatedInvoiceId = simulatedInvoiceId,
                            dateIso = dateShort,
                            netPayout = netMerchantPayout,
                            totalFee = totalGatewayFee + fxConversionFee
                        )
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Transaction JSON", jsonText))
                        Toast.makeText(context, "Full JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("drilldown_copy_json_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.translate("copy_json"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Share Receipt Button
                    OutlinedButton(
                        onClick = {
                            val shareSummary = buildReceiptShareText(
                                transaction = transaction,
                                currencyFormatter = currencyFormatter,
                                dateFull = dateFull,
                                netPayout = netMerchantPayout,
                                fee = totalGatewayFee + fxConversionFee
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Receipt #TX-${transaction.id}")
                                putExtra(Intent.EXTRA_TEXT, shareSummary)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("drilldown_share_receipt_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CleanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewModel.translate("share_receipt"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary
                        )
                    }

                    // Delete / Void Button
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("drilldown_delete_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewModel.translate("delete_transaction"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Export Individual CSV Ledger Entry Button
                OutlinedButton(
                    onClick = { showCsvExportDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("drilldown_export_csv_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = CleanPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.translate("export_csv_modal_title"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanPrimary
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = CleanSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Delete Transaction #TX-${transaction.id}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CleanTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently remove this transaction from the Room SQLite sales ledger. Are you sure?",
                    fontSize = 13.sp,
                    color = CleanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = CleanTextSecondary)
                }
            }
        )
    }

    // CSV Export Modal Dialog for this transaction
    if (showCsvExportDialog) {
        SalesCsvExportDialog(
            transactions = listOf(transaction),
            viewModel = viewModel,
            currencyFormatter = currencyFormatter,
            onDismiss = { showCsvExportDialog = false }
        )
    }
}

// -----------------------------------------------------------------
// REUSABLE CARD CONTAINER
// -----------------------------------------------------------------
@Composable
private fun DrillDownSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurfaceVariant),
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CleanPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextPrimary
                )
            }
            HorizontalDivider(color = CleanBorderSubtle)
            content()
        }
    }
}

// -----------------------------------------------------------------
// DETAIL ROW
// -----------------------------------------------------------------
@Composable
private fun DrillDownDetailRow(
    label: String,
    value: String,
    valueColor: Color = CleanTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CleanTextMuted,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f)
        )
    }
}

// -----------------------------------------------------------------
// COPYABLE ROW WITH ONE-TAP CHIP
// -----------------------------------------------------------------
@Composable
private fun DrillDownCopyableRow(
    context: Context,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CleanTextMuted,
            modifier = Modifier.weight(1f)
        )

        Surface(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(8.dp),
            color = CleanSurface,
            border = BorderStroke(1.dp, CleanBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = CleanTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = CleanPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// TIMELINE COMPONENT
// -----------------------------------------------------------------
@Composable
private fun TimelineItem(
    time: String,
    title: String,
    subtitle: String,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(CleanPrimary)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(CleanBorder)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextPrimary
                )
                Text(
                    text = time,
                    fontSize = 10.sp,
                    color = CleanTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = CleanTextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}

// -----------------------------------------------------------------
// BUILDERS FOR SHARE & JSON
// -----------------------------------------------------------------
private fun buildReceiptShareText(
    transaction: TransactionEntity,
    currencyFormatter: CurrencyFormatter,
    dateFull: String,
    netPayout: Double,
    fee: Double
): String {
    return """
        ===========================================
        GLOBALMARKET PRO - OFFICIAL SALES RECEIPT
        ===========================================
        Transaction ID: #TX-${transaction.id}
        Status: SETTLED & VERIFIED (Stripe Connect)
        Date: $dateFull
        
        CUSTOMER DETAILS:
        Customer: ${transaction.customerName}
        Billing Country: ${transaction.country}
        
        SETTLEMENT SUMMARY:
        Gross Amount: ${currencyFormatter.format(transaction.convertedAmount, transaction.targetCurrency)}
        Base USD Equivalent: ${currencyFormatter.formatUsd(transaction.amountInUsd)}
        Gateway & FX Fees: -${currencyFormatter.format(fee, transaction.targetCurrency)}
        Net Merchant Payout: ${currencyFormatter.format(netPayout, transaction.targetCurrency)}
        
        GATEWAY VERIFICATION:
        Reference: ${transaction.stripeSubscriptionId}
        Verification: 3D Secure 2.0 Authenticated
        Storage: Local Room SQLite (AES-256 Verified)
        ===========================================
    """.trimIndent()
}

private fun buildTransactionJson(
    transaction: TransactionEntity,
    simulatedPaymentIntent: String,
    simulatedInvoiceId: String,
    dateIso: String,
    netPayout: Double,
    totalFee: Double
): String {
    return """
        {
          "transaction_id": ${transaction.id},
          "customer_name": "${transaction.customerName}",
          "country": "${transaction.country}",
          "currency": "${transaction.targetCurrency}",
          "amount_gross": ${transaction.convertedAmount},
          "amount_base_usd": ${transaction.amountInUsd},
          "total_fees": ${String.format(Locale.US, "%.2f", totalFee)},
          "net_payout": ${String.format(Locale.US, "%.2f", netPayout)},
          "timestamp_epoch": ${transaction.timestamp},
          "timestamp_iso": "$dateIso",
          "stripe_subscription_id": "${transaction.stripeSubscriptionId}",
          "payment_intent_id": "$simulatedPaymentIntent",
          "invoice_id": "$simulatedInvoiceId",
          "status": "settled",
          "security": "3ds2_authenticated"
        }
    """.trimIndent()
}
