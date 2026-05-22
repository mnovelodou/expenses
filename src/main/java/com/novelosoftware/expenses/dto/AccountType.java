package com.novelosoftware.expenses.dto;

/**
 * Represents the type of a financial account.
 * Matches the CHECK constraint defined in the accounts table.
 */
public enum AccountType {
    /** A debit account where funds are withdrawn directly. */
    DEBIT,
    /** A credit account where funds are borrowed. */
    CREDIT
}
