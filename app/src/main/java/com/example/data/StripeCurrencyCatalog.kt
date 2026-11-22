package com.example.data

import java.util.Locale

enum class StripeCurrencyRegion(val displayName: String) {
    AMERICAS("Americas"),
    EUROPE("Europe"),
    ASIA_PACIFIC("Asia-Pacific"),
    MIDDLE_EAST("Middle East"),
    AFRICA("Africa")
}

/**
 * Official Stripe-supported currency coin metadata covering all 135+ ISO-4217 currencies
 * supported across Stripe Checkout, PaymentIntents, External Accounts, and Payouts.
 */
data class StripeCurrencyCoin(
    val code: String,
    val name: String,
    val symbol: String,
    val flagEmoji: String,
    val defaultCountry: String,
    val region: StripeCurrencyRegion,
    val decimalPlaces: Int = 2,
    val approximateUsdRate: Double = 1.0,
    val supportsInstantPayout: Boolean = false,
    val supportsBankExternalAccount: Boolean = true,
    val isPopular: Boolean = false,
    val coinTintHex: Long = 0xFF635BFF
)

/**
 * Complete catalog of Stripe-supported ISO-4217 currencies & coins (standard 2-decimal,
 * zero-decimal, and 3-decimal currencies).
 */
object StripeCurrencyCatalog {

    val ZERO_DECIMAL_CODES: Set<String> = setOf(
        "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
        "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF"
    )

    val THREE_DECIMAL_CODES: Set<String> = setOf(
        "BHD", "JOD", "KWD", "OMR", "TND"
    )

