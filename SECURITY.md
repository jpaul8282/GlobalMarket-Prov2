# Security & Cryptographic Policy

The **GlobalMarket Pro** project takes security and responsible vulnerability disclosure seriously. We follow the [OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/) criteria for open source software security.

---

## 🛡️ Supported Versions

Only the latest active major/minor release receives security updates and patches.

| Version | Supported          | Security Maintenance Status |
| ------- | ------------------ | --------------------------- |
| 2.0.x   | :white_check_mark: | Active support & patching   |
| 1.0.x   | :x:                | End of Life (Upgrade to 2.x)|

---

## 🚨 Vulnerability Reporting Process

If you discover a security vulnerability in GlobalMarket Pro, please report it responsibly:

1. **Private Email Disclosure**:
   - Send details to the maintainer: **`westerveldjp@gmail.com`**
   - Alternatively, submit a [Private Vulnerability Report via GitHub Security Advisories](https://github.com/jurgen-paul/GLABA-MARKETING-PRO-APP2/security/advisories/new).
   - **Do NOT** open a public issue on GitHub for unpatched security vulnerabilities.

2. **Information to Include**:
   - Component / module affected (e.g., Room DB, Network, Intent handling, Compose UI)
   - Step-by-step reproduction instructions or proof-of-concept (PoC)
   - Potential security impact (e.g., data exposure, spoofing, injection)

3. **Response SLAs**:
   - **Initial Acknowledgement**: Within **48 hours**
   - **Triage & Assessment**: Within **5 business days**
   - **Patch Release & Advisory Publication**: Within **30 days** of validation

---

## 🔐 Cryptographic Mechanisms & US Export Control Compliance

### 1. Cryptographic Usage
- GlobalMarket Pro uses industry-standard cryptographic algorithms provided by the Android OS platform:
  - **TLS 1.3 / HTTPS**: Enforced for all outbound network traffic via Android `NetworkSecurityConfig` (`cleartextTrafficPermitted="false"`).
  - **AES-GCM-256**: Supported for encrypted local sensitive storage utilizing the Android Keystore system.
  - **SHA-256 / SHA-512**: Used for integrity checks and cryptographic hashing.
- No proprietary, deprecated (e.g., MD5, SHA-1, DES, RC4), or broken cryptographic ciphers are implemented.

### 2. US Export Controls & EAR Compliance
*Note on Open Source Cryptographic Software*:
- Under the U.S. Export Administration Regulations (EAR, 15 C.F.R. § 742.15(b)), publicly available open-source software that utilizes cryptographic functionalities is eligible for export and re-export without a license, provided that the source code is publicly accessible and meets the notification criteria.
- The source code for GlobalMarket Pro is publicly hosted at [https://github.com/jurgen-paul/GLABA-MARKETING-PRO-APP2.git](https://github.com/jurgen-paul/GLABA-MARKETING-PRO-APP2.git).
- The cryptographic mechanisms utilized are standard platform libraries (Android BoringSSL / Conscrypt) rather than custom cryptographic engines.
- Formal notification has been maintained in accordance with standard FLOSS public repository guidelines.

---

## 🌐 Secured Delivery Against Man-in-the-Middle (MITM) Attacks

To prevent MITM eavesdropping and data tampering:
1. **Disabled Cleartext Traffic**: The application manifest enforces `android:networkSecurityConfig="@xml/network_security_config"`, strictly disabling unencrypted HTTP connections.
2. **System Certificate Anchors**: All connections strictly validate against trusted Android system CA trust anchors.
3. **APK Signature Scheme v2/v3**: Release artifacts are cryptographically signed using APK Signature Scheme v2/v3 to guarantee binary integrity from build to device execution.
4. **Git Commit Signing**: Commits to the public repository are signed using GPG keys.

---

## 🔍 Static & Dynamic Code Analysis

### Static Analysis
- **Android Lint**: Automated verification during builds checking for Android security vulnerabilities, permission leaks, and deprecations (`lint_applet` / Gradle `lint`).
- **Kotlin Strict Compiler**: Codebase compiles with strict type safety, zero unchecked casts, and Kotlin sealed state modeling.
- **Dependency Vulnerability Scanning**: Continuous checks for CVEs in third-party Gradle dependencies.

### Dynamic Analysis
- **Robolectric Local JVM Automated Tests**: Continuous verification of Room database queries, concurrent StateFlow mutations, and UI transitions.
- **Android StrictMode Policy**: Used in debug builds to detect unexpected disk reads or network operations on the UI thread.
- **Zero Memory Leaks**: Jetpack Compose state collection adheres to `collectAsStateWithLifecycle()` to avoid resource leakage during Android lifecycle changes.
