package com.example.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CleanBorder
import com.example.ui.theme.CleanBorderSubtle
import com.example.ui.theme.CleanDanger
import com.example.ui.theme.CleanSurface
import com.example.ui.theme.CleanSurfaceVariant
import com.example.ui.theme.CleanTextMuted
import com.example.ui.theme.CleanTextPrimary
import com.example.ui.theme.CleanTextSecondary
import com.example.ui.theme.StripeIndigo
import com.example.ui.theme.StripeOnIndigo
import com.example.util.CurrencyFormatter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Simulated customer profiles pre-populated into Stripe [PaymentSheet.CustomerConfiguration]
 * and [PaymentSheet.BillingDetails] to test localized checkout flows across markets.
 */
data class SimulatedCheckoutCustomer(
    val id: String,
    val displayName: String,
    val email: String,
    val phone: String,
    val countryName: String,
    val countryIso2: String,
    val preferredCurrencyCode: String,
    val city: String,
    val line1: String,
    val postalCode: String,
    val stripeCustomerId: String?,
    val ephemeralKeyHint: String?,
    val defaultCardBrand: String,
    val defaultCardLast4: String,
    val isGuest: Boolean = false
)

enum class SimulatedPaymentSheetOutcome(
    val id: String,
    val label: String,
    val subtitle: String
) {
    COMPLETED(
        id = "completed",
        label = "Instant Approval",
        subtitle = "Simulates PaymentSheetResult.Completed & settles receipt"
    ),
    REQUIRES_SCA(
        id = "requires_sca",
        label = "3DS2 / SCA Flow",
        subtitle = "Triggers issuer 3DS2 challenge before settling PaymentSheet"
    ),
    CANCELED(
        id = "canceled",
        label = "Customer Canceled",
        subtitle = "Simulates PaymentSheetResult.Canceled without charging"
    ),
    FAILED(
        id = "failed",
        label = "Declined / Error",
        subtitle = "Simulates PaymentSheetResult.Failed for retry testing"
    )
}

data class StripePaymentSheetSetupRequest(
    val merchantDisplayName: String = "GlobalMarket Pro",
    val amountUsd: Double,
    val currencyCode: String,
    val exchangeRate: Double,
    val localizedAmountMajor: Double,
    val customerProfile: SimulatedCheckoutCustomer,
    val overrideCustomerName: String? = null,
    val overrideCustomerEmail: String? = null,
    val overrideCountryName: String? = null,
    val allowsDelayedPaymentMethods: Boolean = true,
    val allowsShippingAddress: Boolean = false,
    val enableGooglePay: Boolean = true,
    val collectFullAddress: Boolean = false,
    val paymentMethodLayout: PaymentSheet.PaymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
    val customClientSecret: String = "",
    val preferredRailId: String = "cards"
)

/**
 * Bundles the official Stripe [PaymentSheet.Configuration] and [PaymentSheet.IntentConfiguration]
 * together with non-internal inspection metadata so the UI and tests can display and verify
 * active configuration parameters cleanly.
 */
data class StripePaymentSheetPreparedConfig(
    val publishableKey: String,
    val publishableKeyHint: String,
    val environmentBadge: String,
    val merchantDisplayName: String,
    val configuration: PaymentSheet.Configuration,
    val intentConfiguration: PaymentSheet.IntentConfiguration,
    val defaultBillingDetails: PaymentSheet.BillingDetails,
    val paymentMethodOrder: List<String>,
    val minorUnits: Long,
    val currencyCode: String,
    val localizedAmountMajor: Double,
    val localizedAmountFormatted: String,
    val countryName: String,
    val countryIso2: String,
    val customerId: String?,
    val ephemeralKeyHint: String?,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsShippingAddress: Boolean,
    val googlePayEnabled: Boolean,
    val collectFullAddress: Boolean,
    val paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
    val hasValidClientSecret: Boolean,
    val clientSecret: String?,
    val primaryButtonLabel: String,
    val configDigest: String
)

