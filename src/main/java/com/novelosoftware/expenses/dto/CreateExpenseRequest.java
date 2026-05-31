package com.novelosoftware.expenses.dto;

/**
 * Request for creating a new expense.
 */
public record CreateExpenseRequest(
    Expense value
) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().value(value);
    }

    public static final class Builder {
        private Expense value;

        private Builder() {}

        public Builder value(Expense value) { this.value = value; return this; }

        public CreateExpenseRequest build() {
            return new CreateExpenseRequest(value);
        }
    }
}
