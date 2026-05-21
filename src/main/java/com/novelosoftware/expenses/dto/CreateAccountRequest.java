package com.novelosoftware.expenses.dto;

import com.novelosoftware.expenses.enums.AccountType;

import java.math.BigDecimal;

/**
 * Request body for creating a new account.
 */
public record CreateAccountRequest(
    /** Display name of the account. */
    String name,
    /** Whether this is a debit or credit account. */
    AccountType accountType,
    /** ISO currency code (e.g. USD, EUR). */
    String currency,
    /** Opening balance for the account. */
    BigDecimal initialAmount
) {}