/**
 * Constructs production-grade Stripe [PaymentSheet.Configuration], [PaymentSheet.Appearance],
 * [PaymentSheet.BillingDetails], [PaymentSheet.CustomerConfiguration],
 * [PaymentSheet.GooglePayConfiguration], and [PaymentSheet.IntentConfiguration] instances
 * and coordinates simulated customer checkout execution + [PaymentSheetResult] mapping.
 */
object StripePaymentSheetConfigFactory {

    val defaultCustomers: List<SimulatedCheckoutCustomer> = listOf(
        SimulatedCheckoutCustomer(
            id = "cust_eu_elena",
            displayName = "Elena Rostova",
            email = "elena.rostova@glaba-sim.eu",
            phone = "+49 30 901820",
            countryName = "Germany",
            countryIso2 = "DE",
            preferredCurrencyCode = "EUR",
            city = "Berlin",
            line1 = "Friedrichstrasse 114A",
            postalCode = "10117",
            stripeCustomerId = "cus_sim_eu_elena92",
            ephemeralKeyHint = "ek_test_sim_••92eu",
            defaultCardBrand = "Visa",
            defaultCardLast4 = "4242"
        ),
        SimulatedCheckoutCustomer(
            id = "cust_jp_kenji",
            displayName = "Kenji Takahashi",
            email = "kenji.takahashi@glaba-sim.jp",
            phone = "+81 3 5550 1920",
            countryName = "Japan",
            countryIso2 = "JP",
            preferredCurrencyCode = "JPY",
            city = "Tokyo",
            line1 = "2-24-12 Shibuya",
            postalCode = "150-0002",
            stripeCustomerId = "cus_sim_jp_kenji88",
            ephemeralKeyHint = "ek_test_sim_••88jp",
            defaultCardBrand = "JCB / Visa",
            defaultCardLast4 = "0005"
        ),
        SimulatedCheckoutCustomer(
            id = "cust_gb_arthur",
            displayName = "Arthur Pendelton",
            email = "arthur.pendelton@glaba-sim.co.uk",
            phone = "+44 20 7946 0921",
            countryName = "United Kingdom",
            countryIso2 = "GB",
            preferredCurrencyCode = "GBP",
            city = "London",
            line1 = "22 Bishopsgate",
            postalCode = "EC2N 4BQ",
            stripeCustomerId = "cus_sim_gb_arthur41",
            ephemeralKeyHint = "ek_test_sim_••41gb",
            defaultCardBrand = "Mastercard",
            defaultCardLast4 = "4444"
        ),
        SimulatedCheckoutCustomer(
            id = "cust_us_alex",
            displayName = "Alex Mercer",
            email = "alex.mercer@glaba-sim.com",
            phone = "+1 415 555 0199",
            countryName = "United States",
            countryIso2 = "US",
            preferredCurrencyCode = "USD",
            city = "San Francisco",
            line1 = "354 Oyster Point Blvd",
            postalCode = "94080",
            stripeCustomerId = "cus_sim_us_mercer17",
            ephemeralKeyHint = "ek_test_sim_••17us",
            defaultCardBrand = "American Express",
            defaultCardLast4 = "8431"
        ),
        SimulatedCheckoutCustomer(
            id = "cust_guest",
            displayName = "Guest Checkout",
            email = "",
            phone = "",
            countryName = "United States",
            countryIso2 = "US",
            preferredCurrencyCode = "USD",
            city = "",
            line1 = "",
            postalCode = "",
            stripeCustomerId = null,
            ephemeralKeyHint = null,
            defaultCardBrand = "Card",
            defaultCardLast4 = "4242",
            isGuest = true
        )
    )

