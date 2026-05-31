package com.novelosoftware.expenses.dto;

import java.math.BigDecimal;

/**
 * DTO representing an account returned in API responses.
 * Exposes all account fields except audit timestamps.
 */
public record Account(
    /** Unique identifier of the account. */
    Long accountId,
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
            .createdBy(createdBy);
    }

    public static final class Builder {
        private Long accountId;
        private String name;
        private AccountType accountType;
        private String currency;
        private BigDecimal initialAmount;
        private BigDecimal currentAmount;
        private String createdBy;

        private Builder() {}

        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder accountType(AccountType accountType) { this.accountType = accountType; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder initialAmount(BigDecimal initialAmount) { this.initialAmount = initialAmount; return this; }
        public Builder currentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }

        public Account build() {
            return new Account(accountId, name, accountType, currency, initialAmount, currentAmount, createdBy);
        }
    }
}
