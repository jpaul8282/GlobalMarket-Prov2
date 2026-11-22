package com.example.data

import java.util.Locale

/**
 * Repository contract providing supported Stripe currencies (codes, symbols, and metadata)
 * for the payment simulator and currency selection tools.
 */
interface CurrencyRepository {
    fun getSupportedStripeCurrencies(): List<Currency>
    fun getCurrencyByCode(code: String): Currency?
    fun filterCurrencies(query: String = "", regionFilter: String = "All"): List<Currency>
}

/**
 * Default implementation backed by StripeCurrencyCatalog containing all 136 Stripe-supported
 * ISO-4217 currencies with their codes, symbols, precision, and baseline rates.
 */
class StripeCurrencyRepository(
    private val catalog: StripeCurrencyCatalog = StripeCurrencyCatalog
) : CurrencyRepository {

    private val currencies: List<Currency> by lazy {
        catalog.allStripeCurrencies.map { coin ->
            Currency(
                code = coin.code,
                symbol = coin.symbol,
                name = coin.name,
                flagEmoji = coin.flagEmoji,
                defaultCountry = coin.defaultCountry,
                region = coin.region,
                decimalPlaces = coin.decimalPlaces,
                exchangeRate = coin.approximateUsdRate,
                isPopular = coin.isPopular
            )
        }
    }

    private val currenciesByCode: Map<String, Currency> by lazy {
        currencies.associateBy { it.code.uppercase(Locale.US) }
    }

    override fun getSupportedStripeCurrencies(): List<Currency> = currencies

    override fun getCurrencyByCode(code: String): Currency? {
        val clean = code.trim().uppercase(Locale.US)
        if (clean.isBlank()) return null
        return currenciesByCode[clean]
    }

    override fun filterCurrencies(query: String, regionFilter: String): List<Currency> {
        val cleanQuery = query.trim().lowercase(Locale.US)
        val cleanFilter = regionFilter.trim().lowercase(Locale.US)

        return currencies.filter { currency ->
            val matchesRegion = when (cleanFilter) {
                "", "all" -> true
                "g10" -> currency.code in G10_CURRENCY_CODES
                "popular" -> currency.isPopular
                "0-decimal", "zero-decimal", "zero_decimal" -> currency.decimalPlaces == 0
                "3-decimal", "three-decimal", "three_decimal" -> currency.decimalPlaces == 3
                "europe" -> currency.region == StripeCurrencyRegion.EUROPE
                "asia-pacific", "asia_pacific", "apac" -> currency.region == StripeCurrencyRegion.ASIA_PACIFIC
                "americas" -> currency.region == StripeCurrencyRegion.AMERICAS
                "middle east", "middle_east" -> currency.region == StripeCurrencyRegion.MIDDLE_EAST
                "africa" -> currency.region == StripeCurrencyRegion.AFRICA
                else -> currency.region.displayName.equals(regionFilter.trim(), ignoreCase = true)
            }
            if (!matchesRegion) return@filter false
            if (cleanQuery.isBlank()) return@filter true

            currency.code.lowercase(Locale.US).contains(cleanQuery) ||
                currency.symbol.lowercase(Locale.US).contains(cleanQuery) ||
                currency.name.lowercase(Locale.US).contains(cleanQuery) ||
                currency.defaultCountry.lowercase(Locale.US).contains(cleanQuery) ||
                currency.region.displayName.lowercase(Locale.US).contains(cleanQuery)
        }
    }

    companion object {
        val G10_CURRENCY_CODES = setOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF")
        val DEFAULT: CurrencyRepository = StripeCurrencyRepository()
    }
}
