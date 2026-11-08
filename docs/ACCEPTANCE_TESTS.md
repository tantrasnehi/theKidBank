# Acceptance Tests — My Little Bank

## A. Onboarding and configuration

1. Fresh install opens onboarding.
2. Parent can create child profile.
3. Default currency is INR, symbol ₹, 2 decimals.
4. Parent can choose a custom currency before any transaction.
5. App seeds five activities and editable example reward rules.
6. App restart retains profile, currency, and rules.

## B. Reward engine

1. Reading 1 page at 5.00 units/page credits 5.00.
2. Reading 20 pages at 5.00 units/page credits 100.00.
3. Maths correct answers use configured rate.
4. Homework, chores, and habits use their configured rules.
5. Quantity zero and negative quantities are rejected.
6. Disabled activity cannot be submitted.
7. Daily quantity cap is enforced.
8. Daily reward cap is enforced.
9. Preview and posted amount use identical calculation rules.
10. Successful submission creates exactly one submission and one balanced ledger transaction.
11. Retrying same idempotency key and same payload does not duplicate credit.
12. Reusing same key with a different payload is rejected.
13. Two separate genuine sessions can be submitted separately if within configured limits.
14. Reward rule change applies only to submissions after its effective time.
15. Historical submission retains applied rate and total reward.

## C. Banking and ledger

1. Every posted transaction's ledger entries sum to zero.
2. Available balance equals sum of AVAILABLE entries.
3. Savings balance equals sum of SAVINGS entries.
4. No child account can become negative.
5. Parent credit/debit requires parent authentication and reason.
6. Posted entries cannot be edited or deleted.
7. Reversal creates a new balanced transaction.
8. Duplicate reversal is rejected.
9. App process death during transaction leaves either full commit or no commit.
10. Large amounts do not overflow silently.

## D. Savings

1. Child can create a goal with positive target amount.
2. Zero/negative target is rejected.
3. Deposit requires sufficient available balance.
4. Deposit decreases AVAILABLE and increases SAVINGS by same amount.
5. Withdrawal requires sufficient SAVINGS balance.
6. Withdrawal increases AVAILABLE and decreases SAVINGS by same amount.
7. Goal contributions link to their transfer transaction.
8. Savings progress is calculated consistently.
9. No interest is accrued or displayed.

## E. Currency

1. INR formatting uses ₹ and configured decimal precision.
2. Custom symbol and code display correctly.
3. Zero-decimal currency formats without fractional digits.
4. Amount parsing rejects excess decimal precision.
5. Amount conversion uses exact arithmetic.
6. Currency identity/precision change is blocked after first posted transaction.
7. Historical transaction uses its currency snapshot.
8. No hardcoded currency symbol appears in feature UI.

## F. Parent settings

1. Parent settings are available from the settings button; PIN protection is deferred.
2. Child activity screens cannot change reward rules.

## G. UI and accessibility

1. All primary controls have at least 48dp touch targets.
2. Tablet portrait and landscape layouts work.
3. Loading, empty, success, and error states exist.
4. Text remains readable with larger system font.
5. Screen readers receive meaningful labels.
6. Child errors use friendly, understandable language.
7. Double-tapping submit does not create duplicate credit.

## H. Offline and backup

1. All MVP flows work with no network.
2. No network permission is required unless justified by a dependency.
3. Backup export requires parent authentication.
4. Restore validates checksum/schema/ledger invariants before replacing live data.
5. Invalid backup does not alter current data.
6. Restore preserves balances, history, currency, rules, and learning progress.
7. Database migration tests cover supported schema versions.

## I. Explicit scope guard

Verify that the app contains none of the following:
- Shopping screen
- Product catalog
- Cart
- Purchase flow
- Purchase debit transaction
- Interest calculation
- Real-money integration
- Parent approval queue for routine rewards
- Cloud backend requirement
