# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve source file, line numbers, generic signatures, and runtime annotations
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Exceptions,*Annotation*,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep BuildConfig fields
-keep class com.example.BuildConfig { *; }

# Kotlin Metadata (required for Moshi KotlinJsonAdapterFactory & reflection)
-keep class kotlin.Metadata { *; }

# ==========================================
# Firebase (Firestore, Auth, AI Logic, App Check, Google Sign-In)
# ==========================================
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**

# Keep Firestore data models deserialized via DocumentSnapshot.toObject / toObjects
-keep class com.example.data.Firestore* { *; }
-keepclassmembers class com.example.data.Firestore* {
    <init>(...);
    <fields>;
    <methods>;
}

# ==========================================
# Retrofit 2 & OkHttp 3 / Okio
# ==========================================
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.example.data.*ApiService { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ==========================================
# Moshi JSON Serialization (KotlinJsonAdapterFactory & Codegen)
# ==========================================
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep @com.squareup.moshi.JsonQualifier interface *
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep project Retrofit/Moshi DTOs and API response models
-keep class com.example.data.*Dto { *; }
-keep class com.example.data.ExchangeRatesApiResponse { *; }

# ==========================================
# Room Database & Entities / DAOs
# ==========================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.*Entity { *; }
-keep class com.example.data.*Dao { *; }
-dontwarn androidx.room.paging.**

# ==========================================
# Kotlin Coroutines
# ==========================================
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory {
    kotlinx.coroutines.internal.MainDispatcherFactory SERVICE;
}
-keepclassmembernames class kotlinx.coroutines.CoroutineExceptionHandler {
    kotlinx.coroutines.CoroutineExceptionHandler SERVICE;
}
-dontwarn kotlinx.coroutines.**

# ==========================================
# Stripe Android SDK (PaymentConfiguration, PaymentMethodCreateParams, 3DS2)
# ==========================================
-keep class com.stripe.android.** { *; }
-keepclassmembers class com.stripe.android.** { *; }
-dontwarn com.stripe.android.**
-keep class com.example.data.Stripe* { *; }

