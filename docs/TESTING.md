# Testing & Code Quality Guide

## 🧪 Testing Strategy Overview

GlobalMarket Pro enforces a multi-tiered testing and verification strategy to ensure maximum reliability, security, and performance.

---

## 1. Automated Test Suite (Local JVM)

We utilize **Robolectric** to execute local JVM unit and integration tests with zero device/emulator lag.

### Running Tests
Execute the entire test suite via Gradle:
```bash
gradle :app:testDebugUnitTest
```

### Covered Test Scenarios (`app/src/test/java/com/example/ExampleRobolectricTest.kt`):
1. **Marketing Share Hub**:
   - Verification of campaign preset generation (Checkout, Partner, Flash Deal).
   - Validation of discount promo code attachment and parameterization.
   - Validation of localized pitch formatting.
2. **Localization & Language Switching**:
   - Dynamic currency and language dictionary mapping for English, Spanish, French, German, and Chinese.
3. **Enhanced Account & Security Settings**:
   - Profile updating and persistence.
   - 2FA state toggles and Auto-FX switching.
4. **Room Database DAO Operations**:
   - Insertion, querying, and deletion of `TransactionEntity` records using in-memory Room instances.

---

## 2. Screenshot & Visual Regression Testing

We utilize **Roborazzi** for pixel-accurate Compose UI testing:
```bash
# Verify UI against golden screenshot references
gradle :app:verifyRoborazziDebug

# Record new screenshot references
gradle :app:recordRoborazziDebug
```

---

## 3. Policy for Testing New Functionality

In accordance with OpenSSF Best Practices:
- Every new user-facing or architectural feature **must** be accompanied by automated unit or integration tests.
- PRs without test coverage for new logic will not be merged.
- Test suites must run cleanly and return `BUILD SUCCESSFUL` with 0 failures before any release.

---

## 4. Compiler Warning Flags & Static Analysis

### Compiler Configuration
The Kotlin and Java compilation pipelines are configured with strict warning checks:
- **Lint Checks**: Executed with `gradle :app:lintDebug` to detect Android SDK deprecations, accessibility violations, and missing resources.
- **Unused Dependency Pruning**: Unused dependencies are commented out in `build.gradle.kts` to optimize APK size and minimize attack vectors.
- **Kotlin Type Safety**: Strict type validation, nullable safety, and sealed class exhaustive `when` expressions.

---

## 5. Dynamic Analysis

- **Robolectric Simulated Runtime**: Verifies asynchronous coroutine dispatchers and reactive flow emissions.
- **StrictMode Verification**: Validates that all disk IO and database transactions occur off the main UI thread via `Dispatchers.IO`.
