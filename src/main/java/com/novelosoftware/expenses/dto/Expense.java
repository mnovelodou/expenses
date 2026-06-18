package com.novelosoftware.expenses.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Expense exposes a single expense line
 */
public record Expense(
    /** ID of the expense */
    Long expenseId,
    /** Date when the expense happened */
    LocalDate expenseDate,
    /** refernece to the account when expense was originated */
    Long accountId,
    /** amount of the expense */
    BigDecimal amount,
    /** amount of the original (pre-split) transaction this line was derived from */
    BigDecimal transactionAmount,
    /** description of the expense */
    String description,
    /** sub-category o the expense, category can be infered from here */
    SubCategory subCategory,
    /** onwer of expense */
    String createdBy
) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .expenseId(expenseId)
            .expenseDate(expenseDate)
            .accountId(accountId)
            .amount(amount)
            .transactionAmount(transactionAmount)
            .description(description)
            .subCategory(subCategory)
            .createdBy(createdBy);
    }

    public static final class Builder {
        private Long expenseId;
        private LocalDate expenseDate;
        private Long accountId;
        private BigDecimal amount;
        private BigDecimal transactionAmount;
        private String description;
        private SubCategory subCategory;
        private String createdBy;

        private Builder() {}

        public Builder expenseId(Long expenseId) { this.expenseId = expenseId; return this; }
        public Builder expenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder transactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder subCategory(SubCategory subCategory) { this.subCategory = subCategory; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }

        public Expense build() {
            return new Expense(expenseId, expenseDate, accountId, amount, transactionAmount, description, subCategory, createdBy);
        }
    }
}
