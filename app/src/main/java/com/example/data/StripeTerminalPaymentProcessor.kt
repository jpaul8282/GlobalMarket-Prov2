package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.util.CurrencyFormatter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Stripe SDK-backed Payment Processing Engine for localized Sales Terminal transactions.
 *
 * Security & PCI-DSS guarantees:
 * - Raw PAN (card number) and CVC/CVV are used strictly in memory to validate and construct
 *   [PaymentMethodCreateParams] for the Stripe SDK.
 * - Neither raw card numbers nor CVC codes are ever persisted to Room, Firestore, or logs.
 * - Simulated terminal transactions use Stripe's official PaymentConfiguration /
 *   PaymentMethodCreateParams / ConfirmPaymentIntentParams structures so payloads match
 *   production Stripe integration without charging real cards during terminal simulation.
 */
enum class StripeTerminalPaymentMode(val id: String, val label: String) {
    CARD("card", "Card (Stripe SDK)"),
    LOCAL_RAIL("local_rail", "Localized Rail")
}

enum class StripeCardBrand(
    val displayName: String,
    val expectedCvcLength: Int
) {
    VISA("Visa", 3),
    MASTERCARD("Mastercard", 3),
    AMEX("American Express", 4),
    DISCOVER("Discover", 3),
    JCB("JCB", 3),
    UNIONPAY("UnionPay", 3),
    UNKNOWN("Card", 3)
}

data class StripeTerminalValidationResult(
    val isValid: Boolean,
    val cardNumberError: String? = null,
    val expiryError: String? = null,
    val cvcError: String? = null,
    val holderNameError: String? = null,
    val ibanError: String? = null,
    val detectedBrand: StripeCardBrand = StripeCardBrand.UNKNOWN,
    val sanitizedDigits: String = "",
    val parsedExpiryMonth: Int = 0,
    val parsedExpiryYearFull: Int = 0
)

data class StripeLocalizedRailSpec(
    val railId: String,
    val railName: String,
    val stripeMethodType: PaymentMethod.Type,
    val stripeMethodCode: String,
    val requiresBankOrIban: Boolean,
    val defaultRequiresScaChallenge: Boolean,
    val localizedPrompt: String
)

data class StripeTerminalPaymentRequest(
    val amountUsd: Double,
    val targetCurrencyCode: String,
    val exchangeRate: Double,
    val localizedAmountMajor: Double,
    val paymentMode: StripeTerminalPaymentMode,
    val railId: String,
    val railName: String,
    val customerName: String,
    val customerEmail: String? = null,
    val countryName: String,
    val cardNumber: String = "",
    val expiryMmYy: String = "",
    val cvc: String = "",
    val idealBankCode: String = "ing",
    val sepaIban: String = "",
    val force3dsChallenge: Boolean = false,
    val terminalId: String = "POS-TERM-STRIPE",
    val itemsSummary: String = "Sales Terminal Localized Checkout",
    val idempotencyKey: String = UUID.randomUUID().toString()
)

data class Stripe3dsChallenge(
    val paymentIntentId: String,
    val paymentMethodId: String,
    val clientSecretMasked: String,
    val localizedAmountFormatted: String,
    val minorUnits: Long,
    val currencyCode: String,
    val amountUsd: Double,
    val exchangeRate: Double,
    val railId: String,
    val railName: String,
    val stripeMethodCode: String,
    val cardBrand: String,
    val last4: String,
    val customerName: String,
    val countryName: String,
    val countryIso2: String,
    val challengeReason: String,
    val sdkPublishableKeyHint: String,
    val idempotencyKey: String,
    val terminalId: String,
    val itemsSummary: String,
    val sdkParamsFingerprint: String,
    val createdAtMs: Long
)

