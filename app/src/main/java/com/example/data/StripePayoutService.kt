package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

// ==========================================
// Stripe REST API DTOs (https://api.stripe.com/v1/)
// ==========================================

data class StripeCurrencyBalanceDto(
    @param:Json(name = "amount") val amountMinor: Long = 0L,
    @param:Json(name = "currency") val currency: String = "usd"
)

data class StripeBalanceResponseDto(
    @param:Json(name = "object") val objectType: String = "balance",
    @param:Json(name = "livemode") val livemode: Boolean = false,
    @param:Json(name = "available") val available: List<StripeCurrencyBalanceDto> = emptyList(),
    @param:Json(name = "pending") val pending: List<StripeCurrencyBalanceDto> = emptyList()
)

data class StripeExternalAccountDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "object") val objectType: String = "bank_account",
    @param:Json(name = "bank_name") val bankName: String? = null,
    @param:Json(name = "brand") val brand: String? = null,
    @param:Json(name = "country") val country: String? = null,
    @param:Json(name = "currency") val currency: String? = null,
    @param:Json(name = "last4") val last4: String? = null,
    @param:Json(name = "routing_number") val routingNumber: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "default_for_currency") val defaultForCurrency: Boolean = false
)

data class StripeExternalAccountListDto(
    @param:Json(name = "object") val objectType: String = "list",
    @param:Json(name = "data") val data: List<StripeExternalAccountDto> = emptyList()
)

data class StripeBusinessProfileDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "support_email") val supportEmail: String? = null,
    @param:Json(name = "url") val url: String? = null
)

data class StripeDashboardSettingsDto(
    @param:Json(name = "display_name") val displayName: String? = null
)

data class StripeAccountSettingsDto(
    @param:Json(name = "dashboard") val dashboard: StripeDashboardSettingsDto? = null
)

data class StripeAccountDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "object") val objectType: String = "account",
    @param:Json(name = "country") val country: String? = null,
    @param:Json(name = "default_currency") val defaultCurrency: String? = null,
    @param:Json(name = "payouts_enabled") val payoutsEnabled: Boolean = false,
    @param:Json(name = "charges_enabled") val chargesEnabled: Boolean = false,
    @param:Json(name = "details_submitted") val detailsSubmitted: Boolean = false,
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "business_profile") val businessProfile: StripeBusinessProfileDto? = null,
    @param:Json(name = "settings") val settings: StripeAccountSettingsDto? = null,
    @param:Json(name = "external_accounts") val externalAccounts: StripeExternalAccountListDto? = null
)

data class StripePayoutDto(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "object") val objectType: String = "payout",
    @param:Json(name = "amount") val amountMinor: Long = 0L,
    @param:Json(name = "currency") val currency: String = "usd",
    @param:Json(name = "arrival_date") val arrivalDateEpochSec: Long = 0L,
    @param:Json(name = "created") val createdEpochSec: Long = 0L,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "destination") val destination: String? = null,
    @param:Json(name = "failure_code") val failureCode: String? = null,
    @param:Json(name = "failure_message") val failureMessage: String? = null,
    @param:Json(name = "livemode") val livemode: Boolean = false,
    @param:Json(name = "method") val method: String = "standard", // "standard" or "instant"
    @param:Json(name = "statement_descriptor") val statementDescriptor: String? = null,
    @param:Json(name = "status") val status: String = "pending", // "paid", "pending", "in_transit", "canceled", "failed"
    @param:Json(name = "type") val type: String = "bank_account" // "bank_account" or "card"
)

data class StripePayoutListResponseDto(
    @param:Json(name = "object") val objectType: String = "list",
    @param:Json(name = "data") val data: List<StripePayoutDto> = emptyList(),
    @param:Json(name = "has_more") val hasMore: Boolean = false
)

// ==========================================
// Domain Models & Result Wrapper
// ==========================================

enum class StripeConnectionHealth(val label: String) {
    KEY_REQUIRED("KEY REQUIRED"),
    DISCONNECTED("NOT TESTED"),
    CHECKING("CONNECTING..."),
    CONNECTED("CONNECTED"),
    RESTRICTED_CONNECTED("RESTRICTED CONNECTED"),
    ERROR("CONNECTION ERROR")
}

