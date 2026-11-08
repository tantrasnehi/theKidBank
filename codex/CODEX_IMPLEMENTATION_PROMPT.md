# Codex Master Implementation Prompt — My Little Bank

You are implementing **My Little Bank**, a native Android tablet app. Treat the documents in this handoff bundle as the source of truth.

## First: inspect, do not assume

1. Inspect the entire existing repository structure.
2. Inspect Gradle files, Kotlin/Android versions, package ID, SDK configuration, dependencies, existing screens, architecture, and tests.
3. Identify whether this is a new or existing project.
4. Do not overwrite working code or perform a large refactor without explaining why.
5. Provide a concise repository assessment and a proposed phase plan before implementation.

## Locked product scope

Build an offline-first Android app using Kotlin, Jetpack Compose, MVVM/Clean Architecture, Room/SQLite, Coroutines/Flow, and DataStore.

Child activities:
- Reading
- Maths
- Homework
- Chores
- Good habits

Child can submit activity completions. Valid submissions are credited automatically and immediately. There is no mandatory parent approval queue.

Child can:
- View dashboard and balances.
- Submit activities and earn virtual money.
- View bank/passbook and transaction details.
- Create savings goals.
- Transfer available money into savings.
- Withdraw savings into available balance.
- Learn financial concepts and complete quizzes.

Parent can:
- Access parent settings from the settings button; PIN protection is deferred.
- Configure currency and reward rules.
- Enable/disable activities and set optional daily limits.
- View transactions and reports.
- Make reasoned manual adjustments.
- Reverse erroneous transactions.
- Export and restore encrypted backups.

Currency is configurable; default INR (₹). No real money.

## Explicitly DO NOT implement

- Shopping screen or any shopping functionality.
- Product catalog, cart, purchase, purchase fulfillment.
- Interest, compounding, or interest accrual.
- Real bank/payment/UPI/card integration.
- Cloud backend, remote REST API, online account, or multi-device sync.
- Mandatory parent approval for routine reward submissions.
- Ads, social features, or external links.

Do not reintroduce shopping because it appeared in older planning material. The latest user decision explicitly removed it.

## Financial correctness is mandatory

- Use integer minor units (`Long`); never Float/Double for money.
- Use configurable decimal precision 0–3.
- Use a shared currency formatter; no hardcoded ₹ in feature screens.
- Use a double-entry ledger: every transaction's entries sum to zero.
- Derive balances from ledger entries or maintain a transactionally updated cache with reconciliation.
- All financial writes must run in one Room/SQLite transaction.
- Available and savings balances must never be negative.
- Use UUID idempotency keys and unique database constraints.
- A retry with same key and same payload must not duplicate a reward or transfer.
- Posted ledger entries are immutable. Corrections use reversal transactions.
- Reward rule changes create new effective-dated versions; historical submissions retain snapshots.
- Lock currency identity/precision after first posted transaction; migration is out of scope.
- Use checked arithmetic to prevent overflow.

## Implementation approach

Implement in vertical slices following `docs/IMPLEMENTATION_PLAN.md`:
1. Inspect repo and foundation.
2. Room schema, accounts, ledger, balance queries.
3. Reward engine and child activity flow.
4. Bank/passbook screens.
5. Parent console and configuration.
6. Savings goals/transfers.
7. Money School.
8. Backup/restore, hardening, release.

Read:
- `docs/PRODUCT_REQUIREMENTS.md`
- `docs/UI_SPECIFICATION.md`
- `docs/ARCHITECTURE.md`
- `docs/DATA_MODEL.md`
- `contracts/DOMAIN_CONTRACTS.md`
- `docs/ACCEPTANCE_TESTS.md`

## Coding expectations

- Use domain use cases; Composables must not access DAOs.
- ViewModels expose immutable StateFlow UI state.
- Add unit tests for domain logic and database integration tests for ledger atomicity.
- Use Room migrations, not destructive migration in production.
- Handle loading, empty, success, and error states.
- Use adaptive layouts for tablet portrait and landscape.
- Keep touch targets at least 48dp.
- Use Android Keystore for encryption key protection.
- Backup/restore must validate a staging copy before replacing live data.
- Avoid unnecessary dependencies and network permissions.
- Do not add analytics or telemetry SDKs.

## Work protocol

For each phase:
1. State what you plan to change.
2. Implement only that phase.
3. Run tests, lint (if available), and `./gradlew assembleDebug`.
4. Fix failures before proceeding.
5. Summarize files changed, behavior implemented, test results, build result, and remaining issues.
6. Do not claim success unless commands actually pass.

If a requirement is ambiguous, prefer the simplest safe offline implementation and document the assumption. Do not silently add features.

## Definition of done for MVP

- Fresh install and onboarding work.
- Currency can be configured before first transaction.
- Five activity types can be submitted.
- Rewards are calculated and credited automatically.
- Ledger balances and idempotency invariants hold.
- Child can view balance and transaction history.
- Parent can configure reward rules and limits.
- Savings transfers work.
- Money School lessons work without affecting bank balance.
- App works offline.
- Backup/restore is validated.
- Tests pass and debug APK builds.
- No shopping or interest functionality exists.
