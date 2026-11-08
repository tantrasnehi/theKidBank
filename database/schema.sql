-- Reference SQLite schema for Room implementation.
-- Use UUID strings (TEXT) and UTC epoch milliseconds (INTEGER).
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS child_profile (
    id TEXT PRIMARY KEY NOT NULL,
    display_name TEXT NOT NULL,
    avatar_key TEXT,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS currency_config (
    id TEXT PRIMARY KEY NOT NULL,
    code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    symbol TEXT NOT NULL,
    decimal_places INTEGER NOT NULL CHECK(decimal_places BETWEEN 0 AND 3),
    is_locked INTEGER NOT NULL DEFAULT 0 CHECK(is_locked IN (0,1)),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS bank_account (
    id TEXT PRIMARY KEY NOT NULL,
    account_type TEXT NOT NULL CHECK(account_type IN ('AVAILABLE','SAVINGS','REWARD_FUND','MANUAL_ADJUSTMENT')),
    owner_profile_id TEXT,
    currency_config_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 1 CHECK(is_active IN (0,1)),
    FOREIGN KEY(owner_profile_id) REFERENCES child_profile(id),
    FOREIGN KEY(currency_config_id) REFERENCES currency_config(id)
);

CREATE TABLE IF NOT EXISTS ledger_transaction (
    id TEXT PRIMARY KEY NOT NULL,
    idempotency_key TEXT NOT NULL UNIQUE,
    transaction_type TEXT NOT NULL CHECK(transaction_type IN (
        'REWARD_CREDIT','SAVINGS_TRANSFER','SAVINGS_WITHDRAWAL',
        'PARENT_CREDIT','PARENT_DEBIT','REVERSAL'
    )),
    description TEXT NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('POSTED','REVERSED')),
    reference_type TEXT,
    reference_id TEXT,
    currency_code_snapshot TEXT NOT NULL,
    decimal_places_snapshot INTEGER NOT NULL CHECK(decimal_places_snapshot BETWEEN 0 AND 3),
    created_at INTEGER NOT NULL,
    reverses_transaction_id TEXT UNIQUE,
    FOREIGN KEY(reverses_transaction_id) REFERENCES ledger_transaction(id)
);

CREATE TABLE IF NOT EXISTS ledger_entry (
    id TEXT PRIMARY KEY NOT NULL,
    transaction_id TEXT NOT NULL,
    account_id TEXT NOT NULL,
    amount_minor INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(transaction_id) REFERENCES ledger_transaction(id),
    FOREIGN KEY(account_id) REFERENCES bank_account(id)
);

CREATE TABLE IF NOT EXISTS activity_type (
    id TEXT PRIMARY KEY NOT NULL,
    activity_key TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    unit_label TEXT NOT NULL,
    description TEXT,
    is_enabled INTEGER NOT NULL DEFAULT 1 CHECK(is_enabled IN (0,1)),
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS reward_rule (
    id TEXT PRIMARY KEY NOT NULL,
    activity_type_id TEXT NOT NULL,
    reward_per_unit_minor INTEGER NOT NULL CHECK(reward_per_unit_minor >= 0),
    daily_quantity_limit INTEGER CHECK(daily_quantity_limit IS NULL OR daily_quantity_limit > 0),
    daily_reward_limit_minor INTEGER CHECK(daily_reward_limit_minor IS NULL OR daily_reward_limit_minor >= 0),
    effective_from INTEGER NOT NULL,
    effective_to INTEGER,
    created_at INTEGER NOT NULL,
    FOREIGN KEY(activity_type_id) REFERENCES activity_type(id),
    CHECK(effective_to IS NULL OR effective_to > effective_from)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_reward_rule_current
ON reward_rule(activity_type_id) WHERE effective_to IS NULL;

CREATE TABLE IF NOT EXISTS activity_submission (
    id TEXT PRIMARY KEY NOT NULL,
    idempotency_key TEXT NOT NULL UNIQUE,
    activity_type_id TEXT NOT NULL,
    applied_reward_rule_id TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity > 0),
    description TEXT,
    reward_per_unit_minor_snapshot INTEGER NOT NULL CHECK(reward_per_unit_minor_snapshot >= 0),
    total_reward_minor INTEGER NOT NULL CHECK(total_reward_minor >= 0),
    currency_code_snapshot TEXT NOT NULL,
    decimal_places_snapshot INTEGER NOT NULL CHECK(decimal_places_snapshot BETWEEN 0 AND 3),
    ledger_transaction_id TEXT NOT NULL UNIQUE,
    submitted_at INTEGER NOT NULL,
    FOREIGN KEY(activity_type_id) REFERENCES activity_type(id),
    FOREIGN KEY(applied_reward_rule_id) REFERENCES reward_rule(id),
    FOREIGN KEY(ledger_transaction_id) REFERENCES ledger_transaction(id)
);

CREATE TABLE IF NOT EXISTS savings_goal (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    target_amount_minor INTEGER NOT NULL CHECK(target_amount_minor > 0),
    icon_key TEXT,
    status TEXT NOT NULL CHECK(status IN ('ACTIVE','COMPLETED','ARCHIVED')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS savings_goal_contribution (
    id TEXT PRIMARY KEY NOT NULL,
    goal_id TEXT NOT NULL,
    ledger_transaction_id TEXT NOT NULL,
    amount_minor INTEGER NOT NULL CHECK(amount_minor > 0),
    direction TEXT NOT NULL CHECK(direction IN ('DEPOSIT','WITHDRAWAL')),
    created_at INTEGER NOT NULL,
    FOREIGN KEY(goal_id) REFERENCES savings_goal(id),
    FOREIGN KEY(ledger_transaction_id) REFERENCES ledger_transaction(id)
);

CREATE TABLE IF NOT EXISTS learning_module (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    sequence INTEGER NOT NULL,
    content_json TEXT NOT NULL,
    is_enabled INTEGER NOT NULL DEFAULT 1 CHECK(is_enabled IN (0,1))
);

CREATE TABLE IF NOT EXISTS learning_progress (
    id TEXT PRIMARY KEY NOT NULL,
    module_id TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK(status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    score INTEGER,
    completed_at INTEGER,
    FOREIGN KEY(module_id) REFERENCES learning_module(id)
);

CREATE TABLE IF NOT EXISTS app_setting (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_ledger_entry_transaction ON ledger_entry(transaction_id);
CREATE INDEX IF NOT EXISTS ix_ledger_entry_account_date ON ledger_entry(account_id, created_at);
CREATE INDEX IF NOT EXISTS ix_activity_submission_type_date ON activity_submission(activity_type_id, submitted_at);
CREATE INDEX IF NOT EXISTS ix_reward_rule_activity_effective ON reward_rule(activity_type_id, effective_from, effective_to);
CREATE INDEX IF NOT EXISTS ix_savings_contribution_goal_date ON savings_goal_contribution(goal_id, created_at);
