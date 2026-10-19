package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TransactionEntity
import com.example.data.TransactionLogEntity
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.SalesCsvExporter
import java.text.SimpleDateFormat
import java.util.*

enum class CsvExportDataset {
    SALES_TRANSACTIONS,
    ROOM_DATABASE_LOGS
}

@Composable
fun SalesCsvExportDialog(
    transactions: List<TransactionEntity>,
    transactionLogs: List<TransactionLogEntity> = emptyList(),
    initialDataset: CsvExportDataset = if (transactionLogs.isNotEmpty() && transactions.isEmpty()) CsvExportDataset.ROOM_DATABASE_LOGS else CsvExportDataset.ROOM_DATABASE_LOGS,
    viewModel: GlobalMarketViewModel,
    currencyFormatter: CurrencyFormatter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }
    var selectedDataset by remember { mutableStateOf(initialDataset) }

    val isLogsMode = selectedDataset == CsvExportDataset.ROOM_DATABASE_LOGS
    val currentRecordCount = if (isLogsMode) transactionLogs.size else transactions.size

    val timestampStr = remember {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    val defaultFileName = remember(selectedDataset, timestampStr) {
        if (selectedDataset == CsvExportDataset.ROOM_DATABASE_LOGS) {
            "room_transaction_logs_$timestampStr.csv"
        } else {
            "sales_ledger_$timestampStr.csv"
        }
    }

    // Android Storage Access Framework (SAF) Document Creator
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            isSaving = true
            val success = if (isLogsMode) {
                SalesCsvExporter.writeLogsToUri(context, uri, transactionLogs, currencyFormatter)
            } else {
                SalesCsvExporter.writeToUri(context, uri, transactions, currencyFormatter)
            }
            isSaving = false
            if (success) {
                Toast.makeText(context, viewModel.translate("csv_saved_success"), Toast.LENGTH_LONG).show()
                onDismiss()
            } else {
                Toast.makeText(context, viewModel.translate("csv_save_failed"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Preview snippet of the CSV
    val csvPreview = remember(selectedDataset, transactions, transactionLogs) {
        val full = if (isLogsMode) {
            SalesCsvExporter.generateLogsCsvString(transactionLogs.take(3), currencyFormatter)
        } else {
            SalesCsvExporter.generateCsvString(transactions.take(3), currencyFormatter)
        }
        // Trim BOM if present for visual preview
        full.replace("\uFEFF", "").lines().take(6).joinToString("\n")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            border = BorderStroke(1.dp, CleanBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CleanPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Export CSV for Spreadsheets",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Excel, QuickBooks, Xero & Sheets",
                                fontSize = 11.sp,
                                color = CleanTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CleanTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dataset Selector (Room DB Logs vs Sales Ledger)
                Text(
                    text = "SELECT DATASET TO EXPORT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanTextMuted,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CleanSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Room Database Logs Option
                    Surface(
                        onClick = { selectedDataset = CsvExportDataset.ROOM_DATABASE_LOGS },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isLogsMode) CleanSurface else Color.Transparent,
                        shadowElevation = if (isLogsMode) 2.dp else 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (isLogsMode) CleanPrimary else CleanTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Room DB Logs (${transactionLogs.size})",
                                fontSize = 11.sp,
                                fontWeight = if (isLogsMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLogsMode) CleanTextPrimary else CleanTextSecondary
                            )
                        }
                    }

                    // Sales Ledger Option
                    Surface(
                        onClick = { selectedDataset = CsvExportDataset.SALES_TRANSACTIONS },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isLogsMode) CleanSurface else Color.Transparent,
                        shadowElevation = if (!isLogsMode) 2.dp else 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = if (!isLogsMode) CleanPrimary else CleanTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sales Ledger (${transactions.size})",
                                fontSize = 11.sp,
                                fontWeight = if (!isLogsMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isLogsMode) CleanTextPrimary else CleanTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isLogsMode) {
                        "Export complete SQLite Room logs with barcode values, item formats, amounts, and customer references for audit & inventory reconciliation."
                    } else {
                        viewModel.translate("export_csv_modal_desc")
                    },
                    fontSize = 12.sp,
                    color = CleanTextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CleanSurfaceVariant)
                        .border(BorderStroke(1.dp, CleanBorder), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Specification", fontSize = 11.sp, color = CleanTextMuted)
                        Surface(
                            shape = CircleShape,
                            color = CleanSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "RFC 4180 • UTF-8 BOM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dataset Count", fontSize = 11.sp, color = CleanTextMuted)
                        Text(
                            text = "$currentRecordCount records",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target File", fontSize = 11.sp, color = CleanTextMuted)
                        Text(
                            text = defaultFileName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CleanPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Compatibility", fontSize = 11.sp, color = CleanTextMuted)
                        Text(
                            text = "Excel, Google Sheets, QuickBooks",
                            fontSize = 11.sp,
                            color = CleanTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CSV Raw Preview
                Text(
                    text = "CSV Preview (Header & Records)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (csvPreview.isNotBlank()) csvPreview else "# No records found in dataset",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action 1: Save CSV to Device via SAF (Primary)
                Button(
                    onClick = {
                        createDocumentLauncher.launch(defaultFileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CleanPrimary,
                        contentColor = CleanOnPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = currentRecordCount > 0 && !isSaving
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save CSV to Device",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Share CSV via FileProvider
                OutlinedButton(
                    onClick = {
                        try {
                            val file = if (isLogsMode) {
                                SalesCsvExporter.exportLogsToCacheFile(context, transactionLogs, currencyFormatter)
                            } else {
                                SalesCsvExporter.exportToCacheFile(context, transactions, currencyFormatter)
                            }
                            val shareIntent = SalesCsvExporter.createShareIntent(context, file)
                            context.startActivity(Intent.createChooser(shareIntent, "Share CSV Export"))
                            Toast.makeText(context, "CSV ready to share", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanPrimary),
                    border = BorderStroke(1.dp, CleanPrimary.copy(alpha = 0.5f)),
                    enabled = currentRecordCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share CSV File",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 3: Copy Raw CSV to Clipboard
                TextButton(
                    onClick = {
                        val csv = if (isLogsMode) {
                            SalesCsvExporter.generateLogsCsvString(transactionLogs, currencyFormatter)
                        } else {
                            SalesCsvExporter.generateCsvString(transactions, currencyFormatter)
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Transaction CSV", csv))
                        Toast.makeText(context, "CSV data copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = currentRecordCount > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CleanTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copy Raw CSV Text",
                        fontSize = 12.sp,
                        color = CleanTextSecondary
                    )
                }
            }
        }
    }
}
