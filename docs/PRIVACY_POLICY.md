# Privacy Policy for GlobalMarket Pro

**Effective Date:** September 29, 2026  
**Last Updated:** September 29, 2026

## 1. Overview
**GlobalMarket Pro** ("we," "our," or "the application") is committed to protecting your privacy. This Privacy Policy details our practices concerning data collection, processing, local persistence, and device permissions when using our mobile application on Android devices.

GlobalMarket Pro is architected with a **privacy-first, local-first philosophy**. We do not sell, rent, or monetize your personal data.

---

## 2. Information We Collect and How We Use It

### A. Local Transaction and Inventory Data
- **What is stored:** Transaction amounts, timestamps, chosen currencies, customer names entered for receipts, product inventory catalogs, and transaction logs.
- **Where it is stored:** Exclusively on your local device within the sandboxed Android SQLite database (via Android Jetpack Room).
- **Network Transmission:** Local transaction data is **never transmitted** to external third-party tracking servers or cloud analytics providers.

### B. Device Camera Usage
- **Permission:** `android.permission.CAMERA`
- **Purpose:** Scanning product barcodes (UPC-A, EAN-13, Code 128) and payment QR codes in real-time.
- **Processing:** Image analysis is executed strictly **on-device** using the Google ML Kit Barcode Scanning SDK. Video feeds and camera frames are analyzed in-memory and are **never recorded, photographed, stored, or uploaded** to external servers.
- **Hardware Requirement:** Marked as optional (`android:required="false"`), allowing devices without cameras to install and utilize all non-camera terminal features.

### C. Network Access
- **Permission:** `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`
- **Purpose:** Fetching real-time foreign exchange (FX) rates from public currency rate providers to support accurate multi-currency point-of-sale calculations.
- **Data Sent:** Only currency pairs (e.g., USD to EUR); no device identifiers, user identifiers, or geographic coordinates are attached.

---

## 3. Data Safety & Security
- **Local Storage Isolation:** Data stored in Room SQLite is protected by the Android application sandbox.
- **No Third-Party Advertising:** The application contains zero third-party advertising SDKs, zero ad trackers, and zero behavioral tracking tools.
- **Encrypted Communication:** All external exchange rate lookups are transmitted over TLS 1.3 / HTTPS.

---

## 4. User Control & Data Deletion
- **Granular Deletion:** You can delete individual transaction records or product inventory items at any time.
- **Bulk Clear:** Use the **Clear Ledger** action within the Sales History screen to permanently wipe all stored transaction history.
- **App Uninstall:** Uninstalling the application automatically and permanently deletes all locally stored Room database files.

---

## 5. Google Play Policy Compliance
- **Target SDK:** Android 16 (API Level 36).
- **Least-Privilege Permissions:** We only request permissions essential for the user-invoked features.
- **No Broad Storage Access:** We do not request `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`. File sharing (such as CSV sales reports) utilizes the secure Android `FileProvider` architecture.
- **No Dynamic Code Loading (DCL):** All executable code is bundled within the signed application package.

---

## 6. Contact Us
If you have any questions or inquiries regarding this Privacy Policy:
- **Developer / Maintainer:** Jurgen-Paul Westerveld
- **Email:** westerveldjp@gmail.com
- **Repository:** [https://github.com/jurgen-paul/GLABA-MARKETING-PRO-APP2](https://github.com/jurgen-paul/GLABA-MARKETING-PRO-APP2)