data class StripeTerminalReceipt(
    val paymentIntentId: String,
    val paymentMethodId: String,
    val clientSecretMasked: String,
    val chargeId: String,
    val referenceCode: String,
    val status: String,
    val scaStatus: String,
    val localizedAmountMajor: Double,
    val localizedAmountFormatted: String,
    val minorUnits: Long,
    val currencyCode: String,
    val amountUsd: Double,
    val exchangeRate: Double,
    val railId: String,
    val railName: String,
    val stripeMethodCode: String,
    val cardBrand: String,
    val last4: String,
    val customerName: String,
    val countryName: String,
    val countryIso2: String,
    val idempotencyKey: String,
    val sdkEnvironmentBadge: String,
    val sdkPublishableKeyHint: String,
    val sdkParamsFingerprint: String,
    val terminalId: String,
    val itemsSummary: String,
    val settledAtMs: Long
)

sealed class StripeSdkPreparationOutcome {
    data class Ready(
        val paymentMethodCreateParams: PaymentMethodCreateParams,
        val confirmParamsFingerprint: String,
        val stripeMethodCode: String,
        val sdkEnvironmentBadge: String,
        val sdkPublishableKeyHint: String
    ) : StripeSdkPreparationOutcome()

    data class ValidationFailed(
        val errorCode: String,
        val message: String
    ) : StripeSdkPreparationOutcome()
}

sealed class StripeTerminalExecutionStep {
    data class Requires3dsAuthentication(val challenge: Stripe3dsChallenge) : StripeTerminalExecutionStep()
    data class Settled(val receipt: StripeTerminalReceipt) : StripeTerminalExecutionStep()
    data class Declined(
        val errorCode: String,
        val message: String,
        val isRetryable: Boolean = true
    ) : StripeTerminalExecutionStep()
}

object StripeCardValidator {

    fun digitsOnly(input: String): String = input.filter { it.isDigit() }

    fun detectBrand(rawDigits: String): StripeCardBrand {
        val digits = digitsOnly(rawDigits)
        if (digits.isEmpty()) return StripeCardBrand.UNKNOWN
        return when {
            digits.startsWith("4") -> StripeCardBrand.VISA
            digits.startsWith("34") || digits.startsWith("37") -> StripeCardBrand.AMEX
            digits.startsWith("62") -> StripeCardBrand.UNIONPAY
            digits.startsWith("35") -> StripeCardBrand.JCB
            digits.startsWith("6011") || digits.startsWith("65") ||
                (digits.length >= 2 && digits.substring(0, 2).toIntOrNull() in 64..65) -> StripeCardBrand.DISCOVER
            (digits.length >= 2 && digits.substring(0, 2).toIntOrNull() in 51..55) ||
                (digits.length >= 4 && digits.substring(0, 4).toIntOrNull() in 2221..2720) -> StripeCardBrand.MASTERCARD
            else -> StripeCardBrand.UNKNOWN
        }
    }