    val allStripeCurrencies: List<StripeCurrencyCoin> = listOf(
        // Popular / Primary Settlement Coins (12)
        StripeCurrencyCoin("USD", "United States Dollar", "$", "🇺🇸", "United States", StripeCurrencyRegion.AMERICAS, 2, 1.0, supportsInstantPayout = true, isPopular = true, coinTintHex = 0xFF0EA5E9),
        StripeCurrencyCoin("EUR", "Euro", "€", "🇪🇺", "Netherlands", StripeCurrencyRegion.EUROPE, 2, 0.92, supportsInstantPayout = true, isPopular = true, coinTintHex = 0xFF4F46E5),
        StripeCurrencyCoin("GBP", "British Pound Sterling", "£", "🇬🇧", "United Kingdom", StripeCurrencyRegion.EUROPE, 2, 0.78, supportsInstantPayout = true, isPopular = true, coinTintHex = 0xFF7C3AED),
        StripeCurrencyCoin("JPY", "Japanese Yen", "¥", "🇯🇵", "Japan", StripeCurrencyRegion.ASIA_PACIFIC, 0, 155.20, isPopular = true, coinTintHex = 0xFFE11D48),
        StripeCurrencyCoin("CNY", "Chinese Renminbi Yuan", "¥", "🇨🇳", "China", StripeCurrencyRegion.ASIA_PACIFIC, 2, 7.25, isPopular = true, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("CAD", "Canadian Dollar", "CA$", "🇨🇦", "Canada", StripeCurrencyRegion.AMERICAS, 2, 1.37, supportsInstantPayout = true, isPopular = true, coinTintHex = 0xFFEF4444),
        StripeCurrencyCoin("AUD", "Australian Dollar", "A$", "🇦🇺", "Australia", StripeCurrencyRegion.ASIA_PACIFIC, 2, 1.52, supportsInstantPayout = true, isPopular = true, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("CHF", "Swiss Franc", "CHF", "🇨🇭", "Switzerland", StripeCurrencyRegion.EUROPE, 2, 0.90, isPopular = true, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("SGD", "Singapore Dollar", "S$", "🇸🇬", "Singapore", StripeCurrencyRegion.ASIA_PACIFIC, 2, 1.34, supportsInstantPayout = true, isPopular = true, coinTintHex = 0xFF0D9488),
        StripeCurrencyCoin("INR", "Indian Rupee", "₹", "🇮🇳", "India", StripeCurrencyRegion.ASIA_PACIFIC, 2, 83.50, isPopular = true, coinTintHex = 0xFFEA580C),
        StripeCurrencyCoin("BRL", "Brazilian Real", "R$", "🇧🇷", "Brazil", StripeCurrencyRegion.AMERICAS, 2, 5.45, isPopular = true, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("MXN", "Mexican Peso", "Mex$", "🇲🇽", "Mexico", StripeCurrencyRegion.AMERICAS, 2, 18.20, isPopular = true, coinTintHex = 0xFF15803D),

        // Americas (25 additional = 29 Americas total)
        StripeCurrencyCoin("ARS", "Argentine Peso", "AR$", "🇦🇷", "Argentina", StripeCurrencyRegion.AMERICAS, 2, 950.0, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("AWG", "Aruban Florin", "Afl.", "🇦🇼", "Aruba", StripeCurrencyRegion.AMERICAS, 2, 1.80, coinTintHex = 0xFF0EA5E9),
        StripeCurrencyCoin("BBD", "Barbadian Dollar", "Bds$", "🇧🇧", "Barbados", StripeCurrencyRegion.AMERICAS, 2, 2.00, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("BMD", "Bermudian Dollar", "BD$", "🇧🇲", "Bermuda", StripeCurrencyRegion.AMERICAS, 2, 1.00, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("BOB", "Bolivian Boliviano", "Bs.", "🇧🇴", "Bolivia", StripeCurrencyRegion.AMERICAS, 2, 6.91, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("BSD", "Bahamian Dollar", "B$", "🇧🇸", "Bahamas", StripeCurrencyRegion.AMERICAS, 2, 1.00, coinTintHex = 0xFF0891B2),
        StripeCurrencyCoin("BZD", "Belize Dollar", "BZ$", "🇧🇿", "Belize", StripeCurrencyRegion.AMERICAS, 2, 2.01, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("CLP", "Chilean Peso", "CLP$", "🇨🇱", "Chile", StripeCurrencyRegion.AMERICAS, 0, 940.0, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("COP", "Colombian Peso", "COL$", "🇨🇴", "Colombia", StripeCurrencyRegion.AMERICAS, 2, 4150.0, coinTintHex = 0xFFD97706),
        StripeCurrencyCoin("CRC", "Costa Rican Colón", "₡", "🇨🇷", "Costa Rica", StripeCurrencyRegion.AMERICAS, 2, 515.0, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("DOP", "Dominican Peso", "RD$", "🇩🇴", "Dominican Republic", StripeCurrencyRegion.AMERICAS, 2, 59.5, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("FKP", "Falkland Islands Pound", "£", "🇫🇰", "Falkland Islands", StripeCurrencyRegion.AMERICAS, 2, 0.78, coinTintHex = 0xFF4F46E5),
        StripeCurrencyCoin("GTQ", "Guatemalan Quetzal", "Q", "🇬🇹", "Guatemala", StripeCurrencyRegion.AMERICAS, 2, 7.75, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("GYD", "Guyanese Dollar", "G$", "🇬🇾", "Guyana", StripeCurrencyRegion.AMERICAS, 2, 209.0, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("HNL", "Honduran Lempira", "L", "🇭🇳", "Honduras", StripeCurrencyRegion.AMERICAS, 2, 24.8, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("HTG", "Haitian Gourde", "G", "🇭🇹", "Haiti", StripeCurrencyRegion.AMERICAS, 2, 132.0, coinTintHex = 0xFF1E40AF),
        StripeCurrencyCoin("JMD", "Jamaican Dollar", "J$", "🇯🇲", "Jamaica", StripeCurrencyRegion.AMERICAS, 2, 156.5, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("KYD", "Cayman Islands Dollar", "CI$", "🇰🇾", "Cayman Islands", StripeCurrencyRegion.AMERICAS, 2, 0.83, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("NIO", "Nicaraguan Córdoba", "C$", "🇳🇮", "Nicaragua", StripeCurrencyRegion.AMERICAS, 2, 36.8, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("PAB", "Panamanian Balboa", "B/.", "🇵🇦", "Panama", StripeCurrencyRegion.AMERICAS, 2, 1.00, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("PEN", "Peruvian Sol", "S/", "🇵🇪", "Peru", StripeCurrencyRegion.AMERICAS, 2, 3.75, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("PYG", "Paraguayan Guaraní", "₲", "🇵🇾", "Paraguay", StripeCurrencyRegion.AMERICAS, 0, 7550.0, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("SRD", "Surinamese Dollar", "$", "🇸🇷", "Suriname", StripeCurrencyRegion.AMERICAS, 2, 31.5, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("TTD", "Trinidad & Tobago Dollar", "TT$", "🇹🇹", "Trinidad & Tobago", StripeCurrencyRegion.AMERICAS, 2, 6.78, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("UYU", "Uruguayan Peso", "${'$'}U", "🇺🇾", "Uruguay", StripeCurrencyRegion.AMERICAS, 2, 40.5, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("XCD", "East Caribbean Dollar", "EC$", "🇦🇬", "East Caribbean", StripeCurrencyRegion.AMERICAS, 2, 2.70, coinTintHex = 0xFF0D9488),

        // Europe (22 additional = 25 Europe total)
        StripeCurrencyCoin("ALL", "Albanian Lek", "L", "🇦🇱", "Albania", StripeCurrencyRegion.EUROPE, 2, 92.5, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("AMD", "Armenian Dram", "֏", "🇦🇲", "Armenia", StripeCurrencyRegion.EUROPE, 2, 388.0, coinTintHex = 0xFFD97706),
        StripeCurrencyCoin("AZN", "Azerbaijani Manat", "₼", "🇦🇿", "Azerbaijan", StripeCurrencyRegion.EUROPE, 2, 1.70, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("BAM", "Bosnia-Herzegovina Mark", "KM", "🇧🇦", "Bosnia & Herzegovina", StripeCurrencyRegion.EUROPE, 2, 1.80, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("BGN", "Bulgarian Lev", "лв", "🇧🇬", "Bulgaria", StripeCurrencyRegion.EUROPE, 2, 1.80, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("BYN", "Belarusian Ruble", "Br", "🇧🇾", "Belarus", StripeCurrencyRegion.EUROPE, 2, 3.27, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("CZK", "Czech Koruna", "Kč", "🇨🇿", "Czechia", StripeCurrencyRegion.EUROPE, 2, 23.2, isPopular = true, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("DKK", "Danish Krone", "kr", "🇩🇰", "Denmark", StripeCurrencyRegion.EUROPE, 2, 6.86, isPopular = true, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("GEL", "Georgian Lari", "₾", "🇬🇪", "Georgia", StripeCurrencyRegion.EUROPE, 2, 2.72, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("GIP", "Gibraltar Pound", "£", "🇬🇮", "Gibraltar", StripeCurrencyRegion.EUROPE, 2, 0.78, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("HUF", "Hungarian Forint", "Ft", "🇭🇺", "Hungary", StripeCurrencyRegion.EUROPE, 2, 362.0, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("ISK", "Icelandic Króna", "kr", "🇮🇸", "Iceland", StripeCurrencyRegion.EUROPE, 2, 138.0, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("MDL", "Moldovan Leu", "L", "🇲🇩", "Moldova", StripeCurrencyRegion.EUROPE, 2, 17.8, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("MKD", "Macedonian Denar", "ден", "🇲🇰", "North Macedonia", StripeCurrencyRegion.EUROPE, 2, 56.8, coinTintHex = 0xFFD97706),
        StripeCurrencyCoin("NOK", "Norwegian Krone", "kr", "🇳🇴", "Norway", StripeCurrencyRegion.EUROPE, 2, 10.65, isPopular = true, coinTintHex = 0xFF1E40AF),
        StripeCurrencyCoin("PLN", "Polish Złoty", "zł", "🇵🇱", "Poland", StripeCurrencyRegion.EUROPE, 2, 3.96, isPopular = true, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("RON", "Romanian Leu", "lei", "🇷🇴", "Romania", StripeCurrencyRegion.EUROPE, 2, 4.58, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("RSD", "Serbian Dinar", "дин", "🇷🇸", "Serbia", StripeCurrencyRegion.EUROPE, 2, 108.0, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("RUB", "Russian Ruble", "₽", "🇷🇺", "Russia", StripeCurrencyRegion.EUROPE, 2, 91.5, supportsBankExternalAccount = false, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("SEK", "Swedish Krona", "kr", "🇸🇪", "Sweden", StripeCurrencyRegion.EUROPE, 2, 10.45, isPopular = true, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("TRY", "Turkish Lira", "₺", "🇹🇷", "Turkey", StripeCurrencyRegion.EUROPE, 2, 33.8, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("UAH", "Ukrainian Hryvnia", "₴", "🇺🇦", "Ukraine", StripeCurrencyRegion.EUROPE, 2, 41.2, coinTintHex = 0xFF0284C7),

        // Asia-Pacific (32 additional = 37 Asia-Pacific total)
        StripeCurrencyCoin("AFN", "Afghan Afghani", "؋", "🇦🇫", "Afghanistan", StripeCurrencyRegion.ASIA_PACIFIC, 2, 70.5, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("BDT", "Bangladeshi Taka", "৳", "🇧🇩", "Bangladesh", StripeCurrencyRegion.ASIA_PACIFIC, 2, 119.5, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("BND", "Brunei Dollar", "B$", "🇧🇳", "Brunei", StripeCurrencyRegion.ASIA_PACIFIC, 2, 1.34, coinTintHex = 0xFFD97706),
        StripeCurrencyCoin("FJD", "Fijian Dollar", "FJ$", "🇫🇯", "Fiji", StripeCurrencyRegion.ASIA_PACIFIC, 2, 2.24, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("HKD", "Hong Kong Dollar", "HK$", "🇭🇰", "Hong Kong", StripeCurrencyRegion.ASIA_PACIFIC, 2, 7.80, isPopular = true, coinTintHex = 0xFFE11D48),
        StripeCurrencyCoin("IDR", "Indonesian Rupiah", "Rp", "🇮🇩", "Indonesia", StripeCurrencyRegion.ASIA_PACIFIC, 2, 15900.0, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("KGS", "Kyrgyzstani Som", "с", "🇰🇬", "Kyrgyzstan", StripeCurrencyRegion.ASIA_PACIFIC, 2, 85.8, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("KHR", "Cambodian Riel", "៛", "🇰🇭", "Cambodia", StripeCurrencyRegion.ASIA_PACIFIC, 2, 4080.0, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("KRW", "South Korean Won", "₩", "🇰🇷", "South Korea", StripeCurrencyRegion.ASIA_PACIFIC, 0, 1365.0, isPopular = true, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("KZT", "Kazakhstani Tenge", "₸", "🇰🇿", "Kazakhstan", StripeCurrencyRegion.ASIA_PACIFIC, 2, 478.0, coinTintHex = 0xFF0891B2),
        StripeCurrencyCoin("LAK", "Lao Kip", "₭", "🇱🇦", "Laos", StripeCurrencyRegion.ASIA_PACIFIC, 2, 22100.0, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("LKR", "Sri Lankan Rupee", "Rs", "🇱🇰", "Sri Lanka", StripeCurrencyRegion.ASIA_PACIFIC, 2, 301.0, coinTintHex = 0xFFD97706),
        StripeCurrencyCoin("MMK", "Myanmar Kyat", "K", "🇲🇲", "Myanmar", StripeCurrencyRegion.ASIA_PACIFIC, 2, 2100.0, coinTintHex = 0xFFCA8A04),
        StripeCurrencyCoin("MNT", "Mongolian Tögrög", "₮", "🇲🇳", "Mongolia", StripeCurrencyRegion.ASIA_PACIFIC, 2, 3400.0, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("MOP", "Macanese Pataca", "MOP$", "🇲🇴", "Macao", StripeCurrencyRegion.ASIA_PACIFIC, 2, 8.03, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("MVR", "Maldivian Rufiyaa", "Rf", "🇲🇻", "Maldives", StripeCurrencyRegion.ASIA_PACIFIC, 2, 15.4, coinTintHex = 0xFF0D9488),
        StripeCurrencyCoin("MYR", "Malaysian Ringgit", "RM", "🇲🇾", "Malaysia", StripeCurrencyRegion.ASIA_PACIFIC, 2, 4.42, isPopular = true, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("NPR", "Nepalese Rupee", "रू", "🇳🇵", "Nepal", StripeCurrencyRegion.ASIA_PACIFIC, 2, 133.6, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("NZD", "New Zealand Dollar", "NZ$", "🇳🇿", "New Zealand", StripeCurrencyRegion.ASIA_PACIFIC, 2, 1.65, isPopular = true, coinTintHex = 0xFF0F766E),
        StripeCurrencyCoin("PGK", "Papua New Guinean Kina", "K", "🇵🇬", "Papua New Guinea", StripeCurrencyRegion.ASIA_PACIFIC, 2, 3.92, coinTintHex = 0xFFD97706),
        StripeCurrencyCoin("PHP", "Philippine Peso", "₱", "🇵🇭", "Philippines", StripeCurrencyRegion.ASIA_PACIFIC, 2, 56.8, isPopular = true, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("PKR", "Pakistani Rupee", "₨", "🇵🇰", "Pakistan", StripeCurrencyRegion.ASIA_PACIFIC, 2, 278.5, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("SBD", "Solomon Islands Dollar", "SI$", "🇸🇧", "Solomon Islands", StripeCurrencyRegion.ASIA_PACIFIC, 2, 8.45, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("THB", "Thai Baht", "฿", "🇹🇭", "Thailand", StripeCurrencyRegion.ASIA_PACIFIC, 2, 34.8, isPopular = true, coinTintHex = 0xFF4F46E5),
        StripeCurrencyCoin("TJS", "Tajikistani Somoni", "SM", "🇹🇯", "Tajikistan", StripeCurrencyRegion.ASIA_PACIFIC, 2, 10.65, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("TOP", "Tongan Paʻanga", "T$", "🇹🇴", "Tonga", StripeCurrencyRegion.ASIA_PACIFIC, 2, 2.36, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("TWD", "New Taiwan Dollar", "NT$", "🇹🇼", "Taiwan", StripeCurrencyRegion.ASIA_PACIFIC, 2, 32.1, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("UZS", "Uzbekistani Soʻm", "so'm", "🇺🇿", "Uzbekistan", StripeCurrencyRegion.ASIA_PACIFIC, 2, 12650.0, coinTintHex = 0xFF0891B2),
        StripeCurrencyCoin("VND", "Vietnamese Đồng", "₫", "🇻🇳", "Vietnam", StripeCurrencyRegion.ASIA_PACIFIC, 0, 25100.0, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("VUV", "Vanuatu Vatu", "VT", "🇻🇺", "Vanuatu", StripeCurrencyRegion.ASIA_PACIFIC, 0, 119.5, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("WST", "Samoan Tālā", "WS$", "🇼🇸", "Samoa", StripeCurrencyRegion.ASIA_PACIFIC, 2, 2.74, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("XPF", "CFP Franc", "₣", "🇵🇫", "French Polynesia", StripeCurrencyRegion.ASIA_PACIFIC, 0, 110.2, coinTintHex = 0xFF0284C7),

        // Middle East (11 total — includes 3-decimal dinars/rials!)
        StripeCurrencyCoin("AED", "United Arab Emirates Dirham", "د.إ", "🇦🇪", "United Arab Emirates", StripeCurrencyRegion.MIDDLE_EAST, 2, 3.67, isPopular = true, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("BHD", "Bahraini Dinar", ".د.ب", "🇧🇭", "Bahrain", StripeCurrencyRegion.MIDDLE_EAST, 3, 0.376, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("EGP", "Egyptian Pound", "E£", "🇪🇬", "Egypt", StripeCurrencyRegion.MIDDLE_EAST, 2, 48.6, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("ILS", "Israeli New Shekel", "₪", "🇮🇱", "Israel", StripeCurrencyRegion.MIDDLE_EAST, 2, 3.72, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("JOD", "Jordanian Dinar", "د.ا", "🇯🇴", "Jordan", StripeCurrencyRegion.MIDDLE_EAST, 3, 0.709, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("KWD", "Kuwaiti Dinar", "د.ك", "🇰🇼", "Kuwait", StripeCurrencyRegion.MIDDLE_EAST, 3, 0.306, coinTintHex = 0xFF0D9488),
        StripeCurrencyCoin("LBP", "Lebanese Pound", "ل.ل", "🇱🇧", "Lebanon", StripeCurrencyRegion.MIDDLE_EAST, 2, 89500.0, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("OMR", "Omani Rial", "ر.ع.", "🇴🇲", "Oman", StripeCurrencyRegion.MIDDLE_EAST, 3, 0.385, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("QAR", "Qatari Riyal", "ر.ق", "🇶🇦", "Qatar", StripeCurrencyRegion.MIDDLE_EAST, 2, 3.64, coinTintHex = 0xFF7C3AED),
        StripeCurrencyCoin("SAR", "Saudi Riyal", "﷼", "🇸🇦", "Saudi Arabia", StripeCurrencyRegion.MIDDLE_EAST, 2, 3.75, isPopular = true, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("YER", "Yemeni Rial", "﷼", "🇾🇪", "Yemen", StripeCurrencyRegion.MIDDLE_EAST, 2, 250.3, coinTintHex = 0xFFDC2626),

        // Africa (33 total — includes 0-decimal CFA/francs & 3-decimal TND!)
        StripeCurrencyCoin("AOA", "Angolan Kwanza", "Kz", "🇦🇴", "Angola", StripeCurrencyRegion.AFRICA, 2, 912.0, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("BIF", "Burundian Franc", "FBu", "🇧🇮", "Burundi", StripeCurrencyRegion.AFRICA, 0, 2890.0, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("BWP", "Botswana Pula", "P", "🇧🇼", "Botswana", StripeCurrencyRegion.AFRICA, 2, 13.5, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("CDF", "Congolese Franc", "FC", "🇨🇩", "DR Congo", StripeCurrencyRegion.AFRICA, 2, 2840.0, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("CVE", "Cape Verdean Escudo", "$", "🇨🇻", "Cabo Verde", StripeCurrencyRegion.AFRICA, 2, 101.4, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("DJF", "Djiboutian Franc", "Fdj", "🇩🇯", "Djibouti", StripeCurrencyRegion.AFRICA, 0, 177.7, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("DZD", "Algerian Dinar", "د.ج", "🇩🇿", "Algeria", StripeCurrencyRegion.AFRICA, 2, 134.2, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("ETB", "Ethiopian Birr", "Br", "🇪🇹", "Ethiopia", StripeCurrencyRegion.AFRICA, 2, 112.0, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("GMD", "Gambian Dalasi", "D", "🇬🇲", "Gambia", StripeCurrencyRegion.AFRICA, 2, 69.5, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("GNF", "Guinean Franc", "FG", "🇬🇳", "Guinea", StripeCurrencyRegion.AFRICA, 0, 8620.0, coinTintHex = 0xFFCA8A04),
        StripeCurrencyCoin("KES", "Kenyan Shilling", "KSh", "🇰🇪", "Kenya", StripeCurrencyRegion.AFRICA, 2, 129.0, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("KMF", "Comorian Franc", "CF", "🇰🇲", "Comoros", StripeCurrencyRegion.AFRICA, 0, 452.5, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("LSL", "Lesotho Loti", "L", "🇱🇸", "Lesotho", StripeCurrencyRegion.AFRICA, 2, 17.8, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("MAD", "Moroccan Dirham", "د.م.", "🇲🇦", "Morocco", StripeCurrencyRegion.AFRICA, 2, 9.85, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("MGA", "Malagasy Ariary", "Ar", "🇲🇬", "Madagascar", StripeCurrencyRegion.AFRICA, 0, 4560.0, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("MUR", "Mauritian Rupee", "₨", "🇲🇺", "Mauritius", StripeCurrencyRegion.AFRICA, 2, 46.2, coinTintHex = 0xFFEA580C),
        StripeCurrencyCoin("MWK", "Malawian Kwacha", "MK", "🇲🇼", "Malawi", StripeCurrencyRegion.AFRICA, 2, 1735.0, coinTintHex = 0xFFB91C1C),
        StripeCurrencyCoin("MZN", "Mozambican Metical", "MT", "🇲🇿", "Mozambique", StripeCurrencyRegion.AFRICA, 2, 63.9, coinTintHex = 0xFF0D9488),
        StripeCurrencyCoin("NAD", "Namibian Dollar", "N$", "🇳🇦", "Namibia", StripeCurrencyRegion.AFRICA, 2, 17.8, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("NGN", "Nigerian Naira", "₦", "🇳🇬", "Nigeria", StripeCurrencyRegion.AFRICA, 2, 1610.0, isPopular = true, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("RWF", "Rwandan Franc", "FRw", "🇷🇼", "Rwanda", StripeCurrencyRegion.AFRICA, 0, 1345.0, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("SCR", "Seychellois Rupee", "₨", "🇸🇨", "Seychelles", StripeCurrencyRegion.AFRICA, 2, 13.8, coinTintHex = 0xFF0EA5E9),
        StripeCurrencyCoin("SHP", "Saint Helena Pound", "£", "🇸🇭", "Saint Helena", StripeCurrencyRegion.AFRICA, 2, 0.78, coinTintHex = 0xFF2563EB),
        StripeCurrencyCoin("SLE", "Sierra Leonean Leone", "Le", "🇸🇱", "Sierra Leone", StripeCurrencyRegion.AFRICA, 2, 22.6, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("SOS", "Somali Shilling", "Sh", "🇸🇴", "Somalia", StripeCurrencyRegion.AFRICA, 2, 571.0, coinTintHex = 0xFF0284C7),
        StripeCurrencyCoin("SZL", "Swazi Lilangeni", "E", "🇸🇿", "Eswatini", StripeCurrencyRegion.AFRICA, 2, 17.8, coinTintHex = 0xFF1D4ED8),
        StripeCurrencyCoin("TND", "Tunisian Dinar", "د.ت", "🇹🇳", "Tunisia", StripeCurrencyRegion.AFRICA, 3, 3.08, coinTintHex = 0xFFDC2626),
        StripeCurrencyCoin("TZS", "Tanzanian Shilling", "TSh", "🇹🇿", "Tanzania", StripeCurrencyRegion.AFRICA, 2, 2710.0, coinTintHex = 0xFF0D9488),
        StripeCurrencyCoin("UGX", "Ugandan Shilling", "USh", "🇺🇬", "Uganda", StripeCurrencyRegion.AFRICA, 0, 3710.0, coinTintHex = 0xFFD97706),
        StripeCurrencyCoin("XAF", "Central African CFA Franc", "FCFA", "🇨🇲", "CEMAC Zone", StripeCurrencyRegion.AFRICA, 0, 603.5, coinTintHex = 0xFF15803D),
        StripeCurrencyCoin("XOF", "West African CFA Franc", "CFA", "🇸🇳", "UEMOA Zone", StripeCurrencyRegion.AFRICA, 0, 603.5, coinTintHex = 0xFF0F766E),
        StripeCurrencyCoin("ZAR", "South African Rand", "R", "🇿🇦", "South Africa", StripeCurrencyRegion.AFRICA, 2, 17.85, isPopular = true, coinTintHex = 0xFF16A34A),
        StripeCurrencyCoin("ZMW", "Zambian Kwacha", "ZK", "🇿🇲", "Zambia", StripeCurrencyRegion.AFRICA, 2, 26.4, coinTintHex = 0xFF15803D)
    )

    private val coinsByCode: Map<String, StripeCurrencyCoin> =
        allStripeCurrencies.associateBy { it.code.uppercase(Locale.US) }

    fun findByCode(code: String?): StripeCurrencyCoin? {
        val clean = code?.trim()?.uppercase(Locale.US).orEmpty()
        if (clean.isBlank()) return null
        return coinsByCode[clean]
    }

    fun decimalPlacesFor(code: String): Int {
        val clean = code.trim().uppercase(Locale.US)
        return when {
            ZERO_DECIMAL_CODES.contains(clean) -> 0
            THREE_DECIMAL_CODES.contains(clean) -> 3
            else -> coinsByCode[clean]?.decimalPlaces ?: 2
        }
    }

    fun filterCoins(
        query: String = "",
        categoryFilter: String = "ALL"
    ): List<StripeCurrencyCoin> {
        val cleanQuery = query.trim().lowercase(Locale.US)
        return allStripeCurrencies.filter { coin ->
            val matchesCategory = when (categoryFilter.uppercase(Locale.US)) {
                "ALL" -> true
                "POPULAR" -> coin.isPopular
                "ZERO_DECIMAL" -> coin.decimalPlaces == 0
                "THREE_DECIMAL" -> coin.decimalPlaces == 3
                "INSTANT" -> coin.supportsInstantPayout
                "AMERICAS" -> coin.region == StripeCurrencyRegion.AMERICAS
                "EUROPE" -> coin.region == StripeCurrencyRegion.EUROPE
                "ASIA_PACIFIC" -> coin.region == StripeCurrencyRegion.ASIA_PACIFIC
                "MIDDLE_EAST" -> coin.region == StripeCurrencyRegion.MIDDLE_EAST
                "AFRICA" -> coin.region == StripeCurrencyRegion.AFRICA
                else -> true
            }
            if (!matchesCategory) return@filter false
            if (cleanQuery.isBlank()) return@filter true

            coin.code.lowercase(Locale.US).contains(cleanQuery) ||
                coin.name.lowercase(Locale.US).contains(cleanQuery) ||
                coin.symbol.lowercase(Locale.US).contains(cleanQuery) ||
                coin.defaultCountry.lowercase(Locale.US).contains(cleanQuery) ||
                coin.region.displayName.lowercase(Locale.US).contains(cleanQuery)
        }
    }
}