    fun buildAppearance(): PaymentSheet.Appearance {
        val lightColors = PaymentSheet.Colors(
            primary = StripeIndigo,
            surface = CleanSurface,
            component = CleanSurfaceVariant,
            componentBorder = CleanBorder,
            componentDivider = CleanBorderSubtle,
            onComponent = CleanTextPrimary,
            onSurface = CleanTextPrimary,
            subtitle = CleanTextSecondary,
            placeholderText = CleanTextMuted,
            appBarIcon = CleanTextPrimary,
            error = CleanDanger
        )
        val darkColors = PaymentSheet.Colors(
            primary = Color(0xFF82B1FF),
            surface = Color(0xFF1E293B),
            component = Color(0xFF334155),
            componentBorder = Color(0xFF475569),
            componentDivider = Color(0xFF334155),
            onComponent = Color(0xFFF1F5F9),
            onSurface = Color(0xFFF1F5F9),
            subtitle = Color(0xFF94A3B8),
            placeholderText = Color(0xFF94A3B8),
            appBarIcon = Color(0xFFF1F5F9),
            error = Color(0xFFFCA5A5)
        )
        val primaryButton = PaymentSheet.PrimaryButton(
            colorsLight = PaymentSheet.PrimaryButtonColors(
                background = StripeIndigo,
                onBackground = StripeOnIndigo,
                border = StripeIndigo
            ),
            colorsDark = PaymentSheet.PrimaryButtonColors(
                background = Color(0xFF82B1FF),
                onBackground = Color(0xFF002F6C),
                border = Color(0xFF82B1FF)
            ),
            shape = PaymentSheet.PrimaryButtonShape(
                cornerRadiusDp = 16f,
                borderStrokeWidthDp = 0f,
                heightDp = 50f
            )
        )
        return PaymentSheet.Appearance.Builder()
            .colorsLight(lightColors)
            .colorsDark(darkColors)
            .shapes(
                PaymentSheet.Shapes(
                    cornerRadiusDp = 12f,
                    borderStrokeWidthDp = 1f
                )
            )
            .primaryButton(primaryButton)
            .build()
    }

    fun paymentMethodOrderForCurrency(
        currencyCode: String,
        allowsDelayedPaymentMethods: Boolean
    ): List<String> {
        val regional = when (currencyCode.uppercase(Locale.US)) {
            "EUR" -> listOf("card", "ideal", "bancontact", "klarna", "sepa_debit")
            "GBP" -> listOf("card", "klarna", "bacs_debit")
            "JPY" -> listOf("card", "konbini")
            "CNY" -> listOf("card", "alipay", "wechat_pay")
            "SGD" -> listOf("card", "paynow")
            "BRL" -> listOf("card", "pix", "boleto")
            "MXN" -> listOf("card", "oxxo")
            "USD" -> listOf("card", "us_bank_account")
            else -> listOf("card")
        }
        return if (allowsDelayedPaymentMethods) {
            regional
        } else {
            regional.filterNot { it in setOf("sepa_debit", "bacs_debit", "us_bank_account", "boleto", "konbini", "oxxo") }
        }
    }

    fun buildPreparedConfig(
        context: Context?,
        processor: StripeTerminalPaymentProcessor,
        request: StripePaymentSheetSetupRequest
    ): StripePaymentSheetPreparedConfig {
        val publishableKey = if (context != null) {
            processor.ensurePaymentConfigurationInitialized(context)
        } else {
            processor.resolvedPublishableKey() ?: "pk_test_sandbox_terminal_simulator"
        }

        val upperCurrency = request.currencyCode.uppercase(Locale.US)
        val minorUnits = StripePayoutGateway.toMinorUnits(request.localizedAmountMajor, upperCurrency)
            .coerceAtLeast(1L)
        val majorNormalized = StripePayoutGateway.toMajorAmount(minorUnits, upperCurrency)
        val formattedTotal = CurrencyFormatter.DEFAULT.format(majorNormalized, upperCurrency)

        val effectiveName = request.overrideCustomerName?.trim()?.ifBlank { null }
            ?: request.customerProfile.displayName
        val effectiveEmail = request.overrideCustomerEmail?.trim()?.ifBlank { null }
            ?: request.customerProfile.email.ifBlank { null }
        val effectiveCountryName = request.overrideCountryName?.trim()?.ifBlank { null }
            ?: request.customerProfile.countryName
        val countryIso2 = StripeTerminalRailMapper.toCountryIso2(effectiveCountryName, upperCurrency)

        val addressBuilder = PaymentSheet.Address.Builder()
            .country(countryIso2)
        if (request.customerProfile.city.isNotBlank()) {
            addressBuilder.city(request.customerProfile.city)
        }
        if (request.customerProfile.line1.isNotBlank()) {
            addressBuilder.line1(request.customerProfile.line1)
        }
        if (request.customerProfile.postalCode.isNotBlank()) {
            addressBuilder.postalCode(request.customerProfile.postalCode)
        }

        val billingBuilder = PaymentSheet.BillingDetails.Builder()
            .name(effectiveName)
            .address(addressBuilder.build())
        effectiveEmail?.let { billingBuilder.email(it) }
        if (request.customerProfile.phone.isNotBlank()) {
            billingBuilder.phone(request.customerProfile.phone)
        }
        val billingDetails = billingBuilder.build()

        val billingCollection = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = if (request.collectFullAddress) {
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
            } else {
                PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
            },
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = if (request.collectFullAddress) {
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            } else {
                PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
            },
            attachDefaultsToPaymentMethod = true
        )

