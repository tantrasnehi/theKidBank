# Product Requirements — My Little Bank

## 1. Product vision

My Little Bank is a child-friendly virtual banking and financial education application for an Android tablet. Children submit activities to earn virtual money, see their account balance, move money into savings goals, review their transaction history, and learn basic financial concepts.

The app should teach:
- Money is earned through effort.
- Money accumulates over time.
- A bank balance changes through credits, debits, and transfers.
- Saving means reserving money for a future goal.
- Addition, subtraction, multiplication, and division can be applied to money.

## 2. Users and permissions

### Child
Can:
- View the child dashboard and account balances.
- Submit activity completions.
- See the reward calculation before submitting.
- Receive automatic reward credit after a valid submission.
- View their own transaction history and transaction details.
- Create savings goals.
- Transfer available money into savings.
- Withdraw money from savings to available balance.
- Access financial literacy lessons and quizzes.

Cannot:
- Change reward rules or currency settings.
- Edit or delete posted financial transactions.
- Access parent administration.
- Create shopping items or make purchases (shopping is out of scope).

### Parent
Can:
- Enter the parent area from the settings button; PIN protection is deferred.
- Configure currency before the first posted transaction.
- Configure reward rules and activity availability.
- Configure optional per-activity daily quantity/reward limits.
- View all activity submissions and financial transactions.
- View dashboard summaries.
- Make manual adjustments using reasoned, auditable transactions.
- Reverse an erroneous posted transaction.
- Manage app settings and backups.

Parent approval is not part of the normal activity flow. Child submissions are automatically credited after validation.

## 3. Activity and reward rules

Initial activity types:
1. Reading — quantity measured in pages.
2. Maths — quantity measured in correct answers.
3. Homework — quantity measured in completed tasks.
4. Chores — quantity measured in completed chores.
5. Good habits — quantity measured in completed habit instances.

Reward values are parent-configurable. Example seed values (editable):
- Reading: 5 currency units per page.
- Maths: 2 currency units per correct answer.
- Homework: 10 currency units per task.
- Chores: 10 currency units per task.
- Good habits: 5 currency units per completion.

These are examples, not hardcoded business rules.

Submission flow:
1. Child chooses an enabled activity.
2. Child enters required quantity/details.
3. App loads the currently active reward rule.
4. App calculates the reward using integer minor units.
5. App displays a confirmation summary.
6. Child confirms submission.
7. App validates quantity, rule, daily cap, and idempotency.
8. In one Room transaction, save the submission, create a posted ledger transaction, and create balanced ledger entries.
9. Show success state and updated balance.

No approval queue. A successful valid submission is credited immediately.

### Duplicate handling
- Every submission attempt has a generated UUID idempotency key.
- Replaying the same submission request must not create a second credit.
- Do not automatically reject two separate submissions merely because activity, quantity, and date are identical. They may represent two genuine sessions.
- Daily limits are the control against excessive claims; parent can configure them.

## 4. Banking model

Two child-owned accounts:
- AVAILABLE — money that can be used for future choices (no shopping feature in MVP).
- SAVINGS — money reserved toward savings goals.

System clearing/funding accounts:
- REWARD_FUND — counterparty for earned rewards.
- MANUAL_ADJUSTMENT — counterparty for parent adjustments.

Every financial transaction has at least two ledger entries and balances to zero.

Examples:
- Reward 25: AVAILABLE +25, REWARD_FUND -25.
- Save 10: AVAILABLE -10, SAVINGS +10.
- Withdraw 10: AVAILABLE +10, SAVINGS -10.
- Parent credit 5: AVAILABLE +5, MANUAL_ADJUSTMENT -5.
- Parent debit 5: AVAILABLE -5, MANUAL_ADJUSTMENT +5.

The child-facing available balance must never become negative. Savings balance must never become negative.

## 5. Savings goals

Child can:
- Create a goal with name, optional image/icon, and target amount.
- See total saved amount and progress.
- Deposit from AVAILABLE to SAVINGS and optionally associate the deposit with a goal.
- Withdraw from SAVINGS to AVAILABLE.
- See goal status: ACTIVE, COMPLETED, ARCHIVED.

No interest, compounding, or time-based yield is included.

MVP simplification: savings goals are labels/targets over the single SAVINGS account. Do not create separate money accounts per goal. If goal-specific allocations are implemented, maintain an allocation table and ensure total allocations never exceed savings balance. Otherwise show goal progress based on deposits explicitly associated with that goal.

## 6. Currency

Currency is configurable by the parent during onboarding or settings before the first financial transaction:
- display name (e.g. Indian Rupee, Star Coin)
- symbol (e.g. ₹, ★)
- code (e.g. INR, CUSTOM)
- decimal places (0–3; default 2)

Store all amounts as integer minor units. Example: ₹5.00 is 500 when decimal places = 2. For a zero-decimal currency, 5 units is stored as 5.

After the first posted financial transaction, lock currency identity and precision. Parent may still edit display label only if it does not change the meaning of existing values. Currency migration is out of scope.

Do not use hardcoded `₹` in UI strings. All money display must go through a shared CurrencyFormatter.

## 7. Money School

Financial learning module topics:
- What money is; earning; bank balance; credit and debit.
- Addition and subtraction using money.
- Multiplication and division (e.g. pages × reward per page).
- Needs versus wants.
- Saving and simple budgeting.
- Comparing amounts and understanding shortfall.

Learning progress and quiz scores are separate from the ledger. Completing a lesson does not automatically credit money.

## 8. Parent dashboard and reports

Show:
- Available balance.
- Savings balance.
- Lifetime total earned.
- Lifetime total transferred to savings.
- Lifetime total withdrawn from savings.
- Activity count by type.
- Recent activity submissions.
- Recent transactions.

Lifetime earned is derived from posted reward-credit transactions, not manually maintained counters.

## 9. Non-functional requirements

- Offline-first: all MVP functionality works without network access.
- No backend service, remote API, cloud authentication, analytics SDK, ads, or external payment provider.
- Tablet-friendly responsive layouts; support landscape and portrait.
- Large child touch targets (minimum 48dp).
- Accessibility: content descriptions, readable contrast, scalable text.
- Database writes are atomic.
- App process death during a financial operation must not leave partial ledger entries.
- Provide backup/export and restore, with validation before replacing live data.
- Do not log secrets or sensitive child data.

## 10. Explicitly out of scope

- Shopping catalog, product entry, cart, purchases, product fulfillment.
- Interest calculations.
- Real bank accounts, actual money, UPI, cards, payment gateway.
- Cloud backend, multi-device sync, online accounts.
- Parent approval workflow for routine rewards.
- Social features, chat, ads, external links.
- AI-generated educational content in MVP.
