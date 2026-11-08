# Data Model — My Little Bank

## 1. Core entities

### ChildProfile
- id: UUID
- displayName: String
- avatarKey: String?
- createdAt: Instant

### CurrencyConfig
- id: UUID
- code: String
- displayName: String
- symbol: String
- decimalPlaces: Int (0–3)
- isLocked: Boolean
- createdAt: Instant
- updatedAt: Instant

### BankAccount
- id: UUID
- accountType: AVAILABLE | SAVINGS | REWARD_FUND | MANUAL_ADJUSTMENT
- ownerProfileId: UUID? (null for system accounts)
- currencyConfigId: UUID
- createdAt: Instant
- isActive: Boolean

### LedgerTransaction
- id: UUID
- idempotencyKey: UUID (unique)
- transactionType: REWARD_CREDIT | SAVINGS_TRANSFER | SAVINGS_WITHDRAWAL | PARENT_CREDIT | PARENT_DEBIT | REVERSAL
- description: String
- status: POSTED | REVERSED
- referenceType: String?
- referenceId: UUID?
- currencyCodeSnapshot: String
- decimalPlacesSnapshot: Int
- createdAt: Instant
- reversesTransactionId: UUID? (unique nullable)

### LedgerEntry
- id: UUID
- transactionId: UUID
- accountId: UUID
- amountMinor: Long (signed)
- createdAt: Instant

### ActivityType
- id: UUID
- key: READING | MATHS | HOMEWORK | CHORES | GOOD_HABITS (extensible string key)
- displayName: String
- unitLabel: String
- description: String?
- isEnabled: Boolean
- createdAt: Instant

### RewardRule
- id: UUID
- activityTypeId: UUID
- rewardPerUnitMinor: Long
- dailyQuantityLimit: Long? (nullable means unlimited)
- dailyRewardLimitMinor: Long? (nullable means unlimited)
- effectiveFrom: Instant
- effectiveTo: Instant? (null means current)
- createdAt: Instant

### ActivitySubmission
- id: UUID
- idempotencyKey: UUID (unique)
- activityTypeId: UUID
- appliedRewardRuleId: UUID
- quantity: Long
- description: String?
- rewardPerUnitMinorSnapshot: Long
- totalRewardMinor: Long
- currencyCodeSnapshot: String
- decimalPlacesSnapshot: Int
- ledgerTransactionId: UUID (unique)
- submittedAt: Instant

### SavingsGoal
- id: UUID
- name: String
- targetAmountMinor: Long
- iconKey: String?
- status: ACTIVE | COMPLETED | ARCHIVED
- createdAt: Instant
- updatedAt: Instant

### SavingsGoalContribution (recommended for accurate goal progress)
- id: UUID
- goalId: UUID
- ledgerTransactionId: UUID
- amountMinor: Long
- direction: DEPOSIT | WITHDRAWAL
- createdAt: Instant

### LearningModule
- id: UUID
- title: String
- category: String
- sequence: Int
- contentJson: String (versioned lesson content)
- isEnabled: Boolean

### LearningProgress
- id: UUID
- moduleId: UUID
- status: NOT_STARTED | IN_PROGRESS | COMPLETED
- score: Int?
- completedAt: Instant?

### AppSetting
- key: String
- value: String
- updatedAt: Instant

## 2. Relationships

- ChildProfile 1 -> many BankAccount (child-owned accounts).
- CurrencyConfig 1 -> many BankAccount.
- LedgerTransaction 1 -> many LedgerEntry.
- BankAccount 1 -> many LedgerEntry.
- ActivityType 1 -> many RewardRule.
- ActivityType 1 -> many ActivitySubmission.
- RewardRule 1 -> many ActivitySubmission (applied rule).
- ActivitySubmission 1 -> 1 LedgerTransaction.
- SavingsGoal 1 -> many SavingsGoalContribution.
- LearningModule 1 -> 1 LearningProgress per child in single-child MVP.

## 3. Ledger examples

Reward 25 units:
- CHILD_AVAILABLE: +2500 minor units (for 2 decimal currency)
- SYSTEM_REWARD_FUND: -2500 minor units

Transfer 10 units to savings:
- CHILD_AVAILABLE: -1000
- CHILD_SAVINGS: +1000

Withdraw 4 units:
- CHILD_AVAILABLE: +400
- CHILD_SAVINGS: -400

Every transaction sums to zero.

## 4. Balance queries

Available:
`SUM(amount_minor)` for AVAILABLE account entries.

Savings:
`SUM(amount_minor)` for SAVINGS account entries.

Lifetime earned:
Sum positive AVAILABLE entries whose transaction type is REWARD_CREDIT.

Lifetime saved:
Sum positive SAVINGS entries whose transaction type is SAVINGS_TRANSFER.

Lifetime withdrawn:
Sum negative SAVINGS entries whose transaction type is SAVINGS_WITHDRAWAL, absolute value.

## 5. Important indexes and constraints

- Unique ledger transaction idempotency key.
- Unique activity submission idempotency key.
- Unique activity submission ledger transaction ID.
- Unique reversal reference.
- Index ledger_entry(transaction_id).
- Index ledger_entry(account_id, created_at).
- Index activity_submission(activity_type_id, submitted_at).
- Index reward_rule(activity_type_id, effective_from, effective_to).
- Foreign keys enabled.
- Check decimalPlaces between 0 and 3.
- Check quantities > 0.
- Check target amount > 0.
- Check reward amounts >= 0.
- Application-level transaction validation must enforce ledger sum = 0 and nonnegative child balances.

Note: SQLite CHECK constraints cannot enforce aggregate sums across child rows. Validate ledger balance in the domain/repository transaction and cover it with tests.