        val customerConfig = if (!request.customerProfile.isGuest &&
            !request.customerProfile.stripeCustomerId.isNullOrBlank()
        ) {
            val suffix = request.customerProfile.stripeCustomerId.takeLast(8)
            PaymentSheet.CustomerConfiguration(
                id = request.customerProfile.stripeCustomerId,
                ephemeralKeySecret = "ek_test_sim_${suffix}"
            )
        } else {
            null
        }

        val googlePayConfig = if (request.enableGooglePay) {
            PaymentSheet.GooglePayConfiguration(
                environment = if (publishableKey.startsWith("pk_live_")) {
                    PaymentSheet.GooglePayConfiguration.Environment.Production
                } else {
                    PaymentSheet.GooglePayConfiguration.Environment.Test
                },
                countryCode = countryIso2,
                currencyCode = upperCurrency,
                amount = minorUnits,
                label = request.merchantDisplayName
            )
        } else {
            null
        }

        val methodOrder = paymentMethodOrderForCurrency(upperCurrency, request.allowsDelayedPaymentMethods)
        val primaryButtonLabel = "Pay $formattedTotal"

        val configBuilder = PaymentSheet.Configuration.Builder(request.merchantDisplayName)
            .defaultBillingDetails(billingDetails)
            .allowsDelayedPaymentMethods(request.allowsDelayedPaymentMethods)
            .allowsPaymentMethodsRequiringShippingAddress(request.allowsShippingAddress)
            .appearance(buildAppearance())
            .primaryButtonLabel(primaryButtonLabel)
            .billingDetailsCollectionConfiguration(billingCollection)
            .paymentMethodLayout(request.paymentMethodLayout)
            .paymentMethodOrder(methodOrder)

        if (customerConfig != null) {
            configBuilder.customer(customerConfig)
        }
        if (googlePayConfig != null) {
            configBuilder.googlePay(googlePayConfig)
        }