    fun isValidLuhn(rawDigits: String): Boolean {
        val digits = digitsOnly(rawDigits)
        if (digits.length < 13 || digits.length > 19) return false
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    fun formatCardNumberForDisplay(input: String): String {
        val digits = digitsOnly(input).take(19)
        return digits.chunked(4).joinToString(" ")
    }

    fun parseExpiry(expiryMmYy: String): Pair<Int, Int>? {
        val clean = digitsOnly(expiryMmYy)
        if (clean.length != 4) return null
        val month = clean.substring(0, 2).toIntOrNull() ?: return null
        val yy = clean.substring(2, 4).toIntOrNull() ?: return null
        if (month !in 1..12) return null
        return month to (2000 + yy)
    }

    fun isExpiryInFuture(month: Int, yearFull: Int, calendar: Calendar = Calendar.getInstance()): Boolean {
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        return yearFull > currentYear || (yearFull == currentYear && month >= currentMonth)
    }

    fun isValidBasicIban(iban: String): Boolean {
        val normalized = iban.replace(" ", "").uppercase(Locale.US)
        return normalized.length in 15..34 &&
            normalized.take(2).all { it.isLetter() } &&
            normalized.drop(2).take(2).all { it.isDigit() } &&
            normalized.drop(4).all { it.isLetterOrDigit() }
    }

    fun validateRequest(
        request: StripeTerminalPaymentRequest,
        calendar: Calendar = Calendar.getInstance()
    ): StripeTerminalValidationResult {
        val holderError = if (request.customerName.trim().length < 2) {
            "Enter customer or cardholder name"
        } else null

        if (request.paymentMode == StripeTerminalPaymentMode.LOCAL_RAIL) {
            val spec = StripeTerminalRailMapper.resolveSpec(request.railId, request.targetCurrencyCode)
            val ibanError = if (spec.stripeMethodType == PaymentMethod.Type.SepaDebit &&
                !isValidBasicIban(request.sepaIban)
            ) {
                "Enter a valid SEPA IBAN (e.g. NL43INGB0006372146)"
            } else null

            return StripeTerminalValidationResult(
                isValid = holderError == null && ibanError == null,
                holderNameError = holderError,
                ibanError = ibanError
            )
        }

        val digits = digitsOnly(request.cardNumber)
        val brand = detectBrand(digits)
        val cardError = when {
            digits.isEmpty() -> "Card number is required"
            !isValidLuhn(digits) -> "Enter a valid card number"
            else -> null
        }

        val parsedExp = parseExpiry(request.expiryMmYy)
        val expiryError = when {
            parsedExp == null -> "Use MM/YY format"
            !isExpiryInFuture(parsedExp.first, parsedExp.second, calendar) -> "Card is expired"
            else -> null
        }

        val cvcDigits = digitsOnly(request.cvc)
        val cvcError = when {
            cvcDigits.length !in 3..4 -> "Enter 3-4 digit CVC"
            brand == StripeCardBrand.AMEX && cvcDigits.length != 4 -> "Amex requires 4-digit CVC"
            else -> null
        }

        return StripeTerminalValidationResult(
            isValid = cardError == null && expiryError == null && cvcError == null && holderError == null,
            cardNumberError = cardError,
            expiryError = expiryError,
            cvcError = cvcError,
            holderNameError = holderError,
            detectedBrand = brand,
            sanitizedDigits = digits,
            parsedExpiryMonth = parsedExp?.first ?: 0,
            parsedExpiryYearFull = parsedExp?.second ?: 0
        )
    }
}

object StripeTerminalRailMapper {

    fun toCountryIso2(countryName: String, currencyCode: String): String {
        val normalized = countryName.trim().lowercase(Locale.US)
        return when {
            normalized.contains("netherlands") || normalized == "nl" -> "NL"
            normalized.contains("germany") || normalized == "de" -> "DE"
            normalized.contains("france") || normalized == "fr" -> "FR"
            normalized.contains("belgium") || normalized == "be" -> "BE"
            normalized.contains("spain") || normalized == "es" -> "ES"
            normalized.contains("italy") || normalized == "it" -> "IT"
            normalized.contains("united kingdom") || normalized == "uk" || normalized == "gb" -> "GB"
            normalized.contains("japan") || normalized == "jp" -> "JP"
            normalized.contains("china") || normalized == "cn" -> "CN"
            normalized.contains("canada") || normalized == "ca" -> "CA"
            normalized.contains("australia") || normalized == "au" -> "AU"
            normalized.contains("switzerland") || normalized == "ch" -> "CH"
            normalized.contains("singapore") || normalized == "sg" -> "SG"
            normalized.contains("india") || normalized == "in" -> "IN"
            normalized.contains("brazil") || normalized == "br" -> "BR"
            normalized.contains("mexico") || normalized == "mx" -> "MX"
            normalized.contains("united states") || normalized == "us" || normalized == "usa" -> "US"
            else -> when (currencyCode.uppercase(Locale.US)) {
                "EUR" -> "DE"
                "GBP" -> "GB"
                "JPY" -> "JP"
                "CNY" -> "CN"
                "CAD" -> "CA"
                "AUD" -> "AU"
                "CHF" -> "CH"
                "SGD" -> "SG"
                "INR" -> "IN"
                "BRL" -> "BR"
                "MXN" -> "MX"
                else -> "US"
            }
        }
    }

