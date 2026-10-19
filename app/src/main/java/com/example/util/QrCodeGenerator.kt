package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Utility for generating QR Code Bitmaps for customer mobile wallet payments.
 */
object QrCodeGenerator {

    /**
     * Generates a square Bitmap of a QR code representing the given content string.
     *
     * @param content The string payload (e.g. payment deep link, EPC-QR, UPI, or formatted payment URI).
     * @param size The width and height in pixels.
     * @param foregroundColor ARGB color for the dark modules (default Black).
     * @param backgroundColor ARGB color for the light modules/quiet zone (default White).
     * @return Bitmap of the QR code, or null if generation failed.
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank() || size <= 0) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1) // compact quiet zone
            }

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds a standardized mobile wallet payment URI string based on currency and amount.
     * Compatible with standard mobile wallet QR scanners and deep link handlers.
     */
    fun buildPaymentUri(
        merchantId: String,
        currencyCode: String,
        amount: Double,
        reference: String,
        merchantName: String = "Glaba Global Merchant"
    ): String {
        val formattedAmount = String.format(java.util.Locale.US, "%.2f", amount)
        return when (currencyCode.uppercase()) {
            "EUR" -> {
                // EPC QR Code (European Payments Council) / SEPA or universal pay URI
                "https://glaba.pro/pay?m=$merchantId&c=EUR&amt=$formattedAmount&ref=$reference"
            }
            "GBP" -> {
                // UK Pay by Bank / Open Banking universal URI
                "https://glaba.pro/pay?m=$merchantId&c=GBP&amt=$formattedAmount&ref=$reference"
            }
            "CNY" -> {
                // Alipay / WeChat Pay compatible payment string
                "https://glaba.pro/pay?m=$merchantId&c=CNY&amt=$formattedAmount&ref=$reference"
            }
            "JPY" -> {
                val intAmount = amount.toInt()
                "https://glaba.pro/pay?m=$merchantId&c=JPY&amt=$intAmount&ref=$reference"
            }
            else -> {
                // USD and default: universal mobile wallet checkout link
                "https://glaba.pro/pay?m=$merchantId&c=$currencyCode&amt=$formattedAmount&ref=$reference"
            }
        }
    }
}
