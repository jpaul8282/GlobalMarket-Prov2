package com.example.util

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.example.ui.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Currency utility class that leverages the user's locale settings to format sales amounts.
 *
 * It provides locale-aware formatting for international currencies (USD, EUR, GBP, JPY, CNY, etc.),
 * correctly rendering locale-specific currency symbols, symbol placement (prefix vs suffix),
 * grouping separators, decimal separators, and standard fraction digits (e.g. 0 decimals for JPY,
 * 2 decimals for USD/EUR/GBP).
 */
class CurrencyFormatter(
    val locale: Locale = Locale.getDefault()
) {

    /**
     * Formats a monetary sales amount for a given currency code using the configured locale.
     * Examples:
     * - US Locale, EUR 45.99 -> "€45.99"
     * - France Locale, EUR 45.99 -> "45,99 €"
     * - US Locale, JPY 23280.0 -> "¥23,280"
     * - Japan Locale, JPY 23280.0 -> "￥23,280"
     */
    fun format(amount: Double, currencyCode: String): String {
        return CurrencyUtils.formatAmount(amount, currencyCode, locale)
    }

    /**
     * Formats a sales amount specifically for list items and cards.
     */
    fun formatSalesAmount(amount: Double, currencyCode: String): String {
        return CurrencyUtils.formatSalesAmount(amount, currencyCode, locale)
    }

    /**
     * Formats a USD base amount using the configured locale.
     * Example: 49.99 -> "$49.99" in US locale, "49,99 $US" in French locale.
     */
    fun formatUsd(amount: Double): String {
        return CurrencyUtils.formatUsd(amount, locale)
    }

    /**
     * Formats a sales amount with explicit currency code annotation.
     * Example: "$45.99 (USD)" or "45,99 € (EUR)"
     */
    fun formatWithCode(amount: Double, currencyCode: String): String {
        val formatted = format(amount, currencyCode)
        return "$formatted (${currencyCode.trim().uppercase()})"
    }

    /**
     * Gets the currency symbol for the given currency code under this locale.
     */
    fun getSymbol(currencyCode: String): String {
        return CurrencyUtils.getCurrencySymbol(currencyCode, locale)
    }

    companion object {
        val DEFAULT = CurrencyFormatter()

        /**
         * Creates a CurrencyFormatter instance configured with the user's active locale settings,
         * resolving from Context and optional in-app AppLanguage.
         */
        fun from(context: Context? = null, appLanguage: AppLanguage? = null): CurrencyFormatter {
            val resolvedLocale = CurrencyUtils.resolveUserLocale(context, appLanguage)
            return CurrencyFormatter(resolvedLocale)
        }

        /**
         * Creates a CurrencyFormatter instance configured for a specific AppLanguage.
         */
        fun fromLanguage(appLanguage: AppLanguage): CurrencyFormatter {
            return CurrencyFormatter(CurrencyUtils.localeFromLanguage(appLanguage))
        }
    }
}

/**
 * Static currency formatting utility methods and locale resolvers.
 */
object CurrencyUtils {

    /**
     * Maps the app's supported language enumeration to a standard Java Locale.
     */
    fun localeFromLanguage(appLanguage: AppLanguage): Locale {
        return when (appLanguage) {
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.ESPANOL -> Locale.forLanguageTag("es-ES")
            AppLanguage.FRANCAIS -> Locale.FRANCE
            AppLanguage.DEUTSCH -> Locale.GERMANY
            AppLanguage.CHINESE -> Locale.SIMPLIFIED_CHINESE
        }
    }

    /**
     * Resolves the effective user locale from device context and optional app language selection.
     */
    fun resolveUserLocale(context: Context? = null, appLanguage: AppLanguage? = null): Locale {
        if (appLanguage != null) {
            return localeFromLanguage(appLanguage)
        }

        if (context != null) {
            val locales = ConfigurationCompat.getLocales(context.resources.configuration)
            if (!locales.isEmpty) {
                locales[0]?.let { return it }
            }
        }

        return Locale.getDefault()
    }

    /**
     * Formats an amount with a currency code using the specified locale.
     * Uses Java NumberFormat currency instance configured with the appropriate Currency instance.
     */
    fun formatAmount(
        amount: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault()
    ): String {
        val cleanCode = currencyCode.trim().uppercase()
        return try {
            val currency = Currency.getInstance(cleanCode)
            val formatter = NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
                maximumFractionDigits = currency.defaultFractionDigits
                minimumFractionDigits = currency.defaultFractionDigits
            }
            formatter.format(amount)
        } catch (e: Exception) {
            // Graceful fallback for custom or unrecognised currency tokens
            val symbol = getFallbackSymbol(cleanCode)
            val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(locale))
            if (symbol.isNotEmpty()) "$symbol${decimalFormat.format(amount)}" else "${decimalFormat.format(amount)} $cleanCode"
        }
    }

    /**
     * Formats a sales amount with graceful handling and clean representation.
     */
    fun formatSalesAmount(
        amount: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault()
    ): String {
        return formatAmount(amount, currencyCode, locale)
    }

    /**
     * Formats a USD amount using the specified locale.
     */
    fun formatUsd(amount: Double, locale: Locale = Locale.getDefault()): String {
        return formatAmount(amount, "USD", locale)
    }

    /**
     * Formats an amount using context to resolve the user's locale settings.
     */
    fun formatWithContext(
        context: Context,
        amount: Double,
        currencyCode: String,
        appLanguage: AppLanguage? = null
    ): String {
        val locale = resolveUserLocale(context, appLanguage)
        return formatAmount(amount, currencyCode, locale)
    }

    /**
     * Retrieves the currency symbol according to the given currency code and locale.
     */
    fun getCurrencySymbol(currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val cleanCode = currencyCode.trim().uppercase()
        return try {
            val currency = Currency.getInstance(cleanCode)
            currency.getSymbol(locale)
        } catch (e: Exception) {
            getFallbackSymbol(cleanCode)
        }
    }

    /**
     * Fallback symbol for known or unregistered currencies.
     */
    fun getFallbackSymbol(currencyCode: String): String {
        return when (currencyCode.trim().uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CNY" -> "¥"
            "CAD" -> "CA$"
            "AUD" -> "A$"
            "CHF" -> "CHF"
            else -> currencyCode
        }
    }

    /**
     * Retrieves the standard number of fraction digits for a currency (e.g. JPY = 0, USD = 2).
     */
    fun getFractionDigits(currencyCode: String): Int {
        return try {
            Currency.getInstance(currencyCode.trim().uppercase()).defaultFractionDigits
        } catch (e: Exception) {
            2
        }
    }

    /**
     * Gets the decimal separator char for the given locale (e.g. '.' in US, ',' in France/Germany).
     */
    fun getDecimalSeparator(locale: Locale = Locale.getDefault()): Char {
        return DecimalFormatSymbols.getInstance(locale).decimalSeparator
    }

    /**
     * Gets the grouping separator char for the given locale (e.g. ',' in US, '.' in Germany, ' ' in France).
     */
    fun getGroupingSeparator(locale: Locale = Locale.getDefault()): Char {
        return DecimalFormatSymbols.getInstance(locale).groupingSeparator
    }
}
