package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PayoutWireEntity
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanOnPrimary
import com.example.ui.theme.CleanPrimary
import com.example.ui.theme.CleanPrimaryContainer
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanSuccessContainer
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.util.CurrencyFormatter
import com.example.util.PayoutExportFormat
import com.example.util.PayoutExportScope
import com.example.util.PayoutHistoryExporter

@Composable
fun PayoutHistoryExportDialog(
    allRecords: List<PayoutWireEntity>,
    currencyFormatter: CurrencyFormatter,
    initialFormat: PayoutExportFormat = PayoutExportFormat.CSV,
    initialScope: PayoutExportScope = if (
        PayoutHistoryExporter.filterForScope(allRecords, PayoutExportScope.PROCESSED_ONLY).isNotEmpty()
    ) {
        PayoutExportScope.PROCESSED_ONLY
    } else if (allRecords.isNotEmpty()) {
        PayoutExportScope.ALL_HISTORY
    } else {
        PayoutExportScope.PROCESSED_ONLY
    },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    var selectedScope by remember { mutableStateOf(initialScope) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val processedRecords = remember(allRecords) {
        PayoutHistoryExporter.filterForScope(allRecords, PayoutExportScope.PROCESSED_ONLY)
    }
    val scopedRecords = remember(allRecords, selectedScope) {
        PayoutHistoryExporter.filterForScope(allRecords, selectedScope)
    }
    val formattedOutput = remember(scopedRecords, selectedFormat, selectedScope) {
        PayoutHistoryExporter.generateFormattedText(
            records = scopedRecords,
            format = selectedFormat,
            scope = selectedScope
        )
    }
    val previewOutput = remember(formattedOutput) {
        val lines = formattedOutput.lines()
        if (lines.size > 16) {
            lines.take(16).joinToString("\n") + "\n... (${lines.size - 16} more lines)"
        } else {
            formattedOutput
        }
    }
    val totalNetUsd = remember(scopedRecords) { scopedRecords.sumOf { it.netAmountUsd } }
    val totalFeesUsd = remember(scopedRecords) { scopedRecords.sumOf { it.feeUsd } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .padding(vertical = 16.dp)
                .testTag("payout_export_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CleanSurface),
            border = BorderStroke(1.dp, CleanBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CleanPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedFormat == PayoutExportFormat.CSV) {
                                    Icons.Default.TableChart
                                } else {
                                    Icons.Default.Code
                                },
                                contentDescription = null,
                                tint = CleanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Export Payout History",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanTextPrimary
                            )
                            Text(
                                text = "Copy Room database records as CSV or JSON",
                                fontSize = 11.sp,
                                color = CleanTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("payout_export_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close export dialog",
                            tint = CleanTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Format Switcher: CSV vs JSON
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "EXPORT FORMAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CleanSurfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PayoutExportFormat.entries.forEach { format ->
                            val isSelected = selectedFormat == format
                            Surface(
                                onClick = {
                                    selectedFormat = format
                                    copiedFeedback = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) CleanSurface else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("payout_export_format_${format.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (format == PayoutExportFormat.CSV) {
                                            Icons.Default.TableChart
                                        } else {
                                            Icons.Default.Code
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) CleanPrimary else CleanTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${format.label} Text",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CleanTextPrimary else CleanTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Scope Switcher: Processed Payouts vs All Room Records
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ROOM DATASET SCOPE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanTextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                selectedScope = PayoutExportScope.PROCESSED_ONLY
                                copiedFeedback = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedScope == PayoutExportScope.PROCESSED_ONLY) {
                                CleanPrimaryContainer
                            } else {
                                CleanSurfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selectedScope == PayoutExportScope.PROCESSED_ONLY) {
                                    CleanPrimary
                                } else {
                                    CleanBorderSubtle
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("payout_export_scope_processed")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = "Processed (${processedRecords.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedScope == PayoutExportScope.PROCESSED_ONLY) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    color = if (selectedScope == PayoutExportScope.PROCESSED_ONLY) {
                                        CleanPrimary
                                    } else {
                                        CleanTextSecondary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                selectedScope = PayoutExportScope.ALL_HISTORY
                                copiedFeedback = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedScope == PayoutExportScope.ALL_HISTORY) {
                                CleanPrimaryContainer
                            } else {
                                CleanSurfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selectedScope == PayoutExportScope.ALL_HISTORY) {
                                    CleanPrimary
                                } else {
                                    CleanBorderSubtle
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("payout_export_scope_all")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = "All History (${allRecords.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedScope == PayoutExportScope.ALL_HISTORY) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    color = if (selectedScope == PayoutExportScope.ALL_HISTORY) {
                                        CleanPrimary
                                    } else {
                                        CleanTextSecondary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Summary Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CleanSurfaceVariant)
                        .border(BorderStroke(1.dp, CleanBorderSubtle), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Records", fontSize = 10.sp, color = CleanTextMuted)
                        Text(
                            text = "${scopedRecords.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Fees (USD)", fontSize = 10.sp, color = CleanTextMuted)
                        Text(
                            text = currencyFormatter.formatUsd(totalFeesUsd),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Disbursed", fontSize = 10.sp, color = CleanTextMuted)
                        Text(
                            text = currencyFormatter.formatUsd(totalNetUsd),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanPrimary
                        )
                    }
                }

                // Formatted Output Preview
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedFormat.label} Clipboard Preview",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanTextSecondary
                        )
                        Text(
                            text = "${formattedOutput.length} chars",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CleanTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .testTag("payout_export_preview_box")
                    ) {
                        Text(
                            text = previewOutput,
                            color = if (selectedFormat == PayoutExportFormat.CSV) {
                                Color(0xFF38BDF8)
                            } else {
                                Color(0xFF34D399)
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp,
                            modifier = Modifier.testTag("payout_export_preview_text")
                        )
                    }
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val copied = PayoutHistoryExporter.copyToClipboard(
                                context = context,
                                label = "Payout History (${selectedFormat.label})",
                                content = formattedOutput
                            )
                            if (copied) {
                                copiedFeedback = true
                                Toast.makeText(
                                    context,
                                    "${selectedFormat.label} payout history copied to clipboard!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Unable to access system clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = scopedRecords.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("payout_export_copy_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copiedFeedback) CleanSuccess else CleanPrimary,
                            contentColor = CleanOnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (copiedFeedback) {
                                "Copied ${selectedFormat.label} to Clipboard!"
                            } else {
                                "Copy ${selectedFormat.label} to Clipboard"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (scopedRecords.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CleanSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No records found for \"${selectedScope.label}\". Switch scope or record a payout first.",
                                fontSize = 11.sp,
                                color = CleanTextSecondary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else if (copiedFeedback) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CleanSuccessContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payout_export_copied_banner")
                        ) {
                            Text(
                                text = "Ready to paste (${scopedRecords.size} ${selectedScope.label.lowercase()} exported as ${selectedFormat.label}).",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanSuccess,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanTextSecondary
                        )
                    }
                }
            }
        }
    }
}
