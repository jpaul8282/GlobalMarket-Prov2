package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

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
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

data class FirestoreUserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "OWNER",
    val organization: String = "",
    val orgId: String = "",
    val country: String = "",
    val currency: String = "USD",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestorePayoutSetting(
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
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestoreOwnerPayoutRecord(
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
    val createdAt: Timestamp? = null
)

data class FirestoreCloudTransactionLog(
    val userId: String = "",
    val logId: String = "",
    val logType: String = "PAYMENT",
    val title: String = "",
    val amount: Double = 0.0,
    val currencyCode: String = "USD",
    val referenceCode: String = "",
    val customerOrRecipient: String = "",
    val status: String = "SETTLED",
    val createdAt: Timestamp? = null
)

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing Firestore.")
    }

    suspend fun saveUserProfile(profile: FirestoreUserProfile): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
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
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<FirestoreUserProfile?> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                Result.success(snapshot.toObject(FirestoreUserProfile::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun savePayoutSetting(setting: FirestorePayoutSetting): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/payout_settings/active"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "selectedMethod" to setting.selectedMethod,
                "bankName" to setting.bankName,
                "accountHolderName" to setting.accountHolderName,
                "accountNumberOrIban" to setting.accountNumberOrIban,
                "swiftBic" to setting.swiftBic,
                "routingNumber" to setting.routingNumber,
                "intermediaryBank" to setting.intermediaryBank,
                "paypalEmailOrId" to setting.paypalEmailOrId,
                "recipientName" to setting.recipientName,
                "phoneOrPaypalMe" to setting.phoneOrPaypalMe,
                "accountType" to setting.accountType,
                "createdAt" to (setting.createdAt ?: FieldValue.serverTimestamp()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("payout_settings").document("active")
                .set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun getPayoutSetting(): Result<FirestorePayoutSetting?> {
        val uid = requireUserId()
        val path = "users/$uid/payout_settings/active"
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("payout_settings").document("active").get().await()
            if (snapshot.exists()) {
                Result.success(snapshot.toObject(FirestorePayoutSetting::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun recordOwnerPayout(payout: FirestoreOwnerPayoutRecord): Result<Unit> {
        val uid = requireUserId()
        val docId = payout.payoutId.ifBlank { "payout_${System.currentTimeMillis()}" }
        val path = "users/$uid/payouts/$docId"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "payoutId" to docId,
                "method" to payout.method,
                "amount" to payout.amount,
                "fee" to payout.fee,
                "netAmount" to payout.netAmount,
                "currency" to payout.currency,
                "recipientName" to payout.recipientName,
                "destinationSummary" to payout.destinationSummary,
                "reference" to payout.reference,
                "status" to payout.status,
                "createdAt" to (payout.createdAt ?: FieldValue.serverTimestamp())
            )
            db.collection("users").document(uid)
                .collection("payouts").document(docId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeOwnerPayouts(): Flow<List<FirestoreOwnerPayoutRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/payouts"
        emitAll(
            db.collection("users").document(uid)
                .collection("payouts")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreOwnerPayoutRecord::class.java) }
        )
    }

    suspend fun recordTransactionLog(log: FirestoreCloudTransactionLog): Result<Unit> {
        val uid = requireUserId()
        val docId = log.logId.ifBlank { "log_${System.currentTimeMillis()}" }
        val path = "users/$uid/transaction_logs/$docId"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "logId" to docId,
                "logType" to log.logType,
                "title" to log.title,
                "amount" to log.amount,
                "currencyCode" to log.currencyCode,
                "referenceCode" to log.referenceCode,
                "customerOrRecipient" to log.customerOrRecipient,
                "status" to log.status,
                "createdAt" to (log.createdAt ?: FieldValue.serverTimestamp())
            )
            db.collection("users").document(uid)
                .collection("transaction_logs").document(docId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeTransactionLogs(): Flow<List<FirestoreCloudTransactionLog>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users").document(uid)
                .collection("transaction_logs")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreCloudTransactionLog::class.java) }
        )
    }
}
