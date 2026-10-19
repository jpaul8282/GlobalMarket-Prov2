# Cryptographic Mechanisms, US Export Controls & Security Controls

## 1. Cryptographic Overview
GlobalMarket Pro implements robust, open, platform-standard cryptography to safeguard user transactions, configuration data, and network traffic.

### Algorithms & Platform Mechanisms
- **Transport Security**: Transport Layer Security (TLS 1.3 / TLS 1.2 minimum) via Android's default Conscrypt/BoringSSL implementation.
- **Data At Rest Encryption**: Supported via Android Keystore system with AES-256 in Galois/Counter Mode (AES-GCM) with random 96-bit initialization vectors (IV) for sensitive secrets.
- **Cryptographic Hashing**: SHA-256 and SHA-512 for data integrity and APK verification.
- **Random Number Generation**: `java.security.SecureRandom` for all token, transaction ID, and cryptographic nonce generation.

---

## 2. United States Export Controls & Open Source Encryption (EAR § 742.15)

### Export Administration Regulations (EAR) Notice
Under U.S. Export Administration Regulations (15 C.F.R. § 742.15(b)), open source software containing or utilizing encryption functionality is classified under **ECCN 5D002** or **ECCN 5D992**, but is eligible for license exception / unrestricted export when made publicly available under FLOSS conditions.

### Project Compliance Attributes:
1. **Public Availability**: The source code is publicly accessible without fee or password restriction at:  
   `https://github.com/jurgen-paul/GLABA-MARKETING-PRO-APP2.git`
2. **Standard Cryptography**: The software does NOT develop novel or proprietary encryption algorithms. It relies entirely on standard Android platform cryptographic providers (Android JCA / Conscrypt / TLS stack).
3. **Export Notification**: Open source software making use of encryption functions must notify the Bureau of Industry and Security (BIS) and the ENC Encryption Request Coordinator via email to `crypt@bis.doc.gov` and `enc@nsa.gov` specifying the URL where the source code is publicly available. This notice is provided in compliance with EAR Section 742.15(b).

---

## 3. Defense Against Man-in-the-Middle (MITM) Attacks

GlobalMarket Pro enforces strict controls against MITM attacks:
1. **Disabled Cleartext Traffic**:
   The application specifies `android:networkSecurityConfig="@xml/network_security_config"` in `AndroidManifest.xml`.
   ```xml
   <network-security-config>
       <base-config cleartextTrafficPermitted="false">
           <trust-anchors>
               <certificates src="system" />
           </trust-anchors>
       </base-config>
   </network-security-config>
   ```
   Any unencrypted HTTP traffic is rejected at the Android socket layer.
2. **System CA Validation**:
   Only verified, system-trusted Certificate Authorities are permitted. User-installed certificates or forged proxy certificates are blocked by default.
3. **Signed APK Distribution**:
   Release APKs and App Bundles are signed using APK Signature Scheme v2 and v3 to ensure binary integrity and origin authenticity.

---

## 4. Vulnerability Mitigation & Threat Modeling

| Threat Category | Potential Attack Vector | Mitigation in GlobalMarket Pro |
| :--- | :--- | :--- |
| **Data in Transit** | Wiretapping, MITM proxying | Strict TLS 1.3 enforcement, disabled cleartext |
| **SQL Injection** | Malicious inputs into SQLite | Room DAO parameterized queries (`:id`, `:amount`) |
| **Secret Exposure** | Hardcoded API keys in Git | BuildConfig injection via `.env.example` / Secrets panel |
| **Memory Corruption** | Native buffer overflows | Pure Kotlin memory-managed codebase, no unsafe JNI |
| **Dependency Risks** | Vulnerable third-party libraries | Continuous dependency scanning and minimal attack surface |