data class StripeCurrencyBalance(
    val currencyCode: String,
    val majorAmount: Double,
    val minorUnits: Long
)

data class StripeBalanceSummary(
    val livemode: Boolean,
    val available: List<StripeCurrencyBalance>,
    val pending: List<StripeCurrencyBalance>
)

data class StripeExternalAccount(
    val id: String,
    val type: String,
    val label: String,
    val last4: String,
    val currencyCode: String,
    val country: String?,
    val status: String?,
    val isDefaultForCurrency: Boolean
)

data class StripeAccountInfo(
    val accountId: String,
    val businessName: String,
    val country: String,
    val defaultCurrency: String,
    val accountType: String,
    val payoutsEnabled: Boolean,
    val chargesEnabled: Boolean,
    val detailsSubmitted: Boolean,
    val externalAccounts: List<StripeExternalAccount>
)

data class StripeConnectionVerification(
    val balanceSummary: StripeBalanceSummary,
    val accountInfo: StripeAccountInfo?,
    val recentPayouts: List<StripePayoutItem>,
    val isRestrictedKey: Boolean,
    val latencyMs: Long,
    val requestId: String?
)

data class StripePayoutItem(
    val id: String,
    val majorAmount: Double,
    val minorUnits: Long,
    val currencyCode: String,
    val method: String,
    val status: String,
    val livemode: Boolean,
    val arrivalDateEpochMs: Long,
    val createdEpochMs: Long,
    val destination: String?,
    val description: String?,
    val statementDescriptor: String?,
    val failureCode: String?,
    val failureMessage: String?,
    val payoutType: String
)

sealed class StripePayoutApiResult<out T> {
    data class Success<T>(val data: T) : StripePayoutApiResult<T>()
    data class MissingApiKey(val message: String) : StripePayoutApiResult<Nothing>()
    data class ApiError(
        val httpCode: Int?,
        val stripeErrorCode: String?,
        val message: String
    ) : StripePayoutApiResult<Nothing>()
}

// ==========================================
// Retrofit Interface
// ==========================================

interface StripePayoutApiService {

    @GET("v1/account")
    suspend fun getAccount(
        @Header("Authorization") authorization: String,
        @Header("Stripe-Account") stripeAccount: String? = null
    ): StripeAccountDto

    @GET("v1/balance")
    suspend fun getBalance(
        @Header("Authorization") authorization: String,
        @Header("Stripe-Account") stripeAccount: String? = null
    ): StripeBalanceResponseDto

    @GET("v1/payouts")
    suspend fun listPayouts(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 15,
        @Header("Stripe-Account") stripeAccount: String? = null
    ): StripePayoutListResponseDto

    @FormUrlEncoded
    @POST("v1/payouts")
    suspend fun createPayout(
        @Header("Authorization") authorization: String,
        @Field("amount") amountMinor: Long,
        @Field("currency") currencyLowercase: String,
        @Field("method") method: String = "standard",
        @Field("destination") destination: String? = null,
        @Field("description") description: String? = null,
        @Field("statement_descriptor") statementDescriptor: String? = null,
        @Field("metadata[source]") metadataSource: String = "GlobalMarketPro",
        @Header("Idempotency-Key") idempotencyKey: String? = null,
        @Header("Stripe-Account") stripeAccount: String? = null
    ): StripePayoutDto

    @POST("v1/payouts/{payoutId}/cancel")
    suspend fun cancelPayout(
        @Header("Authorization") authorization: String,
        @Path("payoutId") payoutId: String,
        @Header("Stripe-Account") stripeAccount: String? = null
    ): StripePayoutDto
}

// ==========================================
// Gateway & Factory
// ==========================================

