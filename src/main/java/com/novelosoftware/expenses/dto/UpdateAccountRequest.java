package com.novelosoftware.expenses.dto;

import com.novelosoftware.expenses.enums.AccountType;

import java.math.BigDecimal;

/**
 * Request body for updating an existing account.
 */
public record UpdateAccountRequest(
    /** Updated display name of the account. */
    String name,
    /** Updated account type. */
    AccountType accountType,
    /** Updated ISO currency code. */
    String currency,
    /** Updated current balance. */
    BigDecimal currentAmount
) {}
