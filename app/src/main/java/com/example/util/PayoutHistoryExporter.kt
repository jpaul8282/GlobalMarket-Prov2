package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.data.PayoutWireEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class PayoutExportFormat(val label: String, val mimeType: String) {
    CSV("CSV", "text/csv"),
    JSON("JSON", "application/json")
}

enum class PayoutExportScope(val label: String) {
    PROCESSED_ONLY("Processed Payouts"),
    ALL_HISTORY("All Payouts & Wires")
}

/**
 * Formats Room-persisted [PayoutWireEntity] history into RFC 4180 CSV or JSON text
 * suitable for clipboard export and external accounting tools.
 */
object PayoutHistoryExporter {

    fun filterForScope(
        records: List<PayoutWireEntity>,
        scope: PayoutExportScope
    ): List<PayoutWireEntity> {
        return when (scope) {
            PayoutExportScope.PROCESSED_ONLY -> records.filter {
                !it.isSimulatedWire && it.transactionType == "PROCESSED_PAYOUT"
            }
            PayoutExportScope.ALL_HISTORY -> records
        }
    }

    fun generateFormattedText(
        records: List<PayoutWireEntity>,
        format: PayoutExportFormat,
        scope: PayoutExportScope = PayoutExportScope.PROCESSED_ONLY
    ): String {
        val scoped = filterForScope(records, scope)
        return when (format) {
            PayoutExportFormat.CSV -> generateCsvString(scoped)
            PayoutExportFormat.JSON -> generateJsonString(scoped, scope)
        }
    }

    fun generateCsvString(records: List<PayoutWireEntity>): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val sb = StringBuilder()
        val headers = listOf(
            "id",
            "referenceCode",
            "transactionType",
            "method",
            "clearingRail",
            "status",
            "isSimulatedWire",
            "grossAmountUsd",
            "feeUsd",
            "netAmountUsd",
            "convertedAmount",
            "currencyCode",
            "exchangeRate",
            "recipientName",
            "bankName",
            "ibanOrAccount",
            "swiftBic",
            "routingNumber",
            "intermediaryBank",
            "country",
            "timestampIsoUtc",
            "timestampEpochMs",
            "notes"
        )
        sb.appendLine(headers.joinToString(",") { escapeCsv(it) })

        records.forEach { rec ->
            val row = listOf(
                rec.id.toString(),
                rec.referenceCode,
                rec.transactionType,
                rec.method,
                rec.clearingRail,
                rec.status,
                rec.isSimulatedWire.toString(),
                String.format(Locale.US, "%.2f", rec.grossAmountUsd),
                String.format(Locale.US, "%.2f", rec.feeUsd),
                String.format(Locale.US, "%.2f", rec.netAmountUsd),
                String.format(Locale.US, "%.2f", rec.convertedAmount),
                rec.currencyCode,
                String.format(Locale.US, "%.6f", rec.exchangeRate),
                rec.recipientName,
                rec.bankName,
                rec.ibanOrAccount,
                rec.swiftBic,
                rec.routingNumber,
                rec.intermediaryBank,
                rec.country,
                isoFormat.format(Date(rec.timestamp)),
                rec.timestamp.toString(),
                rec.notes.orEmpty()
            )
            sb.appendLine(row.joinToString(",") { escapeCsv(it) })
        }

        return sb.toString().trimEnd()
    }

    fun generateJsonString(
        records: List<PayoutWireEntity>,
        scope: PayoutExportScope = PayoutExportScope.PROCESSED_ONLY,
        indentSpaces: Int = 2
    ): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val summaryObj = JSONObject().apply {
            put("totalRecords", records.size)
            put(
                "settledRecords",
                records.count {
                    it.status.equals("COMPLETED", ignoreCase = true) ||
                        it.status.equals("SETTLED", ignoreCase = true)
                }
            )
            put("totalGrossUsd", roundTwoDecimals(records.sumOf { it.grossAmountUsd }))
            put("totalFeesUsd", roundTwoDecimals(records.sumOf { it.feeUsd }))
            put("totalNetUsd", roundTwoDecimals(records.sumOf { it.netAmountUsd }))
        }

        val recordsArray = JSONArray()
        records.forEach { rec ->
            val itemObj = JSONObject().apply {
                put("id", rec.id)
                put("referenceCode", rec.referenceCode)
                put("transactionType", rec.transactionType)
                put("method", rec.method)
                put("clearingRail", rec.clearingRail)
                put("status", rec.status)
                put("isSimulatedWire", rec.isSimulatedWire)
                put("grossAmountUsd", roundTwoDecimals(rec.grossAmountUsd))
                put("feeUsd", roundTwoDecimals(rec.feeUsd))
                put("netAmountUsd", roundTwoDecimals(rec.netAmountUsd))
                put("convertedAmount", roundTwoDecimals(rec.convertedAmount))
                put("currencyCode", rec.currencyCode)
                put("exchangeRate", rec.exchangeRate)
                put("recipientName", rec.recipientName)
                put("bankName", rec.bankName)
                put("ibanOrAccount", rec.ibanOrAccount)
                put("swiftBic", rec.swiftBic)
                put("routingNumber", rec.routingNumber)
                put("intermediaryBank", rec.intermediaryBank)
                put("country", rec.country)
                put("timestampIsoUtc", isoFormat.format(Date(rec.timestamp)))
                put("timestampEpochMs", rec.timestamp)
                put("notes", rec.notes ?: JSONObject.NULL)
            }
            recordsArray.put(itemObj)
        }

        val root = JSONObject().apply {
            put("exportedAtIsoUtc", isoFormat.format(Date()))
            put("scope", scope.name)
            put("summary", summaryObj)
            put("records", recordsArray)
        }

        return root.toString(indentSpaces)
    }

    fun copyToClipboard(
        context: Context,
        label: String,
        content: String
    ): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun roundTwoDecimals(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }
}
