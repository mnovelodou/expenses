package com.novelosoftware.expenses.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.novelosoftware.expenses.dto.AccountType;

/**
 * Internal representation of an account row in the database.
 * Not exposed directly to API consumers — use Account DTO instead.
 */
public record AccountEntity(
    /** Auto-generated primary key. */
    Long accountId,
    /** Display name of the account. */
    String name,
    /** Whether this is a debit or credit account. */
    AccountType accountType,
    /** ISO currency code (e.g. USD, EUR). */
    String currency,
    /** Balance at the time the account was created. */
    BigDecimal initialAmount,
    /** Current balance, updated on transactions. */
    BigDecimal currentAmount,
    /** Timestamp when the record was created. */
    OffsetDateTime createdAt,
    /** Timestamp when the record was last updated. */
    OffsetDateTime updatedAt,
    /** ID of the user who owns this account. */
    String createdBy
) {}
