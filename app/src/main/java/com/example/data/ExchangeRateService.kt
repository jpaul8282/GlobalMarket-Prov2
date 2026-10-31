package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Retrofit data model for exchange rates API response.
 */
data class ExchangeRatesApiResponse(
    @Json(name = "result") val result: String = "success",
    @Json(name = "base_code") val baseCode: String = "USD",
    @Json(name = "rates") val rates: Map<String, Double> = emptyMap(),
    @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long = System.currentTimeMillis() / 1000
)

/**
 * Retrofit API interface for querying live international foreign exchange rates.
 */
interface ExchangeRateApiService {
    @GET("v4/latest/{baseCurrency}")
    suspend fun getLatestRates(
        @Path("baseCurrency") baseCurrency: String = "USD"
    ): ExchangeRatesApiResponse
}

/**
 * Service providing Retrofit network client and fallback live rates calculator.
 */
object ExchangeRateClient {
    private const val BASE_URL = "https://api.exchangerate-api.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val api: ExchangeRateApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRateApiService::class.java)
    }

    /**
     * Standard mid-market baseline exchange rates relative to USD.
     */
    val baselineRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.78,
        "JPY" to 155.20,
        "CNY" to 7.25,
        "CAD" to 1.37,
        "AUD" to 1.52,
        "CHF" to 0.90,
        "SGD" to 1.34,
        "INR" to 83.50,
        "BRL" to 5.45,
        "MXN" to 18.20
    )

    /**
     * Attempts to fetch live rates from the API for the requested base currency,
     * falling back to offline cross-currency calculated rates if network is unavailable.
     */
    suspend fun fetchRates(baseCurrency: String = "USD"): Pair<Map<String, Double>, Boolean> {
        return try {
            val response = api.getLatestRates(baseCurrency)
            if (response.rates.isNotEmpty()) {
                Pair(response.rates, true)
            } else {
                Pair(calculateBaselineRatesForBase(baseCurrency), false)
            }
        } catch (e: Exception) {
            Pair(calculateBaselineRatesForBase(baseCurrency), false)
        }
    }

    /**
     * Computes cross rates for any base currency relative to USD.
     */
    fun calculateBaselineRatesForBase(baseCurrency: String): Map<String, Double> {
        val baseToUsd = baselineRates[baseCurrency] ?: 1.0
        return baselineRates.mapValues { (_, rateToUsd) ->
            Math.round((rateToUsd / baseToUsd) * 10000.0) / 10000.0
        }
    }
}
