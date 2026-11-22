package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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

    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

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

data class AuthenticatedUserSummary(
    val uid: String,
    val displayName: String,
    val email: String
)

open class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
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

        val ALLOWED_LOG_TYPES = setOf(
            "PAYMENT",
            "SCANNED_ITEM",
            "PAYOUT",
            "WEB_CHECKOUT"
        )

        fun sanitizeFirestoreId(raw: String, fallbackPrefix: String): String {
            val sanitized = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim('_')
            return sanitized.ifBlank { "${fallbackPrefix}_${System.currentTimeMillis()}" }.take(128)
        }
    }

    open fun currentUserSummary(): AuthenticatedUserSummary? {
        val user = auth.currentUser ?: return null
        val uid = user.uid.takeIf { it.isNotBlank() } ?: return null
        return AuthenticatedUserSummary(
            uid = uid,
            displayName = user.displayName?.trim().orEmpty(),
            email = user.email?.trim().orEmpty()
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
            val docRef = db.collection("users").document(uid)
            val safeName = profile.fullName.trim().ifBlank {
                auth.currentUser?.displayName?.trim().orEmpty().ifBlank { "Merchant User" }
            }.take(100)
            val safeEmail = profile.email.trim().ifBlank {
                auth.currentUser?.email?.trim().orEmpty().ifBlank { "merchant@globalmarket.pro" }
            }.take(120)

            val mutableFields = mapOf<String, Any?>(
                "fullName" to safeName,
                "email" to safeEmail,
                "role" to profile.role.take(50),
                "organization" to profile.organization.take(100),
                "orgId" to profile.orgId.take(50),
                "country" to profile.country.take(60),
                "currency" to profile.currency.ifBlank { "USD" }.take(10),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(mutableFields).await()
            } else {
                val createPayload = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
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
            val docRef = db.collection("users").document(uid)
                .collection("payout_settings").document("active")
            val normalizedMethod = setting.selectedMethod.uppercase().takeIf {
                it in setOf("BANK_WIRE", "PAYPAL", "STRIPE_PAYOUT")
            } ?: "BANK_WIRE"

            val mutableFields = mapOf<String, Any?>(
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
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(mutableFields).await()
            } else {
                val createPayload = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
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
            val docId = sanitizeFirestoreId(payout.payoutId, "payout")
            path = "users/$uid/payouts/$docId"
            val docRef = db.collection("users").document(uid)
                .collection("payouts").document(docId)
            val normalizedMethod = payout.method.uppercase().takeIf { it in ALLOWED_PAYOUT_METHODS } ?: "BANK_WIRE"
            val normalizedStatus = payout.status.uppercase().takeIf { it in ALLOWED_PAYOUT_STATUSES } ?: "PROCESSING"

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "status" to normalizedStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val safeAmount = payout.amount.coerceAtLeast(0.01)
                val safeFee = payout.fee.coerceAtLeast(0.0)
                val safeNet = payout.netAmount.coerceAtLeast(0.0)
                val payload = mapOf<String, Any?>(
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
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
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
            val docId = sanitizeFirestoreId(payoutId, "payout")
            val normalizedStatus = newStatus.uppercase()
            require(normalizedStatus in ALLOWED_PAYOUT_STATUSES) {
                "Unsupported payout status '$newStatus'"
            }
            path = "users/$uid/payouts/$docId"
            db.collection("users").document(uid)
                .collection("payouts").document(docId)
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
            val docId = sanitizeFirestoreId(log.logId, "log")
            path = "users/$uid/transaction_logs/$docId"
            val docRef = db.collection("users").document(uid)
                .collection("transaction_logs").document(docId)
            val safeType = log.logType.uppercase().takeIf { it in ALLOWED_LOG_TYPES } ?: "PAYMENT"
            val safeTitle = log.title.ifBlank { "Transaction" }.take(160)
            val safeCustomer = log.customerOrRecipient.take(120)
            val safeStatus = log.status.ifBlank { "SETTLED" }.take(40)

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "customerOrRecipient" to safeCustomer,
                        "status" to safeStatus
                    )
                ).await()
            } else {
                val payload = mapOf<String, Any?>(
                    "userId" to uid,
                    "logId" to docId,
                    "logType" to safeType,
                    "title" to safeTitle,
                    "amount" to log.amount.coerceAtLeast(0.0),
                    "currencyCode" to log.currencyCode.ifBlank { "USD" }.take(10),
                    "referenceCode" to log.referenceCode.ifBlank { docId }.take(128),
                    "customerOrRecipient" to safeCustomer,
                    "status" to safeStatus,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
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
