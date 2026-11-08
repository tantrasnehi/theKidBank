# Technical Architecture — My Little Bank

## 1. Architecture style

Native Android, offline-first, modular Clean Architecture with MVVM.

Layers:
- Presentation: Jetpack Compose screens, ViewModels, UI state.
- Domain: entities/models, repository interfaces, use cases, business rules.
- Data: Room entities/DAOs, repository implementations, mappers.
- Core: currency formatting, security, validation, dispatchers, backup utilities.

No server/backend in MVP. “API” means internal Kotlin use-case and repository contracts, not REST endpoints.

## 2. Technology choices

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room
- SQLite
- Kotlin Coroutines + Flow
- DataStore
- Android Keystore
- AndroidX Biometric is optional; not required for MVP
- JUnit, kotlinx-coroutines-test, Room in-memory tests, Compose UI tests
- Gradle Kotlin DSL
- Minimum SDK: choose a currently supported baseline after inspecting target device; default recommendation API 26+ unless repository/device constraints require otherwise.
- Target SDK: latest stable required by current Android tooling at implementation time.

Use stable dependency versions compatible with the installed Android Studio/Gradle environment. Do not blindly copy outdated versions from this spec.

## 3. Package/module organization

Single Gradle app module is acceptable for MVP; organize by feature and layer. Avoid premature multi-module Gradle complexity.

Suggested package:
com.example.mylittlebank (replace with final application ID chosen by developer)

- app/
- core/common/
- core/currency/
- core/security/
- core/ui/
- domain/model/
- domain/repository/
- domain/usecase/
- data/local/entity/
- data/local/dao/
- data/local/database/
- data/repository/
- feature/onboarding/
- feature/child/dashboard/
- feature/child/earn/
- feature/child/bank/
- feature/child/savings/
- feature/child/learning/
- feature/parent/auth/
- feature/parent/dashboard/
- feature/parent/rewards/
- feature/parent/transactions/
- feature/parent/settings/

## 4. Dependency direction

Presentation -> Domain <- Data implementation.

- ViewModels depend on use cases.
- Use cases depend on domain repository interfaces.
- Repository implementations depend on Room DAOs.
- Domain must not depend on Android framework or Room.
- Composables must not access DAOs directly.

Use constructor injection. Hilt is optional; prefer Hilt if already configured, otherwise use a small explicit dependency container for MVP. Do not introduce a complex DI framework solely for this app.

## 5. Financial ledger architecture

Accounts:
- CHILD_AVAILABLE
- CHILD_SAVINGS
- SYSTEM_REWARD_FUND
- SYSTEM_MANUAL_ADJUSTMENT

Each posted transaction has:
- UUID
- type
- description
- currency code/precision snapshot
- idempotency key
- created timestamp
- optional related entity ID
- status (POSTED or REVERSED; reversal is represented by a separate transaction)

Each ledger entry:
- transaction ID
- account ID
- signed integer minor amount
- currency snapshot or account currency reference

Invariant:
SUM(entries.amount_minor) = 0 for each transaction.

Balance:
SUM(ledger_entry.amount_minor) grouped by account.

Available and savings child accounts must never have a negative balance. Validate inside the same database transaction that writes ledger entries. SQLite transaction serialization protects concurrent writes; still ensure repository operations are transactional.

Use Room @Transaction / database.withTransaction. Add unique index on idempotency key. Validate transaction balancing in domain and test it. If practical, enforce some invariants with SQLite triggers, but do not rely only on triggers.

## 6. Reward engine

Input:
- activity type ID
- quantity
- optional description
- idempotency UUID
- submission timestamp

Processing:
- load enabled activity and effective reward rule
- validate quantity > 0
- validate optional daily limits
- calculate `rewardMinor = quantity * rewardPerUnitMinor` using checked Long arithmetic
- create activity submission
- create balanced reward ledger transaction
- commit atomically
- return transaction and updated balances

Use checked multiplication (`Math.multiplyExact`) and reject overflow.

Reward rule history:
- rules have effective_from and optional effective_to
- new rule version closes previous version and inserts new one
- submission stores applied rule ID, reward rate snapshot, quantity, total reward, and currency snapshot
- historical values never recalculate

## 7. Shopping and interest exclusions

Do not create shopping or purchase packages, tables, navigation, UI, or use cases.
Do not create interest rate, accrual, compounding, or interest transaction logic.

## 8. Currency design

Money is represented by:
- amountMinor: Long
- currencyCode: String
- decimalPlaces: Int
- symbol: String snapshot for display/history if needed

Formatting must use a shared CurrencyFormatter. Do not assume 2 decimals. Avoid floating point.
Parsing user-entered amounts should use BigDecimal or string parsing with explicit scale, then convert exactly to Long minor units. Reject excess decimal places and overflow.

Lock currency identity and decimal precision after first posted transaction. Changing a display label is allowed only if it does not alter historical meaning.

## 9. Security

- Use Android Keystore to protect backup encryption keys.
- Do not log secrets.

Local app data is private to the app sandbox. Explain backup behavior to parent. Backup should be encrypted and exported through Storage Access Framework.

## 10. Offline and lifecycle

- No internet permission required unless a dependency unexpectedly requires it; avoid unnecessary network libraries.
- All functionality uses local Room data.
- Observe database through Flow.
- Financial writes are synchronous from the user's perspective but executed off main thread.
- Survive process death; Room transaction atomicity ensures no partial writes.
- Do not rely on in-memory balances.

## 11. Backup and restore

Backup package includes:
- Database export or a consistent snapshot.
- Schema version.
- App version.
- Export timestamp.
- Currency metadata.
- SHA-256 integrity checksum.
- Encryption metadata.

Restore:
- Authenticate parent.
- Read file via SAF.
- Decrypt and validate.
- Check schema compatibility.
- Verify every transaction balances.
- Verify nonnegative child account balances.
- Confirm replacement.
- Restore atomically using staging database then replace.
- Never partially overwrite live DB.

## 12. Observability and error handling

No remote telemetry in MVP.
Use structured local debug logs without secrets.
Map domain failures to user-friendly messages:
- Invalid quantity
- Activity disabled
- Daily limit reached
- Insufficient available funds
- Insufficient savings
- Duplicate submission
- Currency configuration locked
- Storage/backup failure
- Unexpected database error

Do not expose raw SQL exceptions to child UI.