    fun resolveSpec(railId: String, currencyCode: String): StripeLocalizedRailSpec {
        return when (railId.lowercase(Locale.US)) {
            "ideal" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "iDEAL",
                stripeMethodType = PaymentMethod.Type.Ideal,
                stripeMethodCode = "ideal",
                requiresBankOrIban = true,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "Redirects to selected Dutch bank app for iDEAL confirmation"
            )
            "sepa_instant", "sepa" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "SEPA Instant",
                stripeMethodType = PaymentMethod.Type.SepaDebit,
                stripeMethodCode = "sepa_debit",
                requiresBankOrIban = true,
                defaultRequiresScaChallenge = false,
                localizedPrompt = "Validates SEPA IBAN mandate & authorizes Eurozone transfer"
            )
            "bancontact" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "Bancontact",
                stripeMethodType = PaymentMethod.Type.Bancontact,
                stripeMethodCode = "bancontact",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "Initiates Belgian Bancontact QR / 3DS2 app approval"
            )
            "sofort" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "Sofort / Klarna",
                stripeMethodType = PaymentMethod.Type.Klarna,
                stripeMethodCode = "klarna",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "Klarna / Eurozone instant pay authorization via Stripe SDK"
            )
            "alipay" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "Alipay Global",
                stripeMethodType = PaymentMethod.Type.Alipay,
                stripeMethodCode = "alipay",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "Alipay wallet authentication & instant CNY escrow lock"
            )
            "wechat" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "WeChat Pay",
                stripeMethodType = PaymentMethod.Type.WeChatPay,
                stripeMethodCode = "wechat_pay",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "WeChat Pay QR challenge for localized CNY settlement"
            )
            "konbini" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "Konbini Convenience",
                stripeMethodType = PaymentMethod.Type.Konbini,
                stripeMethodCode = "konbini",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "Generates Japanese convenience store voucher confirmation"
            )
            "paynow" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "PayNow",
                stripeMethodType = PaymentMethod.Type.PayNow,
                stripeMethodCode = "paynow",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "PayNow SGQR instant bank authorization"
            )
            "pix" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "Pix Instant",
                stripeMethodType = PaymentMethod.Type.Pix,
                stripeMethodCode = "pix",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "Brazilian Central Bank Pix QR instant challenge"
            )
            "upi" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "UPI Instant",
                stripeMethodType = PaymentMethod.Type.Card,
                stripeMethodCode = "upi",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "NPCI UPI collect approval challenge in INR"
            )
            "oxxo" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "OXXO Pay",
                stripeMethodType = PaymentMethod.Type.Oxxo,
                stripeMethodCode = "oxxo",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "OXXO MXN cash voucher confirmation flow"
            )
            "boleto" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "Boleto Bancário",
                stripeMethodType = PaymentMethod.Type.Boleto,
                stripeMethodCode = "boleto",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = false,
                localizedPrompt = "Boleto barcode settlement in BRL"
            )
            "bacs" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "BACS Direct Debit",
                stripeMethodType = PaymentMethod.Type.BacsDebit,
                stripeMethodCode = "bacs_debit",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = false,
                localizedPrompt = "UK BACS Direct Debit mandate confirmation in GBP"
            )
            "ach" -> StripeLocalizedRailSpec(
                railId = railId,
                railName = "ACH Bank Debit",
                stripeMethodType = PaymentMethod.Type.USBankAccount,
                stripeMethodCode = "us_bank_account",
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "US Bank ACH instant micro-verification simulation"
            )
            else -> StripeLocalizedRailSpec(
                railId = railId,
                railName = railId.replace('_', ' ').uppercase(Locale.US),
                stripeMethodType = PaymentMethod.Type.Card,
                stripeMethodCode = when (currencyCode.uppercase(Locale.US)) {
                    "EUR" -> "sepa_debit"
                    "GBP" -> "bacs_debit"
                    else -> "card"
                },
                requiresBankOrIban = false,
                defaultRequiresScaChallenge = true,
                localizedPrompt = "Localized regional rail settlement via Stripe terminal bridge"
            )
        }
    }
}