        val configuration = configBuilder.build()

        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = minorUnits,
                currency = upperCurrency.lowercase(Locale.US)
            ),
            paymentMethodTypes = methodOrder
        )

        val trimmedSecret = request.customClientSecret.trim()
        val hasValidSecret = isLikelyLivePaymentIntentClientSecret(trimmedSecret)

        val digest = sha256Short(
            listOf(
                request.merchantDisplayName,
                request.customerProfile.stripeCustomerId ?: "guest",
                countryIso2,
                upperCurrency,
                minorUnits.toString(),
                request.allowsDelayedPaymentMethods.toString(),
                request.enableGooglePay.toString(),
                request.paymentMethodLayout.name,
                methodOrder.joinToString(",")
            ).joinToString("|")
        )

        return StripePaymentSheetPreparedConfig(
            publishableKey = publishableKey,
            publishableKeyHint = processor.maskedPublishableKeyHint(),
            environmentBadge = processor.sdkEnvironmentBadge(),
            merchantDisplayName = request.merchantDisplayName,
            configuration = configuration,
            intentConfiguration = intentConfiguration,
            defaultBillingDetails = billingDetails,
            paymentMethodOrder = methodOrder,
            minorUnits = minorUnits,
            currencyCode = upperCurrency,
            localizedAmountMajor = majorNormalized,
            localizedAmountFormatted = formattedTotal,
            countryName = effectiveCountryName,
            countryIso2 = countryIso2,
            customerId = request.customerProfile.stripeCustomerId,
            ephemeralKeyHint = request.customerProfile.ephemeralKeyHint,
            allowsDelayedPaymentMethods = request.allowsDelayedPaymentMethods,
            allowsShippingAddress = request.allowsShippingAddress,
            googlePayEnabled = request.enableGooglePay,
            collectFullAddress = request.collectFullAddress,
            paymentMethodLayout = request.paymentMethodLayout,
            hasValidClientSecret = hasValidSecret,
            clientSecret = if (hasValidSecret) trimmedSecret else null,
            primaryButtonLabel = primaryButtonLabel,
            configDigest = digest
        )
    }

    fun isLikelyLivePaymentIntentClientSecret(secret: String): Boolean {
        val trimmed = secret.trim()
        return trimmed.startsWith("pi_") && trimmed.contains("_secret_") && trimmed.length >= 24
    }

    fun simulateCustomerCheckout(
        request: StripePaymentSheetSetupRequest,
        prepared: StripePaymentSheetPreparedConfig,
        outcome: SimulatedPaymentSheetOutcome,
        nowMs: Long = System.currentTimeMillis()
    ): StripeTerminalExecutionStep {
        if (!request.localizedAmountMajor.isFinite() || request.localizedAmountMajor <= 0.0) {
            return buildFailedStep(
                errorCode = "paymentsheet_failed",
                message = "Checkout amount must be greater than zero."
            )
        }

        return when (outcome) {
            SimulatedPaymentSheetOutcome.COMPLETED -> buildCompletedStep(
                request = request,
                prepared = prepared,
                nowMs = nowMs
            )
            SimulatedPaymentSheetOutcome.CANCELED -> buildCanceledStep()
            SimulatedPaymentSheetOutcome.FAILED -> buildFailedStep(
                errorCode = "paymentsheet_failed",
                message = "Simulated issuer decline inside Stripe PaymentSheet (card_declined)."
            )
            SimulatedPaymentSheetOutcome.REQUIRES_SCA -> {
                val piId = generateStripeId("pi_ps_sim")
                val pmId = generateStripeId("pm_ps_sim")
                val customerLabel = request.overrideCustomerName?.trim()?.ifBlank { null }
                    ?: request.customerProfile.displayName
                StripeTerminalExecutionStep.Requires3dsAuthentication(
                    challenge = Stripe3dsChallenge(
                        paymentIntentId = piId,
                        paymentMethodId = pmId,
                        clientSecretMasked = "${piId}_secret_••••••••",
                        localizedAmountFormatted = prepared.localizedAmountFormatted,
                        minorUnits = prepared.minorUnits,
                        currencyCode = prepared.currencyCode,
                        amountUsd = request.amountUsd,
                        exchangeRate = request.exchangeRate,
                        railId = "payment_sheet",
                        railName = "Stripe PaymentSheet (${prepared.paymentMethodOrder.firstOrNull()?.uppercase(Locale.US) ?: "CARD"})",
                        stripeMethodCode = prepared.paymentMethodOrder.firstOrNull() ?: "card",
                        cardBrand = request.customerProfile.defaultCardBrand,
                        last4 = request.customerProfile.defaultCardLast4,
                        customerName = customerLabel,
                        countryName = prepared.countryName,
                        countryIso2 = prepared.countryIso2,
                        challengeReason = "3DS2 / Strong Customer Authentication triggered during simulated PaymentSheet checkout for $customerLabel",
                        sdkPublishableKeyHint = prepared.publishableKeyHint,
                        idempotencyKey = UUID.randomUUID().toString(),
                        terminalId = "PS-SHEET-SIM",
                        itemsSummary = "PaymentSheet Customer Checkout (${prepared.configDigest})",
                        sdkParamsFingerprint = prepared.configDigest,
                        createdAtMs = nowMs
                    )
                )
            }
        }
    }

    fun mapPaymentSheetResultToStep(
        result: PaymentSheetResult,
        request: StripePaymentSheetSetupRequest,
        prepared: StripePaymentSheetPreparedConfig,
        nowMs: Long = System.currentTimeMillis()
    ): StripeTerminalExecutionStep {
        return when (result) {
            is PaymentSheetResult.Completed -> buildCompletedStep(
                request = request,
                prepared = prepared,
                nowMs = nowMs
            )
            is PaymentSheetResult.Canceled -> buildCanceledStep()
            is PaymentSheetResult.Failed -> buildFailedStep(
                errorCode = "paymentsheet_failed",
                message = result.error.localizedMessage
                    ?: "Stripe PaymentSheet failed to complete checkout."
            )
        }
    }

    private fun buildCompletedStep(
        request: StripePaymentSheetSetupRequest,
        prepared: StripePaymentSheetPreparedConfig,
        nowMs: Long
    ): StripeTerminalExecutionStep.Settled {
        val piId = prepared.clientSecret
            ?.substringBefore("_secret_")
            ?.takeIf { it.startsWith("pi_") }
            ?: generateStripeId("pi_ps_sim")
        val pmId = generateStripeId("pm_ps_sim")
        val refSuffix = piId.takeLast(6).uppercase(Locale.US)
        val customerLabel = request.overrideCustomerName?.trim()?.ifBlank { null }
            ?: request.customerProfile.displayName
        val primaryRailCode = prepared.paymentMethodOrder.firstOrNull() ?: "card"

        return StripeTerminalExecutionStep.Settled(
            receipt = StripeTerminalReceipt(
                paymentIntentId = piId,
                paymentMethodId = pmId,
                clientSecretMasked = "${piId}_secret_••••••••",
                chargeId = generateStripeId("ch_ps_sim"),
                referenceCode = "PS-${prepared.currencyCode}-$refSuffix",
                status = "succeeded",
                scaStatus = if (prepared.hasValidClientSecret) {
                    "PaymentSheet Live Intent Confirmed"
                } else {
                    "PaymentSheet Simulated Customer Checkout"
                },
                localizedAmountMajor = prepared.localizedAmountMajor,
                localizedAmountFormatted = prepared.localizedAmountFormatted,
                minorUnits = prepared.minorUnits,
                currencyCode = prepared.currencyCode,
                amountUsd = request.amountUsd,
                exchangeRate = request.exchangeRate,
                railId = "payment_sheet",
                railName = "Stripe PaymentSheet",
                stripeMethodCode = primaryRailCode,
                cardBrand = request.customerProfile.defaultCardBrand,
                last4 = request.customerProfile.defaultCardLast4,
                customerName = customerLabel,
                countryName = prepared.countryName,
                countryIso2 = prepared.countryIso2,
                idempotencyKey = UUID.randomUUID().toString(),
                sdkEnvironmentBadge = prepared.environmentBadge,
                sdkPublishableKeyHint = prepared.publishableKeyHint,
                sdkParamsFingerprint = prepared.configDigest,
                terminalId = "PS-SHEET-SIM",
                itemsSummary = "PaymentSheet Checkout (${prepared.customerId ?: "guest"})",
                settledAtMs = nowMs
            )
        )
    }

    private fun buildCanceledStep(): StripeTerminalExecutionStep.Declined =
        StripeTerminalExecutionStep.Declined(
            errorCode = "paymentsheet_canceled",
            message = "Customer dismissed Stripe PaymentSheet before confirming payment.",
            isRetryable = true
        )

    private fun buildFailedStep(
        errorCode: String,
        message: String
    ): StripeTerminalExecutionStep.Declined =
        StripeTerminalExecutionStep.Declined(
            errorCode = errorCode,
            message = message,
            isRetryable = true
        )

    private fun generateStripeId(prefix: String): String {
        val randomHex = UUID.randomUUID().toString().replace("-", "").take(14)
        return "${prefix}_$randomHex"
    }

    private fun sha256Short(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(6).joinToString("") { "%02x".format(it) }
    }
}
