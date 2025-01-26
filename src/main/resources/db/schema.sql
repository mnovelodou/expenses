-- Create the table if it doesn't already exist
 CREATE TABLE IF NOT EXISTS accounts (
    account_id SERIAL PRIMARY KEY,   -- Auto-incremented account ID
    name TEXT NOT NULL,              -- Account name
    account_type TEXT CHECK (account_type IN ('DEBIT', 'CREDIT')) NOT NULL,  -- Account type (enum-like)
    currency TEXT NOT NULL,          -- Currency symbol (e.g., USD, EUR)
    initial_amount NUMERIC(15, 2) NOT NULL,  -- Initial amount in the account
    current_amount NUMERIC(15, 2) NOT NULL,  -- Current balance
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,  -- Timestamp when the record was created
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,  -- Timestamp when the record was last updated
    created_by TEXT DEFAULT 'default_user_id' -- Default user_id, this can be replaced later
);