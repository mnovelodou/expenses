package com.novelosoftware.expenses.dto;

import com.novelosoftware.expenses.enums.AccountType;

import java.math.BigDecimal;

/**
 * DTO representing an account returned in API responses.
 * Exposes all account fields except audit timestamps.
 */
public record Account(
    /** Unique identifier of the account. */
    Long id,
    /** Display name of the account. */
    String name,
    /** Whether this is a debit or credit account. */
    AccountType accountType,
    /** ISO currency code (e.g. USD, EUR). */
    String currency,
    /** Balance at the time the account was created. */
    BigDecimal initialAmount,
    /** Current balance. */
    BigDecimal currentAmount,
    /** ID of the user who owns this account. */
    String createdBy
) {}
