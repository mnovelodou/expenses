-- Accounts are consider credit or debit card account where expenses can be tracked.
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

CREATE INDEX IF NOT EXISTS idx_accounts_created_by ON accounts(created_by);

-- Expenses table
CREATE TABLE IF NOT EXISTS expenses (
    expense_id SERIAL PRIMARY KEY, -- Auto-increemented expense ID
    expense_date DATE NOT NULL, -- Date where expense was created
    account_id INTEGER NOT NULL, -- Account ID where the expense was done.
    amount NUMERIC(15, 2) NOT NULL, -- Amount of the expense
    description TEXT NOT NULL, -- Text describing how the money was expensed
    category TEXT NOT NULL, -- Category of the expense
    subcategory TEXT NOT NULL, -- Subcategory of the expense
    created_by TEXT DEFAULT 'default_user_id', -- Who created the expense
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_date        ON expenses(created_by, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_account_date     ON expenses(account_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_category_date    ON expenses(created_by, category, expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_subcategory_date ON expenses(created_by, subcategory, expense_date);