/**
 * Coordinates official Stripe Android SDK initialization (`PaymentConfiguration`),
 * `PaymentMethodCreateParams` construction, `ConfirmPaymentIntentParams` preparation,
 * and deterministic localized Sales Terminal transaction simulation (frictionless, 3DS2 SCA,
 * and realistic Stripe decline handling) without persisting raw card credentials.
 */
class StripeTerminalPaymentProcessor(
    private val publishableKeyProvider: () -> String = { BuildConfig.STRIPE_PUBLISHABLE_KEY }
) {

    companion object {
        private const val PLACEHOLDER_PUBLISHABLE = "MY_STRIPE_PUBLISHABLE_KEY"
        private const val SANDBOX_SDK_PUBLISHABLE_KEY = "pk_test_sandbox_terminal_simulator"
        private const val TEST_CARD_DECLINE_GENERIC = "4000000000000002"
        private const val TEST_CARD_INSUFFICIENT_FUNDS = "4000000000009995"
        private val TEST_CARDS_3DS_REQUIRED = setOf(
            "4000002760003184",
            "4000000000003220"
        )
    }

    fun resolvedPublishableKey(): String? {
        val raw = publishableKeyProvider().trim()
        if (raw.isBlank() || raw == PLACEHOLDER_PUBLISHABLE || raw.startsWith("MY_")) {
            return null
        }
        return if (raw.startsWith("pk_")) raw else null
    }

    fun sdkEnvironmentBadge(): String {
        val key = resolvedPublishableKey() ?: return "STRIPE SDK • SANDBOX SIM"
        return when {
            key.startsWith("pk_live_") -> "STRIPE SDK • LIVE KEY ACTIVE"
            key.startsWith("pk_test_") -> "STRIPE SDK • TEST KEY ACTIVE"
            else -> "STRIPE SDK • SANDBOX"
        }
    }

    fun maskedPublishableKeyHint(): String {
        val key = resolvedPublishableKey() ?: return "Sandbox Key (No Secret Required)"
        if (key.length <= 12) return "••••••••"
        return "${key.take(8)}••••${key.takeLast(4)}"
    }

    /**
     * Initializes Stripe's official [PaymentConfiguration] on the application context.
     * Uses the configured publishable key if present in Secrets, or an explicit sandbox
     * publishable key token during offline terminal simulation so Stripe SDK components
     * stay cleanly initialized without mutating live merchant accounts.
     */
    fun ensurePaymentConfigurationInitialized(context: Context): String {
        val effectiveKey = resolvedPublishableKey() ?: SANDBOX_SDK_PUBLISHABLE_KEY
        PaymentConfiguration.init(context.applicationContext, effectiveKey)
        return effectiveKey
    }

    fun prepareStripeSdkParams(
        context: Context?,
        request: StripeTerminalPaymentRequest
    ): StripeSdkPreparationOutcome {
        val validation = StripeCardValidator.validateRequest(request)
        if (!validation.isValid) {
            val firstError = validation.cardNumberError
                ?: validation.expiryError
                ?: validation.cvcError
                ?: validation.ibanError
                ?: validation.holderNameError
                ?: "Check your payment inputs."
            return StripeSdkPreparationOutcome.ValidationFailed(
                errorCode = "invalid_payment_input",
                message = firstError
            )
        }

        if (context != null) {
            runCatching { ensurePaymentConfigurationInitialized(context) }
        }

        val countryIso2 = StripeTerminalRailMapper.toCountryIso2(
            request.countryName,
            request.targetCurrencyCode
        )
        val billingDetails = PaymentMethod.BillingDetails(
            name = request.customerName.trim(),
            email = request.customerEmail?.trim()?.ifBlank { null },
            address = Address(country = countryIso2)
        )

        val (createParams, stripeMethodCode) = if (request.paymentMode == StripeTerminalPaymentMode.CARD) {
            val cardParams = PaymentMethodCreateParams.Card(
                number = validation.sanitizedDigits,
                expiryMonth = validation.parsedExpiryMonth,
                expiryYear = validation.parsedExpiryYearFull,
                cvc = StripeCardValidator.digitsOnly(request.cvc)
            )
            PaymentMethodCreateParams.create(
                card = cardParams,
                billingDetails = billingDetails
            ) to "card"
        } else {
            val railSpec = StripeTerminalRailMapper.resolveSpec(
                request.railId,
                request.targetCurrencyCode
            )
            val params = when (railSpec.stripeMethodType) {
                PaymentMethod.Type.Ideal -> PaymentMethodCreateParams.create(
                    ideal = PaymentMethodCreateParams.Ideal(
                        bank = request.idealBankCode.ifBlank { "ing" }
                    ),
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.SepaDebit -> PaymentMethodCreateParams.create(
                    sepaDebit = PaymentMethodCreateParams.SepaDebit(
                        iban = request.sepaIban.replace(" ", "").uppercase(Locale.US)
                    ),
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.Bancontact -> PaymentMethodCreateParams.createBancontact(
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.Klarna -> PaymentMethodCreateParams.createKlarna(
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.Alipay -> PaymentMethodCreateParams.createAlipay()
                PaymentMethod.Type.WeChatPay -> PaymentMethodCreateParams.createWeChatPay(
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.PayNow -> PaymentMethodCreateParams.createGrabPay(
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.Pix -> PaymentMethodCreateParams.createPix(
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.Oxxo -> PaymentMethodCreateParams.createOxxo(
                    billingDetails = billingDetails
                )
                PaymentMethod.Type.USBankAccount -> PaymentMethodCreateParams.createUSBankAccount(
                    billingDetails = billingDetails
                )
                else -> PaymentMethodCreateParams.create(
                    card = PaymentMethodCreateParams.Card(
                        number = "4242424242424242",
                        expiryMonth = 12,
                        expiryYear = 2030,
                        cvc = "424"
                    ),
                    billingDetails = billingDetails
                )
            }
            params to railSpec.stripeMethodCode
        }

        // Build ConfirmPaymentIntentParams strictly in memory to verify Stripe SDK payload integrity
        val simIntentId = generateStripeId("pi_sim")
        val inMemoryClientSecret = "${simIntentId}_secret_${request.idempotencyKey.replace("-", "").take(16)}"
        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = createParams,
            clientSecret = inMemoryClientSecret
        )
        val fingerprint = sha256Short(
            "${createParams.typeCode}:${request.targetCurrencyCode}:${request.idempotencyKey}:${confirmParams.clientSecret.take(12)}"
        )

        return StripeSdkPreparationOutcome.Ready(
            paymentMethodCreateParams = createParams,
            confirmParamsFingerprint = fingerprint,
            stripeMethodCode = stripeMethodCode,
            sdkEnvironmentBadge = sdkEnvironmentBadge(),
            sdkPublishableKeyHint = maskedPublishableKeyHint()
        )
    }

    fun executeLocalizedSimulation(
        request: StripeTerminalPaymentRequest,
        prepared: StripeSdkPreparationOutcome.Ready,
        nowMs: Long = System.currentTimeMillis()
    ): StripeTerminalExecutionStep {
        if (!request.localizedAmountMajor.isFinite() || request.localizedAmountMajor <= 0.0) {
            return StripeTerminalExecutionStep.Declined(
                errorCode = "amount_too_small",
                message = "Transaction amount must be greater than zero.",
                isRetryable = true
            )
        }

        val upperCurrency = request.targetCurrencyCode.uppercase(Locale.US)
        val minorUnits = StripePayoutGateway.toMinorUnits(request.localizedAmountMajor, upperCurrency)
        val majorNormalized = StripePayoutGateway.toMajorAmount(minorUnits, upperCurrency)
        val formattedLocalized = CurrencyFormatter.DEFAULT.format(majorNormalized, upperCurrency)
        val countryIso2 = StripeTerminalRailMapper.toCountryIso2(request.countryName, upperCurrency)

        val digits = StripeCardValidator.digitsOnly(request.cardNumber)
        if (request.paymentMode == StripeTerminalPaymentMode.CARD) {
            if (digits == TEST_CARD_DECLINE_GENERIC) {
                return StripeTerminalExecutionStep.Declined(
                    errorCode = "card_declined",
                    message = "Your card was declined by issuer risk checks (Stripe test scenario).",
                    isRetryable = true
                )
            }
            if (digits == TEST_CARD_INSUFFICIENT_FUNDS) {
                return StripeTerminalExecutionStep.Declined(
                    errorCode = "insufficient_funds",
                    message = "Your card has insufficient funds for $formattedLocalized.",
                    isRetryable = true
                )
            }
        }

        val brandLabel: String
        val last4: String
        if (request.paymentMode == StripeTerminalPaymentMode.CARD) {
            brandLabel = StripeCardValidator.detectBrand(digits).displayName
            last4 = digits.takeLast(4).padStart(4, '•')
        } else {
            val railSpec = StripeTerminalRailMapper.resolveSpec(request.railId, upperCurrency)
            brandLabel = railSpec.railName
            last4 = if (request.sepaIban.isNotBlank()) {
                request.sepaIban.replace(" ", "").takeLast(4)
            } else {
                upperCurrency
            }
        }

        val piId = generateStripeId("pi_sim")
        val pmId = generateStripeId("pm_sim")
        val maskedSecret = "${piId}_secret_••••••••"

        val railSpec = StripeTerminalRailMapper.resolveSpec(request.railId, upperCurrency)
        val needsScaChallenge = when (request.paymentMode) {
            StripeTerminalPaymentMode.CARD ->
                request.force3dsChallenge || digits in TEST_CARDS_3DS_REQUIRED
            StripeTerminalPaymentMode.LOCAL_RAIL ->
                request.force3dsChallenge || railSpec.defaultRequiresScaChallenge
        }

        if (needsScaChallenge) {
            val challengeReason = if (request.paymentMode == StripeTerminalPaymentMode.CARD) {
                "3D Secure 2 (EMV 3DS) Strong Customer Authentication required by issuer"
            } else {
                railSpec.localizedPrompt
            }
            return StripeTerminalExecutionStep.Requires3dsAuthentication(
                challenge = Stripe3dsChallenge(
                    paymentIntentId = piId,
                    paymentMethodId = pmId,
                    clientSecretMasked = maskedSecret,
                    localizedAmountFormatted = formattedLocalized,
                    minorUnits = minorUnits,
                    currencyCode = upperCurrency,
                    amountUsd = request.amountUsd,
                    exchangeRate = request.exchangeRate,
                    railId = request.railId,
                    railName = request.railName,
                    stripeMethodCode = prepared.stripeMethodCode,
                    cardBrand = brandLabel,
                    last4 = last4,
                    customerName = request.customerName.trim(),
                    countryName = request.countryName.trim(),
                    countryIso2 = countryIso2,
                    challengeReason = challengeReason,
                    sdkPublishableKeyHint = prepared.sdkPublishableKeyHint,
                    idempotencyKey = request.idempotencyKey,
                    terminalId = request.terminalId,
                    itemsSummary = request.itemsSummary,
                    sdkParamsFingerprint = prepared.confirmParamsFingerprint,
                    createdAtMs = nowMs
                )
            )
        }

        return StripeTerminalExecutionStep.Settled(
            receipt = buildReceipt(
                piId = piId,
                pmId = pmId,
                maskedSecret = maskedSecret,
                scaStatus = "Frictionless (No Challenge Required)",
                localizedAmountMajor = majorNormalized,
                localizedAmountFormatted = formattedLocalized,
                minorUnits = minorUnits,
                currencyCode = upperCurrency,
                amountUsd = request.amountUsd,
                exchangeRate = request.exchangeRate,
                railId = request.railId,
                railName = request.railName,
                stripeMethodCode = prepared.stripeMethodCode,
                cardBrand = brandLabel,
                last4 = last4,
                customerName = request.customerName.trim(),
                countryName = request.countryName.trim(),
                countryIso2 = countryIso2,
                idempotencyKey = request.idempotencyKey,
                sdkEnvironmentBadge = prepared.sdkEnvironmentBadge,
                sdkPublishableKeyHint = prepared.sdkPublishableKeyHint,
                sdkParamsFingerprint = prepared.confirmParamsFingerprint,
                terminalId = request.terminalId,
                itemsSummary = request.itemsSummary,
                nowMs = nowMs
            )
        )
    }

    fun complete3dsChallenge(
        challenge: Stripe3dsChallenge,
        nowMs: Long = System.currentTimeMillis()
    ): StripeTerminalReceipt {
        val majorAmount = StripePayoutGateway.toMajorAmount(challenge.minorUnits, challenge.currencyCode)
        return buildReceipt(
            piId = challenge.paymentIntentId,
            pmId = challenge.paymentMethodId,
            maskedSecret = challenge.clientSecretMasked,
            scaStatus = "3DS2 / SCA Authenticated",
            localizedAmountMajor = majorAmount,
            localizedAmountFormatted = challenge.localizedAmountFormatted,
            minorUnits = challenge.minorUnits,
            currencyCode = challenge.currencyCode,
            amountUsd = challenge.amountUsd,
            exchangeRate = challenge.exchangeRate,
            railId = challenge.railId,
            railName = challenge.railName,
            stripeMethodCode = challenge.stripeMethodCode,
            cardBrand = challenge.cardBrand,
            last4 = challenge.last4,
            customerName = challenge.customerName,
            countryName = challenge.countryName,
            countryIso2 = challenge.countryIso2,
            idempotencyKey = challenge.idempotencyKey,
            sdkEnvironmentBadge = sdkEnvironmentBadge(),
            sdkPublishableKeyHint = challenge.sdkPublishableKeyHint,
            sdkParamsFingerprint = challenge.sdkParamsFingerprint,
            terminalId = challenge.terminalId,
            itemsSummary = challenge.itemsSummary,
            nowMs = nowMs
        )
    }

    private fun buildReceipt(
        piId: String,
        pmId: String,
        maskedSecret: String,
        scaStatus: String,
        localizedAmountMajor: Double,
        localizedAmountFormatted: String,
        minorUnits: Long,
        currencyCode: String,
        amountUsd: Double,
        exchangeRate: Double,
        railId: String,
        railName: String,
        stripeMethodCode: String,
        cardBrand: String,
        last4: String,
        customerName: String,
        countryName: String,
        countryIso2: String,
        idempotencyKey: String,
        sdkEnvironmentBadge: String,
        sdkPublishableKeyHint: String,
        sdkParamsFingerprint: String,
        terminalId: String,
        itemsSummary: String,
        nowMs: Long
    ): StripeTerminalReceipt {
        val refSuffix = piId.takeLast(6).uppercase(Locale.US)
        return StripeTerminalReceipt(
            paymentIntentId = piId,
            paymentMethodId = pmId,
            clientSecretMasked = maskedSecret,
            chargeId = generateStripeId("ch_sim"),
            referenceCode = "STR-$currencyCode-$refSuffix",
            status = "succeeded",
            scaStatus = scaStatus,
            localizedAmountMajor = localizedAmountMajor,
            localizedAmountFormatted = localizedAmountFormatted,
            minorUnits = minorUnits,
            currencyCode = currencyCode,
            amountUsd = amountUsd,
            exchangeRate = exchangeRate,
            railId = railId,
            railName = railName,
            stripeMethodCode = stripeMethodCode,
            cardBrand = cardBrand,
            last4 = last4,
            customerName = customerName,
            countryName = countryName,
            countryIso2 = countryIso2,
            idempotencyKey = idempotencyKey,
            sdkEnvironmentBadge = sdkEnvironmentBadge,
            sdkPublishableKeyHint = sdkPublishableKeyHint,
            sdkParamsFingerprint = sdkParamsFingerprint,
            terminalId = terminalId,
            itemsSummary = itemsSummary,
            settledAtMs = nowMs
        )
    }

    private fun generateStripeId(prefix: String): String {
        val randomHex = UUID.randomUUID().toString().replace("-", "").take(14)
        return "${prefix}_$randomHex"
    }

    private fun sha256Short(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(6).joinToString("") { "%02x".format(it) }
    }
}
