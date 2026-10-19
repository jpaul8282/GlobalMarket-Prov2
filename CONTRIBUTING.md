# Contributing to GlobalMarket Pro

Thank you for your interest in contributing to **GlobalMarket Pro**! We welcome contributions from the open-source community to make our multi-currency sales terminal and marketing tools better, safer, and faster.

---

## 📋 Table of Contents
1. [Code of Conduct](#code-of-conduct)
2. [Bug Reporting Process](#bug-reporting-process)
3. [Feature Requests](#feature-requests)
4. [Development & Build Setup](#development--build-setup)
5. [Automated Testing & New Functionality Policy](#automated-testing--new-functionality-policy)
6. [Secure Development Guidelines](#secure-development-guidelines)
7. [Submitting Pull Requests](#submitting-pull-requests)

---

## 📜 Code of Conduct
We are committed to providing a welcoming, inclusive, and harassment-free experience for everyone. Please be respectful, constructive, and collaborative.

---

## 🐛 Bug Reporting Process

If you find a non-security bug:
1. **Search Existing Issues**: Check existing GitHub Issues to see if the problem has already been reported.
2. **Open a Bug Report**: Use our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md) with:
   - Clear and concise summary of the issue
   - Exact steps to reproduce the behavior
   - Expected vs actual behavior
   - Android OS version / device model / screen density
   - Logcat output or stacktrace (if applicable)

*Note: For security vulnerabilities, follow the private disclosure process in [SECURITY.md](SECURITY.md).*

---

## 💡 Feature Requests
Feature suggestions are welcome! Please open an issue outlining:
- The business or user problem you want to solve
- Detailed description of the proposed feature
- Potential alternatives considered

---

## 🛠️ Development & Build Setup

### Prerequisites
- **Android Studio** (Ladybug 2024.2.1 or newer)
- **JDK 17**
- **Android SDK API 36**

### Build Commands
```bash
# Clone the repository
git clone https://github.com/jurgen-paul/GLABA-MARKETING-PRO-APP2.git
cd GLABA-MARKETING-PRO-APP2

# Compile the debug application
gradle :app:assembleDebug

# Run static code analysis
gradle :app:lintDebug
```

---

## 🧪 Automated Testing & New Functionality Policy

Every pull request that introduces new functionality **MUST** include automated tests:
1. **Unit & Database Tests**: Write local JVM Robolectric tests in `app/src/test/java/com/example/` verifying:
   - DAO database queries and room schema persistence
   - ViewModel StateFlow state mutations
   - Dynamic currency and exchange rate computations
2. **Regression Prevention**: Never submit PRs with broken tests. Run:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. **Screenshot Tests**: For visual UI changes, run Roborazzi screenshot verification:
   ```bash
   gradle :app:verifyRoborazziDebug
   ```

---

## 🛡️ Secure Development Guidelines

All contributors must adhere to secure development best practices:
- **Zero Cleartext HTTP**: Never introduce unencrypted network calls. All communications must use HTTPS (TLS 1.3).
- **No Hardcoded Secrets**: Never commit API keys, tokens, or private certificates. Inject configuration via `.env` or `BuildConfig`.
- **SQL Injection Prevention**: Use Room DAO parameter binding (`:param`), never raw concatenated SQLite strings.
- **Input Sanitization**: Validate all inputs (currency amounts, credit card numbers, promo codes) before processing.
- **Cryptographic Standards**: Use Android Keystore and platform-standard algorithms (`AES-GCM-256`, `SHA-256`).

---

## 🚀 Submitting Pull Requests

1. Fork the repo and create a feature branch (`git checkout -b feature/amazing-feature`).
2. Make your changes adhering to Clean Minimalism and M3 guidelines.
3. Verify that the build succeeds and all automated tests pass (`gradle :app:testDebugUnitTest`).
4. Commit your changes with conventional commit messages (`feat: add new currency`, `fix: sanitize checkout input`).
5. Open a Pull Request referencing any related issues.
