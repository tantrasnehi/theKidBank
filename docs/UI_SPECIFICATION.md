# UI Specification — My Little Bank

## 1. UX principles

- Child-first, playful, calm, encouraging.
- Parent mode is functional and compact.
- Avoid banking jargon unless explained simply.
- Use large touch targets (minimum 48dp; preferably 56dp for primary actions).
- Use cards, icons, illustrations, progress indicators, and short explanations.
- No shopping UI anywhere.
- Money School is educational only and does not issue bank credits.

## 2. Navigation

### Child bottom navigation
1. Home
2. My Bank
3. Earn
4. Savings
5. Learn

Use a tablet-friendly navigation rail or bottom navigation depending on window size. Parent entry is a protected icon/menu item, not a child navigation destination.

### Parent navigation
- Overview
- Reward Rules
- Activities
- Transactions
- Settings
- Backup & Restore

## 3. Screen specifications

### C01 — First launch / onboarding
Purpose: initialize the app and child profile.
Fields:
- Child display name (required)
- Avatar selection (optional)
- Currency display name
- Currency symbol
- Currency code
- Decimal precision (0–3)

Behavior:
- Seed default activity types and editable example reward rules.
- Default currency: INR / Indian Rupee / ₹ / 2 decimal places.
- Do not create a financial transaction during setup.
- Explain that money is virtual.

### C02 — Child Home Dashboard
Components:
- Greeting using child display name.
- Prominent AVAILABLE balance card.
- SAVINGS balance card.
- Quick actions: Earn Money, My Bank, My Savings, Money School.
- Recent activity/transactions (latest 3–5).
- Positive, non-competitive encouragement.
- Optional daily activity summary.

Behavior:
- Balances are observed from repository/ledger.
- Tapping available balance opens My Bank.
- Tapping savings opens My Savings.
- Empty state guides child to submit a first activity.

### C03 — Earn Money / Activity selection
Show five cards:
- Reading
- Maths
- Homework
- Chores
- Good Habits

Each card shows:
- Icon/illustration.
- Activity name.
- Reward unit and current rate.
- Optional daily limit remaining.

Disabled activities are hidden or shown as unavailable, based on product design; do not allow submission when disabled.

### C04 — Activity submission
Use activity-specific forms.

Reading:
- Book title (optional)
- Pages read (required positive integer)
- Display current rate and estimated reward.

Maths:
- Number of correct answers (required positive integer)
- Optional topic/description
- Display current rate and estimated reward.

Homework:
- Task description (required)
- Completed task count (default 1; positive integer)
- Display current rate and estimated reward.

Chores:
- Select configured chore or enter description.
- Completed count (default 1)
- Display current rate and estimated reward.

Good Habits:
- Select configured habit or enter description.
- Completion count (default 1)
- Display current rate and estimated reward.

All forms:
- Show reward calculation before submit.
- Confirm button wording: “Submit & Earn”.
- Confirmation dialog summarizes activity, quantity, rate, and total.
- On confirmation, credit automatically and immediately.
- Disable submit while request is in progress.
- Show clear validation errors.
- Never imply parent approval is pending.

### C05 — Reward success
Show:
- Celebration animation (brief, non-disruptive).
- Amount earned.
- Activity description.
- Previous and new available balance.
- “View My Bank” and “Done” actions.

### C06 — My Bank / Passbook
Show:
- Available balance.
- Savings balance.
- Total earned (derived).
- Transaction list.
Filters:
- All
- Earned
- Savings transfers
- Parent adjustments
Each row:
- Date/time
- Description
- Credit/debit label
- Amount with sign
- Account affected
Tapping row opens transaction details.

### C07 — Transaction details
Read-only:
- Transaction reference
- Date/time
- Type
- Description
- Amount
- Account entries
- Related activity submission if any
- Balance after transaction (derived for the relevant account)

Child cannot edit/delete/reverse.

### C08 — My Savings
Show:
- Savings account balance.
- Savings goals and progress.
- Create goal.
- Deposit to savings.
- Withdraw from savings.

Create goal:
- Name (required)
- Target amount (required positive amount)
- Optional icon/image.
Deposit:
- Amount, validate against available balance.
- Optional goal association.
Withdraw:
- Amount, validate against savings balance.
- Confirmation required.

No interest display or interest controls.

### C09 — Money School
Categories:
- Money basics
- Addition and subtraction
- Multiplication and division
- Needs versus wants
- Saving and budgeting
- Comparing prices / shortfall

Lesson page:
- Short text and illustration.
- Example.
- Interactive exercise or quiz.
- Feedback and explanation.
- Completion status.

Quiz scores are learning data only; no automatic bank credit.

### P01 — Parent Area Entry
- Open the parent area from the settings button.
- PIN protection is deferred from the first release.

### P02 — Parent Dashboard
Cards:
- Available balance.
- Savings balance.
- Total earned.
- Total saved/transferred.
- Activity count.
- Recent submissions.
- Recent transactions.

### P03 — Reward Rules
List activity types with:
- Enabled toggle.
- Reward amount.
- Unit label.
- Optional daily quantity cap.
- Optional daily reward cap.

Edit form:
- Validate non-negative/positive reward as appropriate.
- Save a new effective-dated rule version.
- Do not alter historical reward calculations or transactions.
- Changes apply only to future submissions.

### P04 — Activity Configuration
- Enable/disable activity types.
- Configure chores and habit options.
- Configure optional daily limits.
- Do not add approval status or approval queue.

### P05 — Transaction Management
- Search/filter transaction history.
- View details.
- Manual credit/debit with mandatory reason.
- Reverse eligible transaction with mandatory reason.
- Never edit or delete posted ledger entries.

### P06 — Currency and Settings
- Currency display name, symbol, code, precision.
- Lock currency identity/precision after first posted transaction.
- Backup/export.
- Restore.
- About/version.

### P07 — Backup & Restore
- Export encrypted backup through Android Storage Access Framework.
- Restore from a selected backup after validation.
- Open from the parent settings area; PIN protection is deferred.
- Confirm destructive replacement.
- Validate schema version, checksum, and ledger invariants before restore.
- Do not silently merge backups.

## 4. Visual design direction

- Friendly palette: sky blue, mint, warm yellow, lavender.
- Rounded cards and clear iconography.
- Friendly piggy-bank or coin mascot.
- Child UI: large typography and minimal text.
- Parent UI: denser but still touch-friendly.
- Responsive Compose layouts using WindowSizeClass/adaptive layout patterns.
- Avoid full-screen clutter and excessive animation.

## 5. UI state rules

- ViewModels expose immutable StateFlow UI state.
- Loading, empty, success, and error states must be represented.
- UI must not calculate authoritative rewards independently; show estimates from the same domain calculation logic used during submission.
- Database/repository result is the source of truth.
