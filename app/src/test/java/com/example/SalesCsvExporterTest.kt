package com.example

import com.example.data.TransactionLogEntity
import com.example.util.CurrencyFormatter
import com.example.util.SalesCsvExporter
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesCsvExporterTest {

    @Test
    fun testGenerateLogsCsvString_containsExpectedHeadersAndValues() {
        val logs = listOf(
            TransactionLogEntity(
                id = 101,
                logType = "SCANNED_ITEM",
                title = "UPC-A Retail Milk",
                barcodeOrRawValue = "012345678905",
                barcodeFormat = "UPC_A",
                amount = 4.99,
                currencyCode = "USD",
                amountInUsd = 4.99,
                customerOrRecipient = "Supermarket POS",
                country = "United States",
                paymentMethod = "SCANNER",
                status = "COMPLETED",
                referenceCode = "TX-SC-101",
                notes = "Organic 2% Milk"
            ),
            TransactionLogEntity(
                id = 102,
                logType = "PAYMENT",
                title = "Global POS Terminal",
                barcodeOrRawValue = null,
                barcodeFormat = null,
                amount = 89.00,
                currencyCode = "EUR",
                amountInUsd = 97.01,
                customerOrRecipient = "Alice Martin",
                country = "France",
                paymentMethod = "CARD",
                status = "COMPLETED",
                referenceCode = "TX-PM-102",
                notes = "Contactless Visa"
            )
        )

        val csv = SalesCsvExporter.generateLogsCsvString(logs, CurrencyFormatter.DEFAULT)

        // Verify UTF-8 BOM
        assertTrue(csv.startsWith("\uFEFF"))

        // Verify Header Columns
        assertTrue(csv.contains("Log ID"))
        assertTrue(csv.contains("Record Type"))
        assertTrue(csv.contains("Barcode / Raw Value"))
        assertTrue(csv.contains("Base USD Equivalent"))
        assertTrue(csv.contains("Reference Code"))

        // Verify Item 1 values
        assertTrue(csv.contains("SCANNED_ITEM"))
        assertTrue(csv.contains("012345678905"))
        assertTrue(csv.contains("UPC_A"))
        assertTrue(csv.contains("4.99"))

        // Verify Item 2 values
        assertTrue(csv.contains("Alice Martin"))
        assertTrue(csv.contains("EUR"))
        assertTrue(csv.contains("89.00"))

        // Verify Summary Totals row
        assertTrue(csv.contains("SUMMARY_TOTALS"))
        assertTrue(csv.contains("Total Logs: 2"))
    }
}
