# My Little Bank — Codex Implementation Handoff

This bundle is the implementation source of truth for the first release of **My Little Bank**, an offline-first Android tablet app that teaches children financial responsibility through earning, saving, and learning.

## Product decisions (locked)

- App name: My Little Bank
- Platform: Android tablet
- Native stack: Kotlin + Jetpack Compose
- Architecture: Clean Architecture + MVVM
- Persistence: Room / SQLite
- Preferences: DataStore
- Connectivity: Offline-first; no backend server or account system in MVP
- Balance unit: Whole-number points only; no currency or real-money integration
- Reward credit: Automatic immediately after valid child submission
- Verification: Child self-submits; no mandatory parent approval workflow
- Activities: Reading, maths, homework, chores, good habits
- Shopping: **NOT REQUIRED. Do not implement shopping, products, carts, purchases, or purchase debits.**
- Interest: **NOT REQUIRED. Do not implement interest calculations.**
- Parent area: settings and reporting; PIN protection is deferred by the owner
- Daily limits: local device calendar and clock
- Savings: Savings goals and transfers between available and savings accounts
- Learning: Financial literacy lessons and maths activities. Learning scores do not automatically create bank credits.

## How to use this bundle with Codex

1. Open the Android repository in Codex.
2. Give Codex `codex/CODEX_IMPLEMENTATION_PROMPT.md` as the implementation instruction.
3. Ask Codex to inspect the repository before changing anything.
4. Implement in the phases in `docs/IMPLEMENTATION_PLAN.md`.
5. Require tests and a build after each phase.
6. Treat the current implementation decisions in this README as authoritative wherever older documents conflict.

## Current implementation decisions (2026-11-08)

- Build a new Android project in this folder; there is no pre-existing source repository.
- Design for children through approximately age 12 and adapt across tablet sizes/orientations.
- Points are the only balance unit. Seed the example reward values as editable parent defaults; parents choose the points per activity.
- Do not implement a PIN in the initial release. Parent tools are available without PIN protection for now.
- Apply daily limits using the device's local date and clock.
- Savings goal progress counts deposits associated with that goal; general withdrawals do not rewrite goal contribution history.

## Android project status

The initial Android project scaffold is in `app/`. It currently includes child onboarding, tablet-aware navigation, activity earning, editable reward rates and local-day caps, passbook/balances, savings transfers, and starter Money School lessons. This is the first implementation slice; goals, parent adjustments/reversals, backup/restore, and full lesson progress are still to be built.

## Bundle contents

- `docs/PRODUCT_REQUIREMENTS.md` — product scope, roles, requirements, exclusions
- `docs/UI_SPECIFICATION.md` — screen-by-screen UI behavior and navigation
- `docs/ARCHITECTURE.md` — app architecture, modules, security, offline behavior
- `docs/DATA_MODEL.md` — entities, relationships, ledger invariants
- `docs/IMPLEMENTATION_PLAN.md` — ordered development plan and tickets
- `docs/ACCEPTANCE_TESTS.md` — functional, financial, security, and offline acceptance tests
- `database/schema.sql` — SQLite reference schema for Room implementation
- `contracts/DOMAIN_CONTRACTS.md` — use-case and repository contracts
- `codex/CODEX_IMPLEMENTATION_PROMPT.md` — ready-to-paste Codex prompt

## Non-negotiable financial integrity rules

- Store points as whole-number integers (`Long` in Kotlin), never `Float` or `Double`.
- Each posted financial transaction must balance: sum of ledger entries for that transaction is zero.
- Do not directly update a stored balance. Derive balances from ledger entries or use a transactionally maintained cache that is reconciled against the ledger.
- Every operation that changes money must run in one SQLite database transaction.
- Prevent duplicate posting with a unique idempotency key.
- Never edit or delete posted ledger entries. Correct mistakes using reversal transactions.
- Reward rule changes apply only to future submissions.
- No shopping module, interest module, cloud backend, or real-money integration.
