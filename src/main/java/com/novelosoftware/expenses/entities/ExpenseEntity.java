package com.novelosoftware.expenses.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Internal representation of an expense row in the database.
 * Not exposed directly to API consumers — use Expense DTO instead.
 */
public record ExpenseEntity(
    /** Auto-generated primary key. */
    Long expenseId,
    /** Date the expense occurred. */
    LocalDate expenseDate,
    /** ID of the account this expense belongs to. */
    Long accountId,
    /** Amount spent. */
    BigDecimal amount,
    /** Amount of the original (pre-split) transaction this line was derived from. */
    BigDecimal transactionAmount,
    /** Description of what the expense was for. */
    String description,
    /** Top-level category (e.g. Food, Transport). */
    String category,
    /** Sub-category within the category (e.g. Restaurants, Uber). */
    String subcategory,
    /** ID of the user who created this expense. */
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
            .category(category)
            .subcategory(subcategory)
            .createdBy(createdBy);
    }

    public static final class Builder {
        private Long expenseId;
        private LocalDate expenseDate;
        private Long accountId;
        private BigDecimal amount;
        private BigDecimal transactionAmount;
        private String description;
        private String category;
        private String subcategory;
        private String createdBy;

        private Builder() {}

        public Builder expenseId(Long expenseId) { this.expenseId = expenseId; return this; }
        public Builder expenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; return this; }
        public Builder accountId(Long accountId) { this.accountId = accountId; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder transactionAmount(BigDecimal transactionAmount) { this.transactionAmount = transactionAmount; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder subcategory(String subcategory) { this.subcategory = subcategory; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }

        public ExpenseEntity build() {
            return new ExpenseEntity(expenseId, expenseDate, accountId, amount, transactionAmount, description, category, subcategory, createdBy);
        }
    }
}
