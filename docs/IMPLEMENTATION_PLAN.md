# Implementation Plan and Codex Work Breakdown

## Development rules

- Inspect the existing repository before editing.
- If repository already has architecture/conventions, preserve them unless they conflict with requirements.
- Implement in small vertical slices.
- After each milestone run unit tests, lint/static checks, and assemble the debug APK.
- Do not implement out-of-scope features.
- Do not fabricate a successful build; report exact command and result.
- Keep dependencies current and compatible with installed toolchain.
- Add migrations for every schema change after first release.
- Never commit secrets or signing keys.

## Phase 0 — Repository reconnaissance
Tasks:
- Inspect project files, Gradle, SDK settings, package/application ID.
- Identify existing code and tests.
- Report proposed changes before large refactors.
Deliverable: repository assessment and implementation checklist.

## Phase 1 — App foundation
Tasks:
- Configure Compose Material 3 theme.
- Implement adaptive tablet navigation.
- Implement onboarding and child profile.
- Add Room database, migrations framework, DAOs.
- Seed activity types, default INR currency, example reward rules.
Acceptance: app launches, onboarding completes, app restarts with data retained.

## Phase 2 — Ledger and banking core
Tasks:
- Implement account initialization.
- Implement ledger transaction and entry persistence.
- Implement balance calculation.
- Implement balanced transaction validator.
- Implement reward credit posting.
- Implement savings transfer and withdrawal.
- Implement reversal and parent adjustment.
- Add idempotency.
Acceptance: all ledger tests pass; balances never negative; transactions balance.

## Phase 3 — Child Earn and Bank
Tasks:
- Build child dashboard.
- Build activity selection.
- Build activity-specific submission forms.
- Build reward preview and confirmation.
- Build success screen.
- Build My Bank passbook and filters.
- Build transaction details.
Acceptance: child can submit all five activities and see immediate credits.

## Phase 4 — Parent console
Tasks:
- Parent authentication.
- Parent dashboard.
- Reward rule versioning.
- Activity enable/disable and daily limits.
- Currency configuration and lock after first transaction.
- Transaction history, manual adjustment, reversal.
Acceptance: parent can configure future rewards without changing history.

## Phase 5 — Savings and Money School
Tasks:
- Savings goals and progress.
- Goal contribution tracking.
- Money School content model.
- Initial lessons and quizzes.
- Learning progress.
Acceptance: deposits/withdrawals are balanced and learning does not post money.

## Phase 6 — Backup, hardening, release
Tasks:
- Encrypted backup export.
- Validated restore through staging database.
- Accessibility review.
- Tablet layout testing.
- Process death and offline testing.
- Database integrity diagnostics.
- Signed release APK instructions.
Acceptance: all release acceptance tests pass.

## Suggested tickets

| ID | Ticket | Priority |
|---|---|---|
| MLB-001 | Inspect repo and toolchain | P0 |
| MLB-002 | Set up theme and adaptive navigation | P0 |
| MLB-003 | Implement onboarding/profile | P0 |
| MLB-005 | Create Room schema and DAOs | P0 |
| MLB-006 | Seed currency/activity/rules/accounts | P0 |
| MLB-007 | Implement ledger posting engine | P0 |
| MLB-008 | Implement balance queries | P0 |
| MLB-009 | Implement reward rule versioning | P0 |
| MLB-010 | Implement activity submission use case | P0 |
| MLB-011 | Implement daily limit enforcement | P1 |
| MLB-012 | Build child dashboard | P0 |
| MLB-013 | Build activity selection and forms | P0 |
| MLB-014 | Build reward success screen | P0 |
| MLB-015 | Build passbook and transaction details | P0 |
| MLB-016 | Build parent dashboard | P1 |
| MLB-017 | Build reward/activity configuration | P1 |
| MLB-018 | Implement currency settings | P1 |
| MLB-019 | Implement parent adjustments/reversals | P1 |
| MLB-020 | Implement savings transfers | P1 |
| MLB-021 | Implement savings goals | P1 |
| MLB-022 | Implement Money School | P2 |
| MLB-023 | Implement backup/restore | P1 |
| MLB-024 | End-to-end and tablet testing | P0 |

## Build gates

At the end of each phase:
- Run `./gradlew test`
- Run `./gradlew lint` if configured
- Run `./gradlew assembleDebug`
- Report failures and do not proceed with known broken code.
- Add or update tests for every new domain behavior.
