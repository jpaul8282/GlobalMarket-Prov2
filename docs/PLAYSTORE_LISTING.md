# Google Play Store Listing & Submission Guide

This document contains the finalized, policy-compliant store listing metadata, copy, declarations, and asset specifications for publishing **GlobalMarket Pro** on the Google Play Console.

---

## 📋 Store Listing Details

### App Title (Max 30 characters)
```text
GlobalMarket Pro
```
*Policy check: 16 characters. No promotional buzzwords ("Free", "Best", "#1"), no emojis, no ALL CAPS.*

### Short Description (Max 80 characters)
```text
Global multi-currency point of sale terminal, sales ledger & marketing hub.
```
*Policy check: 75 characters. Concise, descriptive, zero promotional hype.*

### Full Description (Max 4,000 characters)
```text
GlobalMarket Pro is an all-in-one point-of-sale (POS) terminal, inventory manager, and marketing growth engine designed for international merchants, retail operators, and digital entrepreneurs.

Accept payments with confidence, calculate real-time foreign exchange conversions across worldwide currencies, manage local inventory, and share viral discount campaigns—all with privacy-focused, on-device local persistence.

KEY FEATURES:

💳 MULTI-CURRENCY POS TERMINAL
• Real-time exchange rate engine supporting USD, EUR, GBP, JPY, CNY, CAD, AUD, CHF, SGD, INR, BRL, and MXN.
• Dual-currency instant price conversion with live markup and fee savings calculations.
• Interactive payment interface with card brand detection, contactless tap simulation, and localized payment rails (SEPA, iDEAL, Pix, UPI, Konbini).

📦 SMART LOCAL INVENTORY & CATALOG
• High-performance on-device product catalog powered by Room SQLite.
• Real-time stock quantity decrement upon sale completion with automatic stock replenishment alerts.
• Barcode (UPC-A, EAN-13, Code 128) and QR code scanning powered by Google ML Kit.
• Fast search by product name, SKU, category, or barcode.

📢 VIRAL MARKETING & SHARE HUB
• Pre-built campaign presets: Storefront Checkout (15% off), Partner Referral (25% commission), and Flash Promo.
• Generate dynamic tracking URLs and QR payment codes instantly.
• Native Android share sheet integration for 1-tap dispatch to WhatsApp, Telegram, Email, and SMS with multi-language copy.

📊 SALES HISTORY & ANALYTICS
• Detailed sales transaction history with full receipt breakdown (subtotal, VAT/tax, discounts, payment rail).
• Regional revenue distribution and order volume tracking across global markets.
• Export sales ledger to RFC 4180 compliant CSV for accounting software (Excel, QuickBooks, Xero).
• Instant search and filtering by currency, date range, or payment status.

🔒 PRIVACY-FIRST & OFFLINE READY
• 100% on-device local database persistence—no mandatory cloud account required.
• Camera frames are processed strictly in-memory by ML Kit and never stored or uploaded.
• Zero third-party advertising trackers or data harvesting.
• Granular control to edit, delete, or clear your entire transaction ledger at any time.

🌍 LOCALIZED FOR GLOBAL COMMERCE
• Full multi-language interface available in English, Spanish, French, German, and Chinese.
• Modern Material Design 3 UI with adaptive layouts for phones, foldables, and tablets.
```

---

## 🏷️ Category & Store Settings
- **Application Type:** App
- **Category:** Business (Secondary: Finance / Productivity)
- **Tags:** Point of Sale, POS Terminal, Multi Currency, Cash Register, Inventory Management, Small Business
- **Target Audience:** 18+ (Retailers, Merchants, Small Businesses)
- **Content Rating:** Everyone (PEGI 3 / ESRB Everyone)
- **Contains Ads:** No

---

## 🔒 Data Safety Declarations (Google Play Console)
- **Data Collection:**
  - *Personal Info:* Stored locally on device; NOT collected or transmitted to developers or third parties.
  - *Financial Info:* Local transaction records stored in Room SQLite; NOT collected or transmitted.
  - *Photos / Videos / Files:* Zero access to media files or external storage.
- **Device Camera:**
  - Used ephemerally for on-device barcode and QR scanning.
  - Zero camera frames or photos are saved or uploaded.
- **Data Sharing:**
  - No user data is shared with third parties.
- **Security Practices:**
  - Data encrypted in transit (TLS 1.3 for exchange rate requests).
  - Users can request deletion of all data directly in-app ("Clear Ledger").

---

## 🎨 Store Graphic Assets Checklist

| Asset | Dimensions | Format | Status | Location in Repo |
|---|---|---|---|---|
| **App Icon** | 512 × 512 px | PNG (32-bit with alpha) | ✅ Ready | `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` |
| **Feature Graphic** | 1024 × 500 px | JPG / PNG (no alpha) | ✅ Ready | `app/src/main/res/drawable/playstore_feature_banner_1790614204610.jpg` |
| **Phone Screenshot 1** | 1080 × 1920 px | JPG / PNG | ✅ Ready | `app/src/main/res/drawable/pos_terminal_checkout_1790614216753.jpg` |
| **Phone Screenshot 2** | 1080 × 1920 px | JPG / PNG | ✅ Ready | `app/src/main/res/drawable/inventory_catalog_manager_1790614228876.jpg` |
| **Phone Screenshot 3** | 1080 × 1920 px | JPG / PNG | ✅ Ready | `app/src/main/res/drawable/marketing_campaign_hub_1790614241500.jpg` |
| **Phone Screenshot 4** | 1080 × 1920 px | JPG / PNG | ✅ Ready | `app/src/main/res/drawable/analytics_sales_dashboard_1790614253448.jpg` |

---

## ⚙️ Technical Release Verification
- **Application ID:** `com.aistudio.globalmarketpro.mfxvqz`
- **Target SDK:** 36 (Android 16)
- **Minimum SDK:** 24 (Android 7.0 Nougat)
- **Build Output:** Android App Bundle (`.aab`) supported via `gradle :app:bundleRelease`
- **Proguard / R8:** Enabled with optimized rules in `app/proguard-rules.pro`
- **Permissions Declared:** Least privilege (`INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA` with `required="false"`)
- **Privacy Policy URL:** Hosted in repository at [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md)
