package com.example.data

/**
 * Represents a supported Stripe currency with its ISO-4217 code, symbol,
 * and localization metadata for the payment simulator and checkout flows.
 */
data class Currency(
    val code: String,
    val symbol: String,
    val name: String = code,
    val flagEmoji: String = "🌐",
    val defaultCountry: String = "Global",
    val region: StripeCurrencyRegion = StripeCurrencyRegion.AMERICAS,
    val decimalPlaces: Int = 2,
    val exchangeRate: Double = 1.0,
    val isPopular: Boolean = false
)