class StripePayoutGateway(
    private val apiProvider: () -> StripePayoutApiService = { StripePayoutClient.api },
    private val publishableKeyProvider: () -> String = { BuildConfig.STRIPE_PUBLISHABLE_KEY },
    private val secretKeyProvider: () -> String = { BuildConfig.STRIPE_SECRET_KEY },
    private val connectedAccountProvider: () -> String = { BuildConfig.STRIPE_CONNECTED_ACCOUNT_ID },
    private val lastRequestIdProvider: () -> String? = { StripePayoutClient.lastRequestId }
) {

    companion object {
        const val STRIPE_API_VERSION = "2024-06-20"
        private const val PLACEHOLDER_PUBLISHABLE = "MY_STRIPE_PUBLISHABLE_KEY"
        private const val PLACEHOLDER_SECRET = "MY_STRIPE_SECRET_KEY"
        private const val PLACEHOLDER_ACCOUNT = "MY_STRIPE_CONNECTED_ACCOUNT_ID"

        private val ZERO_DECIMAL_CURRENCIES = setOf(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
            "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF"
        )

        fun toMinorUnits(majorAmount: Double, currencyCode: String): Long {
            val upper = currencyCode.uppercase(Locale.US)
            return if (ZERO_DECIMAL_CURRENCIES.contains(upper)) {
                majorAmount.roundToLong().coerceAtLeast(1L)
            } else {
                (majorAmount * 100.0).roundToLong().coerceAtLeast(1L)
            }
        }

        fun toMajorAmount(minorUnits: Long, currencyCode: String): Double {
            val upper = currencyCode.uppercase(Locale.US)
            return if (ZERO_DECIMAL_CURRENCIES.contains(upper)) {
                minorUnits.toDouble()
            } else {
                minorUnits / 100.0
            }
        }

        fun mapStripeStatusToRoomStatus(stripeStatus: String): String {
            return when (stripeStatus.lowercase(Locale.US)) {
                "paid" -> "SETTLED"
                "in_transit", "pending" -> "PROCESSING"
                "canceled" -> "CANCELED"
                "failed" -> "FAILED"
                else -> stripeStatus.uppercase(Locale.US)
            }
        }
    }

    private fun resolvedPublishableKey(): String? {
        val raw = publishableKeyProvider().trim()
        if (raw.isBlank() || raw == PLACEHOLDER_PUBLISHABLE || raw.startsWith("MY_")) {
            return null
        }
        return raw
    }

    private fun resolvedSecretKey(): String? {
        val raw = secretKeyProvider().trim()
        if (raw.isBlank() || raw == PLACEHOLDER_SECRET || raw.startsWith("MY_")) {
            return null
        }
        return raw
    }

    fun resolvedConnectedAccountId(): String? {
        val raw = connectedAccountProvider().trim()
        if (raw.isBlank() || raw == PLACEHOLDER_ACCOUNT || raw.startsWith("MY_") || !raw.startsWith("acct_")) {
            return null
        }
        return raw
    }

    fun isPublishableKeyConfigured(): Boolean = resolvedPublishableKey() != null

    fun isPublishableKeyMistakenForSecret(): Boolean = resolvedSecretKey()?.startsWith("pk_") == true

    fun isConfigured(): Boolean = resolvedSecretKey() != null && !isPublishableKeyMistakenForSecret()

    fun isLiveKey(): Boolean = resolvedSecretKey()?.startsWith("sk_live_") == true ||
        resolvedSecretKey()?.startsWith("rk_live_") == true ||
        resolvedPublishableKey()?.startsWith("pk_live_") == true

    fun isRestrictedKey(): Boolean = resolvedSecretKey()?.startsWith("rk_") == true

    fun environmentLabel(): String {
        if (isPublishableKeyMistakenForSecret()) return "SECRET KEY INVALID"
        val key = resolvedSecretKey() ?: resolvedPublishableKey() ?: return "API KEY REQUIRED"
        return when {
            key.startsWith("sk_live_") || key.startsWith("rk_live_") || key.startsWith("pk_live_") -> "LIVE MODE"
            key.startsWith("sk_test_") || key.startsWith("rk_test_") || key.startsWith("pk_test_") -> "TEST MODE"
            else -> "CUSTOM KEY"
        }
    }

    fun maskedPublishableKeyHint(): String {
        val key = resolvedPublishableKey() ?: return "Not configured in Secrets"
        if (key.length <= 12) return "••••••••"
        return "${key.take(8)}••••${key.takeLast(4)}"
    }

    fun maskedKeyHint(): String {
        val key = resolvedSecretKey() ?: return "Not configured in Secrets"
        if (key.length <= 12) return "••••••••"
        val prefix = key.take(8)
        val suffix = key.takeLast(4)
        return "$prefix••••$suffix"
    }

    private fun validateSecretKeyForRestCalls(actionVerb: String): StripePayoutApiResult<Nothing>? {
        val key = resolvedSecretKey()
        if (key == null) {
            return StripePayoutApiResult.MissingApiKey(
                "Configure STRIPE_SECRET_KEY in the AI Studio Secrets panel to $actionVerb."
            )
        }
        if (key.startsWith("pk_")) {
            return StripePayoutApiResult.ApiError(
                httpCode = 401,
                stripeErrorCode = "publishable_key_used_as_secret",
                message = "STRIPE_SECRET_KEY contains a Stripe Publishable Key (pk_...). Move pk_... to STRIPE_PUBLISHABLE_KEY and set a Stripe Secret Key (sk_live_... or sk_test_...) in STRIPE_SECRET_KEY."
            )
        }
        return null
    }

    suspend fun verifyConnection(limit: Int = 15): StripePayoutApiResult<StripeConnectionVerification> {
        validateSecretKeyForRestCalls("connect live Stripe payouts")?.let { return it }
        val key = resolvedSecretKey()!!

        val startedAtMs = System.currentTimeMillis()
        val authHeader = "Bearer $key"
        val stripeAcct = resolvedConnectedAccountId()

        // 1. Validate core balance capability
        val balanceDto = try {
            apiProvider().getBalance(
                authorization = authHeader,
                stripeAccount = stripeAcct
            )
        } catch (e: HttpException) {
            return parseHttpException(e)
        } catch (e: Exception) {
            return StripePayoutApiResult.ApiError(
                httpCode = null,
                stripeErrorCode = "network_error",
                message = e.localizedMessage ?: "Failed to connect to Stripe API"
            )
        }

        // 2. Best-effort account & external bank/card destinations lookup
        var accountInfo: StripeAccountInfo? = null
        var restrictedKeyDetected = isRestrictedKey()
        try {
            val acctDto = apiProvider().getAccount(
                authorization = authHeader,
                stripeAccount = stripeAcct
            )
            accountInfo = mapAccountDto(acctDto)
            restrictedKeyDetected = false
        } catch (e: HttpException) {
            if (e.code() == 403 || e.code() == 401) {
                restrictedKeyDetected = true
            }
        } catch (_: Exception) {
            // Keep verification alive if balance succeeded
        }

        // 3. Fetch recent payouts list
        val recentPayouts = try {
            apiProvider().listPayouts(
                authorization = authHeader,
                limit = limit.coerceIn(1, 50),
                stripeAccount = stripeAcct
            ).data.map { mapDtoToDomain(it) }
        } catch (_: Exception) {
            emptyList()
        }

        val elapsedMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(1L)
        return StripePayoutApiResult.Success(
            StripeConnectionVerification(
                balanceSummary = mapBalanceDto(balanceDto),
                accountInfo = accountInfo,
                recentPayouts = recentPayouts,
                isRestrictedKey = restrictedKeyDetected,
                latencyMs = elapsedMs,
                requestId = lastRequestIdProvider()
            )
        )
    }

    suspend fun fetchBalance(): StripePayoutApiResult<StripeBalanceSummary> {
        validateSecretKeyForRestCalls("connect live Stripe payouts")?.let { return it }
        val key = resolvedSecretKey()!!

        return try {
            val dto = apiProvider().getBalance(
                authorization = "Bearer $key",
                stripeAccount = resolvedConnectedAccountId()
            )
            StripePayoutApiResult.Success(mapBalanceDto(dto))
        } catch (e: HttpException) {
            parseHttpException(e)
        } catch (e: Exception) {
            StripePayoutApiResult.ApiError(
                httpCode = null,
                stripeErrorCode = "network_error",
                message = e.localizedMessage ?: "Failed to reach Stripe API"
            )
        }
    }

    suspend fun listPayouts(limit: Int = 15): StripePayoutApiResult<List<StripePayoutItem>> {
        validateSecretKeyForRestCalls("sync Stripe payouts")?.let { return it }
        val key = resolvedSecretKey()!!

        return try {
            val response = apiProvider().listPayouts(
                authorization = "Bearer $key",
                limit = limit.coerceIn(1, 50),
                stripeAccount = resolvedConnectedAccountId()
            )
            StripePayoutApiResult.Success(response.data.map { mapDtoToDomain(it) })
        } catch (e: HttpException) {
            parseHttpException(e)
        } catch (e: Exception) {
            StripePayoutApiResult.ApiError(
                httpCode = null,
                stripeErrorCode = "network_error",
                message = e.localizedMessage ?: "Failed to list Stripe payouts"
            )
        }
    }

    suspend fun createPayout(
        majorAmount: Double,
        currencyCode: String,
        speedMethod: String = "standard",
        destinationId: String? = null,
        description: String? = null,
        statementDescriptor: String? = "GLOBALMARKET PRO",
        idempotencyKey: String? = null
    ): StripePayoutApiResult<StripePayoutItem> {
        validateSecretKeyForRestCalls("execute live Stripe payouts")?.let { return it }
        val key = resolvedSecretKey()!!

        if (majorAmount <= 0.0) {
            return StripePayoutApiResult.ApiError(
                httpCode = 400,
                stripeErrorCode = "invalid_amount",
                message = "Payout amount must be greater than zero."
            )
        }

        val normalizedMethod = if (speedMethod.equals("instant", ignoreCase = true)) {
            "instant"
        } else {
            "standard"
        }
        val minorUnits = toMinorUnits(majorAmount, currencyCode)
        val descriptor = statementDescriptor?.trim()?.take(22)?.ifBlank { null }
        val resolvedDestination = destinationId?.trim()?.ifBlank { null }

        return try {
            val dto = apiProvider().createPayout(
                authorization = "Bearer $key",
                amountMinor = minorUnits,
                currencyLowercase = currencyCode.lowercase(Locale.US),
                method = normalizedMethod,
                destination = resolvedDestination,
                description = description?.take(200),
                statementDescriptor = descriptor,
                idempotencyKey = idempotencyKey,
                stripeAccount = resolvedConnectedAccountId()
            )
            StripePayoutApiResult.Success(mapDtoToDomain(dto))
        } catch (e: HttpException) {
            parseHttpException(e)
        } catch (e: Exception) {
            StripePayoutApiResult.ApiError(
                httpCode = null,
                stripeErrorCode = "network_error",
                message = e.localizedMessage ?: "Failed to create Stripe payout"
            )
        }
    }

    suspend fun cancelPayout(payoutId: String): StripePayoutApiResult<StripePayoutItem> {
        validateSecretKeyForRestCalls("cancel Stripe payouts")?.let { return it }
        val key = resolvedSecretKey()!!

        return try {
            val dto = apiProvider().cancelPayout(
                authorization = "Bearer $key",
                payoutId = payoutId,
                stripeAccount = resolvedConnectedAccountId()
            )
            StripePayoutApiResult.Success(mapDtoToDomain(dto))
        } catch (e: HttpException) {
            parseHttpException(e)
        } catch (e: Exception) {
            StripePayoutApiResult.ApiError(
                httpCode = null,
                stripeErrorCode = "network_error",
                message = e.localizedMessage ?: "Failed to cancel Stripe payout"
            )
        }
    }

    private fun mapBalanceDto(dto: StripeBalanceResponseDto): StripeBalanceSummary {
        return StripeBalanceSummary(
            livemode = dto.livemode,
            available = dto.available.map {
                val code = it.currency.uppercase(Locale.US)
                StripeCurrencyBalance(
                    currencyCode = code,
                    majorAmount = toMajorAmount(it.amountMinor, code),
                    minorUnits = it.amountMinor
                )
            },
            pending = dto.pending.map {
                val code = it.currency.uppercase(Locale.US)
                StripeCurrencyBalance(
                    currencyCode = code,
                    majorAmount = toMajorAmount(it.amountMinor, code),
                    minorUnits = it.amountMinor
                )
            }
        )
    }

    private fun mapAccountDto(dto: StripeAccountDto): StripeAccountInfo {
        val displayName = dto.businessProfile?.name?.takeIf { it.isNotBlank() }
            ?: dto.settings?.dashboard?.displayName?.takeIf { it.isNotBlank() }
            ?: "Stripe Merchant Account"
        val externals = dto.externalAccounts?.data.orEmpty().map { ext ->
            val label = when {
                !ext.bankName.isNullOrBlank() -> ext.bankName
                !ext.brand.isNullOrBlank() -> ext.brand
                ext.objectType.equals("card", ignoreCase = true) -> "Debit Card"
                else -> "Bank Account"
            }
            StripeExternalAccount(
                id = ext.id,
                type = ext.objectType,
                label = label,
                last4 = ext.last4 ?: "••••",
                currencyCode = ext.currency?.uppercase(Locale.US) ?: dto.defaultCurrency?.uppercase(Locale.US) ?: "USD",
                country = ext.country?.uppercase(Locale.US),
                status = ext.status,
                isDefaultForCurrency = ext.defaultForCurrency
            )
        }
        return StripeAccountInfo(
            accountId = dto.id,
            businessName = displayName,
            country = dto.country?.uppercase(Locale.US) ?: "US",
            defaultCurrency = dto.defaultCurrency?.uppercase(Locale.US) ?: "USD",
            accountType = dto.type ?: "standard",
            payoutsEnabled = dto.payoutsEnabled,
            chargesEnabled = dto.chargesEnabled,
            detailsSubmitted = dto.detailsSubmitted,
            externalAccounts = externals
        )
    }

    private fun mapDtoToDomain(dto: StripePayoutDto): StripePayoutItem {
        val upperCurrency = dto.currency.uppercase(Locale.US)
        return StripePayoutItem(
            id = dto.id,
            majorAmount = toMajorAmount(dto.amountMinor, upperCurrency),
            minorUnits = dto.amountMinor,
            currencyCode = upperCurrency,
            method = dto.method,
            status = dto.status,
            livemode = dto.livemode,
            arrivalDateEpochMs = dto.arrivalDateEpochSec * 1000L,
            createdEpochMs = dto.createdEpochSec * 1000L,
            destination = dto.destination,
            description = dto.description,
            statementDescriptor = dto.statementDescriptor,
            failureCode = dto.failureCode,
            failureMessage = dto.failureMessage,
            payoutType = dto.type
        )
    }

    private fun parseHttpException(e: HttpException): StripePayoutApiResult.ApiError {
        val code = e.code()
        val rawBody = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        var stripeCode: String? = null
        var stripeMessage: String? = null
        if (!rawBody.isNullOrBlank()) {
            try {
                val errObj = JSONObject(rawBody).optJSONObject("error")
                if (errObj != null) {
                    stripeCode = errObj.optString("code").takeIf { it.isNotBlank() }
                    stripeMessage = errObj.optString("message").takeIf { it.isNotBlank() }
                }
            } catch (_: Exception) {
                // Fallback to HTTP status message
            }
        }
        return StripePayoutApiResult.ApiError(
            httpCode = code,
            stripeErrorCode = stripeCode,
            message = stripeMessage ?: "Stripe API HTTP $code: ${e.message()}"
        )
    }
}

object StripePayoutClient {
    private const val BASE_URL = "https://api.stripe.com/"

    @Volatile
    var lastRequestId: String? = null
        private set

    @Volatile
    var lastHttpStatus: Int? = null
        private set

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stripeHeadersAndTelemetryInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestWithVersion = original.newBuilder()
            .header("Stripe-Version", StripePayoutGateway.STRIPE_API_VERSION)
            .build()
        val response: Response = chain.proceed(requestWithVersion)
        lastRequestId = response.header("Request-Id")
        lastHttpStatus = response.code
        response
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(stripeHeadersAndTelemetryInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("Stripe-Account")
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val api: StripePayoutApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(StripePayoutApiService::class.java)
    }
}
