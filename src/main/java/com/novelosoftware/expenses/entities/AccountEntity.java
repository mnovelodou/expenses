package com.novelosoftware.expenses.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    String createdBy,
    /** Start of the current tracking period; expenses on or after this date count toward the gap. */
    LocalDate periodStart
) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .accountId(accountId)
            .name(name)
            .accountType(accountType)
            .currency(currency)
            .initialAmount(initialAmount)
            .currentAmount(currentAmount)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .createdBy(createdBy)
            .periodStart(periodStart);
    }

    public static final class Builder {
        private Long accountId;
        private String name;
        private AccountType accountType;
        private String currency;
        private BigDecimal initialAmount;
        private BigDecimal currentAmount;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private String createdBy;
        private LocalDate periodStart;

        private Builder() {}

        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder accountType(AccountType accountType) { this.accountType = accountType; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder initialAmount(BigDecimal initialAmount) { this.initialAmount = initialAmount; return this; }
        public Builder currentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder periodStart(LocalDate periodStart) { this.periodStart = periodStart; return this; }

        public AccountEntity build() {
            return new AccountEntity(accountId, name, accountType, currency, initialAmount, currentAmount, createdAt, updatedAt, createdBy, periodStart);
        }
    }
}
