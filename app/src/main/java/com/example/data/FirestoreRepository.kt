package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "OWNER",
    val organization: String = "",
    val orgId: String = "",
    val country: String = "",
    val currency: String = "USD",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestorePayoutSetting(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val selectedMethod: String = "BANK_WIRE",
    val bankName: String = "",
    val accountHolderName: String = "",
    val accountNumberOrIban: String = "",
    val swiftBic: String = "",
    val routingNumber: String = "",
    val intermediaryBank: String = "",
    val paypalEmailOrId: String = "",
    val recipientName: String = "",
    val phoneOrPaypalMe: String = "",
    val accountType: String = "Business",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreOwnerPayoutRecord(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val payoutId: String = "",
    val method: String = "BANK_WIRE",
    val amount: Double = 0.0,
    val fee: Double = 0.0,
    val netAmount: Double = 0.0,
    val currency: String = "USD",
    val recipientName: String = "",
    val destinationSummary: String = "",
    val reference: String = "",
    val status: String = "COMPLETED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreCloudTransactionLog(
    @DocumentId val docId: String = "",
    val userId: String = "",
    val logId: String = "",
    val logType: String = "PAYMENT",
    val title: String = "",
    val amount: Double = 0.0,
    val currencyCode: String = "USD",
    val referenceCode: String = "",
    val customerOrRecipient: String = "",
    val status: String = "SETTLED",
    @ServerTimestamp val createdAt: Timestamp? = null
)

open class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        val ALLOWED_PAYOUT_METHODS = setOf(
            "BANK_WIRE",
            "PAYPAL",
            "STRIPE_PAYOUT",
            "IDEAL",
            "SEPA_INSTANT"
        )

        val ALLOWED_PAYOUT_STATUSES = setOf(
            "PENDING",
            "PROCESSING",
            "IN_TRANSIT",
            "COMPLETED",
            "SETTLED",
            "PAID",
            "FAILED",
            "CANCELED"
        )
    }

    open fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be authenticated before accessing Firestore.")
        }
        return uid
    }

    open suspend fun saveUserProfile(profile: FirestoreUserProfile): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val payload = mapOf(
                "userId" to uid,
                "fullName" to profile.fullName,
                "email" to profile.email,
                "role" to profile.role,
                "organization" to profile.organization,
                "orgId" to profile.orgId,
                "country" to profile.country,
                "currency" to profile.currency,
                "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    open suspend fun getUserProfile(): Result<FirestoreUserProfile?> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val snapshot = db.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                Result.success(
                    snapshot.toObject(
                        FirestoreUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    open suspend fun savePayoutSetting(setting: FirestorePayoutSetting): Result<Unit> {
        var path = "users/unauthenticated/payout_settings/active"
        return try {
            val uid = requireUserId()
            path = "users/$uid/payout_settings/active"
            val normalizedMethod = setting.selectedMethod.uppercase().takeIf {
                it in setOf("BANK_WIRE", "PAYPAL", "STRIPE_PAYOUT")
            } ?: "BANK_WIRE"
            val payload = mapOf(
                "userId" to uid,
                "selectedMethod" to normalizedMethod,
                "bankName" to setting.bankName.take(100),
                "accountHolderName" to setting.accountHolderName.take(100),
                "accountNumberOrIban" to setting.accountNumberOrIban.take(50),
                "swiftBic" to setting.swiftBic.take(20),
                "routingNumber" to setting.routingNumber.take(30),
                "intermediaryBank" to setting.intermediaryBank.take(100),
                "paypalEmailOrId" to setting.paypalEmailOrId.take(120),
                "recipientName" to setting.recipientName.take(100),
                "phoneOrPaypalMe" to setting.phoneOrPaypalMe.take(100),
                "accountType" to setting.accountType.take(40),
                "createdAt" to (setting.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("payout_settings").document("active")
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    open suspend fun getPayoutSetting(): Result<FirestorePayoutSetting?> {
        var path = "users/unauthenticated/payout_settings/active"
        return try {
            val uid = requireUserId()
            path = "users/$uid/payout_settings/active"
            val snapshot = db.collection("users").document(uid)
                .collection("payout_settings").document("active").get().await()
            if (snapshot.exists()) {
                Result.success(
                    snapshot.toObject(
                        FirestorePayoutSetting::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    open suspend fun recordOwnerPayout(payout: FirestoreOwnerPayoutRecord): Result<Unit> {
        var path = "users/unauthenticated/payouts"
        return try {
            val uid = requireUserId()
            val docId = payout.payoutId.ifBlank { "payout_${System.currentTimeMillis()}" }.take(128)
            path = "users/$uid/payouts/$docId"
            val normalizedMethod = payout.method.uppercase().takeIf { it in ALLOWED_PAYOUT_METHODS } ?: "BANK_WIRE"
            val normalizedStatus = payout.status.uppercase().takeIf { it in ALLOWED_PAYOUT_STATUSES } ?: "PROCESSING"
            val safeAmount = payout.amount.coerceAtLeast(0.01)
            val safeFee = payout.fee.coerceAtLeast(0.0)
            val safeNet = payout.netAmount.coerceAtLeast(0.0)
            val payload = mapOf(
                "userId" to uid,
                "payoutId" to docId,
                "method" to normalizedMethod,
                "amount" to safeAmount,
                "fee" to safeFee,
                "netAmount" to safeNet,
                "currency" to payout.currency.ifBlank { "USD" }.take(10),
                "recipientName" to payout.recipientName.ifBlank { "Merchant Owner" }.take(120),
                "destinationSummary" to payout.destinationSummary.take(160),
                "reference" to payout.reference.ifBlank { docId }.take(128),
                "status" to normalizedStatus,
                "createdAt" to (payout.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("payouts").document(docId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    open suspend fun updateOwnerPayoutStatus(payoutId: String, newStatus: String): Result<Unit> {
        var path = "users/unauthenticated/payouts/$payoutId"
        return try {
            val uid = requireUserId()
            require(payoutId.isNotBlank()) { "Payout ID must not be blank for status update" }
            val normalizedStatus = newStatus.uppercase()
            require(normalizedStatus in ALLOWED_PAYOUT_STATUSES) {
                "Unsupported payout status '$newStatus'"
            }
            path = "users/$uid/payouts/$payoutId"
            db.collection("users").document(uid)
                .collection("payouts").document(payoutId)
                .update(
                    mapOf(
                        "status" to normalizedStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    open fun observeOwnerPayouts(): Flow<List<FirestoreOwnerPayoutRecord>> = flow {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        if (uid == null) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$uid/payouts"
        emitAll(
            db.collection("users").document(uid)
                .collection("payouts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreOwnerPayoutRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { record ->
                        record.updatedAt ?: record.createdAt ?: Timestamp(0, 0)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    open suspend fun recordTransactionLog(log: FirestoreCloudTransactionLog): Result<Unit> {
        var path = "users/unauthenticated/transaction_logs"
        return try {
            val uid = requireUserId()
            val docId = log.logId.ifBlank { "log_${System.currentTimeMillis()}" }
            path = "users/$uid/transaction_logs/$docId"
            val payload = mapOf(
                "userId" to uid,
                "logId" to docId,
                "logType" to log.logType,
                "title" to log.title.ifBlank { "Transaction" },
                "amount" to log.amount,
                "currencyCode" to log.currencyCode.ifBlank { "USD" },
                "referenceCode" to log.referenceCode.ifBlank { docId },
                "customerOrRecipient" to log.customerOrRecipient,
                "status" to log.status,
                "createdAt" to (log.createdAt ?: FieldValue.serverTimestamp())
            )
            db.collection("users").document(uid)
                .collection("transaction_logs").document(docId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    open fun observeTransactionLogs(): Flow<List<FirestoreCloudTransactionLog>> = flow {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        if (uid == null) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$uid/transaction_logs"
        emitAll(
            db.collection("users").document(uid)
                .collection("transaction_logs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreCloudTransactionLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }
}
