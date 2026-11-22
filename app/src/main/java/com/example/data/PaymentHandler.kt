package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.ui.GlobalMarketViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Coordinates Stripe Android SDK initialization from environment secrets
 * (`BuildConfig.STRIPE_PUBLISHABLE_KEY`) and presents [PaymentSheet] checkout flows.
 */
class PaymentHandler(
    private val publishableKeyProvider: () -> String = { BuildConfig.STRIPE_PUBLISHABLE_KEY }
) {

    companion object {
        private const val PLACEHOLDER_PUBLISHABLE = "MY_STRIPE_PUBLISHABLE_KEY"
        private const val SANDBOX_FALLBACK_PUBLISHABLE_KEY = "pk_test_sandbox_terminal_simulator"
    }

    @Volatile
    var isInitialized: Boolean = false
        private set

    @Volatile
    var activePublishableKey: String? = null
        private set

    fun resolveEnvironmentPublishableKey(): String? {
        val raw = publishableKeyProvider().trim()
        if (raw.isBlank() || raw == PLACEHOLDER_PUBLISHABLE || raw.startsWith("MY_")) {
            return null
        }
        return if (raw.startsWith("pk_")) raw else null
    }

    fun isEnvironmentKeyConfigured(): Boolean = resolveEnvironmentPublishableKey() != null

    fun isLiveEnvironmentKey(): Boolean =
        resolveEnvironmentPublishableKey()?.startsWith("pk_live_") == true

    fun maskedPublishableKey(): String {
        val key = resolveEnvironmentPublishableKey() ?: return "Sandbox Simulator Key"
        if (key.length <= 12) return "••••••••"
        return "${key.take(8)}••••${key.takeLast(4)}"
    }

    /**
     * Initializes the Stripe Android SDK ([PaymentConfiguration]) using `STRIPE_PUBLISHABLE_KEY`
     * from the build environment, falling back to a local sandbox key if not yet configured.
     */
    fun initializeStripeSdk(context: Context): String {
        val effectiveKey = resolveEnvironmentPublishableKey() ?: SANDBOX_FALLBACK_PUBLISHABLE_KEY
        PaymentConfiguration.init(context.applicationContext, effectiveKey)
        activePublishableKey = effectiveKey
        isInitialized = true
        return effectiveKey
    }

    fun buildPaymentSheetConfiguration(
        merchantDisplayName: String = "GlobalMarket Pro",
        customerConfiguration: PaymentSheet.CustomerConfiguration? = null,
        allowsDelayedPaymentMethods: Boolean = true
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration.Builder(merchantDisplayName)
            .apply {
                if (customerConfiguration != null) {
                    customer(customerConfiguration)
                }
            }
            .allowsDelayedPaymentMethods(allowsDelayedPaymentMethods)
            .build()
    }

    /**
     * Ensures the Stripe SDK is initialized and presents the native Stripe [PaymentSheet]
     * for a valid `PaymentIntent` client secret (`pi_..._secret_...`).
     */
    fun presentPaymentSheet(
        context: Context,
        paymentSheet: PaymentSheet,
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration = buildPaymentSheetConfiguration()
    ): Boolean {
        initializeStripeSdk(context)
        val trimmedSecret = paymentIntentClientSecret.trim()
        if (!StripePaymentSheetConfigFactory.isLikelyLivePaymentIntentClientSecret(trimmedSecret)) {
            return false
        }
        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret = trimmedSecret,
            configuration = configuration
        )
        return true
    }

    /**
     * Triggers the main UI checkout action: initializes the Stripe SDK with `STRIPE_PUBLISHABLE_KEY`,
     * prepares the localized [PaymentSheet.Configuration], and either launches the live native
     * [PaymentSheet] (if a valid backend `client_secret` is already attached) or opens the
     * Stripe PaymentSheet simulator sheet ready for one-tap transaction simulation.
     */
    fun launchCheckout(
        context: Context,
        viewModel: GlobalMarketViewModel,
        paymentSheet: PaymentSheet? = null
    ) {
        initializeStripeSdk(context)
        val prepared = viewModel.preparePaymentSheetConfiguration(context)
        val attachedSecret = viewModel.paymentSheetLiveClientSecret.value.trim()
        if (
            paymentSheet != null &&
            StripePaymentSheetConfigFactory.isLikelyLivePaymentIntentClientSecret(attachedSecret)
        ) {
            presentPaymentSheet(
                context = context,
                paymentSheet = paymentSheet,
                paymentIntentClientSecret = attachedSecret,
                configuration = prepared.configuration
            )
        } else {
            viewModel.openPaymentSheetCheckout()
        }
    }
}
