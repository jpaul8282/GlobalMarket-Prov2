package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PayoutWireEntity
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanDanger
import com.example.ui.theme.CleanDangerContainer
import com.example.ui.theme.CleanOnPrimary
import com.example.ui.theme.CleanOrange
import com.example.ui.theme.CleanOrangeContainer
import com.example.ui.theme.CleanOrangeText
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSecondary
import com.example.ui.theme.CleanSecondaryContainer
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanSuccessContainer
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceSecondary
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTertiary
import com.example.ui.theme.CleanTertiaryContainer
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PayoutHistoryFilter(val key: String, val label: String) {
    ALL("ALL", "All"),
    PROCESSED("PROCESSED", "Processed Payouts"),
    SIMULATED("SIMULATED", "Simulated Wires"),
    BANK_WIRE("BANK_WIRE", "Bank / iDEAL"),
    PAYPAL("PAYPAL", "PayPal")
}

private enum class PayoutHistorySort(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    HIGHEST_USD("Highest USD"),
    LOWEST_USD("Lowest USD")
}

@Composable
fun PayoutHistoryScreen(
    viewModel: GlobalMarketViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allPayoutWires by viewModel.payoutWireHistory.collectAsStateWithLifecycle()
    val processedPayouts by viewModel.processedPayouts.collectAsStateWithLifecycle()
    val simulatedWires by viewModel.simulatedBankWires.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalPayoutWireCount.collectAsStateWithLifecycle()
    val totalNetUsdFromDb by viewModel.totalPayoutWireNetDisbursedUsd.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    val currencyFormatter = remember(context, selectedLanguage) {
        CurrencyFormatter.from(context, selectedLanguage)
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(PayoutHistoryFilter.ALL) }
    var activeSort by remember { mutableStateOf(PayoutHistorySort.NEWEST) }
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<PayoutWireEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showExportDialog || selectedRecord != null || onBack != null) {
        when {
            showExportDialog -> showExportDialog = false
            selectedRecord != null -> selectedRecord = null
            else -> onBack?.invoke()
        }
    }

    // Summary metrics derived from Room records
    val summaryTotalCount = if (totalCount > 0) totalCount else allPayoutWires.size
    val summaryProcessedCount = processedPayouts.size
    val summarySimulatedCount = simulatedWires.size
    val summaryGrossUsd = remember(allPayoutWires) { allPayoutWires.sumOf { it.grossAmountUsd } }
    val summaryFeesUsd = remember(allPayoutWires) { allPayoutWires.sumOf { it.feeUsd } }
    val summaryNetUsd = remember(allPayoutWires, totalNetUsdFromDb) {
        totalNetUsdFromDb ?: allPayoutWires.sumOf { it.netAmountUsd }
    }
    val completedCount = remember(allPayoutWires) {
        allPayoutWires.count {
            it.status.equals("COMPLETED", ignoreCase = true) ||
                it.status.equals("SETTLED", ignoreCase = true)
        }
    }
    val distinctRails = remember(allPayoutWires) {
        allPayoutWires.map { it.clearingRail }.filter { it.isNotBlank() }.distinct()
    }

    val filteredRecords = remember(allPayoutWires, activeFilter, searchQuery, activeSort) {
        allPayoutWires
            .filter { record ->
                val matchesFilter = when (activeFilter) {
                    PayoutHistoryFilter.ALL -> true
                    PayoutHistoryFilter.PROCESSED -> !record.isSimulatedWire && record.transactionType == "PROCESSED_PAYOUT"
                    PayoutHistoryFilter.SIMULATED -> record.isSimulatedWire || record.transactionType == "SIMULATED_BANK_WIRE"
                    PayoutHistoryFilter.BANK_WIRE -> record.method != "PAYPAL"
                    PayoutHistoryFilter.PAYPAL -> record.method.equals("PAYPAL", ignoreCase = true)
                }
                val q = searchQuery.trim()
                val matchesSearch = q.isEmpty() ||
                    record.referenceCode.contains(q, ignoreCase = true) ||
                    record.recipientName.contains(q, ignoreCase = true) ||
                    record.bankName.contains(q, ignoreCase = true) ||
                    record.ibanOrAccount.contains(q, ignoreCase = true) ||
                    record.swiftBic.contains(q, ignoreCase = true) ||
                    record.clearingRail.contains(q, ignoreCase = true) ||
                    record.currencyCode.contains(q, ignoreCase = true) ||
                    (record.notes?.contains(q, ignoreCase = true) == true)
                matchesFilter && matchesSearch
            }
            .let { list ->
                when (activeSort) {
                    PayoutHistorySort.NEWEST -> list.sortedByDescending { it.timestamp }
                    PayoutHistorySort.OLDEST -> list.sortedBy { it.timestamp }
                    PayoutHistorySort.HIGHEST_USD -> list.sortedByDescending { it.netAmountUsd }
                    PayoutHistorySort.LOWEST_USD -> list.sortedBy { it.netAmountUsd }
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CleanBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("payout_history_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // 1. TOP BAR
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
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
                                    .testTag("payout_history_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Payout History Ledger",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary,
                                letterSpacing = (-0.4).sp
                            )
                            Text(
                                text = "Room Database • Payouts & Simulated Bank Wires",
                                fontSize = 12.sp,
                                color = CleanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (allPayoutWires.isNotEmpty()) {
                            IconButton(
                                onClick = { showExportDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CleanPrimaryContainer)
                                    .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                                    .testTag("payout_history_copy_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Export Payout History as CSV or JSON",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. SUMMARY OF TOTAL PROCESSED TRANSACTIONS HERO CARD
            item {
                PayoutSummaryHeroCard(
                    totalTransactions = summaryTotalCount,
                    processedPayoutsCount = summaryProcessedCount,
                    simulatedWiresCount = summarySimulatedCount,
                    completedCount = completedCount,
                    totalNetUsd = summaryNetUsd,
                    totalGrossUsd = summaryGrossUsd,
                    totalFeesUsd = summaryFeesUsd,
                    distinctRails = distinctRails,
                    currencyFormatter = currencyFormatter
                )
            }

            // 3. SEARCH & SORT ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search ref, IBAN, bank, recipient...",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search payouts",
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(visible = searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("payout_history_clear_search")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = CleanTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanPrimary,
                            unfocusedBorderColor = CleanBorder,
                            focusedContainerColor = CleanSurface,
                            unfocusedContainerColor = CleanSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payout_history_search_input")
                    )

                    Box {
                        Surface(
                            onClick = { isSortMenuOpen = true },
                            shape = RoundedCornerShape(14.dp),
                            color = CleanSurface,
                            border = BorderStroke(1.dp, CleanBorder),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("payout_history_sort_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort payout history",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = activeSort.label.substringBefore(" "),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanTextPrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false },
                            modifier = Modifier.background(CleanSurface)
                        ) {
                            PayoutHistorySort.entries.forEach { sortOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOption.label,
                                            fontWeight = if (sortOption == activeSort) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOption == activeSort) CleanPrimary else CleanTextPrimary
                                        )
                                    },
                                    onClick = {
                                        activeSort = sortOption
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. FILTER CHIPS ROW
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PayoutHistoryFilter.entries) { filter ->
                        val isSelected = activeFilter == filter
                        val countForChip = when (filter) {
                            PayoutHistoryFilter.ALL -> allPayoutWires.size
                            PayoutHistoryFilter.PROCESSED -> summaryProcessedCount
                            PayoutHistoryFilter.SIMULATED -> summarySimulatedCount
                            PayoutHistoryFilter.BANK_WIRE -> allPayoutWires.count { it.method != "PAYPAL" }
                            PayoutHistoryFilter.PAYPAL -> allPayoutWires.count { it.method.equals("PAYPAL", ignoreCase = true) }
                        }

                        Surface(
                            onClick = { activeFilter = filter },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CleanPrimaryContainer else CleanSurface,
                            border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorderSubtle),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("payout_history_chip_${filter.key}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${filter.label} ($countForChip)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CleanPrimary else CleanTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 5. SECTION HEADER + EXPORT & QUICK SIMULATE ACTIONS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STORED TRANSACTIONS (${filteredRecords.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (allPayoutWires.isNotEmpty()) {
                            TextButton(
                                onClick = { showExportDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("payout_history_export_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Export CSV / JSON",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanPrimary
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.simulateBankWireTransfer(
                                    amountInUsd = 250.0,
                                    currencyCode = "EUR",
                                    method = "IDEAL",
                                    clearingRail = "iDEAL / SEPA Instant",
                                    notes = "Quick simulated bank wire from Payout History",
                                    onSuccess = { saved ->
                                        Toast.makeText(
                                            context,
                                            "Simulated wire ${saved.referenceCode} saved",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("payout_history_quick_simulate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = CleanOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Simulate Wire",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOrangeText
                            )
                        }
                    }
                }
            }

            // 6. PAYOUT HISTORY LIST OR EMPTY STATE
            if (filteredRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payout_history_empty_state"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CleanSurface),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CleanPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "No payout history",
                                    tint = CleanPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (allPayoutWires.isEmpty()) {
                                    "No Processed Payouts or Wires Yet"
                                } else {
                                    "No Matching Payout Records"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (allPayoutWires.isEmpty()) {
                                    "Tip: Disburse a Bank Wire or PayPal payout in Owner Payouts, or run a simulated iDEAL wire to populate your Room ledger."
                                } else {
                                    "Try clearing your search query or switching the active filter chip above."
                                },
                                fontSize = 12.sp,
                                color = CleanTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.simulateBankWireTransfer(
                                            amountInUsd = 250.0,
                                            currencyCode = "EUR",
                                            method = "IDEAL",
                                            clearingRail = "iDEAL / SEPA Instant",
                                            notes = "Simulated wire initiated from empty state"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("payout_history_empty_simulate_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CleanOrange)
                                ) {
                                    Text("Simulate Wire", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onBack?.invoke()
                                        viewModel.openOwnerPayoutSection()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("payout_history_open_owner_payouts_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CleanPrimary)
                                ) {
                                    Text("Owner Payouts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CleanPrimary)
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredRecords,
                    key = { it.id }
                ) { record ->
                    PayoutHistoryItemCard(
                        record = record,
                        currencyFormatter = currencyFormatter,
                        onClick = { selectedRecord = record }
                    )
                }
            }
        }
    }

    selectedRecord?.let { record ->
        PayoutWireDetailDialog(
            record = record,
            currencyFormatter = currencyFormatter,
            onDismiss = { selectedRecord = null },
            onToggleStatus = {
                val next = if (record.status == "SETTLED") "COMPLETED" else "SETTLED"
                viewModel.updatePayoutWireStatus(record.id, next)
                selectedRecord = record.copy(status = next)
            },
            onDelete = {
                viewModel.deletePayoutWireRecord(record)
                selectedRecord = null
                Toast.makeText(context, "Record ${record.referenceCode} deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showExportDialog) {
        PayoutHistoryExportDialog(
            allRecords = allPayoutWires,
            currencyFormatter = currencyFormatter,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
private fun PayoutSummaryHeroCard(
    totalTransactions: Int,
    processedPayoutsCount: Int,
    simulatedWiresCount: Int,
    completedCount: Int,
    totalNetUsd: Double,
    totalGrossUsd: Double,
    totalFeesUsd: Double,
    distinctRails: List<String>,
    currencyFormatter: CurrencyFormatter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payout_history_summary_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(CleanPrimary, CleanSecondary)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL PROCESSED TRANSACTIONS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanOnPrimary.copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$totalTransactions Transactions",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanOnPrimary,
                            modifier = Modifier.testTag("payout_summary_total_count")
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CleanOnPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$completedCount Settled",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimary
                            )
                        }
                    }
                }

                // Net Disbursed Volume
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "NET DISBURSED VOLUME (USD)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanOnPrimary.copy(alpha = 0.8f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = currencyFormatter.formatUsd(totalNetUsd),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CleanOnPrimary,
                        modifier = Modifier.testTag("payout_summary_net_usd")
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.22f))

                // 3-Column Breakdown: Processed vs Simulated Wires vs Gross/Fees
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetricColumn(
                        label = "Processed Payouts",
                        value = "$processedPayoutsCount",
                        subValue = currencyFormatter.formatUsd(totalGrossUsd)
                    )
                    SummaryMetricColumn(
                        label = "Simulated Wires",
                        value = "$simulatedWiresCount",
                        subValue = if (distinctRails.isEmpty()) "SEPA / iDEAL" else "${distinctRails.size} rails"
                    )
                    SummaryMetricColumn(
                        label = "Total Fees",
                        value = currencyFormatter.formatUsd(totalFeesUsd),
                        subValue = "Room SQLite"
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricColumn(
    label: String,
    value: String,
    subValue: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = CleanOnPrimary.copy(alpha = 0.78f)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CleanOnPrimary
        )
        Text(
            text = subValue,
            fontSize = 10.sp,
            color = CleanOnPrimary.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun PayoutHistoryItemCard(
    record: PayoutWireEntity,
    currencyFormatter: CurrencyFormatter,
    onClick: () -> Unit
) {
    val dateString = remember(record.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }
    val isBankOrIdeal = record.method != "PAYPAL"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("payout_history_item_${record.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(
            1.dp,
            if (record.isSimulatedWire) CleanOrange.copy(alpha = 0.45f) else CleanBorder
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    record.isSimulatedWire -> CleanOrangeContainer
                                    isBankOrIdeal -> CleanPrimaryContainer
                                    else -> CleanSecondaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBankOrIdeal) Icons.Default.AccountBalance else Icons.Default.Payments,
                            contentDescription = record.clearingRail,
                            tint = when {
                                record.isSimulatedWire -> CleanOrange
                                isBankOrIdeal -> CleanPrimary
                                else -> CleanSecondary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = record.recipientName.ifBlank { record.bankName },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (record.isSimulatedWire) CleanOrangeContainer else CleanPrimaryContainer
                            ) {
                                Text(
                                    text = if (record.isSimulatedWire) "SIMULATED WIRE" else "PAYOUT",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (record.isSimulatedWire) CleanOrangeText else CleanPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${record.clearingRail} • ${record.referenceCode}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CleanTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateString,
                            fontSize = 10.sp,
                            color = CleanTextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.formatUsd(record.netAmountUsd),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (record.isSimulatedWire) CleanOrange else CleanPrimary
                    )
                    Text(
                        text = "≈ ${currencyFormatter.format(record.convertedAmount, record.currencyCode)}",
                        fontSize = 11.sp,
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
                    text = "${record.bankName} (${record.ibanOrAccount})",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CleanTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (record.status.uppercase()) {
                        "COMPLETED", "SETTLED" -> CleanSuccessContainer
                        "FAILED" -> CleanDangerContainer
                        else -> CleanTertiaryContainer
                    }
                ) {
                    Text(
                        text = record.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status.uppercase()) {
                            "COMPLETED", "SETTLED" -> CleanSuccess
                            "FAILED" -> CleanDanger
                            else -> CleanTertiary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PayoutWireDetailDialog(
    record: PayoutWireEntity,
    currencyFormatter: CurrencyFormatter,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val formattedDate = remember(record.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CleanSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (record.isSimulatedWire) "Simulated Bank Wire" else "Processed Payout",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )
                    Text(
                        text = record.referenceCode,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CleanPrimary
                    )
                }
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(record.referenceCode))
                    Toast.makeText(context, "Reference copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy reference",
                        tint = CleanPrimary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Status", record.status)
                DetailRow("Clearing Rail", record.clearingRail)
                DetailRow("Method", record.method)
                DetailRow("Timestamp", formattedDate)
                HorizontalDivider(color = CleanBorderSubtle)
                DetailRow("Gross (USD)", currencyFormatter.formatUsd(record.grossAmountUsd))
                DetailRow("Fee (USD)", "-${currencyFormatter.formatUsd(record.feeUsd)}")
                DetailRow("Net Disbursed (USD)", currencyFormatter.formatUsd(record.netAmountUsd))
                DetailRow(
                    "Converted (${record.currencyCode})",
                    currencyFormatter.format(record.convertedAmount, record.currencyCode)
                )
                HorizontalDivider(color = CleanBorderSubtle)
                DetailRow("Recipient", record.recipientName)
                DetailRow("Bank", record.bankName)
                DetailRow("IBAN / Account", record.ibanOrAccount)
                if (record.swiftBic.isNotBlank()) {
                    DetailRow("SWIFT / BIC", record.swiftBic)
                }
                if (!record.notes.isNullOrBlank()) {
                    DetailRow("Notes", record.notes)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = CleanDanger)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (record.status == "SETTLED") "Mark Completed" else "Mark Settled",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CleanTextSecondary)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CleanTextMuted,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CleanTextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
