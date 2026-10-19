package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionEntity
import com.example.data.TransactionLogEntity
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.CurrencyUtils
import com.example.util.SalesCsvExporter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionSummaryScreen(
    viewModel: GlobalMarketViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currencyFilter by viewModel.currencyFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val selectedTransaction by viewModel.selectedTransaction.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val transactionLogs by viewModel.transactionLogs.collectAsStateWithLifecycle()
    val logTypeFilter by viewModel.logTypeFilter.collectAsStateWithLifecycle()
    val selectedLog by viewModel.selectedLog.collectAsStateWithLifecycle()

    val currencyFormatter = remember(context, selectedLanguage) {
        CurrencyFormatter.from(context, selectedLanguage)
    }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    // Filter and sort transaction logs (Scanned items & Payments & Payouts)
    val filteredLogs = remember(transactionLogs, logTypeFilter, currencyFilter, searchQuery, sortOrder) {
        transactionLogs
            .filter { log ->
                val matchesType = when (logTypeFilter) {
                    LogTypeFilter.ALL -> true
                    LogTypeFilter.SCANNED_ITEMS -> log.logType == "SCANNED_ITEM"
                    LogTypeFilter.PAYMENTS -> log.logType == "PAYMENT" || log.logType == "WEB_CHECKOUT"
                    LogTypeFilter.PAYOUTS -> log.logType == "PAYOUT"
                }
                val matchesCurrency = currencyFilter == null || log.currencyCode.equals(currencyFilter, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        log.title.contains(searchQuery, ignoreCase = true) ||
                        log.customerOrRecipient.contains(searchQuery, ignoreCase = true) ||
                        log.referenceCode.contains(searchQuery, ignoreCase = true) ||
                        (log.barcodeOrRawValue?.contains(searchQuery, ignoreCase = true) == true) ||
                        log.country.contains(searchQuery, ignoreCase = true) ||
                        log.currencyCode.contains(searchQuery, ignoreCase = true)
                matchesType && matchesCurrency && matchesSearch
            }
            .let { list ->
                when (sortOrder) {
                    TransactionSortOrder.NEWEST -> list.sortedByDescending { it.timestamp }
                    TransactionSortOrder.OLDEST -> list.sortedBy { it.timestamp }
                    TransactionSortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
                    TransactionSortOrder.LOWEST_AMOUNT -> list.sortedBy { it.amount }
                }
            }
    }

    // Filter and sort transactions
    val filteredTransactions = remember(transactions, currencyFilter, searchQuery, sortOrder) {
        transactions
            .filter { tx ->
                val matchesCurrency = currencyFilter == null || tx.targetCurrency.equals(currencyFilter, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        tx.customerName.contains(searchQuery, ignoreCase = true) ||
                        tx.country.contains(searchQuery, ignoreCase = true) ||
                        tx.targetCurrency.contains(searchQuery, ignoreCase = true) ||
                        tx.stripeSubscriptionId.contains(searchQuery, ignoreCase = true)
                matchesCurrency && matchesSearch
            }
            .let { list ->
                when (sortOrder) {
                    TransactionSortOrder.NEWEST -> list.sortedByDescending { it.timestamp }
                    TransactionSortOrder.OLDEST -> list.sortedBy { it.timestamp }
                    TransactionSortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.convertedAmount }
                    TransactionSortOrder.LOWEST_AMOUNT -> list.sortedBy { it.convertedAmount }
                }
            }
    }

    // Key financial metrics
    val totalRevenueUsd = remember(transactions) { transactions.sumOf { it.amountInUsd } }
    val totalOrdersCount = transactions.size
    val averageOrderValueUsd = remember(transactions) {
        if (totalOrdersCount > 0) totalRevenueUsd / totalOrdersCount else 0.0
    }
    val distinctCurrenciesCount = remember(transactions) {
        transactions.map { it.targetCurrency }.distinct().size
    }

    BackHandler(enabled = selectedTransaction != null) {
        viewModel.selectTransaction(null)
    }

    AnimatedContent(
        targetState = selectedTransaction,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
            } else {
                slideInHorizontally { width -> -width / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "TransactionDrillDownScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { currentTx ->
        if (currentTx != null) {
            TransactionDrillDownView(
                transaction = currentTx,
                viewModel = viewModel,
                currencyFormatter = currencyFormatter,
                onBack = { viewModel.selectTransaction(null) },
                onDelete = {
                    viewModel.deleteTransaction(currentTx)
                    viewModel.selectTransaction(null)
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("transaction_summary_screen")
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
            ) {
        // 1. HEADER SECTION
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.translate("sales_summary_title"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = viewModel.translate("sales_summary_subtitle"),
                            fontSize = 12.sp,
                            color = CleanTextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Barcode scanner trigger
                        IconButton(
                            onClick = { viewModel.openBarcodeScanner() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer)
                                .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                                .testTag("summary_scan_barcode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode Entry",
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Export CSV button
                        IconButton(
                            onClick = {
                                showExportCsvDialog = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CleanSurfaceVariant)
                                .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear history button
                        if (transactions.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearConfirmDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CleanSurfaceVariant)
                                    .border(BorderStroke(1.dp, CleanBorder), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear Ledger",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. FINANCIAL SUMMARY METRIC CARDS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CleanPrimaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.translate("gross_sales").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanOnPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "ROOM SQLITE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = currencyFormatter.formatUsd(totalRevenueUsd),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanOnPrimaryContainer,
                                letterSpacing = (-0.5).sp
                            )
                            val convertedSummary = totalRevenueUsd * selectedCurrency.exchangeRate
                            Text(
                                text = "≈ ${currencyFormatter.format(convertedSummary, selectedCurrency.code)} (${selectedCurrency.code})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanOnPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        // Seed sample CTA if empty
                        if (transactions.isEmpty()) {
                            Button(
                                onClick = { viewModel.seedSampleTransactions() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanPrimary,
                                    contentColor = CleanOnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("seed_sample_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = viewModel.translate("seed_sample_data"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = CleanOnPrimaryContainer.copy(alpha = 0.15f))

                    // 3-Metric Sub-row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = viewModel.translate("total_orders"),
                                fontSize = 11.sp,
                                color = CleanOnPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$totalOrdersCount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = viewModel.translate("avg_order_value"),
                                fontSize = 11.sp,
                                color = CleanOnPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = currencyFormatter.formatUsd(averageOrderValueUsd),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = viewModel.translate("active_markets"),
                                fontSize = 11.sp,
                                color = CleanOnPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$distinctCurrenciesCount Currencies",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOnPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 3. SEARCH & SORT TOOLBAR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = viewModel.translate("search_placeholder"),
                            fontSize = 13.sp,
                            color = CleanTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CleanTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = CleanTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CleanTextPrimary,
                        unfocusedTextColor = CleanTextPrimary,
                        focusedBorderColor = CleanPrimary,
                        unfocusedBorderColor = CleanBorder,
                        focusedContainerColor = CleanSurface,
                        unfocusedContainerColor = CleanSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("search_input")
                )

                // Sort Order Menu Button
                Box {
                    Surface(
                        onClick = { isSortMenuExpanded = true },
                        shape = RoundedCornerShape(16.dp),
                        color = CleanSurface,
                        border = BorderStroke(1.dp, CleanBorder),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = CleanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = CleanTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                        modifier = Modifier.background(CleanSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newest First", fontWeight = if (sortOrder == TransactionSortOrder.NEWEST) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setSortOrder(TransactionSortOrder.NEWEST)
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First", fontWeight = if (sortOrder == TransactionSortOrder.OLDEST) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setSortOrder(TransactionSortOrder.OLDEST)
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Highest Amount", fontWeight = if (sortOrder == TransactionSortOrder.HIGHEST_AMOUNT) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setSortOrder(TransactionSortOrder.HIGHEST_AMOUNT)
                                isSortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lowest Amount", fontWeight = if (sortOrder == TransactionSortOrder.LOWEST_AMOUNT) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setSortOrder(TransactionSortOrder.LOWEST_AMOUNT)
                                isSortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // 3.5 LOG TYPE FILTER TABS (All Activity / Scanned Items / Payments / Payouts)
        item {
            val totalLogs = transactionLogs.size
            val scannedCount = remember(transactionLogs) { transactionLogs.count { it.logType == "SCANNED_ITEM" } }
            val paymentLogsCount = remember(transactionLogs) {
                transactionLogs.count { it.logType == "PAYMENT" || it.logType == "WEB_CHECKOUT" }
            }
            val payoutLogsCount = remember(transactionLogs) {
                transactionLogs.count { it.logType == "PAYOUT" }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CleanSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // All Activity
                val isAllSelected = logTypeFilter == LogTypeFilter.ALL
                Surface(
                    onClick = { viewModel.setLogTypeFilter(LogTypeFilter.ALL) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllSelected) CleanSurface else Color.Transparent,
                    shadowElevation = if (isAllSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("log_filter_all")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All",
                            fontSize = 11.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) CleanTextPrimary else CleanTextSecondary
                        )
                        if (totalLogs > 0) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isAllSelected) CleanPrimary else CleanTextMuted.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$totalLogs",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllSelected) CleanOnPrimary else CleanTextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Scanned Items Tab
                val isScannedSelected = logTypeFilter == LogTypeFilter.SCANNED_ITEMS
                Surface(
                    onClick = { viewModel.setLogTypeFilter(LogTypeFilter.SCANNED_ITEMS) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isScannedSelected) CleanSurface else Color.Transparent,
                    shadowElevation = if (isScannedSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("log_filter_scanned")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = if (isScannedSelected) CleanPrimary else CleanTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Scanned",
                            fontSize = 11.sp,
                            fontWeight = if (isScannedSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isScannedSelected) CleanTextPrimary else CleanTextSecondary
                        )
                        if (scannedCount > 0) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isScannedSelected) CleanPrimary else CleanTextMuted.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$scannedCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isScannedSelected) CleanOnPrimary else CleanTextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Payments Tab
                val isPaymentsSelected = logTypeFilter == LogTypeFilter.PAYMENTS
                Surface(
                    onClick = { viewModel.setLogTypeFilter(LogTypeFilter.PAYMENTS) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPaymentsSelected) CleanSurface else Color.Transparent,
                    shadowElevation = if (isPaymentsSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("log_filter_payments")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = if (isPaymentsSelected) CleanPrimary else CleanTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Payments",
                            fontSize = 11.sp,
                            fontWeight = if (isPaymentsSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPaymentsSelected) CleanTextPrimary else CleanTextSecondary
                        )
                        if (paymentLogsCount > 0) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isPaymentsSelected) CleanPrimary else CleanTextMuted.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$paymentLogsCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaymentsSelected) CleanOnPrimary else CleanTextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Payouts Tab
                val isPayoutsSelected = logTypeFilter == LogTypeFilter.PAYOUTS
                Surface(
                    onClick = { viewModel.setLogTypeFilter(LogTypeFilter.PAYOUTS) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPayoutsSelected) CleanSurface else Color.Transparent,
                    shadowElevation = if (isPayoutsSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("log_filter_payouts")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (isPayoutsSelected) CleanPrimary else CleanTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Payouts",
                            fontSize = 11.sp,
                            fontWeight = if (isPayoutsSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPayoutsSelected) CleanTextPrimary else CleanTextSecondary
                        )
                        if (payoutLogsCount > 0) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isPayoutsSelected) CleanPrimary else CleanTextMuted.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$payoutLogsCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayoutsSelected) CleanOnPrimary else CleanTextSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. CURRENCY FILTER CHIPS (HORIZONTAL SCROLL)
        item {
            val currencyCounts = remember(transactions) {
                val map = mutableMapOf<String, Int>()
                transactions.forEach {
                    map[it.targetCurrency] = (map[it.targetCurrency] ?: 0) + 1
                }
                map
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "All" filter chip
                item {
                    val isSelected = currencyFilter == null
                    Surface(
                        onClick = { viewModel.setCurrencyFilter(null) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) CleanPrimary else CleanSurface,
                        border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorder),
                        modifier = Modifier.testTag("currency_filter_all")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = viewModel.translate("filter_all"),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CleanOnPrimary else CleanTextPrimary
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) CleanOnPrimary.copy(alpha = 0.25f) else CleanSurfaceVariant
                            ) {
                                Text(
                                    text = "${transactions.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CleanOnPrimary else CleanPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                val availableCurrencies = listOf(
                    Triple("USD", "$", "🇺🇸"),
                    Triple("EUR", "€", "🇪🇺"),
                    Triple("GBP", "£", "🇬🇧"),
                    Triple("JPY", "¥", "🇯🇵"),
                    Triple("CNY", "¥", "🇨🇳")
                )

                items(availableCurrencies) { (code, symbol, flag) ->
                    val isSelected = currencyFilter.equals(code, ignoreCase = true)
                    val count = currencyCounts[code] ?: 0

                    Surface(
                        onClick = { viewModel.setCurrencyFilter(if (isSelected) null else code) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) CleanPrimary else CleanSurface,
                        border = BorderStroke(1.dp, if (isSelected) CleanPrimary else CleanBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = flag, fontSize = 14.sp)
                            Text(
                                text = "$code ($symbol)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CleanOnPrimary else CleanTextPrimary
                            )
                            if (count > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) CleanOnPrimary.copy(alpha = 0.25f) else CleanSurfaceVariant
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CleanOnPrimary else CleanPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. LEDGER SECTION TITLE WITH COUNT
        item {
            val titleText = when (logTypeFilter) {
                LogTypeFilter.ALL -> "ACTIVITY HISTORY (${filteredLogs.size.coerceAtLeast(filteredTransactions.size)})"
                LogTypeFilter.SCANNED_ITEMS -> "SCANNED ITEMS HISTORY (${filteredLogs.size})"
                LogTypeFilter.PAYMENTS -> "PAYMENTS HISTORY (${filteredLogs.size})"
                LogTypeFilter.PAYOUTS -> "PAYOUTS HISTORY (${filteredLogs.size})"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.8.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (transactions.isNotEmpty() || transactionLogs.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showExportCsvDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewModel.translate("export_csv"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    TextButton(
                        onClick = { viewModel.seedSampleTransactions() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CleanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Add Sample",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanPrimary
                        )
                    }
                }
            }
        }

        // 6. TRANSACTION & LOG LIST ITEMS OR EMPTY STATE
        val showLogs = filteredLogs.isNotEmpty() || logTypeFilter != LogTypeFilter.ALL
        val isListEmpty = if (showLogs) filteredLogs.isEmpty() else filteredTransactions.isEmpty()

        if (isListEmpty) {
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
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (logTypeFilter) {
                                    LogTypeFilter.SCANNED_ITEMS -> Icons.Default.QrCodeScanner
                                    LogTypeFilter.PAYMENTS -> Icons.Default.CreditCard
                                    LogTypeFilter.PAYOUTS -> Icons.Default.AccountBalance
                                    LogTypeFilter.ALL -> Icons.AutoMirrored.Outlined.ReceiptLong
                                },
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = when (logTypeFilter) {
                                LogTypeFilter.SCANNED_ITEMS -> "No scanned items recorded yet"
                                LogTypeFilter.PAYMENTS -> "No payment transactions recorded yet"
                                LogTypeFilter.PAYOUTS -> "No payout disbursements recorded yet"
                                LogTypeFilter.ALL -> if (transactions.isEmpty() && transactionLogs.isEmpty()) viewModel.translate("no_transactions") else viewModel.translate("no_results_filter")
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = when (logTypeFilter) {
                                LogTypeFilter.SCANNED_ITEMS -> "Scan product barcodes, QR codes, or payment slips using the camera scanner to log items automatically into Room SQLite."
                                LogTypeFilter.PAYMENTS -> "Process payments via the Global Terminal or Online Checkout to log verified payments here."
                                LogTypeFilter.PAYOUTS -> "Disburse payouts to merchant via Bank Wire or PayPal from the Owner Payout Hub to log transactions here."
                                LogTypeFilter.ALL -> if (transactions.isEmpty() && transactionLogs.isEmpty())
                                    "Start scanning items or processing payments to persist transaction logs in Room SQLite."
                                else
                                    "Try resetting search filters or selecting 'All Logs' to inspect recorded data."
                            },
                            fontSize = 12.sp,
                            color = CleanTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (transactions.isEmpty() && transactionLogs.isEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.seedSampleTransactions() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CleanPrimary,
                                    contentColor = CleanOnPrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.translate("seed_sample_data"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    viewModel.setCurrencyFilter(null)
                                    viewModel.setLogTypeFilter(LogTypeFilter.ALL)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reset Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (showLogs) {
            items(
                items = filteredLogs,
                key = { it.id }
            ) { log ->
                TransactionLogCard(
                    log = log,
                    currencyFormatter = currencyFormatter,
                    onClick = { viewModel.selectLog(log) },
                    onDelete = { viewModel.deleteLog(log) }
                )
            }
        } else {
            items(
                items = filteredTransactions,
                key = { it.id }
            ) { transaction ->
                TransactionSummaryCard(
                    transaction = transaction,
                    currencyFormatter = currencyFormatter,
                    onClick = { viewModel.selectTransaction(transaction) },
                    onDelete = { viewModel.deleteTransaction(transaction) }
                )
            }
        }
    }
    }
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = CleanSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = viewModel.translate("clear_all_tx"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CleanTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to erase all transaction records from local Room SQLite storage? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = CleanTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Erase All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = CleanTextSecondary)
                }
            }
        )
    }

    // CSV Accounting Export Modal Dialog
    if (showExportCsvDialog) {
        val initialDataset = if (logTypeFilter == LogTypeFilter.ALL || logTypeFilter == LogTypeFilter.SCANNED_ITEMS || logTypeFilter == LogTypeFilter.PAYMENTS) {
            CsvExportDataset.ROOM_DATABASE_LOGS
        } else {
            CsvExportDataset.SALES_TRANSACTIONS
        }
        SalesCsvExportDialog(
            transactions = transactions,
            transactionLogs = transactionLogs,
            initialDataset = initialDataset,
            viewModel = viewModel,
            currencyFormatter = currencyFormatter,
            onDismiss = { showExportCsvDialog = false }
        )
    }

    // Transaction Log Detail Dialog
    if (selectedLog != null) {
        TransactionLogDetailsDialog(
            log = selectedLog!!,
            currencyFormatter = currencyFormatter,
            onDismiss = { viewModel.selectLog(null) },
            onDelete = {
                viewModel.deleteLog(it)
                viewModel.selectLog(null)
            }
        )
    }
}

// -------------------------------------------------------------
// INDIVIDUAL TRANSACTION SUMMARY CARD COMPONENT
// -------------------------------------------------------------
@Composable
fun TransactionSummaryCard(
    transaction: TransactionEntity,
    currencyFormatter: CurrencyFormatter = CurrencyFormatter.DEFAULT,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(transaction.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    val flagEmoji = when (transaction.targetCurrency.uppercase()) {
        "USD" -> "🇺🇸"
        "EUR" -> "🇪🇺"
        "GBP" -> "🇬🇧"
        "JPY" -> "🇯🇵"
        "CNY" -> "🇨🇳"
        else -> "💳"
    }

    val currencySymbol = when (transaction.targetCurrency.uppercase()) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "CNY" -> "¥"
        else -> ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Upper Row: Avatar + Customer & Country info + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Currency Flag Badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CleanSurfaceVariant)
                            .border(BorderStroke(1.dp, CleanBorder), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = flagEmoji, fontSize = 22.sp)
                    }

                    Column {
                        Text(
                            text = transaction.customerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = transaction.country,
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                            Text(
                                text = "•",
                                fontSize = 11.sp,
                                color = CleanTextMuted
                            )
                            Text(
                                text = dateString,
                                fontSize = 11.sp,
                                color = CleanTextMuted
                            )
                        }
                    }
                }

                // Amount Section
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(transaction.convertedAmount, transaction.targetCurrency),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CleanPrimary
                    )
                    Text(
                        text = "Base: " + currencyFormatter.formatUsd(transaction.amountInUsd),
                        fontSize = 11.sp,
                        color = CleanTextMuted
                    )
                }
            }

            HorizontalDivider(color = CleanBorderSubtle)

            // Lower Row: Monospace Reference ID + Settled Status Pill + Detail Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CleanSurfaceVariant,
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Text(
                            text = transaction.stripeSubscriptionId.take(18) + if (transaction.stripeSubscriptionId.length > 18) "..." else "",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CleanTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CleanTertiaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CleanTertiary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Settled",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTertiary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanPrimary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View transaction details",
                        tint = CleanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TRANSACTION RECEIPT MODAL DIALOG
// -------------------------------------------------------------
@Composable
fun TransactionReceiptModal(
    transaction: TransactionEntity,
    viewModel: GlobalMarketViewModel,
    currencyFormatter: CurrencyFormatter = CurrencyFormatter.DEFAULT,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateFull = remember(transaction.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    val flagEmoji = when (transaction.targetCurrency.uppercase()) {
        "USD" -> "🇺🇸"
        "EUR" -> "🇪🇺"
        "GBP" -> "🇬🇧"
        "JPY" -> "🇯🇵"
        "CNY" -> "🇨🇳"
        else -> "💳"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CleanSurface,
        shape = RoundedCornerShape(28.dp),
        title = null,
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Close
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
                            Text(text = flagEmoji, fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = viewModel.translate("receipt_details"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Transaction #${transaction.id}",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CleanTextSecondary
                        )
                    }
                }

                HorizontalDivider(color = CleanBorderSubtle)

                // Prominent Amount Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CleanSurfaceVariant),
                    border = BorderStroke(1.dp, CleanBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SETTLEMENT AMOUNT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currencyFormatter.format(transaction.convertedAmount, transaction.targetCurrency),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CleanPrimary
                        )
                        Text(
                            text = "Base Amount: " + currencyFormatter.formatUsd(transaction.amountInUsd),
                            fontSize = 12.sp,
                            color = CleanTextSecondary
                        )
                    }
                }

                // Metadata Rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReceiptDetailRow("Customer", transaction.customerName)
                    ReceiptDetailRow("Billing Country", transaction.country)
                    ReceiptDetailRow("Timestamp", dateFull)
                    ReceiptDetailRow("Gateway Method", "Stripe Multi-Currency Direct")
                    ReceiptDetailRow("Security Status", "Encrypted (AES-256 Room DB)")
                    ReceiptDetailRow("Reference ID", transaction.stripeSubscriptionId)
                }

                HorizontalDivider(color = CleanBorderSubtle)

                // Actions: Copy Receipt Summary & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val receiptText = """
                                --- GLOBALMARKET PRO RECEIPT ---
                                Tx ID: #${transaction.id}
                                Customer: ${transaction.customerName}
                                Country: ${transaction.country}
                                Settlement: ${currencyFormatter.format(transaction.convertedAmount, transaction.targetCurrency)}
                                Base USD: ${currencyFormatter.formatUsd(transaction.amountInUsd)}
                                Date: $dateFull
                                Ref: ${transaction.stripeSubscriptionId}
                                Gateway: Stripe AES-256 Verified
                            """.trimIndent()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Receipt", receiptText))
                            Toast.makeText(context, "Receipt copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CleanBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}

@Composable
fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = CleanTextMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CleanTextPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f)
        )
    }
}

// -------------------------------------------------------------
// HELPER: EXPORT LEDGER TO CLIPBOARD
// -------------------------------------------------------------
private fun exportLedgerToClipboard(
    context: Context,
    transactions: List<TransactionEntity>,
    viewModel: GlobalMarketViewModel,
    currencyFormatter: CurrencyFormatter
) {
    val csv = SalesCsvExporter.generateCsvString(transactions, currencyFormatter)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Sales Ledger CSV", csv))
    Toast.makeText(context, viewModel.translate("export_success"), Toast.LENGTH_SHORT).show()
}

// -------------------------------------------------------------
// TRANSACTION LOG CARD (SCANNED ITEMS & PAYMENTS)
// -------------------------------------------------------------
@Composable
fun TransactionLogCard(
    log: TransactionLogEntity,
    currencyFormatter: CurrencyFormatter = CurrencyFormatter.DEFAULT,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(log.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(log.timestamp))
    }

    val isScannedItem = log.logType == "SCANNED_ITEM"

    val typeLabel = when (log.logType) {
        "SCANNED_ITEM" -> "SCANNED ITEM"
        "PAYMENT" -> "TERMINAL PAYMENT"
        "WEB_CHECKOUT" -> "ONLINE CHECKOUT"
        "PAYOUT" -> "PAYOUT"
        else -> log.logType
    }

    val typeColor = when (log.logType) {
        "SCANNED_ITEM" -> Color(0xFF059669) // Emerald
        "PAYMENT" -> CleanPrimary
        "WEB_CHECKOUT" -> Color(0xFF7C3AED) // Violet
        "PAYOUT" -> Color(0xFFEA580C) // Amber/Orange
        else -> CleanPrimary
    }

    val flagEmoji = when (log.currencyCode.uppercase()) {
        "USD" -> "🇺🇸"
        "EUR" -> "🇪🇺"
        "GBP" -> "🇬🇧"
        "JPY" -> "🇯🇵"
        "CNY" -> "🇨🇳"
        else -> if (isScannedItem) "🏷️" else "💳"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("transaction_log_item_${log.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CleanSurface),
        border = BorderStroke(1.dp, CleanBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Type Pill + Date + Reference Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isScannedItem) Icons.Default.QrCodeScanner else Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = typeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeColor,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    if (log.barcodeFormat != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CleanSurfaceVariant
                        ) {
                            Text(
                                text = log.barcodeFormat,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = log.referenceCode,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = CleanTextMuted
                )
            }

            // Main Row: Avatar/Flag + Title & Details + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CleanSurfaceVariant)
                            .border(BorderStroke(1.dp, CleanBorder), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = flagEmoji, fontSize = 22.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = log.customerOrRecipient,
                                fontSize = 11.sp,
                                color = CleanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(text = "•", fontSize = 11.sp, color = CleanTextMuted)
                            Text(
                                text = log.country,
                                fontSize = 11.sp,
                                color = CleanTextMuted
                            )
                        }

                        if (!log.barcodeOrRawValue.isNullOrBlank()) {
                            Text(
                                text = "Code: ${log.barcodeOrRawValue}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = typeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(log.amount, log.currencyCode),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CleanPrimary
                    )
                    Text(
                        text = "Base: " + currencyFormatter.formatUsd(log.amountInUsd),
                        fontSize = 11.sp,
                        color = CleanTextMuted
                    )
                }
            }

            // Bottom metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    fontSize = 10.sp,
                    color = CleanTextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = log.status.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete log",
                            tint = CleanTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TRANSACTION LOG DETAILS DIALOG
// -------------------------------------------------------------
@Composable
fun TransactionLogDetailsDialog(
    log: TransactionLogEntity,
    currencyFormatter: CurrencyFormatter,
    onDismiss: () -> Unit,
    onDelete: (TransactionLogEntity) -> Unit
) {
    val context = LocalContext.current
    val dateString = remember(log.timestamp) {
        SimpleDateFormat("MMMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }

    val isScannedItem = log.logType == "SCANNED_ITEM"
    val typeLabel = when (log.logType) {
        "SCANNED_ITEM" -> "Scanned Item Record"
        "PAYMENT" -> "Terminal Payment"
        "WEB_CHECKOUT" -> "Online Web Checkout"
        "PAYOUT" -> "Disbursement Payout"
        else -> log.logType
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CleanSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        imageVector = if (isScannedItem) Icons.Default.QrCodeScanner else Icons.Default.Receipt,
                        contentDescription = null,
                        tint = CleanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = typeLabel,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextPrimary
                    )
                    Text(
                        text = log.referenceCode,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CleanTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title and amount summary box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CleanSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = log.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = currencyFormatter.format(log.amount, log.currencyCode),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CleanPrimary
                            )
                            Text(
                                text = "Base USD: " + currencyFormatter.formatUsd(log.amountInUsd),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = CleanTextSecondary
                            )
                        }
                    }
                }

                // Metadata breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReceiptDetailRow("Customer / Recipient", log.customerOrRecipient)
                    ReceiptDetailRow("Country / Region", log.country)
                    ReceiptDetailRow("Payment Method", log.paymentMethod)
                    ReceiptDetailRow("Status", log.status.uppercase())
                    ReceiptDetailRow("Timestamp", dateString)

                    if (!log.barcodeOrRawValue.isNullOrBlank()) {
                        ReceiptDetailRow(
                            label = if (log.barcodeFormat != null) "Barcode (${log.barcodeFormat})" else "Barcode / QR",
                            value = log.barcodeOrRawValue
                        )
                    }

                    if (!log.notes.isNullOrBlank()) {
                        ReceiptDetailRow("Notes", log.notes)
                    }
                }

                // Copy barcode button if available
                if (!log.barcodeOrRawValue.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Barcode Value", log.barcodeOrRawValue))
                            Toast.makeText(context, "Barcode value copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Barcode: ${log.barcodeOrRawValue.take(16)}...", fontSize = 12.sp)
                    }
                }

                // Export this single record as CSV row
                OutlinedButton(
                    onClick = {
                        val singleCsv = SalesCsvExporter.generateLogsCsvString(listOf(log), currencyFormatter)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Transaction CSV Record", singleCsv))
                        Toast.makeText(context, "Record CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Record as CSV", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanPrimary)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDelete(log) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Record", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
