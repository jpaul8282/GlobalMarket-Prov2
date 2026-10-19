# GlobalMarket Pro Architecture Documentation

## 🏛️ System Overview

GlobalMarket Pro is engineered with **Modern Android Architecture** principles utilizing **Jetpack Compose**, **Kotlin Coroutines / StateFlow**, and the **Room Database** persistence layer.

```
┌─────────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                       │
│  MainActivity (Edge-to-Edge, Clean Minimalism Theme)        │
│  ├─ GlobalMarketDashboard                                   │
│  ├─ AnalyticsView                                           │
│  ├─ AudienceView                                            │
│  ├─ EnhancedAccountSettingsView                             │
│  └─ MarketingShareHubModal                                  │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow (UI State)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 GlobalMarketViewModel                       │
│  ├─ Multi-Currency Exchange Engine (USD, EUR, GBP, JPY, CNY)│
│  ├─ Marketing Campaign Link & Content Generator             │
│  ├─ Multi-Language Localization State                       │
│  └─ Payment Simulator & Account Settings State              │
└──────────────────────────────┬──────────────────────────────┘
                               │ Coroutine Dispatches
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               TransactionRepository (Data Layer)            │
│  ├─ getTransactions(): Flow<List<TransactionEntity>>        │
│  ├─ insertTransaction(entity)                               │
│  └─ clearAllTransactions()                                  │
└──────────────────────────────┬──────────────────────────────┘
                               │ SQLite SQL Queries
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  Android Room Database                      │
│  AppDatabase (SQLite Local Persistent Storage)              │
│  └─ TransactionDao / TransactionEntity                      │
└─────────────────────────────────────────────────────────────┘
```

## 📐 Key Design Patterns

1. **Unidirectional Data Flow (UDF)**:
   - UI emits events (e.g., `onCurrencyChange`, `onShareCampaign`, `onProcessPayment`).
   - `ViewModel` updates mutable state flows internally and exposes immutable `StateFlow` to composables.
   - Jetpack Compose reacts with lifecycle-safe rendering (`collectAsStateWithLifecycle()`).

2. **Clean Minimalism Design System**:
   - Palette: Deep Indigo `#4F46E5`, Clean Whites/Slates `#F8FAFC`, Muted Slate `#64748B`, Subtle Borders `#E2E8F0`.
   - Geometry: 16–24dp rounded cards, generous 8dp-grid padding.
   - Accessibility: 48dp minimum interactive component targets, TalkBack content descriptions.

3. **Room Database Persistence**:
   - Entities: `TransactionEntity` with auto-generating primary keys, timestamps, customer name, base amount, converted amount, and currency ISO codes.
   - DAOs: Fully asynchronous queries returning Kotlin `Flow` for instant UI updates.
