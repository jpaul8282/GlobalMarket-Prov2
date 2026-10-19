# Changelog

All notable changes to the **GlobalMarket Pro** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-09-14

### Added
- **Marketing Campaign Share Hub**: Interactive sharing modal with dynamic campaign presets (*Storefront Checkout*, *Merchant Partner*, *Flash Deal*), custom discount promo codes (`GLOBAL15`, `PARTNER25`, `SUMMER2026`), and multi-language promotional copy.
- **Native Android Share Integration**: One-click sharing via system share sheet (`Intent.ACTION_SEND`), plus direct channel dispatch for SMS, Email, Chat, and web preview.
- **Enhanced Account & Security Settings**: Full merchant profile customization, organization credentials (`org_6RsNhnGbSQ3s7rFPAoYfQjA`), hardware 2FA toggle, auto-FX settlement options, and notification controls.
- **OpenSSF Best Practices Compliance**: Comprehensive documentation, FLOSS MIT licensing, automated test suite, security disclosure policies, and MITM defense via strict Network Security Configuration.
- **Strict HTTPS Network Security Config**: Prevention of cleartext traffic across all Android endpoints to defend against MITM attacks.
- **5 High-Fidelity Content Graphics**: Generated visual assets for dashboard hero, payment terminal, marketing share hub, sales analytics, and OpenSSF security badge.

### Changed
- Refactored UI to **Clean Minimalism** theme (high-contrast neutrals, crisp indigo accents `#4F46E5`, subtle borders, 16–24dp rounded geometry).
- Migrated transaction persistence to robust **Room Database** (SQLite) with reactive Kotlin `Flow` streaming.
- Replaced all deprecated vector icons with modern `Icons.AutoMirrored` equivalents.
- Bumped `versionCode` to `2` and `versionName` to `"2.0.0"`.

### Security
- Added explicit `network_security_config.xml` with `cleartextTrafficPermitted="false"`.
- Documented US Export Controls (EAR Section 742.15) compliance and cryptographic assessment.
- Hardened APK signing configuration and dependency audit.

---

## [1.0.0] - 2026-08-15

### Added
- Initial release of GlobalMarket Pro sales terminal and localized payment simulator.
- Real-time exchange rate engine supporting USD, EUR, GBP, JPY, and CNY.
- 5-language localization support (English, Español, Français, Deutsch, 中文).
- Credit card checkout simulation with Stripe-compatible UI state management.
- Offline transaction logging and sales metrics.
